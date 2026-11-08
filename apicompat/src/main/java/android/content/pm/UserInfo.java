package android.content.pm;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.UserHandle;

public class UserInfo implements Parcelable {
  public int id;
  public int serialNumber;
  public String name;
  public String iconPath;
  public int flags;


  public boolean isPrimary() {
    return false;
  }

  public boolean isAdmin() {
    return false;
  }

  public boolean isManagedProfile() {
    return false;
  }


  public UserHandle getUserHandle() {
    return null;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
  }
}
