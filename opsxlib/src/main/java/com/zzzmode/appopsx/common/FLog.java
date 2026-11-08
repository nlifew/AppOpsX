package com.zzzmode.appopsx.common;

import android.system.Os;
import android.system.OsConstants;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zl on 2017/2/26.
 */
public class FLog {

  public static boolean writeLog = false;
  private static FileOutputStream fos;
  private static AtomicInteger sBufferSize = new AtomicInteger();
  private static AtomicInteger sErrorCount = new AtomicInteger();

  private static void openFile() {
    try {
      if (writeLog && fos == null && sErrorCount.get() < 5) {
        File file = new File("/data/local/tmp/opsx.txt");
        fos = new FileOutputStream(file);

        fos.write("\n\n\n--------------------".getBytes());
        fos.write(new Date().toString().getBytes());
        fos.write("\n\n".getBytes());
        chown(file.getAbsolutePath(),2000,2000);
        chmod(file.getAbsolutePath(),0755);
      }
    } catch (Exception e) {
      e.printStackTrace();
      sErrorCount.incrementAndGet();
      fos = null;
    }
  }

  private static void chown(String path, int uid, int gid){
    try {
      Os.chown(path,uid,gid);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void chmod(String path,int mode){
    try {
      Os.chmod(path, mode);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void log(String log) {
    if (writeLog) {
      System.out.println(log);
    } else {
      Log.e("appopsx", "Flog --> " + log);
    }

    try {
      if (writeLog) {
        openFile();
        if (fos != null) {
          fos.write(log.getBytes());
          fos.write("\n".getBytes());

          if (sBufferSize.incrementAndGet() > 10) {
            fos.getFD().sync();
            fos.flush();
            sBufferSize.set(0);
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void log(Throwable e) {
    log(Log.getStackTraceString(e));
  }

  public static void close() {
    try {
      if (writeLog && fos != null) {
        fos.getFD().sync();
        fos.close();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

