package com.arctic.worldtime;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import rikka.shizuku.Shizuku;

public final class MainActivity extends Activity {
  private static final int BG=0xff0c1321, CYAN=0xff4ddee4;
  private static final String[] ZONES=TimeZone.getAvailableIDs();
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler handler=new Handler(Looper.getMainLooper());
  private TextView clock,city,date,system,status;
  private String selectedZone="Asia/Vladivostok";
  private boolean pendingClockPermission=false;
  private static final int REQUEST=472;
  private final Shizuku.OnRequestPermissionResultListener permission=(request,result)->{
    if(request==REQUEST)runOnUiThread(()->{
      if(result==PackageManager.PERMISSION_GRANTED){
        if(pendingClockPermission){pendingClockPermission=false;openManualTime();}
        else applyZone();
      } else showStatus("Доступ Shizuku не предоставлен.");
    });
  };
  private final Runnable ticker=new Runnable(){public void run(){render();handler.postDelayed(this,1000);}};

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    Shizuku.addRequestPermissionResultListener(permission);
    selectedZone=getPreferences(MODE_PRIVATE).getString("zone","Asia/Vladivostok");
    if(!Arrays.asList(ZONES).contains(selectedZone))selectedZone="Asia/Vladivostok";
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(36),dp(24),dp(36),dp(18));root.setBackgroundColor(BG);
    setContentView(root);
    root.addView(text("МИРОВОЕ ВРЕМЯ  •  WORLD TIME",23,CYAN,true));
    city=text("",27,Color.WHITE,true);root.addView(city);
    clock=text("",76,Color.WHITE,false);root.addView(clock);
    date=text("",19,0xffbbcbdc,false);root.addView(date);
    system=text("",16,0xffbbcbdc,false);root.addView(system);
    Button zones=button("ЧАСОВЫЕ ПОЯСА  •  ВЫБРАТЬ",()->openZones());
    Button manual=button("НАСТРОИТЬ ДАТУ И ВРЕМЯ ВРУЧНУЮ",()->openManualTime());
    root.addView(zones);root.addView(manual);
    status=text("Выбор пояса применяется к системе через Shizuku. Для ручной установки времени автоматическая синхронизация будет отключена.",15,0xffadc4d6,false);
    root.addView(status);
    zones.requestFocus();render();
  }
  private void openZones(){
    LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(18),dp(12),dp(18),dp(12));layout.setBackgroundColor(BG);
    EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(0xff9eb9c5);
    search.setHint("Поиск: город или UTC");layout.addView(search);
    ListView list=new ListView(this);layout.addView(list,new LinearLayout.LayoutParams(-1,dp(340)));
    ArrayList<String> shown=new ArrayList<>(Arrays.asList(ZONES));
    ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1,shown);
    list.setAdapter(adapter);list.setFastScrollEnabled(true);
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Часовые пояса — "+ZONES.length).setView(layout).setNegativeButton("Закрыть",null).create();
    search.addTextChangedListener(new TextWatcher(){
      public void beforeTextChanged(CharSequence s,int st,int count,int after){}
      public void onTextChanged(CharSequence s,int st,int before,int count){
        String q=s.toString().toLowerCase(Locale.ROOT).trim();shown.clear();
        for(String id:ZONES){
          String match=id.toLowerCase(Locale.ROOT);
          TimeZone zone=TimeZone.getTimeZone(id);
          int offset=zone.getOffset(System.currentTimeMillis())/60000;
          String off=String.format(Locale.ROOT,"UTC%+03d:%02d",offset/60,Math.abs(offset%60));
          if(match.contains(q)||off.toLowerCase(Locale.ROOT).contains(q))shown.add(id);
        }
        adapter.notifyDataSetChanged();
      }
      public void afterTextChanged(Editable s){}
    });
    list.setOnItemClickListener((parent,v,pos,id)->{
      selectedZone=shown.get(pos);
      getPreferences(MODE_PRIVATE).edit().putString("zone",selectedZone).apply();
      dialog.dismiss();render();applyZone();
    });
    dialog.show();list.requestFocus();
  }
  private void openManualTime(){
    if(!ensureShizuku(true))return;
    // All manual input is in the currently selected time zone. Arrow/OK navigation is TV-friendly.
    Calendar c=Calendar.getInstance(TimeZone.getTimeZone(selectedZone));
    int[] values={c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)};
    String[] names={"Год","Месяц","День","Час","Минута"};
    int[] low={2000,1,1,0,0}, high={2099,12,31,23,59};
    LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(18),dp(6),dp(18),dp(6));
    TextView[] numbers=new TextView[5];
    for(int i=0;i<5;i++){
      final int index=i;
      LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
      TextView title=text(names[i],18,Color.WHITE,true);line.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
      line.addView(button("−",()->{values[index]=values[index]<=low[index]?high[index]:values[index]-1;numbers[index].setText(String.format(Locale.ROOT,"%02d",values[index]));}));
      numbers[i]=text(String.format(Locale.ROOT,"%02d",values[i]),21,Color.WHITE,true);
      numbers[i].setGravity(Gravity.CENTER);line.addView(numbers[i],new LinearLayout.LayoutParams(dp(90),dp(48)));
      line.addView(button("+",()->{values[index]=values[index]>=high[index]?low[index]:values[index]+1;numbers[index].setText(String.format(Locale.ROOT,"%02d",values[index]));}));
      layout.addView(line);
    }
    AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Установить системные дату и время")
      .setMessage("Часовой пояс: "+selectedZone+"\nАвтоматическая синхронизация времени будет отключена.")
      .setView(layout).setNegativeButton("Отмена",null)
      .setPositiveButton("ПРИМЕНИТЬ",(d,w)->setManualTime(values)).create();
    dialog.show();
  }
  private void setManualTime(int[] values){
    Calendar c=Calendar.getInstance(TimeZone.getTimeZone(selectedZone));
    c.setLenient(false);c.clear();
    try {
      c.set(values[0],values[1]-1,values[2],values[3],values[4],0);
      long timestamp=c.getTimeInMillis();
      showStatus("Применяю дату и время...");
      worker.execute(()->{
        String error=execute("settings","put","global","auto_time","0");
        if(error==null)error=execute("cmd","alarm","set-time",Long.toString(timestamp));
        final String message=error==null?"Дата и время установлены. Автоматическая синхронизация отключена.":error;
        runOnUiThread(()->{showStatus(message);render();});
      });
    }catch(IllegalArgumentException e){showStatus("Неверная дата. Проверь день и месяц.");}
  }
  private boolean ensureShizuku(boolean manual){
    try {
      if(!Shizuku.pingBinder()){showStatus("Shizuku остановлена. Запусти Shizuku через atvTools.");return false;}
      if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED){
        pendingClockPermission=manual;showStatus("Разреши доступ к Shizuku.");
        Shizuku.requestPermission(REQUEST);return false;
      }return true;
    }catch(Exception e){showStatus("Shizuku недоступна: "+e.getMessage());return false;}
  }
  private void applyZone(){
    if(!ensureShizuku(false))return;
    final String requested=selectedZone;
    showStatus("Устанавливаю системный пояс: "+requested);
    worker.execute(()->{
      String error=execute("cmd","alarm","set-timezone",requested);
      final String msg=error==null?"Системный пояс установлен: "+requested:error;
      runOnUiThread(()->{showStatus(msg);render();});
    });
  }
  private String execute(String... argv){
    try{
      rikka.shizuku.ShizukuRemoteProcess p=Shizuku.newProcess(argv,null,null);
      // Read stderr before waiting to avoid deadlocks on verbose failures.
      BufferedReader err=new BufferedReader(new InputStreamReader(p.getErrorStream()));
      StringBuilder b=new StringBuilder();String line;
      while((line=err.readLine())!=null)b.append(line).append(' ');
      int exit=p.waitFor();p.destroy();
      if(exit!=0)return "Команда не выполнена ("+exit+"): "+b;
      return null;
    }catch(Exception e){return "Ошибка Shizuku: "+e.getMessage();}
  }
  private String fmt(String zone,String pattern){
    SimpleDateFormat f=new SimpleDateFormat(pattern,new Locale("ru","RU"));
    f.setTimeZone(TimeZone.getTimeZone(zone));return f.format(new Date());
  }
  private void render(){
    if(clock==null)return;
    city.setText(selectedZone);
    clock.setText(fmt(selectedZone,"HH:mm:ss"));
    date.setText(fmt(selectedZone,"EEEE, d MMMM yyyy"));
    system.setText("Часовой пояс телевизора: "+TimeZone.getDefault().getID());
  }
  private void showStatus(String value){if(status!=null)status.setText(value);}
  private TextView text(String value,int sp,int color,boolean bold){
    TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);
    t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;
  }
  private Button button(String value,Runnable onClick){
    Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(Color.WHITE);
    b.setOnClickListener(v->onClick.run());b.setFocusable(true);return b;
  }
  private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
  @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
  @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
  @Override protected void onDestroy(){Shizuku.removeRequestPermissionResultListener(permission);handler.removeCallbacks(ticker);worker.shutdownNow();super.onDestroy();}
}
