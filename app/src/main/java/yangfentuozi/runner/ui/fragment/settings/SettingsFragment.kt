package yangfentuozi.runner.ui.fragment.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import rikka.core.util.ResourceUtils
import rikka.material.preference.MaterialSwitchPreference
import rikka.preference.SimpleMenuPreference
import rikka.recyclerview.fixEdgeEffect
import yangfentuozi.runner.App
import yangfentuozi.runner.BuildConfig
import yangfentuozi.runner.R
import yangfentuozi.runner.base.BaseDialogBuilder
import yangfentuozi.runner.base.BaseDialogBuilder.DialogShowingException
import yangfentuozi.runner.base.BaseFragment
import yangfentuozi.runner.databinding.DialogEditBinding
import yangfentuozi.runner.databinding.FragmentSettingsBinding
import yangfentuozi.runner.ui.activity.MainActivity
import yangfentuozi.runner.ui.activity.envmanage.EnvManageActivity
import yangfentuozi.runner.util.ThemeUtil
import yangfentuozi.runner.util.UpdateUtil
import yangfentuozi.runner.util.UpdateUtil.UpdateException


class SettingsFragment : BaseFragment() {
    private var binding: FragmentSettingsBinding? = null

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
                if (mMainActivity!!.isDialogShowing) true
                mMainActivity!!.isDialogShowing = true
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("application/json")
                intent.putExtra(Intent.EXTRA_TITLE, "runner_data.json")
                true
            }
            val importData = findPreference<Preference?>("import_data")
            importData?.setOnPreferenceClickListener {
                if (mMainActivity!!.isDialogShowing) true
                mMainActivity!!.isDialogShowing = true
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.setType("application/json")
                true
            }
            findPreference<Preference>("env_configs")?.setOnPreferenceClickListener {
                startActivity(Intent(mMainActivity, EnvManageActivity::class.java))
                true
            }
            findPreference<Preference>("startup_script")?.setOnPreferenceClickListener {
                val sharedPreferences = mMainActivity?.getSharedPreferences("startup_script", Context.MODE_PRIVATE)
                val dialogBinding = DialogEditBinding.inflate(LayoutInflater.from(mMainActivity))

                dialogBinding.apply {
                    command.setText(sharedPreferences?.getString("command", ""))
                    reducePerm.isChecked = sharedPreferences?.getBoolean("reduce_perm", false) == true
                    targetPerm.setText(sharedPreferences?.getString("target_perm", ""))

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
                            sharedPreferences?.edit()?.apply {
                                putString("command", dialogBinding.command.text.toString())
                                putBoolean("reduce_perm", dialogBinding.reducePerm.isChecked)
                                putString("target_perm", if (dialogBinding.reducePerm.isChecked) dialogBinding.targetPerm.text.toString() else null)
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
}