package com.zzzmode.appopsx.ui.core;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import androidx.appcompat.app.AppCompatDelegate;
import com.zzzmode.appopsx.BuildConfig;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zl on 2017/1/7.
 */

public class AppGlobal extends Application implements Application.ActivityLifecycleCallbacks {

  private AtomicInteger mAliveActivity = new AtomicInteger(0);

  @Override
  public void onCreate() {
    super.onCreate();

    if (BuildConfig.DEBUG) {

      StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();

      builder.detectNonSdkApiUsage();
      StrictMode.setVmPolicy(builder.penaltyLog()
          .build());

    }



    // 夜间模式是进程级设置，要在第一个页面创建前设好，否则 AppCompat 会把已创建的页面重建一次
    AppCompatDelegate.setDefaultNightMode(SpHelper.getThemeMode(this));
    LangHelper.updateLanguage(this);
    registerActivityLifecycleCallbacks(this);
    Helper.updataShortcuts(this);
    AppOpsx.getInstance(getApplicationContext());
    installReceiver();

  }


  private void installReceiver(){
    // PACKAGE_ADDED 是系统广播，NOT_EXPORTED 也能收到
    IntentFilter filter = new IntentFilter();
    filter.addAction(Intent.ACTION_PACKAGE_ADDED);
    filter.addDataScheme("package");
    registerReceiver(new AppInstalledReceiver(), filter, Context.RECEIVER_NOT_EXPORTED);
  }


  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    LangHelper.updateLanguage(this);
  }

  @Override
  protected void attachBaseContext(Context base) {
    super.attachBaseContext(LangHelper.attachBaseContext(base));
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
    mAliveActivity.getAndIncrement();
  }

  @Override
  public void onActivityStarted(Activity activity) {

  }

  @Override
  public void onActivityResumed(Activity activity) {

  }

  @Override
  public void onActivityPaused(Activity activity) {

  }

  @Override
  public void onActivityStopped(Activity activity) {

  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    int i = mAliveActivity.decrementAndGet();
    if (i <= 0) {
      Log.e("test", "onActivityDestroyed --> ");
      Helper.closeBgServer(getApplicationContext());
    }
  }
}
