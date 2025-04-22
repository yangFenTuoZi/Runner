package yangFenTuoZi.runner.plus.ui.fragment.settings

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.preference.SimpleMenuPreference
import rikka.recyclerview.fixEdgeEffect
import yangFenTuoZi.runner.plus.BuildConfig
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.base.BaseFragment
import yangFenTuoZi.runner.plus.databinding.FragmentSettingsBinding
import yangFenTuoZi.runner.plus.ui.activity.MainActivity
import yangFenTuoZi.runner.plus.utils.ThemeUtils
import yangFenTuoZi.runner.plus.utils.UpdateUtils
import yangFenTuoZi.runner.plus.utils.UpdateUtils.UpdateException

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
        private var mContext: MainActivity? = null
        private var mParentFragment: SettingsFragment? = null

        override fun onAttach(context: Context) {
            super.onAttach(context)
            if (parentFragment is SettingsFragment) mParentFragment = parentFragment as SettingsFragment?
        }

        override fun onDetach() {
            super.onDetach()
            mParentFragment = null
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            mContext = requireContext() as MainActivity
            addPreferencesFromResource(R.xml.preference_setting)
            val darkTheme = findPreference<SimpleMenuPreference?>("dark_theme")
            darkTheme?.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                    val oldIsDark = mContext!!.mApp.isDark
                    mContext!!.mApp.isDark = if (ThemeUtils.isDark(mContext)) 1 else 0
                    if (oldIsDark != mContext!!.mApp.isDark) {
                        mContext!!.mApp.setTheme(ThemeUtils.getTheme(mContext!!.mApp.isDark == 1))
                        mContext!!.recreate()
                    }
                    true
                }

            val ver = findPreference<Preference?>("ver")
            if (ver != null) {
                ver.title = getString(
                    R.string.settings_ver,
                    BuildConfig.VERSION_NAME,
                    BuildConfig.VERSION_CODE
                )
                ver.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference? ->
                    if (mContext!!.isDialogShow) true
                    try {
                        val updateInfo = UpdateUtils.Update(
                            (findPreference<Preference?>("update_channel") as SimpleMenuPreference)
                                .value == resources.getStringArray(R.array.update_channel_values)[1]
                        )
                        if (updateInfo.version_code > BuildConfig.VERSION_CODE) {
                            mContext!!.isDialogShow = true
                            MaterialAlertDialogBuilder(mContext!!)
                                .setTitle(R.string.settings_check_update)
                                .setMessage(
                                    getString(
                                        R.string.settings_check_update_msg,
                                        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE,
                                        updateInfo.version_name, updateInfo.version_code,
                                        updateInfo.update_msg
                                    )
                                )
                                .setPositiveButton(
                                    R.string.settings_check_update
                                ) { dialog, which -> }
                                .setOnDismissListener(DialogInterface.OnDismissListener { dialog: DialogInterface? ->
                                    mContext!!.isDialogShow = false
                                })
                                .show()
                        } else {
                            Toast.makeText(
                                mContext,
                                R.string.settings_latest_version,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: UpdateException) {
                        Toast.makeText(
                            mContext, when (e.what) {
                                UpdateException.WHAT_CAN_NOT_CONNECT_UPDATE_SERVER -> R.string.settings_can_not_connect_update_server
                                UpdateException.WHAT_CAN_NOT_PARSE_JSON -> R.string.settings_can_not_parse_json
                                UpdateException.WHAT_JSON_FORMAT_ERROR -> R.string.settings_json_format_error
                                else -> R.string.settings_error
                            }, Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
            }
            val help = findPreference<Preference?>("help")
            help?.onPreferenceClickListener = Preference.OnPreferenceClickListener { preference: Preference? ->
                if (mContext!!.isDialogShow) true
                mContext!!.isDialogShow = true
                MaterialAlertDialogBuilder(mContext!!)
                    .setTitle(R.string.settings_help)
                    .setMessage("没做")
                    .setOnDismissListener(DialogInterface.OnDismissListener { dialog: DialogInterface? ->
                        mContext!!.isDialogShow = false
                    })
                    .show()
                true
            }
            val exportData = findPreference<Preference?>("export_data")
            exportData?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    if (mContext!!.isDialogShow) true
                    mContext!!.isDialogShow = true
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                    intent.addCategory(Intent.CATEGORY_OPENABLE)
                    intent.setType("application/json")
                    intent.putExtra(Intent.EXTRA_TITLE, "runner_data.json")
                    true
                }
            val importData = findPreference<Preference?>("import_data")
            importData?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener { preference: Preference? ->
                    if (mContext!!.isDialogShow) true
                    mContext!!.isDialogShow = true
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