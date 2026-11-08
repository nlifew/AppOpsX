package com.zzzmode.appopsx.remote;

import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.IPackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ParceledListSlice;
import android.content.pm.UserInfo;
import android.os.IUserManager;
import android.os.ServiceManager;
import android.util.Log;
import com.zzzmode.appopsx.IOpsxService;
import com.zzzmode.appopsx.common.OpsResult;
import com.zzzmode.appopsx.common.OpsTable;
import com.zzzmode.appopsx.common.ParcelableListSlice;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Shizuku UserService：由 Shizuku 在独立进程中以 shell（adb 启动）或 root 身份实例化，
 * 因此可以直接调用系统隐藏接口。客户端见 {@link com.zzzmode.appopsx.OpsxManager}。
 */
public class OpsxService extends IOpsxService.Stub {

  private static final String TAG = "OpsxService";

  private final AppOpsHandler mAppOpsHandler = new AppOpsHandler();

  private final OpsTable mOpsTable;

  public OpsxService() {
    Log.i(TAG, "started, uid: " + android.os.Process.myUid());
    mOpsTable = loadOpsTable();
    // 本进程里 permissionToCode 也要用到这张表
    OpsTable.setCurrent(mOpsTable);
  }

  /** 读取失败时返回 null，getOpsTable() 会把失败报给客户端，不使用不完整的表 */
  private static OpsTable loadOpsTable() {
    try {
      Class<?> cls = AppOpsManager.class;
      Field numOp = cls.getDeclaredField("_NUM_OP");
      numOp.setAccessible(true);
      Method opToName = cls.getDeclaredMethod("opToName", int.class);
      Method opToSwitch = cls.getDeclaredMethod("opToSwitch", int.class);
      Method opToPermission = cls.getDeclaredMethod("opToPermission", int.class);
      opToName.setAccessible(true);
      opToSwitch.setAccessible(true);
      opToPermission.setAccessible(true);

      int n = numOp.getInt(null);
      String[] names = new String[n];
      int[] switches = new int[n];
      String[] permissions = new String[n];
      for (int op = 0; op < n; op++) {
        names[op] = (String) opToName.invoke(null, op);
        switches[op] = (Integer) opToSwitch.invoke(null, op);
        permissions[op] = (String) opToPermission.invoke(null, op);
      }
      Log.i(TAG, "loaded op table, size: " + n);
      return new OpsTable(names, switches, permissions);
    } catch (Throwable throwable) {
      Log.e(TAG, "load op table failed", throwable);
      return null;
    }
  }

  @Override
  public OpsTable getOpsTable() {
    if (mOpsTable == null) {
      // IllegalStateException 能通过 binder 传回客户端
      throw new IllegalStateException("failed to read the op table from AppOpsManager");
    }
    return mOpsTable;
  }

  @Override
  public void destroy() {
    Log.i(TAG, "destroy");
    System.exit(0);
  }

  @Override
  public OpsResult getOpsForPackage(String packageName, int userId) {
    return mAppOpsHandler.getOpsForPackage(packageName, userId);
  }

  @Override
  public OpsResult getPackagesForOps(int[] ops, boolean reqNet, int userId) {
    return mAppOpsHandler.getPackagesForOps(ops, reqNet, userId);
  }

  @Override
  public OpsResult setOpsMode(String packageName, int op, int mode, int userId) {
    return mAppOpsHandler.setOpsMode(packageName, op, mode, userId);
  }

  @Override
  public OpsResult resetAllModes(String packageName, int userId) {
    return mAppOpsHandler.resetAllModes(packageName, userId);
  }

  @Override
  public ParcelableListSlice getInstalledPackages(long flags, int userId) {
    try {
      IPackageManager pm = ActivityThread.getPackageManager();
      ParceledListSlice<PackageInfo> slice = pm.getInstalledPackages(flags, userId);
      return new ParcelableListSlice<>(slice.getList());
    } catch (Throwable throwable) {
      throw rethrow(throwable);
    }
  }

  @Override
  public ParcelableListSlice getUsers(boolean excludeDying) {
    try {
      IUserManager um = IUserManager.Stub.asInterface(
          ServiceManager.getService(Context.USER_SERVICE));
      List<UserInfo> users = um.getUsers(true, excludeDying, true);
      return new ParcelableListSlice<>(users);
    } catch (Throwable throwable) {
      throw rethrow(throwable);
    }
  }

  /** binder 只能把少数几种异常传回客户端，其他异常统一包装成 IllegalStateException。 */
  private static RuntimeException rethrow(Throwable throwable) {
    Log.e(TAG, "call failed", throwable);
    return new IllegalStateException(Log.getStackTraceString(throwable));
  }
}
