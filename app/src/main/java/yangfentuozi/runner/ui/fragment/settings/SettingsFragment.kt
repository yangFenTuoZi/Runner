package yangfentuozi.runner.ui.fragment.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import yangfentuozi.runner.base.BaseDialogBuilder.DialogShowException
import yangfentuozi.runner.base.BaseFragment
import yangfentuozi.runner.databinding.FragmentSettingsBinding
import yangfentuozi.runner.ui.activity.MainActivity
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


            findPreference<Preference?>("dark_theme")?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
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

            findPreference<Preference?>("black_dark_theme")?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    if (mMainActivity != null && ResourceUtils.isNightMode(resources.configuration)) {
                        mMainActivity!!.recreate()
                    }
                    true
                }

            val primaryColor = findPreference<Preference?>("theme_color")
            primaryColor?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    mMainActivity?.recreate()
                    true
                }

            findPreference<MaterialSwitchPreference?>("follow_system_accent")?.apply {
                if (DynamicColors.isDynamicColorAvailable()) {
                    primaryColor?.isVisible = !isChecked
                    isVisible = true
                    onPreferenceChangeListener =
                        Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                            mMainActivity?.recreate()
                            true
                        }
                }
            }

            findPreference<Preference?>("ver")?.apply {
                title = getString(
                    R.string.settings_ver,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
                onPreferenceClickListener =
                    Preference.OnPreferenceClickListener {
                        Thread {
                            if (mMainActivity!!.isDialogShow) return@Thread
                            try {
                                val updateInfo = UpdateUtil.update(
                                    (findPreference<Preference?>("update_channel") as SimpleMenuPreference)
                                        .value == resources.getStringArray(R.array.update_channel_values)[1]
                                )
                                if (updateInfo.versionCode > BuildConfig.VERSION_CODE) {
                                    mMainActivity!!.runOnUiThread {
                                        try {
                                            BaseDialogBuilder(mMainActivity!!)
                                                .setTitle(R.string.settings_check_update)
                                                .setMessage(
                                                    getString(
                                                        R.string.settings_check_update_msg,
                                                        BuildConfig.VERSION_NAME,
                                                        BuildConfig.VERSION_CODE,
                                                        updateInfo.versionName,
                                                        updateInfo.versionCode,
                                                        updateInfo.updateMsg
                                                    )
                                                )
                                                .setPositiveButton(R.string.settings_check_update) { dialog, which -> }
                                                .show()
                                        } catch (_: DialogShowException) {
                                        }
                                    }
                                } else {
                                    mMainActivity!!.runOnUiThread {
                                        Toast.makeText(
                                            mMainActivity,
                                            R.string.settings_latest_version,
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: UpdateException) {
                                mMainActivity!!.runOnUiThread {
                                    Toast.makeText(
                                        mMainActivity, when (e.what) {
                                            UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER -> R.string.settings_can_not_connect_update_server
                                            UpdateException.WHAT_CAN_NOT_PARSE_JSON -> R.string.settings_can_not_parse_json
                                            UpdateException.WHAT_JSON_FORMAT_ERROR -> R.string.settings_json_format_error
                                            else -> R.string.settings_error
                                        }, Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            true
                        }.start()
                        true
                    }
            }
            val help = findPreference<Preference?>("help")
            help?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    try {
                        BaseDialogBuilder(mMainActivity!!)
                            .setTitle(R.string.settings_help)
                            .setMessage("没做")
                            .show()
                    } catch (_: DialogShowException) {
                    }
                    true
                }
            val exportData = findPreference<Preference?>("export_data")
            exportData?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    if (mMainActivity!!.isDialogShow) true
                    mMainActivity!!.isDialogShow = true
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.setType("application/json")
                    intent.putExtra(Intent.EXTRA_TITLE, "runner_data.json")
                    true
                }
            val importData = findPreference<Preference?>("import_data")
            importData?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    if (mMainActivity!!.isDialogShow) true
                    mMainActivity!!.isDialogShow = true
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.setType("application/json")
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
            mParentFragment!!.getToolbar().setOnClickListener(l)
            return recyclerView
        }
    }
}