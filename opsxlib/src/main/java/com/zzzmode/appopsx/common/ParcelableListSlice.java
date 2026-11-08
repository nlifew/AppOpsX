package com.zzzmode.appopsx.common;

import android.os.BadParcelableException;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;

/**
 * 跨 binder 传递大列表：先在当前 Parcel 里写一部分，剩下的由接收方通过附带的 binder 分批拉取，
 * 避免超过 binder 事务的大小上限。思路同 AOSP 的 ParceledListSlice（对普通 app 不可用）。
 */
public class ParcelableListSlice<T extends Parcelable> implements Parcelable {

  private static final int MAX_IPC_SIZE = 64 * 1024;

  private final List<T> list;

  public ParcelableListSlice(List<T> list) {
    this.list = list != null ? list : new ArrayList<T>();
  }

  public List<T> getList() {
    return list;
  }

  @SuppressWarnings({"unchecked", "deprecation"})
  private ParcelableListSlice(Parcel in, ClassLoader loader) {
    final int n = in.readInt();
    list = new ArrayList<>(n);
    int i = 0;
    while (i < n && in.readInt() != 0) {
      list.add((T) in.readParcelable(loader));
      i++;
    }
    if (i >= n) {
      return;
    }

    final IBinder retriever = in.readStrongBinder();
    while (i < n) {
      Parcel data = Parcel.obtain();
      Parcel reply = Parcel.obtain();
      try {
        data.writeInt(i);
        retriever.transact(IBinder.FIRST_CALL_TRANSACTION, data, reply, 0);
        while (i < n && reply.readInt() != 0) {
          list.add((T) reply.readParcelable(loader));
          i++;
        }
      } catch (RemoteException e) {
        throw new BadParcelableException(e);
      } finally {
        reply.recycle();
        data.recycle();
      }
    }
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    final int n = list.size();
    dest.writeInt(n);
    int i = 0;
    while (i < n && dest.dataSize() < MAX_IPC_SIZE) {
      dest.writeInt(1);
      dest.writeParcelable(list.get(i), flags);
      i++;
    }
    if (i < n) {
      dest.writeInt(0);
      dest.writeStrongBinder(new Binder() {
        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int callFlags)
            throws RemoteException {
          if (code != FIRST_CALL_TRANSACTION) {
            return super.onTransact(code, data, reply, callFlags);
          }
          int i = data.readInt();
          while (i < n && reply.dataSize() < MAX_IPC_SIZE) {
            reply.writeInt(1);
            reply.writeParcelable(list.get(i), flags);
            i++;
          }
          if (i < n) {
            reply.writeInt(0);
          }
          return true;
        }
      });
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Parcelable.ClassLoaderCreator<ParcelableListSlice> CREATOR =
      new Parcelable.ClassLoaderCreator<ParcelableListSlice>() {
        @Override
        public ParcelableListSlice createFromParcel(Parcel source) {
          return new ParcelableListSlice(source, ParcelableListSlice.class.getClassLoader());
        }

        @Override
        public ParcelableListSlice createFromParcel(Parcel source, ClassLoader loader) {
          return new ParcelableListSlice(source, loader);
        }

        @Override
        public ParcelableListSlice[] newArray(int size) {
          return new ParcelableListSlice[size];
        }
      };
}
