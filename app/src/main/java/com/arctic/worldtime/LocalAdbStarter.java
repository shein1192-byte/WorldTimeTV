package com.arctic.worldtime;

import android.content.Context;
import android.util.Base64;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.spec.*;
import java.util.concurrent.TimeUnit;
import io.github.muntashirakon.adb.AdbConnection;
import io.github.muntashirakon.adb.AdbStream;

/** ADB TCP client: connects only to the same TV, never another network device. */
public final class LocalAdbStarter {
  private static final String SCRIPT="/sdcard/Android/data/moe.shizuku.privileged.api/start.sh";
  private LocalAdbStarter(){}
  public static String start(Context context) throws Exception {
    KeyPair keys=loadKeys(context);
    Certificate cert=new PublicKeyCertificate(keys.getPublic());
    try(AdbConnection conn=AdbConnection.create("127.0.0.1",5555,keys.getPrivate(),cert,android.os.Build.VERSION.SDK_INT)){
      if(!conn.connect(15000,TimeUnit.MILLISECONDS,false))
        return "ADB не авторизован. Подтверди RSA-ключ на экране телевизора и повтори запуск.";
      try(AdbStream stream=conn.open("shell:sh "+SCRIPT+"; echo WORLDTIME_EXIT:$?")){
        InputStream input=stream.openInputStream();
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte[] buf=new byte[2048];int n;
        try {
          while((n=input.read(buf))!=-1){
            if(output.size()<32000)output.write(buf,0,Math.min(n,32000-output.size()));
          }
        } catch(IOException closed) {
          // Some ADB versions signal remote stream close via IOException.
          if(output.size()==0)throw closed;
        }
        String result=output.toString("UTF-8");
        if(result.contains("WORLDTIME_EXIT:0"))return "Команда запуска Shizuku выполнена. Проверяю сервис...";
        return "Запуск Shizuku не подтверждён: "+(result.length()>450?result.substring(0,450):result);
      }
    }
  }
  private static KeyPair loadKeys(Context ctx)throws Exception {
    File dir=ctx.getFilesDir();
    File priv=new File(dir,"worldtime_adb_private.der"),pub=new File(dir,"worldtime_adb_public.der");
    KeyFactory factory=KeyFactory.getInstance("RSA");
    if(priv.exists()&&pub.exists()){
      return new KeyPair(factory.generatePublic(new X509EncodedKeySpec(read(pub))),
          factory.generatePrivate(new PKCS8EncodedKeySpec(read(priv))));
    }
    KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");
    generator.initialize(2048);
    KeyPair pair=generator.generateKeyPair();
    save(priv,pair.getPrivate().getEncoded());save(pub,pair.getPublic().getEncoded());
    return pair;
  }
  private static byte[] read(File file)throws IOException {
    try(FileInputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
      byte[] buf=new byte[2048];int n;while((n=in.read(buf))!=-1)out.write(buf,0,n);
      return out.toByteArray();
    }
  }
  private static void save(File file,byte[] value)throws IOException {
    try(FileOutputStream out=new FileOutputStream(file)){out.write(value);}
  }
  private static final class PublicKeyCertificate extends Certificate {
    private final PublicKey key;
    PublicKeyCertificate(PublicKey k){super("X.509");key=k;}
    @Override public byte[] getEncoded(){return key.getEncoded();}
    @Override public void verify(PublicKey k){}
    @Override public void verify(PublicKey k,String provider){}
    @Override public String toString(){return "ADB local RSA public key";}
    @Override public PublicKey getPublicKey(){return key;}
  }
}
