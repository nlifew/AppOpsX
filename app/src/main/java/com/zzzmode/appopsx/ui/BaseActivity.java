package com.zzzmode.appopsx.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.ui.core.LangHelper;
import com.zzzmode.appopsx.ui.core.SpHelper;

/**
 * Created by zl on 2017/1/7.
 */

public class BaseActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    LangHelper.updateLanguage(this);
    super.onCreate(savedInstanceState);
    AppCompatDelegate.setDefaultNightMode(SpHelper.getThemeMode(this));
    // targetSdk 35 起 Android 15 强制 edge-to-edge，这里让 13、14 也保持一致
    EdgeToEdge.enable(this);
  }

  @Override
  protected void attachBaseContext(Context newBase) {
    super.attachBaseContext(LangHelper.attachBaseContext(newBase));
  }

  @Override
  public void onContentChanged() {
    super.onContentChanged();
    applySystemBarInsets();
  }

  /**
   * 页面都由 layout_appbar 加内容区组成：appBar 向下让出状态栏，背景色延伸到状态栏后面；
   * 内容区让出导航栏和刘海，避免列表底部和 FAB 被遮挡。
   */
  private void applySystemBarInsets() {
    final View content = findViewById(android.R.id.content);
    final View appBar = findViewById(R.id.appBar);
    ViewCompat.setOnApplyWindowInsetsListener(content, (v, windowInsets) -> {
      Insets insets = windowInsets.getInsets(
          WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
      if (appBar != null && appBar.getVisibility() == View.VISIBLE) {
        appBar.setPadding(0, insets.top, 0, 0);
        v.setPadding(insets.left, 0, insets.right, insets.bottom);
      } else {
        v.setPadding(insets.left, insets.top, insets.right, insets.bottom);
      }
      return WindowInsetsCompat.CONSUMED;
    });
  }

}
