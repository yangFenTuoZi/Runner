package yangFenTuoZi.runner.plus.ui.fragment.home;

import java.util.ArrayList;

import rikka.recyclerview.IdBasedRecyclerViewAdapter;
import yangFenTuoZi.runner.plus.Runner;

public class HomeAdapter extends IdBasedRecyclerViewAdapter {

    private static final long ID_SERVICE_STATUS = 0L;
    private static final long ID_SHIZUKU_STATUS = 1L;
    private static final long ID_GRANT_SHIZUKU_PERM = 2L;

    public HomeAdapter() {
        super(new ArrayList<>());
        updateData();
        setHasStableIds(true);
    }

    public void updateData() {
        clear();
        addItem(ServiceStatusViewHolder.CREATOR, null, ID_SERVICE_STATUS);
        addItem(ShizukuStatusViewHolder.CREATOR, null, ID_SHIZUKU_STATUS);

        if (!Runner.shizukuPermission) {
            addItem(GrantShizukuPermViewHolder.CREATOR, null, ID_GRANT_SHIZUKU_PERM);
        }

//        addItem(LearnMoreViewHolder.CREATOR, null, ID_LEARN_MORE);
        notifyDataSetChanged();
    }
}