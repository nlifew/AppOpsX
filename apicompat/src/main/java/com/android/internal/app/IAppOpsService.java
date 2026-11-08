package com.android.internal.app;

import android.os.Parcelable;
import java.util.List;

/**
 * 只保留实际用到的方法（全部在 AppOpsHandler 里调用），签名以 Android 13（API 33）为准。
 * 新增调用前先核对目标版本的 AIDL：例如 noteOperation 从 Android 11 起参数已经变了。
 */
public interface IAppOpsService {
  int checkOperation(int code, int uid, String packageName);

  List<Parcelable> getPackagesForOps(int[] ops);

  List<Parcelable> getOpsForPackage(int uid, String packageName, int[] ops);

  void setMode(int code, int uid, String packageName, int mode);

  void resetAllModes(int reqUserId, String reqPackageName);

  abstract class Stub {
    public static IAppOpsService asInterface(android.os.IBinder obj) {
      return null;
    }
  }
}
