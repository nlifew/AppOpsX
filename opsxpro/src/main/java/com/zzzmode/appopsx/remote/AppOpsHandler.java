package com.zzzmode.appopsx.remote;

import android.Manifest;
import android.app.ActivityThread;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Process;
import android.os.ServiceManager;
import android.util.Log;
import com.android.internal.app.IAppOpsService;
import com.zzzmode.appopsx.common.FLog;
import com.zzzmode.appopsx.common.OpEntry;
import com.zzzmode.appopsx.common.OpsResult;
import com.zzzmode.appopsx.common.OtherOp;
import com.zzzmode.appopsx.common.PackageOps;
import com.zzzmode.appopsx.common.ReflectUtils;
import com.zzzmode.appopsx.common.Shell;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 在 OpsxService 进程（shell/root 身份）中执行的 AppOps 操作。
 * 所有方法都不抛异常，出错时把异常放进 OpsResult 返回给客户端。
 */
class AppOpsHandler {

  private static IptablesController mIptablesController = null;

  static {
    try {
      if (Process.myUid() == 0) {
        mIptablesController = new IptablesController(Shell.getRootShell());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static IAppOpsService getAppOpsService() {
    return IAppOpsService.Stub.asInterface(ServiceManager.getService(Context.APP_OPS_SERVICE));
  }

  OpsResult getOpsForPackage(String packageName, int userHandleId) {
    try {
      final IAppOpsService appOpsService = getAppOpsService();
      int uid = Helper.getPackageUid(packageName, userHandleId);

      List opsForPackage = appOpsService.getOpsForPackage(uid, packageName, null);
      ArrayList<PackageOps> packageOpses = new ArrayList<>();
      if (opsForPackage != null) {
        for (Object o : opsForPackage) {
          PackageOps packageOps = ReflectUtils.opsConvert(o);
          addSupport(appOpsService, packageOps, userHandleId);
          packageOpses.add(packageOps);
        }
      } else {
        PackageOps packageOps = new PackageOps(packageName, uid, new ArrayList<OpEntry>());
        addSupport(appOpsService, packageOps, userHandleId);
        packageOpses.add(packageOps);
      }

      return new OpsResult(packageOpses, null);
    } catch (Throwable throwable) {
      return failed(throwable);
    }
  }

  OpsResult getPackagesForOps(int[] ops, boolean reqNet, int userHandleId) {
    try {
      final IAppOpsService appOpsService = getAppOpsService();

      List opsForPackage = appOpsService.getPackagesForOps(ops);
      ArrayList<PackageOps> packageOpses = new ArrayList<>();

      if (opsForPackage != null) {
        for (Object o : opsForPackage) {
          PackageOps packageOps = ReflectUtils.opsConvert(o);
          addSupport(appOpsService, packageOps, userHandleId, reqNet);
          packageOpses.add(packageOps);
        }
      }

      return new OpsResult(packageOpses, null);
    } catch (Throwable throwable) {
      return failed(throwable);
    }
  }

  OpsResult setOpsMode(String packageName, int op, int mode, int userHandleId) {
    try {
      final int uid = Helper.getPackageUid(packageName, userHandleId);
      if (OtherOp.isOtherOp(op)) {
        setOther(op, mode, uid);
      } else {
        getAppOpsService().setMode(op, uid, packageName, mode);
      }
      return new OpsResult(null, null);
    } catch (Throwable throwable) {
      return failed(throwable);
    }
  }

  OpsResult resetAllModes(String packageName, int userHandleId) {
    try {
      getAppOpsService().resetAllModes(userHandleId, packageName);
      return new OpsResult(null, null);
    } catch (Throwable throwable) {
      return failed(throwable);
    }
  }

  private static OpsResult failed(Throwable throwable) {
    throwable.printStackTrace();
    FLog.log(throwable);
    return new OpsResult(null, throwable);
  }

  private void addSupport(IAppOpsService appOpsService, PackageOps ops, int userHandleId) {

    addSupport(appOpsService, ops, userHandleId, true);
  }

  private void addSupport(IAppOpsService appOpsService, PackageOps ops, int userHandleId, boolean checkNet) {
    try {
      if (checkNet && mIptablesController != null) {
        int mode = mIptablesController.isMobileDataEnable(ops.getUid()) ? AppOpsManager.MODE_ALLOWED
            : AppOpsManager.MODE_IGNORED;
        OpEntry opEntry = new OpEntry(OtherOp.OP_ACCESS_PHONE_DATA, mode, 0, 0, 0, 0, null);
        ops.getOps().add(opEntry);

        mode = mIptablesController.isWifiDataEnable(ops.getUid()) ? AppOpsManager.MODE_ALLOWED
            : AppOpsManager.MODE_IGNORED;
        opEntry = new OpEntry(OtherOp.OP_ACCESS_WIFI_NETWORK, mode, 0, 0, 0, 0, null);
        ops.getOps().add(opEntry);
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println(Log.getStackTraceString(e));
    }
    try {
      PackageInfo packageInfo = ActivityThread.getPackageManager()
          .getPackageInfo(ops.getPackageName(), PackageManager.GET_PERMISSIONS, userHandleId);
      if (packageInfo != null && packageInfo.requestedPermissions != null) {
        for (String permission : packageInfo.requestedPermissions) {
          int code = Helper.permissionToCode(permission);

          if (code <= 0) {
            //correct OP_WIFI_SCAN code.
            if (Manifest.permission.ACCESS_WIFI_STATE.equals(permission)) {
              code = OtherOp.getWifiScanOp();
            }
          }

          if (code > 0 && !ops.hasOp(code)) {
            int mode = appOpsService.checkOperation(code, ops.getUid(), ops.getPackageName());
            if (mode != AppOpsManager.MODE_ERRORED) {
              //
              ops.getOps().add(new OpEntry(code, mode, 0, 0, 0, 0, null));
            }
          }
        }
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  private void setOther(int op, int mode, int uid) {
    if (mIptablesController != null) {
      boolean enable = mode == AppOpsManager.MODE_ALLOWED;
      switch (op) {
        case OtherOp.OP_ACCESS_PHONE_DATA:
          mIptablesController.setMobileData(uid, enable);
          break;
        case OtherOp.OP_ACCESS_WIFI_NETWORK:
          mIptablesController.setWifiData(uid, enable);
          break;
      }
    }
  }
}
