package com.zzzmode.appopsx.ui.model;

import android.app.AppOpsManager;
import com.zzzmode.appopsx.common.OpsTable;
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
      // OpEntry 都来自服务返回的结果，OpsxManager 在返回结果前已经准备好了表
      OpsTable table = OpsTable.current();
      int op = opEntry.getOp();
      if (table != null && op >= 0 && op < table.size()) {
        this.opName = table.names[op];
        this.opPermsName = table.permissions[op];
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
