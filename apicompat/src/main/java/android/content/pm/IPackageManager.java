package android.content.pm;

/**
 * Created by zl on 2017/7/31.
 *
 * 只保留实际用到的方法，签名以 Android 13（API 33）为准：flags 从 13 起是 long。
 */

public interface IPackageManager {

  PackageInfo getPackageInfo(String packageName, long flags, int userId);

  ApplicationInfo getApplicationInfo(String packageName, long flags, int userId);

  ParceledListSlice getInstalledPackages(long flags, int userId);

  int getPackageUid(String packageName, long flags, int userId);
}
