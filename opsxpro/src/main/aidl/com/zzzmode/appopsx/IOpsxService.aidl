package com.zzzmode.appopsx;

import com.zzzmode.appopsx.common.OpsResult;
import com.zzzmode.appopsx.common.OpsTable;
import com.zzzmode.appopsx.common.ParcelableListSlice;

// 由 Shizuku 以 shell/root 身份运行的服务，实现见 OpsxService
interface IOpsxService {

    // Shizuku 约定的销毁 UserService 的事务号
    void destroy() = 16777114;

    OpsResult getOpsForPackage(String packageName, int userId) = 1;

    OpsResult getPackagesForOps(in int[] ops, boolean reqNet, int userId) = 2;

    OpsResult setOpsMode(String packageName, int op, int mode, int userId) = 3;

    OpsResult resetAllModes(String packageName, int userId) = 4;

    // List<PackageInfo>
    ParcelableListSlice getInstalledPackages(long flags, int userId) = 5;

    // List<android.content.pm.UserInfo>
    ParcelableListSlice getUsers(boolean excludeDying) = 6;

    // 当前系统的 op 名称、开关、权限表
    OpsTable getOpsTable() = 7;
}
