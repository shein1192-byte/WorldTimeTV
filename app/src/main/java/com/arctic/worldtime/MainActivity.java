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
  private static final int BG=0xff0c1321, CYAN=0xff4ddee4;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler handler=new Handler(Looper.getMainLooper());
  private TextView clock,date,offset,status;
  private final Runnable ticker=new Runnable(){public void run(){render();handler.postDelayed(this,1000);}};
  @Override public void onCreate(Bundle saved){
    super.onCreate(saved);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(36),dp(24),dp(36),dp(18));
    root.setBackgroundColor(BG);setContentView(root);
    root.addView(text("МИРОВОЕ ВРЕМЯ  •  WORLD TIME",23,CYAN,true));
    clock=text("",76,Color.WHITE,false);root.addView(clock);
    date=text("",19,0xffbbcbdc,false);root.addView(date);
    offset=text("",21,CYAN,true);root.addView(offset);
    Button manual=button("НАСТРОИТЬ ДАТУ И ВРЕМЯ ВРУЧНУЮ",()->openManualTime());
    root.addView(manual);
    status=text("Для изменения времени включи сетевой ADB (порт 5555). Shizuku не требуется. При первом соединении подтверди ключ ADB.",16,0xffadc4d6,false);
    root.addView(status);manual.requestFocus();render();
  }
  private void openManualTime(){
    final TimeZone zone=TimeZone.getDefault();
    Calendar c=Calendar.getInstance(zone);
    int[] values={c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),
                  c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)};
    String[] names={"Год","Месяц","День","Час","Минута"};
    int[] low={2000,1,1,0,0}, high={2099,12,31,23,59};
    LinearLayout layout=new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(18),dp(6),dp(18),dp(6));
    TextView[] numbers=new TextView[5];
    for(int i=0;i<5;i++){
      final int index=i;
      LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
      TextView title=text(names[i],18,Color.WHITE,true);
      line.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
      line.addView(button("−",()->{values[index]=values[index]<=low[index]?high[index]:values[index]-1;
         numbers[index].setText(String.format(Locale.ROOT,"%02d",values[index]));}));
      numbers[i]=text(String.format(Locale.ROOT,"%02d",values[i]),21,Color.WHITE,true);
      numbers[i].setGravity(Gravity.CENTER);
      line.addView(numbers[i],new LinearLayout.LayoutParams(dp(90),dp(48)));
      line.addView(button("+",()->{values[index]=values[index]>=high[index]?low[index]:values[index]+1;
         numbers[index].setText(String.format(Locale.ROOT,"%02d",values[index]));}));
      layout.addView(line);
    }
    new AlertDialog.Builder(this)
      .setTitle("Ручная установка системного времени")
      .setMessage("Текущий системный пояс: "+zone.getID()+" ("+utcLabel(zone,System.currentTimeMillis())+")"+
          "\nВремя вводится в часовом поясе телевизора. Автоматическая синхронизация будет отключена.")
      .setView(layout).setNegativeButton("ОТМЕНА",null)
      .setPositiveButton("ПРИМЕНИТЬ",(dialog,which)->setManualTime(values,zone))
      .show();
  }
  private void setManualTime(int[] values,TimeZone zone){
    Calendar c=Calendar.getInstance(zone);c.setLenient(false);c.clear();
    try{
      c.set(values[0],values[1]-1,values[2],values[3],values[4],0);
      final long timestamp=c.getTimeInMillis();
      showStatus("Подключаюсь к локальному ADB:5555 и устанавливаю время...");
      worker.execute(()->{
        String message;
        try{
          // Execute and verify over one authenticated ADB session.
          String check=LocalAdbStarter.execute(getApplicationContext(),
            "settings put global auto_time 0 && cmd alarm set-time "+timestamp+" && date +%s");
          long seconds=Long.parseLong(check.trim().split("\\s+")[0]);
          message=Math.abs(seconds-timestamp/1000L)<=15
            ?"Время установлено и проверено. Синхронизация по сети отключена."
            :"Команда выполнена, но проверка времени не совпала. Проверь часы телевизора.";
        }catch(Exception e){
          message="Не удалось изменить время: "+e.getMessage()+
            ". Проверь сетевую ADB-отладку и подтверждение RSA-ключа.";
        }
        final String result=message;
        runOnUiThread(()->{showStatus(result);render();});
      });
    }catch(IllegalArgumentException e){showStatus("Неверная дата. Проверь день и месяц.");}
  }
  private String utcLabel(TimeZone zone,long millis){
    int minutes=zone.getOffset(millis)/60000;
    int abs=Math.abs(minutes);
    return String.format(Locale.ROOT,"UTC%s%02d:%02d",minutes>=0?"+":"−",abs/60,abs%60);
  }
  private void render(){
    if(clock==null)return;
    TimeZone zone=TimeZone.getDefault();
    SimpleDateFormat tf=new SimpleDateFormat("HH:mm:ss",Locale.ROOT);tf.setTimeZone(zone);
    SimpleDateFormat df=new SimpleDateFormat("EEEE, d MMMM yyyy",new Locale("ru","RU"));df.setTimeZone(zone);
    Date now=new Date();
    clock.setText(tf.format(now));date.setText(df.format(now));
    offset.setText("Разница с UTC: "+utcLabel(zone,now.getTime())+"  •  "+zone.getID());
  }
  private void showStatus(String value){if(status!=null)status.setText(value);}
  private TextView text(String value,int size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
    t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;
  }
  private Button button(String value,Runnable callback){
    Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextColor(Color.WHITE);
    b.setFocusable(true);b.setOnClickListener(v->callback.run());return b;
  }
  private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density+.5f);}
  @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
  @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
  @Override protected void onDestroy(){handler.removeCallbacks(ticker);worker.shutdownNow();super.onDestroy();}
}
