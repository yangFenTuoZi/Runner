package com.shizuku.runner.plus.ui.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.shizuku.runner.plus.BuildConfig;
import com.shizuku.runner.plus.R;
import com.shizuku.runner.plus.Utils;
import com.shizuku.runner.plus.databinding.FragmentSettingsBinding;

import java.util.Objects;

import rikka.core.util.ResourceUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    private FragmentSettingsBinding binding;
    private Context mContext;

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentSettingsBinding.inflate(getLayoutInflater());
        mContext = getContext();
        addPreferencesFromResource(R.xml.preference_setting);
        Preference ver = findPreference("ver");
        if (ver != null) {
            ver.setTitle(getString(R.string.settings_ver) + " " + BuildConfig.VERSION_NAME + "(" + BuildConfig.VERSION_CODE + ")");
        }
        Preference help = findPreference("help");
        if (help != null) {
            help.setOnPreferenceClickListener((preference) -> {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.settings_help)
                        .setMessage("没做")
                        .show();
                return true;
            });
        }
        Preference export_data = findPreference("export_data");
        if (export_data != null) {
            String json = Utils.Encode.encode(Utils.BackupUtils.backup(mContext).toString());
            export_data.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.settings_export_data)
                        .setMessage(json)
                        .setPositiveButton(R.string.settings_copy, (dialog, which) -> ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", json)))
                        .show();
                return true;
            });
        }
        Preference import_data = findPreference("import_data");
        if (import_data != null) {
            import_data.setOnPreferenceClickListener(preference -> {
                new MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.settings_import_data)
                        .setView(R.layout.dialog_import)
                        .setPositiveButton(R.string.settings_import_data, (dialog, which) -> {
                            TextInputEditText editText = ((AlertDialog) dialog).findViewById(R.id.dialog_data);
                            if (editText != null) {
                                String json = Objects.requireNonNull(editText.getText()).toString();
                                try {
                                    Utils.BackupUtils.restore(mContext, JSONObject.parseObject(Utils.Encode.decode(json)), true);
                                } catch (JSONException jsonException) {
                                    Toast.makeText(mContext, R.string.settings_can_not_parse_json_data, Toast.LENGTH_SHORT).show();
                                } catch (Utils.BackupUtils.RestoreException restoreException) {
                                    Toast.makeText(mContext, switch (restoreException.getWhat()) {
                                        case Utils.BackupUtils.RestoreException.WHAT_VER_IS_LOW -> R.string.settings_ver_is_low;
                                        case Utils.BackupUtils.RestoreException.WHAT_IS_NOT_APP_DATA -> R.string.settings_is_not_app_data;
                                        case Utils.BackupUtils.RestoreException.WHAT_DATA_ERROR -> R.string.settings_data_error;
                                        case Utils.BackupUtils.RestoreException.WHAT_JSON_PARSE_ERROR -> R.string.settings_can_not_parse_json_data;
                                        default -> R.string.exec_other_error;
                                    }, Toast.LENGTH_SHORT).show();
                                } catch (ArrayIndexOutOfBoundsException e) {
                                    Toast.makeText(mContext, R.string.settings_decode_error, Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .show();
                return true;
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = requireActivity().getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        if (ResourceUtils.isNightMode(getResources().getConfiguration())) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }
}