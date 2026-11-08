package com.zzzmode.appopsx.ui.model;

import android.app.AppOpsManager;
import com.zzzmode.appopsx.common.FixCompat;
import com.zzzmode.appopsx.common.OpEntry;

/**
 * Created by zl on 2016/11/18.
 */

public class OpEntryInfo {

  public OpEntry opEntry;
  public String opName;
  public String opPermsName;
  public String opPermsLab;
  public String opPermsDesc;
  public int mode;
  public int icon;
  public String groupName;

  public OpEntryInfo(OpEntry opEntry) {
    if (opEntry != null) {
      this.opEntry = opEntry;
      this.mode = opEntry.getMode();
      // 每次都取当前的表：连上服务前后表的长度会变（内置的 Android 9 表 → 系统实际的表）
      String[] sOpNames = FixCompat.sOpNames();
      String[] sOpPerms = FixCompat.sOpPerms();
      int op = opEntry.getOp();
      if (op >= 0 && op < sOpNames.length) {
        this.opName = sOpNames[op];
        this.opPermsName = sOpPerms[op];
      }
    }
  }

  public boolean isAllowed() {
    return this.mode == AppOpsManager.MODE_ALLOWED;
  }

  public void changeStatus() {
    if (isAllowed()) {
      this.mode = AppOpsManager.MODE_IGNORED;
    } else {
      this.mode = AppOpsManager.MODE_ALLOWED;
    }
  }

  @Override
  public String toString() {
    return "OpEntryInfo{" +
        ", opName='" + opName + '\'' +
        ", opPermsName='" + opPermsName + '\'' +
        ", opPermsLab='" + opPermsLab + '\'' +
        ", mode=" + mode +
        '}';
  }
}
