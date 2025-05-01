package yangFenTuoZi.runner.plus.ui.activity

import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import rikka.html.text.HtmlCompat
import yangFenTuoZi.runner.plus.BuildConfig
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseActivity
import yangFenTuoZi.runner.plus.base.BaseDialogBuilder
import yangFenTuoZi.runner.plus.databinding.ActivityMainBinding
import yangFenTuoZi.runner.plus.databinding.DialogAboutBinding
import yangFenTuoZi.runner.plus.ui.dialog.BlurBehindDialogBuilder
import yangFenTuoZi.runner.plus.utils.ThrowableKT.toErrorDialog
import java.util.Locale

class MainActivity : BaseActivity() {

    lateinit var appBar: AppBarLayout
    lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.navigation_home,
            R.id.navigation_runner,
            R.id.navigation_terminal,
            R.id.navigation_proc,
            R.id.navigation_settings
        ).build()

        val fragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main)
        val navController = (fragment as NavHostFragment).navController
        NavigationUI.setupWithNavController(binding.toolbar, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(binding.navView, navController)

        binding.appBar.setLiftable(true)
        binding.toolbar.inflateMenu(R.menu.menu_home)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_stop_server -> {
                    if (!Runner.pingServer()) return@setOnMenuItemClickListener true
                    try {
                        BaseDialogBuilder(this)
                            .setTitle(R.string.warning)
                            .setMessage(R.string.confirm_stop_server)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                Thread {
                                    try {
                                        Runner.tryUnbindService(true)
                                    } catch (e: Exception) {
                                        e.toErrorDialog(this)
                                    }
                                }.start()
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    } catch (_: BaseDialogBuilder.DialogShowException) {
                    }
                    true
                }

                R.id.menu_about -> {
                    showAbout()
                    true
                }

                else -> true
            }
        }

        toolbar = binding.toolbar
        appBar = binding.appBar
    }

    private fun showAbout() {
        val binding = DialogAboutBinding.inflate(layoutInflater, null, false)
        binding.designAboutTitle.setText(R.string.app_name)
        binding.designAboutInfo.movementMethod = LinkMovementMethod.getInstance()
        binding.designAboutInfo.text = HtmlCompat.fromHtml(
            getString(
                R.string.about_view_source_code,
                "<b><a href=\"https://github.com/yangFenTuoZi/Runner\">GitHub</a></b>"
            ), HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.designAboutVersion.text = String.format(
            Locale.getDefault(),
            "%s (%d)",
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        )

        try {
            BlurBehindDialogBuilder(this)
                .setView(binding.root)
                .show()
        } catch (_: BaseDialogBuilder.DialogShowException) {
        }
    }
}