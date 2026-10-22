package com.arctic.worldtime;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.content.pm.PackageManager;
import rikka.shizuku.Shizuku;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class MainActivity extends Activity {
  private static final String[] CITIES={"Москва","Екатеринбург","Новосибирск","Иркутск","Якутск","Владивосток","Сахалин","Камчатка","Пекин","Токио","Сингапур","Дубай","Лондон","Нью-Йорк"};
  private static final String[] ZONES={"Europe/Moscow","Asia/Yekaterinburg","Asia/Novosibirsk","Asia/Irkutsk","Asia/Yakutsk","Asia/Vladivostok","Asia/Sakhalin","Asia/Kamchatka","Asia/Shanghai","Asia/Tokyo","Asia/Singapore","Asia/Dubai","Europe/London","America/New_York"};
  private static final int CYAN=0xff4ddee4;
  private final Handler handler=new Handler(Looper.getMainLooper());
  private TextView clock,date,city,system,status;
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private static final int SHIZUKU_REQUEST=472;
  private TextView[] tiles=new TextView[CITIES.length];
  private int selected=5;
  private final Runnable ticker=new Runnable(){public void run(){render();handler.postDelayed(this,1000);}};

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    SharedPreferences prefs=getPreferences(MODE_PRIVATE);
    selected=Math.max(0,Math.min(CITIES.length-1,prefs.getInt("city",5)));
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);
    root.setPadding(dp(42),dp(24),dp(42),dp(20));
    root.setBackgroundColor(0xff0c1321);
    setContentView(root);
    root.addView(label("МИРОВОЕ ВРЕМЯ  •  WORLD TIME",25,CYAN,true));
    city=label("",29,Color.WHITE,true); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(18);root.addView(city,lp);
    clock=label("",76,Color.WHITE,false);clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));root.addView(clock);
    date=label("",20,0xffbfcbd7,false);root.addView(date);
    system=label("",15,0xff9daabc,false);root.addView(system);
    TextView tip=label("ВЫБЕРИ ГОРОД С ПУЛЬТА  •  OK — ПОКАЗАТЬ ВРЕМЯ ГОРОДА",17,CYAN,false);
    LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=dp(20);root.addView(tip,tp);
    HorizontalScrollView scroll=new HorizontalScrollView(this);
    scroll.setHorizontalScrollBarEnabled(false);
    LinearLayout cards=new LinearLayout(this);cards.setOrientation(LinearLayout.HORIZONTAL);cards.setPadding(dp(4),dp(12),dp(4),dp(16));scroll.addView(cards);
    root.addView(scroll,new LinearLayout.LayoutParams(-1,dp(135)));
    for(int i=0;i<CITIES.length;i++){
      final int n=i;
      TextView tile=label("",18,Color.WHITE,true);
      tile.setGravity(Gravity.CENTER);tile.setFocusable(true);
      LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(205),dp(105));p.rightMargin=dp(12);cards.addView(tile,p);
      tiles[i]=tile;
      tile.setOnClickListener(v->{selected=n;getPreferences(MODE_PRIVATE).edit().putInt("city",n).apply();render();});
      tile.setOnFocusChangeListener((v,focus)->style(n,focus));
    }
    TextView notice=label("Выбор города меняет часы только в приложении, но не системное время телевизора.",16,0xffa4b4c8,false);
    root.addView(notice);
    TextView action=label("ПРИМЕНИТЬ ЧАСОВОЙ ПОЯС К ТЕЛЕВИЗОРУ  ⏰",19,Color.WHITE,true);
    action.setGravity(Gravity.CENTER);action.setFocusable(true);action.setPadding(dp(12),dp(14),dp(12),dp(14));
    GradientDrawable bg=new GradientDrawable();bg.setColor(0xff167a87);bg.setCornerRadius(dp(12));action.setBackground(bg);
    LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(62));ap.topMargin=dp(16);root.addView(action,ap);
    status=label("Для изменения системы запусти Shizuku через ADB.",15,0xffbfcbd7,false);root.addView(status);
    action.setOnClickListener(v->applySystemTimeZone());
    render();
    tiles[selected].requestFocus();
  }
  private void applySystemTimeZone(){
    try {
      if(!Shizuku.pingBinder()){status.setText("Shizuku не запущена. Запусти Shizuku через atvTools.");return;}
      if(Shizuku.checkSelfPermission()!=PackageManager.PERMISSION_GRANTED){
        status.setText("Подтверди доступ World Time TV к Shizuku.");
        Shizuku.requestPermission(SHIZUKU_REQUEST);return;
      }
      final String zone=ZONES[selected];
      status.setText("Устанавливаю системный часовой пояс: "+zone+"…");
      worker.execute(()->{
        String result="";
        try{
          rikka.shizuku.ShizukuRemoteProcess p=Shizuku.newProcess(new String[]{"cmd","alarm","set-timezone",zone},null,null);
          int exit=p.waitFor();
          BufferedReader err=new BufferedReader(new InputStreamReader(p.getErrorStream()));
          StringBuilder error=new StringBuilder();String line;
          while((line=err.readLine())!=null)error.append(line).append(" ");
          p.destroy();
          if(exit!=0) result="Ошибка установки: "+error+" (код "+exit+")";
          else {
            rikka.shizuku.ShizukuRemoteProcess check=Shizuku.newProcess(new String[]{"getprop","persist.sys.timezone"},null,null);
            BufferedReader rd=new BufferedReader(new InputStreamReader(check.getInputStream()));
            String actual=rd.readLine();check.waitFor();check.destroy();
            result=zone.equals(actual)?"Установлено на телевизоре: "+zone:"Команда выполнена, но подтверждения нет: "+actual;
          }
        }catch(Exception e){result="Ошибка: "+e.getClass().getSimpleName()+": "+e.getMessage();}
        final String message=result;
        runOnUiThread(()->{status.setText(message);render();});
      });
    }catch(Exception e){status.setText("Shizuku недоступна: "+e.getMessage());}
  }
  @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
    super.onRequestPermissionsResult(requestCode,permissions,results);
    if(requestCode==SHIZUKU_REQUEST){
      if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)applySystemTimeZone();
      else status.setText("Доступ Shizuku не предоставлен.");
    }
  }
  private String fmt(String zone,String pattern){
    SimpleDateFormat f=new SimpleDateFormat(pattern,new Locale("ru","RU"));
    f.setTimeZone(TimeZone.getTimeZone(zone));
    return f.format(new Date());
  }
  private void render(){
    if(clock==null)return;
    city.setText(CITIES[selected]+"  ·  "+ZONES[selected]);
    clock.setText(fmt(ZONES[selected],"HH:mm:ss"));
    date.setText(fmt(ZONES[selected],"EEEE, d MMMM yyyy"));
    system.setText("Часовой пояс телевизора: "+TimeZone.getDefault().getID());
    for(int i=0;i<tiles.length;i++){
      if(tiles[i]==null)continue;
      tiles[i].setText(CITIES[i]+"\n"+fmt(ZONES[i],"HH:mm"));
      style(i,tiles[i].hasFocus());
    }
  }
  private void style(int i,boolean focus){
    GradientDrawable d=new GradientDrawable();
    d.setColor(focus?0xff287a85:selected==i?0xff213951:0xff1d2a3d);
    d.setCornerRadius(dp(12));
    d.setStroke(dp(focus?3:1),focus?0xff7fffff:selected==i?CYAN:0xff34465c);
    tiles[i].setBackground(d);
  }
  private TextView label(String s,int size,int color,boolean bold){
    TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);
    t.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return t;
  }
  private int dp(int n){return (int)(getResources().getDisplayMetrics().density*n+.5f);}
  @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
  @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
  @Override protected void onDestroy(){worker.shutdownNow();super.onDestroy();}
}
