package yangfentuozi.runner.app.ui.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _installTermExtUri = MutableStateFlow<Uri?>(null)
    val installTermExtUri: StateFlow<Uri?> = _installTermExtUri.asStateFlow()

    private val _isInstalling = MutableStateFlow(false)
    val isInstalling: StateFlow<Boolean> = _isInstalling.asStateFlow()

    fun setInstallTermExtUri(uri: Uri?) {
        _installTermExtUri.value = uri
    }

    fun setInstalling(installing: Boolean) {
        _isInstalling.value = installing
    }
}

