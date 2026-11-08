package com.zzzmode.appopsx.common;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

/**
 * Created by zl on 2016/11/8.
 */

public class OpsResult implements Parcelable {

  private Throwable exception;
  private List<PackageOps> list;

  public OpsResult(List<PackageOps> list, Throwable exception) {
    this.exception = exception;
    this.list = list;
  }


  public Throwable getException() {
    return exception;
  }

  public List<PackageOps> getList() {
    return list;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  // 列表可能包含所有应用的权限（getPackagesForOps），一次性写入会超过 binder 事务上限，
  // 所以交给 ParcelableListSlice 分批传输
  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeSerializable(this.exception);
    if (this.list == null) {
      dest.writeInt(0);
    } else {
      dest.writeInt(1);
      new ParcelableListSlice<>(this.list).writeToParcel(dest, flags);
    }
  }

  @SuppressWarnings("unchecked")
  protected OpsResult(Parcel in) {
    this.exception = (Throwable) in.readSerializable();
    if (in.readInt() != 0) {
      this.list = (List<PackageOps>) ParcelableListSlice.CREATOR
          .createFromParcel(in, OpsResult.class.getClassLoader()).getList();
    }
  }

  public static final Parcelable.Creator<OpsResult> CREATOR = new Parcelable.Creator<OpsResult>() {
    @Override
    public OpsResult createFromParcel(Parcel source) {
      return new OpsResult(source);
    }

    @Override
    public OpsResult[] newArray(int size) {
      return new OpsResult[size];
    }
  };


  @Override
  public String toString() {
    return "OpsResult{" +
        "exception=" + exception +
        ", list=" + list +
        '}';
  }
}
