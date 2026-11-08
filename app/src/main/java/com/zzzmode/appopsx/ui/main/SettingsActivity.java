package com.zzzmode.appopsx.ui.main;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.SparseBooleanArray;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceGroupAdapter;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.zzzmode.appopsx.BuildConfig;
import com.zzzmode.appopsx.R;
import com.zzzmode.appopsx.common.OpsTable;
import com.zzzmode.appopsx.ui.BaseActivity;
import com.zzzmode.appopsx.ui.core.AppOpsx;
import com.zzzmode.appopsx.ui.core.Helper;
import com.zzzmode.appopsx.ui.core.LangHelper;
import com.zzzmode.appopsx.ui.core.ShizukuHelper;
import com.zzzmode.appopsx.ui.model.OpEntryInfo;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.operators.single.SingleJust;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import rikka.shizuku.Shizuku;

/**
 * Created by zl on 2017/1/16.
 */

public class SettingsActivity extends BaseActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_setting);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    setTitle(R.string.menu_setting);
    getSupportFragmentManager().beginTransaction()
        .replace(R.id.flag_container, new MyPreferenceFragment()).commit();

  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    AppOpsx.updateConfig(getApplicationContext());
  }


  public static class MyPreferenceFragment extends PreferenceFragmentCompat implements
      Preference.OnPreferenceClickListener {

    private Preference mPrefAppSort;
    private Preference mShizukuStatus;

    private final Shizuku.OnRequestPermissionResultListener mShizukuPermissionListener =
        new Shizuku.OnRequestPermissionResultListener() {
          @Override
          public void onRequestPermissionResult(int requestCode, int grantResult) {
            updateShizukuStatus();
          }
        };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      setPreferencesFromResource(R.xml.settings, rootKey);

      findPreference("ignore_premission").setOnPreferenceClickListener(this);
      findPreference("show_sysapp").setOnPreferenceClickListener(this);

      mShizukuStatus = findPreference("shizuku_status");
      mShizukuStatus.setOnPreferenceClickListener(this);
      Shizuku.addRequestPermissionResultListener(mShizukuPermissionListener);

      findPreference("allow_bg_remote").setOnPreferenceClickListener(this);
      findPreference("project").setOnPreferenceClickListener(this);

      findPreference("opensource_licenses").setOnPreferenceClickListener(this);
      findPreference("help").setOnPreferenceClickListener(this);
      findPreference("translate").setOnPreferenceClickListener(this);

      Preference version = findPreference("version");
      version.setSummary(BuildConfig.VERSION_NAME);
      version.setOnPreferenceClickListener(this);

      Preference appLanguage = findPreference("pref_app_language");
      appLanguage.setOnPreferenceClickListener(this);
      appLanguage.setSummary(
          getResources().getStringArray(R.array.languages)[LangHelper.getLocalIndex(getContext())]);

      findPreference("acknowledgments")
          .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              StringBuilder sb = new StringBuilder();
              String[] stringArray = getResources().getStringArray(R.array.acknowledgments_list);
              for (String s : stringArray) {
                sb.append(s).append('\n');
              }
              sb.deleteCharAt(sb.length() - 1);
              showTextDialog(R.string.acknowledgments_list, sb.toString());
              return true;
            }
          });

      findPreference("ignore_premission_templete")
          .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              showPremissionTemplete();
              return true;
            }
          });

      mPrefAppSort = findPreference("pref_app_sort_type");
      mPrefAppSort.setSummary(getString(R.string.app_sort_type_summary,
          getResources().getStringArray(R.array.app_sort_type)[PreferenceManager
              .getDefaultSharedPreferences(getActivity()).getInt(mPrefAppSort.getKey(), 0)]));
      mPrefAppSort.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
          showAppSortDialog(preference);
          return true;
        }
      });

      findPreference("show_log")
          .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              showLog();
              return true;
            }
          });

      findPreference("close_server")
          .setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
              closeServer();
              return true;
            }
          });

      findPreference("pref_app_daynight_mode")
          .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
              // 此时新值还没写入 SharedPreferences，所以直接用 newValue；AppCompat 会重建所有页面
              AppCompatDelegate.setDefaultNightMode(Boolean.TRUE.equals(newValue)
                  ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
              return true;
            }
          });
    }

    @Override
    public void onResume() {
      super.onResume();
      updateShizukuStatus();
    }

    @Override
    public void onDestroy() {
      Shizuku.removeRequestPermissionResultListener(mShizukuPermissionListener);
      super.onDestroy();
    }

    private void updateShizukuStatus() {
      if (mShizukuStatus != null && getContext() != null) {
        mShizukuStatus.setSummary(ShizukuHelper.describe(getContext()));
      }
    }

    /** 未运行时打开 Shizuku 主页，已运行但未授权时请求授权 */
    private void onShizukuStatusClick() {
      if (!ShizukuHelper.isRunning()) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(ShizukuHelper.SHIZUKU_URL)));
      } else if (!ShizukuHelper.isGranted() && !ShizukuHelper.requestPermissionIfNeeded()) {
        Toast.makeText(getContext(), R.string.shizuku_grant_in_app, Toast.LENGTH_LONG).show();
      }
    }


    private void setAllPreferencesToAvoidHavingExtraSpace(Preference preference){
      if (preference != null){
        preference.setIconSpaceReserved(false);
        if (preference instanceof PreferenceGroup){
          PreferenceGroup group =  ((PreferenceGroup) preference);
          int count = group.getPreferenceCount();
          for (int i = 0; i < count; i++){
            setAllPreferencesToAvoidHavingExtraSpace(group.getPreference(i));
          }
        }
      }
    }

    @Override
    public void setPreferenceScreen(PreferenceScreen preferenceScreen) {
      super.setPreferenceScreen(preferenceScreen);
      setAllPreferencesToAvoidHavingExtraSpace(preferenceScreen);
    }

    @Override
    protected Adapter onCreateAdapter(PreferenceScreen preferenceScreen) {
      return new PreferenceGroupAdapter(preferenceScreen){
        @SuppressLint("RestrictedApi")
        public void onPreferenceHierarchyChange(Preference preference){
          setAllPreferencesToAvoidHavingExtraSpace(preference);
          super.onPreferenceHierarchyChange(preference);
        }
      };
    }

    private void closeServer() {
      Helper.closeBgServer(getActivity().getApplicationContext()).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread()).subscribe(new SingleObserver<Boolean>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onSuccess(Boolean value) {
          Activity activity = getActivity();
          if (activity != null) {
            Toast.makeText(activity, R.string.bg_closed, Toast.LENGTH_SHORT).show();
          }
        }

        @Override
        public void onError(Throwable e) {

        }
      });
    }

    /**
     * 模板需要当前系统的 op 表。缓存命中时直接在后台整理列表；未命中时要连接服务，
     * 先显示加载框，失败时按原因提示，不影响设置页的其他功能。
     */
    private void showPremissionTemplete() {
      final Context context = getActivity().getApplicationContext();
      final AlertDialog loading = OpsTable.current() != null ? null
          : new AlertDialog.Builder(getActivity())
              .setTitle(R.string.auto_ignore_permission_title)
              .setView(R.layout.dialog_loading)
              .setNegativeButton(android.R.string.cancel, null)
              .show();

      Single.fromCallable(new Callable<List<OpEntryInfo>>() {
        @Override
        public List<OpEntryInfo> call() throws Exception {
          OpsTable table = AppOpsx.getInstance(context).getOpsTable();
          return Helper.getLocalOpEntryInfos(context, table);
        }
      }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new SingleObserver<List<OpEntryInfo>>() {
            @Override
            public void onSubscribe(Disposable d) {
              if (loading != null) {
                loading.setOnCancelListener(dialog -> d.dispose());
                loading.setOnDismissListener(dialog -> d.dispose());
              }
            }

            @Override
            public void onSuccess(List<OpEntryInfo> infos) {
              if (loading != null) {
                loading.setOnDismissListener(null);
                loading.dismiss();
              }
              if (isAdded()) {
                showPremissionTemplete(infos);
              }
            }

            @Override
            public void onError(Throwable e) {
              if (loading != null) {
                loading.setOnDismissListener(null);
                loading.dismiss();
              }
              if (isAdded()) {
                showTemplateError(e);
              }
            }
          });
    }

    private void showTemplateError(Throwable e) {
      String msg = ShizukuHelper.errorMessage(getContext(), e);
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
          .setTitle(R.string.auto_ignore_permission_title)
          .setMessage(msg != null ? msg : String.valueOf(e.getMessage()))
          .setNegativeButton(android.R.string.cancel, null);
      // 只有 Shizuku 没运行或没授权时，跳到 Shizuku 那一项才有用
      if (!ShizukuHelper.isGranted()) {
        builder.setPositiveButton(R.string.shizuku, (dialog, which) -> onShizukuStatusClick());
      }
      builder.show();
    }

    private void showPremissionTemplete(List<OpEntryInfo> localOpEntryInfos) {

      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.auto_ignore_permission_title);
      // 列表下标 i 就是 op 编号，opEntry 里存的是它的开关 op。多个 op 共用一个开关时，
      // 对话框里只显示一行（用开关 op 自己的名称），保存时展开成组内所有 op，
      // 这样保存的格式（op 编号列表）和以前一致，已有的模板和 default_ignored 都不受影响
      final List<List<Integer>> rows = new ArrayList<>();
      List<CharSequence> labels = new ArrayList<>();
      SparseIntArray switchToRow = new SparseIntArray();
      for (int op = 0; op < localOpEntryInfos.size(); op++) {
        OpEntryInfo opEntryInfo = localOpEntryInfos.get(op);
        int switchOp = opEntryInfo.opEntry.getOp();
        int row = switchToRow.get(switchOp, -1);
        if (row < 0) {
          row = rows.size();
          switchToRow.put(switchOp, row);
          rows.add(new ArrayList<Integer>());
          labels.add(opEntryInfo.opPermsLab);
        }
        rows.get(row).add(op);
        if (op == switchOp) {
          labels.set(row, opEntryInfo.opPermsLab);
        }
      }
      CharSequence[] items = labels.toArray(new CharSequence[0]);

      boolean[] selected = new boolean[rows.size()];
      initCheckd(rows, selected);

      final SparseBooleanArray choiceResult = new SparseBooleanArray();
      for (int i = 0; i < selected.length; i++) {
        choiceResult.put(i, selected[i]);
      }

      saveChoice(rows, choiceResult);

      builder
          .setMultiChoiceItems(items, selected, new DialogInterface.OnMultiChoiceClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
              choiceResult.put(which, isChecked);
            }
          });
      builder.setNegativeButton(android.R.string.cancel, null);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          saveChoice(rows, choiceResult);
        }
      });
      builder.show();
    }

    /** 组内任意一个 op 在已保存的模板里，这一行就算选中 */
    private void initCheckd(List<List<Integer>> rows, boolean[] localChecked) {
      SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
      String result = sp
          .getString("auto_perm_templete", getActivity().getString(R.string.default_ignored));
      SparseIntArray saved = new SparseIntArray();
      for (String s : result.split(",")) {
        try {
          int op = Integer.parseInt(s);
          saved.put(op, op);
        } catch (NumberFormatException e) {
          e.printStackTrace();
        }
      }
      for (int row = 0; row < rows.size(); row++) {
        for (int op : rows.get(row)) {
          if (saved.indexOfKey(op) >= 0) {
            localChecked[row] = true;
            break;
          }
        }
      }
    }

    private void saveChoice(List<List<Integer>> rows, SparseBooleanArray choiceResult) {
      StringBuilder sb = new StringBuilder();
      for (int row = 0; row < rows.size(); row++) {
        if (choiceResult.get(row)) {
          for (int op : rows.get(row)) {
            sb.append(op).append(',');
          }
        }
      }
      String s = sb.toString();
      if (!TextUtils.isEmpty(s)) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
        sp.edit().putString("auto_perm_templete", s).apply();
      }
    }

    private void showTextDialog(int title, String text) {
      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setTitle(title);
      builder.setMessage(text);
      builder.setPositiveButton(android.R.string.ok, null);
      builder.show();
    }

    private void showAppSortDialog(final Preference preference) {
      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.app_sort_type_title);

      final int[] selected = new int[1];
      selected[0] = PreferenceManager.getDefaultSharedPreferences(getActivity())
          .getInt(preference.getKey(), 0);
      builder.setSingleChoiceItems(R.array.app_sort_type, selected[0],
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              selected[0] = which;
            }
          });

      builder.setNegativeButton(android.R.string.cancel, null);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
              .putInt(preference.getKey(), selected[0]).apply();
          mPrefAppSort.setSummary(getString(R.string.app_sort_type_summary,
              getResources().getStringArray(R.array.app_sort_type)[selected[0]]));
        }
      });
      builder.show();
    }

    private void showLanguageDialog(final Preference preference) {
      AlertDialog.Builder builder =
          new AlertDialog.Builder(getActivity());
      builder.setTitle(R.string.app_language);

      final int[] selected = new int[1];
      int defSelected = LangHelper.getLocalIndex(getContext());

      builder.setSingleChoiceItems(R.array.languages, defSelected,
          new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              selected[0] = which;
            }
          });

      builder.setNegativeButton(android.R.string.cancel, null);
      builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          int index = selected[0];
          String language = null;
          if(index == 0){
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .remove(preference.getKey()).apply();
            language = "auto";
          }else {
            language = getResources().getStringArray(R.array.languages_key)[index];
            PreferenceManager.getDefaultSharedPreferences(getActivity()).edit()
                .putString(preference.getKey(), language).apply();
          }
          preference.setSummary(getResources().getStringArray(R.array.languages)[index]);
          switchLanguage();
        }
      });
      builder.show();
    }


    private void switchLanguage() {
      LangHelper.updateLanguage(getContext());
      LangHelper.updateLanguage(getContext().getApplicationContext());

      Intent it = new Intent(getActivity(), MainActivity.class);
      it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      getActivity().startActivity(it);
      getActivity().finish();
    }

    private void showLog() {
      SingleJust.create(new SingleOnSubscribe<String>() {
        @Override
        public void subscribe(SingleEmitter<String> e) throws Exception {
          e.onSuccess(AppOpsx.readLogs(getActivity()));
        }
      }).subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(new SingleObserver<String>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(String value) {
              showTextDialog(R.string.show_log, value);
            }

            @Override
            public void onError(Throwable e) {

            }
          });

    }

    private void showVersion() {
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse("market://details?id=com.zzzmode.appopsx"));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      if (!getContext().getPackageManager().queryIntentActivities(intent, 0).isEmpty()) {
        startActivity(intent);
      } else {
        intent.setData(Uri.parse("https://github.com/nlifew/AppOpsX"));
        startActivity(intent);
      }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
      String key = preference.getKey();

      if ("opensource_licenses".equals(key)) {
        Intent intent = new Intent(getContext(), HtmlActionActivity.class);
        intent.putExtra(Intent.EXTRA_TITLE, preference.getTitle());
        intent.putExtra(HtmlActionActivity.EXTRA_URL, "file:///android_res/raw/licenses.html");
        getActivity().startActivity(intent);
      } else if ("help".equals(key)) {

        Intent intent = new Intent(getContext(), HtmlActionActivity.class);
        intent.putExtra(Intent.EXTRA_TITLE, preference.getTitle());
        intent.putExtra(HtmlActionActivity.EXTRA_URL, "file:///android_res/raw/help.html");
        getActivity().startActivity(intent);
      } else if ("pref_app_language".equals(key)) {

        showLanguageDialog(preference);
      } else if ("shizuku_status".equals(key)) {

        onShizukuStatusClick();
      }
      return false;
    }
  }
}
