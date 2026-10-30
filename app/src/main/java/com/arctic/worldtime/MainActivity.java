package com.arctic.worldtime;
import android.app.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
  private final ExecutorService worker=Executors.newSingleThreadExecutor();
  private final Handler handler=new Handler(Looper.getMainLooper());
  private TextView clock,date,utc,status;
  private final Runnable ticker=new Runnable(){public void run(){render();handler.postDelayed(this,1000);}};
  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    LinearLayout root=new LinearLayout(this);
    root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(32),dp(16),dp(32),dp(12));
    root.setBackgroundColor(0xff0c1321);setContentView(root);
    root.addView(text("МИРОВОЕ ВРЕМЯ • РУЧНАЯ НАСТРОЙКА",23,0xff4ddee4));
    clock=text("",68,-1);root.addView(clock);
    date=text("",20,-1);root.addView(date);
    utc=text("",19,0xff4ddee4);root.addView(utc);
    Button manual=button("НАСТРОИТЬ ДАТУ И ВРЕМЯ ВРУЧНУЮ",()->manual());
    root.addView(manual);
    status=text("Включи ADB TCP 5555 в настройках ТВ. Приложение подключается к этому же телевизору автоматически; Shizuku не требуется.",16,0xffbbccdd);
    root.addView(status);manual.requestFocus();render();
  }
  private void manual(){
    Calendar c=Calendar.getInstance();
    int[] v={c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)};
    int[] lo={2000,1,1,0,0},hi={2099,12,31,23,59};
    String[] names={"Год","Месяц","День","Час","Минута"};
    LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setPadding(dp(16),0,dp(16),0);
    TextView[] fields=new TextView[5];
    for(int i=0;i<5;i++){
      final int j=i;
      LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);
      line.addView(text(names[i],18,-1),new LinearLayout.LayoutParams(0,dp(48),1));
      fields[i]=text(Integer.toString(v[i]),20,-1);fields[i].setGravity(Gravity.CENTER);
      line.addView(button("−",()->{v[j]=v[j]<=lo[j]?hi[j]:v[j]-1;fields[j].setText(Integer.toString(v[j]));}));
      line.addView(fields[i],new LinearLayout.LayoutParams(dp(85),dp(48)));
      line.addView(button("+",()->{v[j]=v[j]>=hi[j]?lo[j]:v[j]+1;fields[j].setText(Integer.toString(v[j]));}));
      layout.addView(line);
    }
    new AlertDialog.Builder(this).setTitle("Установить системные дату и время")
      .setMessage("Ввод производится по текущему часовому поясу ТВ ("+offset()+"). Автоматическая синхронизация будет отключена.")
      .setView(layout).setNegativeButton("ОТМЕНА",null)
      .setPositiveButton("ПРИМЕНИТЬ",(d,w)->apply(v)).show();
  }
  private void apply(int[] values){
    Calendar c=Calendar.getInstance();c.clear();c.setLenient(false);
    try{
      c.set(values[0],values[1]-1,values[2],values[3],values[4],0);
      long ms=c.getTimeInMillis();
      status.setText("Подключаю ADB и устанавливаю системное время...");
      worker.execute(()->{
        String message;
        try{
          String first=LocalAdbStarter.command(getApplicationContext(),"settings put global auto_time 0");
          if(!"OK".equals(first))message=first;
          else{
            String result=LocalAdbStarter.command(getApplicationContext(),"cmd alarm set-time "+ms);
            message="OK".equals(result)?"Команда выполнена. Проверь системное время ТВ.":result;
          }
        }catch(Exception e){message="ADB не подключился: "+e.getClass().getSimpleName()+": "+e.getMessage()+". Убедись, что ADB TCP 5555 включён, затем подтверди RSA-разрешение на ТВ.";}
        final String result=message;
        runOnUiThread(()->{status.setText(result);render();});
      });
    }catch(IllegalArgumentException e){status.setText("Недопустимая дата или время.");}
  }
  private String offset(){
    int mins=TimeZone.getDefault().getOffset(System.currentTimeMillis())/60000;
    return String.format(Locale.ROOT,"UTC%s%02d:%02d",mins>=0?"+":"-",Math.abs(mins)/60,Math.abs(mins)%60);
  }
  private void render(){
    TimeZone z=TimeZone.getDefault();
    SimpleDateFormat f=new SimpleDateFormat("HH:mm:ss",new Locale("ru","RU"));f.setTimeZone(z);
    clock.setText(f.format(new Date()));
    f.applyPattern("EEEE, d MMMM yyyy");date.setText(f.format(new Date()));
    utc.setText("Разница с UTC: "+offset()+"  •  "+z.getID());
  }
  private TextView text(String v,int size,int color){TextView t=new TextView(this);t.setText(v);t.setTextSize(size);t.setTextColor(color);return t;}
  private Button button(String v,Runnable action){Button b=new Button(this);b.setText(v);b.setAllCaps(false);b.setFocusable(true);b.setOnClickListener(view->action.run());return b;}
  private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
  @Override protected void onResume(){super.onResume();handler.removeCallbacks(ticker);handler.post(ticker);}
  @Override protected void onPause(){handler.removeCallbacks(ticker);super.onPause();}
  @Override protected void onDestroy(){handler.removeCallbacks(ticker);worker.shutdownNow();super.onDestroy();}
}
