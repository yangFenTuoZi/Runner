package yangfentuozi.runner.app.ui.fragment.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import rikka.core.util.ResourceUtils
import rikka.material.preference.MaterialSwitchPreference
import rikka.recyclerview.fixEdgeEffect
import yangfentuozi.runner.R
import yangfentuozi.runner.app.App
import yangfentuozi.runner.app.Runner
import yangfentuozi.runner.app.base.BaseDialogBuilder
import yangfentuozi.runner.app.base.BaseFragment
import yangfentuozi.runner.app.data.BackupManager
import yangfentuozi.runner.app.ui.activity.MainActivity
import yangfentuozi.runner.app.ui.activity.envmanage.EnvManageActivity
import yangfentuozi.runner.app.util.ThemeUtil
import yangfentuozi.runner.databinding.DialogPickBackupBinding
import yangfentuozi.runner.databinding.FragmentSettingsBinding


class SettingsFragment : BaseFragment() {
    private var binding: FragmentSettingsBinding? = null
    private var lastArg: BooleanArray? = null
    private var childFragment: PreferenceFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View? = binding!!.getRoot()
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                .add(R.id.setting_container, PreferenceFragment().apply { childFragment = this })
                .commitNow()
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
                if (App.Companion.preferences
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

            findPreference<Preference?>("about")?.apply {
                setOnPreferenceClickListener {
                    mMainActivity?.showAbout()
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
                } catch (_: BaseDialogBuilder.DialogShowingException) {
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
                } catch (_: BaseDialogBuilder.DialogShowingException) {
                }
                true
            }
            val importData = findPreference<Preference?>("import_data")
            importData?.setOnPreferenceClickListener {
                mParentFragment?.pickFileLauncher?.launch(arrayOf("application/x-tar"))
                true
            }
            findPreference<Preference>("env_configs")?.setOnPreferenceClickListener {
                startActivity(Intent(mMainActivity, EnvManageActivity::class.java))
                true
            }
        }

        override fun onCreateRecyclerView(
            inflater: LayoutInflater,
            parent: ViewGroup,
            savedInstanceState: Bundle?
        ): RecyclerView {
            return super.onCreateRecyclerView(inflater, parent, savedInstanceState).apply {
                fixEdgeEffect(overScrollIfContentScrolls = false)
                mParentFragment!!.toolbar.setOnClickListener { smoothScrollToPosition(0) }
            }
        }
    }

    open class CreateBackup : ActivityResultContract<Unit, Uri?>() {

        @CallSuper
        override fun createIntent(context: Context, input: Unit): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT)
                .setType("application/x-tar")
                .putExtra(Intent.EXTRA_TITLE, "runner_backup_${System.currentTimeMillis()}.tar")
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
            val arg: BooleanArray = lastArg!!
            BackupManager.backup(
                requireContext(),
                uri,
                backupAppSettings = arg[1],
                backupDataDb = arg[2],
                backupTermHome = arg[3],
                backupTermUsr = arg[4]
            )
            Toast.makeText(requireContext(), R.string.backup_started, Toast.LENGTH_SHORT).show()
        }

    private val pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@registerForActivityResult
            try {
                BaseDialogBuilder(mMainActivity)
                    .setTitle(R.string.import_data)
                    .setMessage(R.string.import_data_confirm)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        BackupManager.restore(requireContext(), uri) {
                            mMainActivity.runOnUiThread {
                                Toast.makeText(
                                    requireContext(),
                                    R.string.restore_completed,
                                    Toast.LENGTH_SHORT
                                ).show()
                                (mMainActivity.application as App).finishApp()
                            }
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (_: BaseDialogBuilder.DialogShowingException) {
            }
        }
}
