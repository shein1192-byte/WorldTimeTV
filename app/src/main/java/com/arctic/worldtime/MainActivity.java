package com.arctic.worldtime;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
  private static final int BG=0xff0c1321,CYAN=0xff4ddee4;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler handler=new Handler(Looper.getMainLooper());
  private TextView clock,date,utc,status;
  private final Runnable ticker=new Runnable(){
    @Override public void run(){render();handler.postDelayed(this,1000);}
  };
  @Override public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(36),dp(24),dp(36),dp(18));
    root.setBackgroundColor(BG);setContentView(root);
    root.addView(text("МИРОВОЕ ВРЕМЯ  •  WORLD TIME",23,CYAN,true));
    clock=text("",76,Color.WHITE,false);root.addView(clock);
    date=text("",20,0xffbbcbdc,false);root.addView(date);
    utc=text("",19,CYAN,true);root.addView(utc);
    Button manual=button("УСТАНОВИТЬ ДАТУ И ВРЕМЯ ВРУЧНУЮ",this::openManualTime);
    root.addView(manual);
    Button test=button("ПРОВЕРИТЬ ADB 5555",this::testAdb);
    root.addView(test);
    status=text("Shizuku не требуется. Включи ADB по сети (порт 5555). При первом подключении подтверди RSA-ключ.",15,0xffadc4d6,false);
    root.addView(status);
    manual.requestFocus();render();
  }
  private String utcOffset(){
    TimeZone tz=TimeZone.getDefault();
    int minutes=tz.getOffset(System.currentTimeMillis())/60000;
    return String.format(Locale.ROOT,"UTC%s%02d:%02d",minutes<0?"-":"+",Math.abs(minutes)/60,Math.abs(minutes)%60);
  }
  private String format(String pattern){
    SimpleDateFormat f=new SimpleDateFormat(pattern,new Locale("ru","RU"));
    return f.format(new Date());
  }
  private void render(){
    if(clock==null)return;
    clock.setText(format("HH:mm:ss"));
    date.setText(format("EEEE, d MMMM yyyy"));
    utc.setText("Разница с UTC: "+utcOffset()+"  •  Системный пояс: "+TimeZone.getDefault().getID());
  }
  private void testAdb(){
    showStatus("Подключаюсь к ADB этого телевизора: 127.0.0.1:5555...");
    worker.execute(()->{
      String result;
      try{result=LocalAdbStarter.run(getApplicationContext(),"id");}
      catch(Exception e){result="ADB недоступен: "+e.getClass().getSimpleName()+": "+e.getMessage();}
      final String message="OK".equals(result)?"ADB подключён. Можно устанавливать время.":result;
      runOnUiThread(()->showStatus(message));
    });
  }
  private void openManualTime(){
    // Edits are in the device's current timezone; no unrequested timezone changes.
    Calendar c=Calendar.getInstance();
    int[] values={c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)};
    String[] names={"Год","Месяц","День","Час","Минута"};
    int[] low={2000,1,1,0,0},high={2099,12,31,23,59};
    LinearLayout layout=new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(18),dp(6),dp(18),dp(6));
    TextView label=text("Текущая разница с UTC: "+utcOffset(),18,CYAN,true);
    layout.addView(label);
    TextView[] fields=new TextView[5];
    for(int i=0;i<5;i++){
      final int k=i;
      LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
      line.addView(text(names[i],18,Color.WHITE,true),new LinearLayout.LayoutParams(0,dp(48),1));
      line.addView(button("−",()->{values[k]=values[k]<=low[k]?high[k]:values[k]-1;fields[k].setText(String.format(Locale.ROOT,"%02d",values[k]));}));
      fields[i]=text(String.format(Locale.ROOT,"%02d",values[i]),21,Color.WHITE,true);
      fields[i].setGravity(Gravity.CENTER);
      line.addView(fields[i],new LinearLayout.LayoutParams(dp(90),dp(48)));
      line.addView(button("+",()->{values[k]=values[k]>=high[k]?low[k]:values[k]+1;fields[k].setText(String.format(Locale.ROOT,"%02d",values[k]));}));
      layout.addView(line);
    }
    new AlertDialog.Builder(this).setTitle("Ручная установка системного времени")
      .setMessage("Время вводится в часовом поясе телевизора. Изменяется системное время, а не часовой пояс. Автоматическая синхронизация будет отключена.")
      .setView(layout).setNegativeButton("Отмена",null)
      .setPositiveButton("ПРИМЕНИТЬ",(dialog,which)->setManualTime(values)).show();
  }
  private void setManualTime(int[] values){
    Calendar c=Calendar.getInstance();c.setLenient(false);c.clear();
    try{
      c.set(values[0],values[1]-1,values[2],values[3],values[4],0);
      long millis=c.getTimeInMillis();
      showStatus("Устанавливаю время через встроенный ADB...");
      worker.execute(()->{
        String result;
        try{
          result=LocalAdbStarter.run(getApplicationContext(),
             "settings put global auto_time 0 && cmd alarm set-time "+millis);
        }catch(Exception e){result="Ошибка ADB: "+e.getClass().getSimpleName()+": "+e.getMessage();}
        final String message="OK".equals(result)?"Команда установки отправлена. Автоматическое время отключено.":result;
        runOnUiThread(()->{showStatus(message);render();});
      });
    }catch(IllegalArgumentException e){showStatus("Неверная дата. Проверь день и месяц.");}
  }
  private void showStatus(String message){if(status!=null)status.setText(message);}
  private TextView text(String value,int sp,int color,boolean bold){
    TextView t=new TextView(this);t.setText(value);t.setTextSize(sp);t.setTextColor(color);
    t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;
  }
  private Button button(String title,Runnable click){
    Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setTextColor(Color.WHITE);
    b.setFocusable(true);b.setOnClickListener(v->click.run());return b;
  }
  private int dp(int px){return(int)(px*getResources().getDisplayMetrics().density+.5f);}
  @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
  @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
  @Override protected void onDestroy(){handler.removeCallbacks(ticker);worker.shutdownNow();super.onDestroy();}
}
