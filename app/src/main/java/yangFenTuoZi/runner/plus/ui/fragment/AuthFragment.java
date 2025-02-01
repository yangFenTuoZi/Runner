package yangFenTuoZi.runner.plus.ui.fragment;

import static yangFenTuoZi.runner.plus.utils.ExceptionUtils.throwableToDialog;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pokercc.android.expandablerecyclerview.ExpandableRecyclerView;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.auth.AuthAdapter;
import yangFenTuoZi.runner.plus.databinding.DialogAddBinding;
import yangFenTuoZi.runner.plus.databinding.FragmentAuthBinding;

public class AuthFragment extends BaseFragment {
    private SharedPreferences sharedPreferences;
    private PackageManager pm;
    private FragmentAuthBinding binding;
    public SwipeRefreshLayout.OnRefreshListener onRefreshListener = () -> new Handler().postDelayed(() -> {
        initList();
        binding.swipeRefreshLayout.setRefreshing(false);
    }, 1000);

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), permissions  -> {
            if (permissions) initList();
        }).launch("com.android.permission.GET_INSTALLED_APPS");
        binding = FragmentAuthBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(mContext));
        binding.appBar.setLiftable(true);
        setupToolbar(binding.toolbar, binding.clickView, R.string.title_authorization);
        binding.toolbar.setNavigationIcon(null);

        ExpandableRecyclerView recyclerView = binding.recyclerView;
        View.OnClickListener l = v -> {
            binding.appBar.setExpanded(true, true);
            recyclerView.smoothScrollToPosition(0);
        };
        binding.toolbar.setOnClickListener(l);
        binding.clickView.setOnClickListener(l);

        binding.swipeRefreshLayout.setOnRefreshListener(onRefreshListener);

        binding.add.setOnClickListener(item -> {
            DialogAddBinding binding = DialogAddBinding.inflate(LayoutInflater.from(mContext));
            AlertDialog alertDialog = new MaterialAlertDialogBuilder(mContext)
                    .setTitle(R.string.menu_add)
                    .setView(binding.getRoot())
                    .setNegativeButton(R.string.dialog_finish, (v, i) -> {
                        Editable editable = binding.dialogUid.getText();
                        if (editable == null || editable.toString().replaceAll(" ", "").isEmpty())
                            return;
                        SharedPreferences sharedPreferences = mContext.getSharedPreferences("data", 0);
                        Set<String> allow_apps = new HashSet<>(sharedPreferences.getStringSet("allow_apps", new HashSet<>()));
                        allow_apps.add(String.valueOf(Integer.parseInt(editable.toString())));
                        sharedPreferences.edit()
                                .putStringSet("allow_apps", allow_apps)
                                .apply();

                        File file = new File(mContext.getFilesDir(), "apps.json");
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        try {
                            InputStream in = new FileInputStream(file);
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = in.read(buffer)) != -1) {
                                bos.write(buffer, 0, len);
                            }
                            bos.close();
                            in.close();
                        } catch (Exception ignored) {
                        }

//                        {
//                            JSONArray allow_apps;
//                            try {
//                                allow_apps = new JSONArray(bos.toString());
//                            } catch (JSONException e) {
//                                allow_apps = new JSONArray();
//                            }
//                        }
                        initList();
                    })
                    .show();
        });
        pm = mContext.getPackageManager();
        sharedPreferences = mContext.getSharedPreferences("data", 0);

        return binding.getRoot();
    }

    @SuppressLint("WrongConstant")
    public boolean checkQueryAllPackagesPermission() {
        List<PackageInfo> packageInfos = pm.getInstalledPackages(0);
        return packageInfos.size() != 1 || !packageInfos.get(0).packageName.equals(mContext.getPackageName());
    }

    public void initList() {
        if (!checkQueryAllPackagesPermission()) {
            runOnUiThread(() -> {
                try {
                    PermissionInfo permissionInfo = mContext.getPackageManager().getPermissionInfo("com.android.permission.GET_INSTALLED_APPS", 0);
                    if (permissionInfo != null && permissionInfo.packageName.equals("com.lbe.security.miui")) {
                        if (ContextCompat.checkSelfPermission(mContext, "com.android.permission.GET_INSTALLED_APPS") != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(mContext, new String[]{"com.android.permission.GET_INSTALLED_APPS"}, 111);
                        }
                    }
                } catch (Throwable e) {
                    throwableToDialog(mContext, e);
                }
            });
            return;
        }
        new Thread(() -> {
            try {
                Set<String> allow_apps = new ArraySet<>(sharedPreferences.getStringSet("allow_apps", new ArraySet<>()));
                String[] allow_apps_ = allow_apps.toArray(new String[0]);

                AuthAdapter.AuthData[] result = new AuthAdapter.AuthData[allow_apps_.length];
                for (int i = 0; i < allow_apps_.length; i++) {
                    result[i] = new AuthAdapter.AuthData(Integer.parseInt(allow_apps_[i]), true, pm);
                }
                runOnUiThread(() -> binding.recyclerView.setAdapter(new AuthAdapter(mContext, result, this)));
            } catch (Exception e) {
                throwableToDialog(mContext, e);
            }
        }).start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public FragmentAuthBinding getBinding() {
        return binding;
    }

    @Override
    public void onStart() {
        super.onStart();
        initList();
    }
}