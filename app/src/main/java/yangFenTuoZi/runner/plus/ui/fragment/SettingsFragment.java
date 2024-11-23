package yangFenTuoZi.runner.plus.ui.fragment;

import static android.app.Activity.RESULT_OK;

import static yangFenTuoZi.runner.plus.server.Logger.getStackTraceString;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONException;
import org.json.JSONObject;

import rikka.core.util.ResourceUtils;
import yangFenTuoZi.runner.plus.App;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.Utils;
import yangFenTuoZi.runner.plus.databinding.FragmentSettingsBinding;

import yangFenTuoZi.runner.plus.info.Info;
import yangFenTuoZi.runner.plus.server.Server;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

import rikka.preference.SimpleMenuPreference;
import rikka.recyclerview.RecyclerViewKt;
import yangFenTuoZi.runner.plus.ui.dialog.ExecDialog;

public class SettingsFragment extends BaseFragment {

    private FragmentSettingsBinding binding;
    private static final int ACTION_BACKUP_CHOOSE_FILE = 442;
    private static final int ACTION_RESTORE_CHOOSE_FILE = 443;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        binding.appBar.setLiftable(true);
        setupToolbar(binding.toolbar, binding.clickView, R.string.title_settings);
        binding.toolbar.setNavigationIcon(null);
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case ACTION_BACKUP_CHOOSE_FILE: {
                    new Thread(() -> {
                        Uri uri = resultData.getData();
                        JSONObject json = new JSONObject();
                        try {
                            json.put("APP_PACKAGE_NAME", Info.APPLICATION_ID);
                            json.put("APP_VERSION_NAME", Info.VERSION_NAME);
                            json.put("APP_VERSION_CODE", String.valueOf(Info.VERSION_CODE));
                        } catch (JSONException ignored) {
                        }
                        try {
                            AtomicBoolean br = new AtomicBoolean(false);
                            int port = ExecDialog.getUsablePort(8400);
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
                            GZIPOutputStream gzip = new GZIPOutputStream(bos);
                            new Thread(() -> {
                                try {
                                    ServerSocket serverSocket = new ServerSocket(port);
                                    Socket socket = serverSocket.accept();
                                    BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
                                    int len;
                                    byte[] b = new byte[1024];
                                    while ((len = in.read(b)) != -1) {
                                        gzip.write(b, 0, len);
                                        bos1.write(b, 0, len);
                                    }
                                    gzip.close();
                                    bos1.close();
                                    in.close();
                                    socket.close();
                                } catch (IOException ignored) {
                                }
                                br.set(true);
                            }).start();
                            String sha256 = App.iService.readDatabase(port);
                            while (!br.get()) ;
                            String _sha256 = Server.getSHA256(bos1.toByteArray());
                            if (_sha256 != null && sha256.toLowerCase().replaceAll(" ", "").equals(_sha256.toLowerCase().replaceAll(" ", ""))) {
                                json.put("APP_DATABASE", new String(Base64.getEncoder().encode(bos.toByteArray())));
                            }
                        } catch (Exception ignored) {
                        }
                        try {
                            ParcelFileDescriptor pfd = mContext.getContentResolver().openFileDescriptor(uri, "w");
                            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                            fileOutputStream.write(json.toString().getBytes());
                            fileOutputStream.close();
                            pfd.close();
                        } catch (IOException e) {
                            runOnUiThread(() -> new MaterialAlertDialogBuilder(mContext)
                                    .setTitle("Write error!")
                                    .setMessage(getStackTraceString(e))
                                    .show());
                        }
                    }).start();
                }
                case ACTION_RESTORE_CHOOSE_FILE: {

                }
            }
        }
    }

    public static class PreferenceFragment extends PreferenceFragmentCompat {

        private MainActivity mContext;
        private SettingsFragment mParentFragment;

        @Override
        public void onAttach(@NonNull Context context) {
            super.onAttach(context);
            mParentFragment = (SettingsFragment) requireParentFragment();
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
                    short oldIsDark = mContext.mApp.isDark;
                    mContext.mApp.isDark = switch ((String) newValue) {
                        case "MODE_NIGHT_FOLLOW_SYSTEM" ->
                                ResourceUtils.isNightMode(getResources().getConfiguration()) ? (short) 1 : (short) 0;
                        case "MODE_NIGHT_YES" -> 1;
                        default -> 0;
                    };
                    if (oldIsDark != mContext.mApp.isDark) {
                        mContext.mApp.setTheme(mContext.mApp.isDark == 1 ? R.style.Theme : R.style.Theme_Light);
                        mContext.recreate();
                    }
                    return true;
                });
            }

            Preference ver = findPreference("ver");
            if (ver != null) {
                ver.setTitle(getString(R.string.settings_ver, Info.VERSION_NAME, Info.VERSION_CODE));
                ver.setOnPreferenceClickListener(preference -> {
                    if (mContext.isDialogShow)
                        return true;
                    try {
                        Utils.UpdateUtils.UpdateInfo updateInfo = Utils.UpdateUtils.Update(
                                ((SimpleMenuPreference) findPreference("update_channel"))
                                        .getValue().equals(getResources().getStringArray(R.array.update_channel_values)[1]));
                        if (updateInfo.version_code > Info.VERSION_CODE) {
                            mContext.isDialogShow = true;
                            new MaterialAlertDialogBuilder(mContext)
                                    .setTitle(R.string.settings_check_update)
                                    .setMessage(getString(R.string.settings_check_update_msg,
                                            Info.VERSION_NAME, Info.VERSION_CODE,
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
                export_data.setOnPreferenceClickListener(preference -> {
                    if (mContext.isDialogShow)
                        return true;
                    mContext.isDialogShow = true;
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    intent.putExtra(Intent.EXTRA_TITLE, "runner_data.json");
                    mParentFragment.startActivityForResult(intent, ACTION_BACKUP_CHOOSE_FILE);
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
                    mParentFragment.startActivityForResult(intent, ACTION_RESTORE_CHOOSE_FILE);
                    return true;
                });
            }
        }

        @NonNull
        @Override
        public RecyclerView onCreateRecyclerView(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, Bundle savedInstanceState) {
            RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
            RecyclerViewKt.fixEdgeEffect(recyclerView, false, true);
//            recyclerView.getBorderViewDelegate().setBorderVisibilityChangedListener((top, oldTop, bottom, oldBottom) -> mParentFragment.binding.appBar.setLifted(!top));
            var fragment = getParentFragment();
            if (fragment instanceof SettingsFragment settingsFragment) {
                View.OnClickListener l = v -> {
                    settingsFragment.binding.appBar.setExpanded(true, true);
                    recyclerView.smoothScrollToPosition(0);
                };
                settingsFragment.binding.toolbar.setOnClickListener(l);
                settingsFragment.binding.clickView.setOnClickListener(l);
            }
            return recyclerView;
        }
    }
}