package yangFenTuoZi.runner.plus.ui.fragment.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import rikka.preference.SimpleMenuPreference;
import rikka.recyclerview.RecyclerViewKt;
import yangFenTuoZi.runner.plus.BuildConfig;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.base.BaseFragment;
import yangFenTuoZi.runner.plus.databinding.FragmentSettingsBinding;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.utils.ThemeUtils;
import yangFenTuoZi.runner.plus.utils.UpdateUtils;

public class SettingsFragment extends BaseFragment {

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {

        private MainActivity mContext;
        private SettingsFragment mParentFragment;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            if (getParentFragment() instanceof SettingsFragment settingsFragment)
                mParentFragment = settingsFragment;
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mParentFragment = null;
        }

        @Override
        public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
            mContext = (MainActivity) requireContext();
            addPreferencesFromResource(R.xml.preference_setting);
            SimpleMenuPreference dark_theme = findPreference("dark_theme");
            if (dark_theme != null) {
                dark_theme.setOnPreferenceChangeListener((preference, newValue) -> {
                    int oldIsDark = mContext.mApp.isDark;
                    mContext.mApp.isDark = ThemeUtils.isDark(mContext) ? 1 : 0;
                    if (oldIsDark != mContext.mApp.isDark) {
                        mContext.mApp.setTheme(ThemeUtils.getTheme(mContext.mApp.isDark == 1));
                        mContext.recreate();
                    }
                    return true;
                });
            }

            Preference ver = findPreference("ver");
            if (ver != null) {
                ver.setTitle(getString(R.string.settings_ver, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
                ver.setOnPreferenceClickListener(preference -> {
                    if (mContext.isDialogShow)
                        return true;
                    try {
                        UpdateUtils.UpdateInfo updateInfo = UpdateUtils.Update(
                                ((SimpleMenuPreference) findPreference("update_channel"))
                                        .getValue().equals(getResources().getStringArray(R.array.update_channel_values)[1]));
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
                    } catch (UpdateUtils.UpdateException e) {
                        Toast.makeText(mContext, switch (e.getWhat()) {
                            case UpdateUtils.UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER ->
                                    R.string.settings_can_not_connect_update_server;
                            case UpdateUtils.UpdateException.WHAT_CAN_NOT_PARSE_JSON ->
                                    R.string.settings_can_not_parse_json;
                            case UpdateUtils.UpdateException.WHAT_JSON_FORMAT_ERROR ->
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
                export_data.setOnPreferenceClickListener(preference -> {
                    if (mContext.isDialogShow)
                        return true;
                    mContext.isDialogShow = true;
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, "runner_data.json");
//                    mParentFragment.startActivityForResult(intent, ACTION_BACKUP_CHOOSE_FILE);
                    return true;
                });
            }
            Preference import_data = findPreference("import_data");
            if (import_data != null) {
                import_data.setOnPreferenceClickListener(preference -> {
                    if (mContext.isDialogShow)
                        return true;
                    mContext.isDialogShow = true;
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
//                    mParentFragment.startActivityForResult(intent, ACTION_RESTORE_CHOOSE_FILE);
                    return true;
                });
            }
        }

        @NonNull
        @Override
        public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, Bundle savedInstanceState) {
            RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);
            View.OnClickListener l = v -> recyclerView.smoothScrollToPosition(0);
            mParentFragment.getToolbar().setOnClickListener(l);
            return recyclerView;
        }
    }
}