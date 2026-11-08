package com.zzzmode.appopsx.common;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 当前系统的 op 表：下标是 op 编号，依次给出名称、对应的开关 op、关联的权限。
 *
 * 表由 OpsxService 从系统读取（每个大版本都会新增 op），app 进程通过 OpsxManager 获取，
 * 并按设备指纹缓存。对象创建后不再修改；由表派生的数据也放在对象上，换表时随旧对象一起失效。
 */
public final class OpsTable implements Parcelable {

  private static volatile OpsTable sCurrent = null;

  /** 当前进程正在使用的表；还没取到时为 null（app 进程需通过 OpsxManager 获取） */
  public static OpsTable current() {
    return sCurrent;
  }

  public static void setCurrent(OpsTable table) {
    sCurrent = table;
  }

  public final String[] names;
  public final int[] switches;
  public final String[] permissions;

  /** 权限 → 开关 op，按需构建 */
  private volatile Map<String, Integer> mPermissionToSwitch;

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

  /** 权限对应的开关 op，没有对应的 op 时返回 -1 */
  public int permissionToSwitchOp(String permission) {
    Map<String, Integer> map = mPermissionToSwitch;
    if (map == null) {
      map = new HashMap<>();
      for (int op = 0; op < permissions.length; op++) {
        if (permissions[op] != null) {
          map.put(permissions[op], switches[op]);
        }
      }
      mPermissionToSwitch = map;
    }
    Integer op = map.get(permission);
    return op != null ? op : -1;
  }

  // ---- 缓存用的序列化。Parcel 的格式不保证跨版本稳定，所以落盘用 JSON ----

  public String toJson() {
    try {
      JSONObject json = new JSONObject();
      json.put("names", toJsonArray(names));
      JSONArray sw = new JSONArray();
      for (int s : switches) {
        sw.put(s);
      }
      json.put("switches", sw);
      json.put("permissions", toJsonArray(permissions));
      return json.toString();
    } catch (JSONException e) {
      throw new IllegalStateException(e);
    }
  }

  /** 解析失败时抛出，调用方应当作缓存无效处理 */
  public static OpsTable fromJson(String text) throws JSONException {
    JSONObject json = new JSONObject(text);
    JSONArray namesJson = json.getJSONArray("names");
    JSONArray switchesJson = json.getJSONArray("switches");
    JSONArray permsJson = json.getJSONArray("permissions");
    int n = namesJson.length();
    String[] names = new String[n];
    int[] switches = new int[switchesJson.length()];
    String[] perms = new String[permsJson.length()];
    for (int i = 0; i < n; i++) {
      names[i] = namesJson.getString(i);
    }
    for (int i = 0; i < switches.length; i++) {
      switches[i] = switchesJson.getInt(i);
    }
    for (int i = 0; i < perms.length; i++) {
      perms[i] = permsJson.isNull(i) ? null : permsJson.getString(i);
    }
    try {
      return new OpsTable(names, switches, perms);
    } catch (IllegalArgumentException e) {
      throw new JSONException(e.getMessage());
    }
  }

  private static JSONArray toJsonArray(String[] values) {
    JSONArray array = new JSONArray();
    for (String v : values) {
      array.put(v != null ? v : JSONObject.NULL);
    }
    return array;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OpsTable)) {
      return false;
    }
    OpsTable that = (OpsTable) o;
    return Arrays.equals(names, that.names) && Arrays.equals(switches, that.switches)
        && Arrays.equals(permissions, that.permissions);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(names) * 31 + Arrays.hashCode(switches);
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
