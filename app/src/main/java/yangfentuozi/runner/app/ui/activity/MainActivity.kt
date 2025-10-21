package yangfentuozi.runner.app.ui.activity

import android.content.res.Resources
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.activity.compose.setContent
import androidx.core.text.HtmlCompat
import yangfentuozi.runner.BuildConfig
import yangfentuozi.runner.R
import yangfentuozi.runner.app.base.BaseActivity
import yangfentuozi.runner.app.base.BaseDialogBuilder
import yangfentuozi.runner.app.ui.MainScreen
import yangfentuozi.runner.app.ui.theme.RunnerTheme
import yangfentuozi.runner.databinding.DialogAboutBinding
import java.util.Locale

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            RunnerTheme {
                MainScreen(
                    activity = this,
                    onShowAbout = { showAbout() }
                )
            }
        }
    }

    fun showAbout() {
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
            BaseDialogBuilder(this)
                .setView(binding.root)
                .show()
        } catch (_: BaseDialogBuilder.DialogShowingException) {
        }
    }

    override fun onApplyUserThemeResource(theme: Resources.Theme, isDecorView: Boolean) {
        super.onApplyUserThemeResource(theme, isDecorView)
        // Compose 不需要应用 Preference 主题
    }
}