package com.zzzmode.appopsx.remote;

import android.app.ActivityThread;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import com.zzzmode.appopsx.common.OpsTable;
import com.zzzmode.appopsx.common.ReflectUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zl on 2017/1/15.
 */
class Helper {

  static int getPackageUid(String packageName, int userId) {
    int uid = -1;
    try {
      uid = ActivityThread.getPackageManager().getPackageUid(packageName,PackageManager.MATCH_UNINSTALLED_PACKAGES,userId);
    } catch (Throwable e) {
      e.printStackTrace();
    }

    if (uid == -1) {
      try {
        ApplicationInfo applicationInfo = ActivityThread.getPackageManager()
            .getApplicationInfo(packageName, 0, userId);
        List<Class> paramsType = new ArrayList<>(2);
        paramsType.add(int.class);
        paramsType.add(int.class);
        List<Object> params = new ArrayList<>(2);
        params.add(userId);
        params.add(applicationInfo.uid);
        uid = (int) ReflectUtils.invokMethod(UserHandle.class, "getUid", paramsType, params);
      } catch (Throwable e) {
        e.printStackTrace();
      }
    }

    return uid;
  }

  /** 权限对应的开关 op；表没读到（OpsxService 已经把失败报给客户端）或没有对应 op 时返回 -1 */
  static int permissionToCode(String permission) {
    OpsTable table = OpsTable.current();
    return table != null ? table.permissionToSwitchOp(permission) : -1;
  }

}
