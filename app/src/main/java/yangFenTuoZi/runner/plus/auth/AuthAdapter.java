package yangFenTuoZi.runner.plus.auth;

import android.animation.ObjectAnimator;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.LinkedList;
import java.util.List;

import pokercc.android.expandablerecyclerview.ExpandableAdapter;
import yangFenTuoZi.runner.plus.R;
import yangFenTuoZi.runner.plus.ui.activity.MainActivity;
import yangFenTuoZi.runner.plus.ui.fragment.AuthFragment;

public class AuthAdapter extends ExpandableAdapter<AuthAdapter.ViewHolder> {
    private final AuthData[] data;
    private final MainActivity mContext;
    private final AuthFragment authFragment;

    public static class AuthData {
        public int uid;
        public Drawable[] icons;
        public String[] names;
        public String[] packageNames;
        public boolean isAllow;

        public AuthData(int uid, boolean isAllow, PackageManager pm) throws PackageManager.NameNotFoundException {
            this.uid = uid;
            this.isAllow = isAllow;
            packageNames = pm.getPackagesForUid(uid);
            if (packageNames == null)
                throw new PackageManager.NameNotFoundException();
            names = new String[packageNames.length];
            icons = new Drawable[packageNames.length];
            for (int i = 0; i < packageNames.length; i++) {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(packageNames[i], 0);
                names[i] = pm.getApplicationLabel(applicationInfo).toString();
                icons[i] = pm.getApplicationIcon(applicationInfo);
            }
        }
    }

    @Override
    public int getChildCount(int groupPosition) {
        return data[groupPosition].packageNames.length;
    }

    @NonNull
    @Override
    protected ViewHolder onCreateGroupViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        boolean emptyPage = isEmpty();
        if (emptyPage) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.empty_processes, parent, false);
            view.getRootView().setOnClickListener(v -> {
                SwipeRefreshLayout refreshLayout = authFragment.getBinding().swipeRefreshLayout;
                if (!refreshLayout.isRefreshing()) {
                    refreshLayout.setRefreshing(true);
                    authFragment.onRefreshListener.onRefresh();
                }
            });
        } else {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_auth, parent, false);
        }
        ViewHolder holder = new ViewHolder(view, emptyPage);
        view.setOnKeyListener((v, i, keyEvent) -> false);
        return holder;
    }

    @NonNull
    @Override
    protected ViewHolder onCreateChildViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_auth_child, parent, false);
        ViewHolder holder = new ViewHolder(view);
        view.setOnKeyListener((v, i, keyEvent) -> false);
        return holder;
    }

    @Override
    protected void onBindChildViewHolder(@NonNull ViewHolder chidViewHolder, int groupPosition, int childPosition, @NonNull List<?> payloads) {
        chidViewHolder.icon.setImageDrawable(data[groupPosition].icons[childPosition]);
        chidViewHolder.name.setText(data[groupPosition].names[childPosition]);
        chidViewHolder.packageName.setText(data[groupPosition].packageNames[childPosition]);
        chidViewHolder.itemView.findViewById(R.id.card).setOnClickListener(v -> {
            if (isExpand(groupPosition)) collapseGroup(groupPosition, true);
            else expandGroup(groupPosition, true);
        });
    }

    @Override
    protected void onBindGroupViewHolder(@NonNull ViewHolder holder, int groupPosition, boolean expand, @NonNull List<?> payloads) {
        if (!isEmpty()) {
            holder.uid.setText(String.valueOf(data[groupPosition].uid));
//            holder.switchWidget.setOnClickListener((view) -> data[position].isAllow = true);
            var parent = data[groupPosition];
            if (payloads.isEmpty()) {
                holder.arrowButton.setRotation(expand ? 90f : 0f);
            }

            holder.arrowButton.setOnClickListener(v -> {
                if (isExpand(groupPosition)) collapseGroup(groupPosition, true);
                else expandGroup(groupPosition, true);
            });
            holder.itemView.findViewById(R.id.card).setOnClickListener(v -> holder.switchWidget.performClick());
        }
    }

    @Override
    protected void onGroupViewHolderExpandChange(@NonNull ViewHolder holder, int groupPosition, long animDuration, boolean expand) {
        if (expand) {
            ObjectAnimator.ofFloat(holder.arrowButton, View.ROTATION, 90f)
                    .setDuration(animDuration)
                    .start();
        } else {
            ObjectAnimator.ofFloat(holder.arrowButton, View.ROTATION, 0f)
                    .setDuration(animDuration)
                    .start();
        }
    }

    @Override
    public int getGroupCount() {

        int i = getRealItemCount();
        return i == 0 ? 1 : i;
    }


    public AuthAdapter(MainActivity mContext, AuthData[] data, AuthFragment authFragment) {
        //设置adapter需要接收两个参数：上下文、int数组
        super();
        this.mContext = mContext;
        this.data = data;
        this.authFragment = authFragment;
    }

    public int getRealItemCount() {
        return data.length;
    }

    public boolean isEmpty() {
        return getRealItemCount() == 0;
    }

    public static class ViewHolder extends ExpandableAdapter.ViewHolder {
        final boolean isChid;
        final View itemView;

        // parent
        TextView uid;
        MaterialSwitch switchWidget;
        MaterialButton arrowButton;
        List<ViewHolder> chidViewHolders = new LinkedList<>();

        public ViewHolder(View view, boolean emptyPage) {
            super(view);
            itemView = view;
            isChid = false;
            if (emptyPage) return;
            uid = view.findViewById(R.id.uid);
            switchWidget = view.findViewById(R.id.switch_widget);
            arrowButton = view.findViewById(R.id.arrow_button);
        }

        // chid
        ImageView icon;
        TextView name, packageName;

        public ViewHolder(View view) {
            super(view);
            itemView = view;
            isChid = true;
            icon = view.findViewById(R.id.icon);
            name = view.findViewById(R.id.name);
            packageName = view.findViewById(R.id.packageName);
        }
    }
}
