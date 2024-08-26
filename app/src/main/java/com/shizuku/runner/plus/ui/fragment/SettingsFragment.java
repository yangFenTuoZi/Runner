package com.shizuku.runner.plus.ui.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
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
import com.shizuku.runner.plus.databinding.FragmentTerminalBinding;
import com.shizuku.runner.plus.ui.activity.MainActivity;

import java.util.Objects;

import rikka.core.util.ResourceUtils;
import rikka.preference.SimpleMenuPreference;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction().add(R.id.setting_container, new PreferenceFragment()).commitNow();
        }
        return root;
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {

        private MainActivity mContext;

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mContext = (MainActivity) requireContext();
            addPreferencesFromResource(R.xml.preference_setting);
            Preference ver = findPreference("ver");
            if (ver != null) {
                ver.setTitle(getString(R.string.settings_ver, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
                ver.setOnPreferenceClickListener(preference -> {
                    if (mContext.isDialogShow)
                        return true;
                    try {
                        Utils.UpdateUtils.UpdateInfo updateInfo = Utils.UpdateUtils.Update(
                                ((SimpleMenuPreference) findPreference("update_channel"))
                                        .getEntry().equals(getResources().getStringArray(R.array.update_channel_values)[1]));
                        if (updateInfo.version_code > BuildConfig.VERSION_CODE) {
                            mContext.isDialogShow = true;
                            new MaterialAlertDialogBuilder(mContext)
                                    .setTitle(R.string.settings_check_update)
                                    .setMessage(getString(R.string.settings_check_update_msg,
                                            BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                                            updateInfo.version_name, updateInfo.version_code,
                                            updateInfo.update_msg))
                                    .setPositiveButton(R.string.settings_check_update, (dialog, which) -> {
                                    })
                                    .setOnDismissListener(dialog -> mContext.isDialogShow = false)
                                    .show();
                        } else {
                            Toast.makeText(mContext, R.string.settings_latest_version, Toast.LENGTH_SHORT).show();
                        }
                    } catch (Utils.UpdateUtils.UpdateException e) {
                        Toast.makeText(mContext, switch (e.getWhat()) {
                            case Utils.UpdateUtils.UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER ->
                                    R.string.settings_can_not_connect_update_server;
                            case Utils.UpdateUtils.UpdateException.WHAT_CAN_NOT_PARSE_JSON ->
                                    R.string.settings_can_not_parse_json;
                            case Utils.UpdateUtils.UpdateException.WHAT_JSON_FORMAT_ERROR ->
                                    R.string.settings_json_format_error;
                            default -> R.string.settings_error;
                        }, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                });
            }
            Preference help = findPreference("help");
            if (help != null) {
                help.setOnPreferenceClickListener((preference) -> {
                    if (mContext.isDialogShow)
                        return true;
                    mContext.isDialogShow = true;
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.settings_help)
                            .setMessage("没做")
                            .setOnDismissListener(dialog -> mContext.isDialogShow = false)
                            .show();
                    return true;
                });
            }
            Preference export_data = findPreference("export_data");
            if (export_data != null) {
                String json = Utils.Encode.encode(Utils.BackupUtils.backup(mContext, this).toString());
                export_data.setOnPreferenceClickListener(preference -> {
                    if (mContext.isDialogShow)
                        return true;
                    mContext.isDialogShow = true;
                    new MaterialAlertDialogBuilder(mContext)
                            .setTitle(R.string.settings_export_data)
                            .setMessage(json)
                            .setPositiveButton(R.string.settings_copy, (dialog, which) -> ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", json)))
                            .setOnDismissListener(dialog -> mContext.isDialogShow = false)
                            .show();
                    return true;
                });
            }
            Preference import_data = findPreference("import_data");
            if (import_data != null) {
                import_data.setOnPreferenceClickListener(preference -> {
                    if (mContext.isDialogShow)
                        return true;
                    mContext.isDialogShow = true;
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
                                            case Utils.BackupUtils.RestoreException.WHAT_VER_IS_LOW ->
                                                    R.string.settings_ver_is_low;
                                            case Utils.BackupUtils.RestoreException.WHAT_IS_NOT_APP_DATA ->
                                                    R.string.settings_is_not_app_data;
                                            case Utils.BackupUtils.RestoreException.WHAT_DATA_ERROR ->
                                                    R.string.settings_data_error;
                                            case Utils.BackupUtils.RestoreException.WHAT_JSON_PARSE_ERROR ->
                                                    R.string.settings_can_not_parse_json_data;
                                            default -> R.string.exec_other_error;
                                        }, Toast.LENGTH_SHORT).show();
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        Toast.makeText(mContext, R.string.settings_decode_error, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            })
                            .setOnDismissListener(dialog -> mContext.isDialogShow = false)
                            .show();
                    return true;
                });
            }
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