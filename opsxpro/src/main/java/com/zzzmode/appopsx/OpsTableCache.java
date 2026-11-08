package com.zzzmode.appopsx;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import com.zzzmode.appopsx.common.OpsTable;
import org.json.JSONException;

/**
 * 按设备指纹缓存 op 表。同一个系统版本的表不会变，系统升级后指纹变化，缓存自动失效。
 * 这只是最近一次从 OpsxService 拿到的副本：每次连上服务都会核对，不一致就覆盖。
 */
final class OpsTableCache {

  private static final String TAG = "OpsTableCache";

  private static final String PREFS = "opsx_ops_table";
  private static final String KEY_FINGERPRINT = "fingerprint";
  private static final String KEY_FORMAT = "format";
  private static final String KEY_TABLE = "table";

  /** 缓存格式版本，修改 OpsTable 的 JSON 结构时加一 */
  private static final int FORMAT = 1;

  private final SharedPreferences mPrefs;

  OpsTableCache(Context context) {
    mPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  /** 缓存不存在、属于其他系统版本或格式不匹配时返回 null，不使用过期数据 */
  OpsTable load() {
    if (!Build.FINGERPRINT.equals(mPrefs.getString(KEY_FINGERPRINT, null))
        || mPrefs.getInt(KEY_FORMAT, 0) != FORMAT) {
      return null;
    }
    String json = mPrefs.getString(KEY_TABLE, null);
    if (json == null) {
      return null;
    }
    try {
      return OpsTable.fromJson(json);
    } catch (JSONException e) {
      Log.w(TAG, "discard corrupted cache", e);
      return null;
    }
  }

  void save(OpsTable table) {
    mPrefs.edit()
        .putString(KEY_FINGERPRINT, Build.FINGERPRINT)
        .putInt(KEY_FORMAT, FORMAT)
        .putString(KEY_TABLE, table.toJson())
        .apply();
  }
}
