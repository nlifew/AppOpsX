package com.zzzmode.appopsx;

import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import com.zzzmode.android.opsxpro.BuildConfig;
import com.zzzmode.appopsx.ServiceUnavailableException.Reason;
import com.zzzmode.appopsx.common.FixCompat;
import com.zzzmode.appopsx.common.OpEntry;
import com.zzzmode.appopsx.common.OpsResult;
import com.zzzmode.appopsx.common.OpsTable;
import com.zzzmode.appopsx.common.PackageOps;
import com.zzzmode.appopsx.common.ParcelableListSlice;
import com.zzzmode.appopsx.remote.OpsxService;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import rikka.shizuku.Shizuku;

/**
 * Created by zl on 2016/11/13.
 *
 * 通过 Shizuku UserService 调用运行在 shell/root 身份下的 {@link OpsxService}。
 * 除 {@link #isRunning()} 外的方法都可能阻塞，不能在主线程调用。
 */
public class OpsxManager {

  private static final String TAG = "OpsxManager";

  /** 应用启动后 ShizukuProvider 收到 binder 需要一点时间 */
  private static final long BINDER_WAIT_MS = 3000;
  private static final long BIND_TIMEOUT_MS = 10000;

  private final Context mContext;

  private final Config mConfig;

  private int userId;

  private volatile IOpsxService mService;

  private volatile CountDownLatch mBindLatch;

  private final ServiceConnection mConnection = new ServiceConnection() {
    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
      if (binder != null && binder.pingBinder()) {
        mService = IOpsxService.Stub.asInterface(binder);
      }
      CountDownLatch latch = mBindLatch;
      if (latch != null) {
        latch.countDown();
      }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
      Log.w(TAG, "service disconnected");
      mService = null;
    }
  };

  public OpsxManager(Context context) {
    this(context, new Config());
  }

  public OpsxManager(Context context, Config config) {
    mContext = context.getApplicationContext();
    mConfig = config;
    userId = Process.myUid() / 100000; //android.os.UserHandle.myUserId()
  }

  public void setUserHandleId(int uid) {
    this.userId = uid;
  }

  public void updateConfig(Config config) {
    mConfig.allowBgRunning = config.allowBgRunning;
  }

  public Config getConfig() {
    return mConfig;
  }

  private Shizuku.UserServiceArgs userServiceArgs() {
    return new Shizuku.UserServiceArgs(
        new ComponentName(mContext.getPackageName(), OpsxService.class.getName()))
        .tag("opsx")
        .processNameSuffix("opsx")
        .daemon(mConfig.allowBgRunning)
        .debuggable(BuildConfig.DEBUG)
        .version(serviceVersion());
  }

  /** 版本号变化时 Shizuku 会重启已存在的服务；用安装时间，保证每次安装新包后都会换成新代码 */
  private int serviceVersion() {
    try {
      PackageInfo info = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
      return Long.hashCode(info.lastUpdateTime);
    } catch (PackageManager.NameNotFoundException e) {
      return 0;
    }
  }

  private synchronized IOpsxService getService() throws Exception {
    IOpsxService service = mService;
    if (service != null && service.asBinder().pingBinder()) {
      return service;
    }
    if (Looper.myLooper() == Looper.getMainLooper()) {
      throw new IllegalStateException("OpsxManager must not be called on the main thread");
    }

    waitForShizukuBinder();
    if (Shizuku.isPreV11()) {
      throw new ServiceUnavailableException(Reason.SHIZUKU_UNSUPPORTED);
    }
    if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
      throw new ServiceUnavailableException(Reason.PERMISSION_DENIED);
    }

    CountDownLatch latch = new CountDownLatch(1);
    mBindLatch = latch;
    mService = null;
    Shizuku.bindUserService(userServiceArgs(), mConnection);
    latch.await(BIND_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    mBindLatch = null;

    service = mService;
    if (service == null) {
      throw new ServiceUnavailableException(Reason.BIND_TIMEOUT);
    }
    // app 进程无法直接读取当前系统的 op 表（隐藏 API 限制），由服务进程读好后传过来
    OpsTable table = service.getOpsTable();
    if (table != null) {
      FixCompat.setOpsTable(table);
    }
    return service;
  }

  private static void waitForShizukuBinder() throws Exception {
    if (Shizuku.pingBinder()) {
      return;
    }
    final CountDownLatch latch = new CountDownLatch(1);
    Shizuku.OnBinderReceivedListener listener = new Shizuku.OnBinderReceivedListener() {
      @Override
      public void onBinderReceived() {
        latch.countDown();
      }
    };
    Shizuku.addBinderReceivedListenerSticky(listener);
    try {
      latch.await(BINDER_WAIT_MS, TimeUnit.MILLISECONDS);
    } finally {
      Shizuku.removeBinderReceivedListener(listener);
    }
    if (!Shizuku.pingBinder()) {
      throw new ServiceUnavailableException(Reason.SHIZUKU_NOT_RUNNING);
    }
  }

  public OpsResult getOpsForPackage(final String packageName) throws Exception {
    return getService().getOpsForPackage(packageName, userId);
  }

  public OpsResult getPackagesForOps(int[] ops, boolean reqNet) throws Exception {
    return getService().getPackagesForOps(ops, reqNet, userId);
  }

  public OpsResult setOpsMode(String packageName, int opInt, int modeInt) throws Exception {
    return getService().setOpsMode(packageName, opInt, modeInt, userId);
  }

  public OpsResult resetAllModes(String packageName) throws Exception {
    return getService().resetAllModes(packageName, userId);
  }

  @SuppressWarnings("unchecked")
  public List<PackageInfo> getInstalledPackages(long flags, int userId) throws Exception {
    ParcelableListSlice slice = getService().getInstalledPackages(flags, userId);
    return (List<PackageInfo>) slice.getList();
  }

  @SuppressWarnings("unchecked")
  public List<UserInfo> getUsers(boolean excludeDying) throws Exception {
    ParcelableListSlice slice = getService().getUsers(excludeDying);
    return (List<UserInfo>) slice.getList();
  }

  public boolean isRunning() {
    IOpsxService service = mService;
    return service != null && service.asBinder().pingBinder();
  }

  public OpsResult disableAllPermission(final String packageName) throws Exception {
    OpsResult opsForPackage = getOpsForPackage(packageName);
    if (opsForPackage != null) {
      if (opsForPackage.getException() == null) {
        List<PackageOps> list = opsForPackage.getList();
        if (list != null && !list.isEmpty()) {
          for (PackageOps packageOps : list) {
            List<OpEntry> ops = packageOps.getOps();
            if (ops != null) {
              for (OpEntry op : ops) {
                if (op.getMode() != AppOpsManager.MODE_IGNORED) {
                  setOpsMode(packageName, op.getOp(), AppOpsManager.MODE_IGNORED);
                }
              }
            }
          }
        }
      } else {
        throw new Exception(opsForPackage.getException());
      }
    }
    return opsForPackage;
  }

  /** 解绑并结束后台的 OpsxService 进程 */
  public void closeBgServer() {
    mService = null;
    if (Shizuku.pingBinder()) {
      Shizuku.unbindUserService(userServiceArgs(), mConnection, true);
    }
  }

  public static class Config {

    /** 应用退出后 OpsxService 是否继续运行（Shizuku daemon 模式） */
    public boolean allowBgRunning = false;
  }
}
