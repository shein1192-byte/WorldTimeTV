package com.arctic.worldtime;

import android.content.Context;
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
  /** Execute fixed, app-generated shell commands only on this device. */
  public static String execute(Context context,String command)throws Exception {
    KeyPair keys=loadKeys(context);
    Certificate cert=new PublicKeyCertificate(keys.getPublic());
    String host="127.0.0.1";
    try {
      return executeAt(host,keys,cert,command);
    }catch(java.net.ConnectException e){
      String local=localAddress(context);
      if(local==null)throw e;
      return executeAt(local,keys,cert,command);
    }catch(java.io.IOException e){
      // Other I/O errors (including authorization) must not trigger a blind retry.
      throw e;
    }
  }
  private static String executeAt(String host,KeyPair keys,Certificate cert,String command)throws Exception{
    try(AdbConnection conn=AdbConnection.create(host,5555,keys.getPrivate(),cert,android.os.Build.VERSION.SDK_INT)){
      if(!conn.connect(12000,TimeUnit.MILLISECONDS,false))
        throw new IOException("Подтверди RSA-авторизацию ADB на телевизоре и повтори.");
      try(AdbStream stream=conn.open("shell:"+command+"; echo WORLDTIME_EXIT:$?")){
        InputStream input=stream.openInputStream();
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte[] buf=new byte[2048];int n;
        try {
          while((n=input.read(buf))!=-1){
            if(output.size()<32000)output.write(buf,0,Math.min(n,32000-output.size()));
          }
        }catch(IOException closed){
          if(output.size()==0)throw closed;
        }
        String response=output.toString("UTF-8");
        if(!response.contains("WORLDTIME_EXIT:0"))
          throw new IOException("Команда завершилась с ошибкой: "+response);
        return response;
      }
    }
  }
  private static String localAddress(Context context){
    try{
      java.util.Enumeration<java.net.NetworkInterface> interfaces=java.net.NetworkInterface.getNetworkInterfaces();
      while(interfaces.hasMoreElements()){
        java.net.NetworkInterface ni=interfaces.nextElement();
        if(!ni.isUp()||ni.isLoopback())continue;
        java.util.Enumeration<java.net.InetAddress> addresses=ni.getInetAddresses();
        while(addresses.hasMoreElements()){
          java.net.InetAddress addr=addresses.nextElement();
          if(addr instanceof java.net.Inet4Address && addr.isSiteLocalAddress())return addr.getHostAddress();
        }
      }
    }catch(Exception ignored){}
    return null;
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
