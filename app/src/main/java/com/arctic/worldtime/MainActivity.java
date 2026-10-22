package com.arctic.worldtime;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
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
  private TextView clock,date,city,system;
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
    render();
    tiles[selected].requestFocus();
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
}
