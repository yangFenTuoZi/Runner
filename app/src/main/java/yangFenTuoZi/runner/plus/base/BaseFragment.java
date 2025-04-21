package yangFenTuoZi.runner.plus.base;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;

import yangFenTuoZi.runner.plus.ui.activity.MainActivity;

public class BaseFragment extends Fragment {
    public MainActivity mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() instanceof MainActivity mainActivity)
            mContext = mainActivity;
        else throw new RuntimeException("父Activity非MainActivity");
    }

    public AppBarLayout getAppBar() {
        return mContext.getAppBar();
    }

    public MaterialToolbar getToolbar() {
        return mContext.getToolbar();
    }

    public void runOnUiThread(Runnable action) {
        mContext.runOnUiThread(action);
    }
}