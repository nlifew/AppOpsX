package com.zzzmode.appopsx.ui.core;

import android.content.Context;
import android.content.pm.PackageManager;
import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ServiceUnavailableException;
import rikka.shizuku.Shizuku;

/**
 * UI 侧与 Shizuku 相关的状态展示和授权请求。
 */
public final class ShizukuHelper {

  public static final int REQUEST_CODE = 1001;

  public static final String SHIZUKU_URL = "https://shizuku.rikka.app/";

  private ShizukuHelper() {
  }

  public static boolean isRunning() {
    return Shizuku.pingBinder();
  }

  public static boolean isGranted() {
    return isRunning() && !Shizuku.isPreV11()
        && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
  }

  /**
   * Shizuku 正在运行但尚未授权时弹出授权请求。
   * 用户之前选择了“拒绝且不再询问”时 Shizuku 不会再弹窗，返回 false。
   */
  public static boolean requestPermissionIfNeeded() {
    if (!isRunning() || Shizuku.isPreV11() || isGranted()
        || Shizuku.shouldShowRequestPermissionRationale()) {
      return false;
    }
    Shizuku.requestPermission(REQUEST_CODE);
    return true;
  }

  /** 当前状态的一句话说明，用于设置页 */
  public static String describe(Context context) {
    if (!isRunning()) {
      return context.getString(R.string.shizuku_not_running);
    }
    if (Shizuku.isPreV11()) {
      return context.getString(R.string.shizuku_unsupported);
    }
    if (!isGranted()) {
      return context.getString(R.string.shizuku_permission_denied);
    }
    return context.getString(R.string.shizuku_ready, Shizuku.getVersion(),
        Shizuku.getUid() == 0 ? "root" : "adb");
  }

  /** 把连接失败的异常转换成提示文字；不是连接问题时返回 null */
  public static String errorMessage(Context context, Throwable e) {
    if (!(e instanceof ServiceUnavailableException)) {
      return null;
    }
    switch (((ServiceUnavailableException) e).getReason()) {
      case SHIZUKU_NOT_RUNNING:
        return context.getString(R.string.shizuku_not_running);
      case SHIZUKU_UNSUPPORTED:
        return context.getString(R.string.shizuku_unsupported);
      case PERMISSION_DENIED:
        return context.getString(R.string.shizuku_permission_denied);
      case BIND_TIMEOUT:
      default:
        return context.getString(R.string.shizuku_bind_timeout);
    }
  }
}
