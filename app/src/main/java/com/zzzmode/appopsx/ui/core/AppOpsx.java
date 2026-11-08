package com.zzzmode.appopsx.ui.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import com.zzzmode.appopsx.OpsxManager;

/**
 * Created by zl on 2016/11/19.
 */

public class AppOpsx {

  private static OpsxManager sManager;

  public static OpsxManager getInstance(Context context) {
    if (sManager == null) {
      synchronized (AppOpsx.class) {
        if (sManager == null) {
          OpsxManager.Config config = new OpsxManager.Config();
          updateConfig(context, config);
          sManager = new OpsxManager(context.getApplicationContext(), config);
        }
      }
    }
    return sManager;
  }

  public static void updateConfig(Context context) {
    if (sManager != null) {
      OpsxManager.Config config = sManager.getConfig();
      if (config != null) {
        updateConfig(context, config);
      }
    }
  }


  private static void updateConfig(Context context, OpsxManager.Config config) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

    config.allowBgRunning = sp.getBoolean("allow_bg_remote", true);
  }

  /** 问题反馈用的诊断信息 */
  public static String readLogs(Context context) {
    StringBuilder sb = new StringBuilder();
    sb.append("Android:").append(Build.VERSION.RELEASE)
        .append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
    sb.append("Device:").append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n');

    try {
      PackageInfo packageInfo = context.getPackageManager()
          .getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
      sb.append("GitCommitId:").append(packageInfo.applicationInfo.metaData.getString("GIT_COMMIT_ID"));
    } catch (Exception e) {
      e.printStackTrace();
    }
    sb.append('\n');

    sb.append("Shizuku:").append(ShizukuHelper.describe(context)).append('\n');
    sb.append("Service running:").append(getInstance(context).isRunning()).append('\n');
    return sb.toString();
  }
}
