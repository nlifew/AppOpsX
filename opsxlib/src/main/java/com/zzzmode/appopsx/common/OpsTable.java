package com.zzzmode.appopsx.common;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * 当前系统的 op 表：下标是 op 编号，依次给出名称、对应的开关 op、关联的权限。
 * 由 OpsxService 从系统读取（每个大版本都会新增 op），通过 AIDL 交给 app，见 {@link FixCompat#setOpsTable}。
 */
public class OpsTable implements Parcelable {

  public final String[] names;
  public final int[] switches;
  public final String[] permissions;

  public OpsTable(String[] names, int[] switches, String[] permissions) {
    if (names.length != switches.length || names.length != permissions.length) {
      throw new IllegalArgumentException("op table arrays differ in length");
    }
    this.names = names;
    this.switches = switches;
    this.permissions = permissions;
  }

  public int size() {
    return names.length;
  }

  protected OpsTable(Parcel in) {
    names = in.createStringArray();
    switches = in.createIntArray();
    permissions = in.createStringArray();
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeStringArray(names);
    dest.writeIntArray(switches);
    dest.writeStringArray(permissions);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Creator<OpsTable> CREATOR = new Creator<OpsTable>() {
    @Override
    public OpsTable createFromParcel(Parcel source) {
      return new OpsTable(source);
    }

    @Override
    public OpsTable[] newArray(int size) {
      return new OpsTable[size];
    }
  };
}
