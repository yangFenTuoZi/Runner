package yangfentuozi.runner.ui.fragment.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import rikka.core.util.ResourceUtils
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference
import rikka.recyclerview.fixEdgeEffect
import yangfentuozi.runner.App
import yangfentuozi.runner.BuildConfig
import yangfentuozi.runner.R
import yangfentuozi.runner.Runner
import yangfentuozi.runner.base.BaseDialogBuilder
import yangfentuozi.runner.base.BaseDialogBuilder.DialogShowingException
import yangfentuozi.runner.base.BaseFragment
import yangfentuozi.runner.databinding.DialogEditBinding
import yangfentuozi.runner.databinding.DialogPickBackupBinding
import yangfentuozi.runner.databinding.FragmentSettingsBinding
import yangfentuozi.runner.service.ServiceImpl
import yangfentuozi.runner.ui.activity.MainActivity
import yangfentuozi.runner.ui.activity.envmanage.EnvManageActivity
import yangfentuozi.runner.util.ThemeUtil
import yangfentuozi.runner.util.UpdateUtil
import yangfentuozi.runner.util.UpdateUtil.UpdateException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class SettingsFragment : BaseFragment() {
    private var binding: FragmentSettingsBinding? = null
    private var lastArg: BooleanArray? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View? = binding!!.getRoot()
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                .add(R.id.setting_container, PreferenceFragment()).commitNow()
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    class PreferenceFragment : PreferenceFragmentCompat() {
        private var mMainActivity: MainActivity? = null
        private var mParentFragment: SettingsFragment? = null

        override fun onAttach(context: Context) {
            super.onAttach(context)
            if (parentFragment is SettingsFragment) mParentFragment =
                parentFragment as SettingsFragment?
        }

        override fun onDetach() {
            super.onDetach()
            mParentFragment = null
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            mMainActivity = activity as MainActivity
            addPreferencesFromResource(R.xml.preference_setting)


            findPreference<Preference?>("dark_theme")?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                if (App.getPreferences()
                        .getString(
                            "dark_theme",
                            ThemeUtil.MODE_NIGHT_FOLLOW_SYSTEM
                        )!! != newValue
                ) {
                    AppCompatDelegate.setDefaultNightMode(ThemeUtil.getDarkTheme((newValue as String?)!!))
                }
                true
            }

            findPreference<Preference?>("black_dark_theme")?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                if (mMainActivity != null && ResourceUtils.isNightMode(resources.configuration)) {
                    mMainActivity!!.recreate()
                }
                true
            }

            val primaryColor = findPreference<Preference?>("theme_color")
            primaryColor?.setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                mMainActivity?.recreate()
                true
            }

            findPreference<MaterialSwitchPreference?>("follow_system_accent")?.apply {
                if (DynamicColors.isDynamicColorAvailable()) {
                    primaryColor?.isVisible = !isChecked
                    isVisible = true
                    setOnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                        mMainActivity?.recreate()
                        true
                    }
                }
            }

            findPreference<Preference?>("ver")?.apply {
                title = getString(
                    R.string.ver_info,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
                setOnPreferenceClickListener {
                    Thread {
                        if (mMainActivity!!.isDialogShowing) return@Thread
                        try {
                            val updateInfo = UpdateUtil.update(
                                (findPreference<Preference?>("update_channel") as SimpleMenuPreference)
                                    .value == resources.getStringArray(R.array.update_channel_values)[1]
                            )
                            if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
                                mMainActivity!!.runOnUiThread {
                                    try {
                                        BaseDialogBuilder(mMainActivity!!)
                                            .setTitle(R.string.check_update)
                                            .setMessage(
                                                getString(
                                                    R.string.check_update_msg,
                                                    BuildConfig.VERSION_NAME,
                                                    BuildConfig.VERSION_CODE,
                                                    updateInfo.versionName,
                                                    updateInfo.versionCode,
                                                    updateInfo.updateMsg
                                                )
                                            )
                                            .setPositiveButton(R.string.check_update) { dialog, which -> }
                                            .show()
                                    } catch (_: DialogShowingException) {
                                    }
                                }
                            } else {
                                mMainActivity!!.runOnUiThread {
                                    Toast.makeText(
                                        mMainActivity,
                                        R.string.it_is_latest_version,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } catch (e: UpdateException) {
                            mMainActivity!!.runOnUiThread {
                                Toast.makeText(
                                    mMainActivity, when (e.what) {
                                        UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER -> R.string.connect_update_server_error
                                        UpdateException.WHAT_CAN_NOT_PARSE_JSON -> R.string.parse_json_error
                                        UpdateException.WHAT_JSON_FORMAT_ERROR -> R.string.json_format_error
                                        else -> R.string.error
                                    }, Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }.start()
                    true
                }
            }
            val help = findPreference<Preference?>("help")
            help?.setOnPreferenceClickListener {
                try {
                    BaseDialogBuilder(mMainActivity!!)
                        .setTitle(R.string.help)
                        .setMessage("没做")
                        .show()
                } catch (_: DialogShowingException) {
                }
                true
            }
            val exportData = findPreference<Preference?>("export_data")
            exportData?.setOnPreferenceClickListener {
                try {
                    val binding = DialogPickBackupBinding.inflate(layoutInflater)
                    val restricted = if (!Runner.pingServer()) {
                        binding.dataDb.isEnabled = false
                        binding.termHome.isEnabled = false
                        binding.termUsr.isEnabled = false
                        true
                    } else false
                    BaseDialogBuilder(mMainActivity!!)
                        .setTitle(R.string.select_backup_data)
                        .setView(binding.root)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            val backupAppSettings = binding.appSettings.isChecked
                            val backupDataDb = binding.dataDb.isChecked
                            val backupTermHome = binding.termHome.isChecked
                            val backupTermUsr = binding.termUsr.isChecked
                            mParentFragment?.apply {
                                lastArg = booleanArrayOf(
                                    restricted,
                                    backupAppSettings,
                                    backupDataDb,
                                    backupTermHome,
                                    backupTermUsr
                                )
                                saveFileLauncher.launch(Unit)
                            }
                        }
                        .show()
                } catch (_: DialogShowingException) {
                }
                true
            }
            val importData = findPreference<Preference?>("import_data")
            importData?.setOnPreferenceClickListener {
                mParentFragment?.pickFileLauncher?.launch(arrayOf("application/zip"))
                true
            }
            findPreference<Preference>("env_configs")?.setOnPreferenceClickListener {
                startActivity(Intent(mMainActivity, EnvManageActivity::class.java))
                true
            }
            findPreference<Preference>("startup_script")?.setOnPreferenceClickListener {
                val sharedPreferences = App.getPreferences()
                val dialogBinding = DialogEditBinding.inflate(LayoutInflater.from(mMainActivity))

                dialogBinding.apply {
                    command.setText(sharedPreferences.getString("startup_script_command", ""))
                    reducePerm.isChecked =
                        sharedPreferences.getBoolean("startup_script_reduce_perm", false) == true
                    targetPerm.setText(
                        sharedPreferences.getString(
                            "startup_script_target_perm",
                            ""
                        )
                    )

                    name.apply {
                        isEnabled = false
                        parent.let {
                            if (it is ViewGroup)
                                it.visibility = View.GONE
                        }
                    }

                    keepAlive.apply {
                        isEnabled = false
                        parent.let {
                            if (it is ViewGroup)
                                it.visibility = View.GONE
                        }
                    }
                    targetPermParent.visibility = View.GONE
                    reducePerm.setOnCheckedChangeListener { _, isChecked ->
                        targetPermParent.visibility = if (isChecked) View.VISIBLE else View.GONE
                    }

                    name.requestFocus()
                    name.postDelayed({
                        (mMainActivity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showSoftInput(name, InputMethodManager.SHOW_IMPLICIT)
                    }, 200)
                }

                try {
                    BaseDialogBuilder(mMainActivity!!)
                        .setTitle(R.string.edit)
                        .setView(dialogBinding.root)
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            sharedPreferences.edit()?.apply {
                                putString(
                                    "startup_script_command",
                                    dialogBinding.command.text.toString()
                                )
                                putBoolean(
                                    "startup_script_reduce_perm",
                                    dialogBinding.reducePerm.isChecked
                                )
                                putString(
                                    "startup_script_target_perm",
                                    if (dialogBinding.reducePerm.isChecked) dialogBinding.targetPerm.text.toString() else null
                                )
                                apply()
                            }
                        }
                        .show()
                } catch (_: DialogShowingException) {
                }
                true
            }
        }

        override fun onCreateRecyclerView(
            inflater: LayoutInflater,
            parent: ViewGroup,
            savedInstanceState: Bundle?
        ): RecyclerView {
            val recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState)
            recyclerView.fixEdgeEffect(false, true)
            val l = View.OnClickListener { v: View? -> recyclerView.smoothScrollToPosition(0) }
            mParentFragment!!.toolbar.setOnClickListener(l)
            return recyclerView
        }
    }

    open class CreateBackup : ActivityResultContract<Unit, Uri?>() {

        @CallSuper
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("application/zip")
                .putExtra(Intent.EXTRA_TITLE, "runner_backup_${System.currentTimeMillis()}.zip")
        }

        final override fun getSynchronousResult(
            context: Context,
            input: Unit
        ): SynchronousResult<Uri?>? = null

        final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            return intent.takeIf { resultCode == Activity.RESULT_OK }?.data
        }
    }

    private val saveFileLauncher =
        registerForActivityResult(CreateBackup()) { uri ->
            if (uri == null || lastArg == null) return@registerForActivityResult
            Thread {
                val arg: BooleanArray = lastArg!!
                val backupTmpDir = File(mContext.externalCacheDir, "backupData")
                ServiceImpl.rmRF(backupTmpDir)
                backupTmpDir.mkdirs()
                if (!arg[0])
                    Runner.service?.backupData(backupTmpDir.absolutePath, arg[2], arg[3], arg[4])

                if (arg[1]) {
                    val method = PreferenceManager::class.java.getDeclaredMethod(
                        "getDefaultSharedPreferencesName",
                        Context::class.java
                    )
                    method.isAccessible = true
                    val settingsName = method.invoke(null, mContext) as String?
                    if (settingsName != null) {
                        val prefsInput =
                            FileInputStream(mContext.applicationInfo.dataDir + "/shared_prefs/$settingsName.xml")
                        val prefsOutput = FileOutputStream(
                            "${backupTmpDir.absolutePath}/settings.xml"
                        )
                        prefsInput.copyTo(prefsOutput, bufferSize = ServiceImpl.PAGE_SIZE)
                        prefsInput.close()
                        prefsOutput.close()
                    }
                }
                val output = mContext.contentResolver.openOutputStream(uri)
                TarArchiveOutputStream(
                    GzipCompressorOutputStream(output)
                ).use { taos ->
                    taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
                    ServiceImpl.tarGzFileRecursive(backupTmpDir, backupTmpDir, taos)
                }
            }.start()
        }

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) {
        }
}
