package yangFenTuoZi.runner.plus.ui.fragment;

import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {
    public void setupToolbar(Toolbar toolbar, View tipsView, int title) {
        setupToolbar(toolbar, tipsView, getString(title), -1);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, int title, int menu) {
        setupToolbar(toolbar, tipsView, getString(title), menu);
    }

    public void setupToolbar(Toolbar toolbar, View tipsView, String title, int menu) {
        toolbar.setTitle(title);
        toolbar.setTooltipText(title);
        if (tipsView != null) tipsView.setTooltipText(title);
        if (menu != -1) {
            toolbar.inflateMenu(menu);
            if (this instanceof MenuProvider self) {
                self.onPrepareMenu(toolbar.getMenu());
            }
        }
    }
}
