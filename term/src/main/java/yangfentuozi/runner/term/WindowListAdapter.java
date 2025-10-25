/*
 * Copyright (C) 2011 Steven Luo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package yangfentuozi.runner.term;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.radiobutton.MaterialRadioButton;

import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.emulatorview.UpdateCallback;
import yangfentuozi.runner.term.util.SessionList;

public class WindowListAdapter extends BaseAdapter implements UpdateCallback {
    private SessionList mSessions;
    private int displayedChild;
    private TermViewFlipper mViewFlipper;

    public WindowListAdapter(SessionList sessions) {
        setSessions(sessions);
    }

    public void setTermViewFlipper(TermViewFlipper viewFlipper) {
        mViewFlipper = viewFlipper;
        displayedChild = mViewFlipper == null ? -1 : mViewFlipper.getDisplayedChild();
    }

    public void setSessions(SessionList sessions) {
        mSessions = sessions;

        if (sessions != null) {
            sessions.addCallback(this);
            sessions.addTitleChangedListener(this);
        } else {
            onUpdate();
        }
    }

    public int getCount() {
        if (mSessions != null) {
            return mSessions.size();
        } else {
            return 0;
        }
    }

    public Object getItem(int position) {
        return mSessions.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    protected String getSessionTitle(int position, String defaultTitle) {
        TermSession session = mSessions.get(position);
        if (session instanceof RishTermSession rishTermSession) {
            return rishTermSession.getTitle(defaultTitle);
        } else {
            return defaultTitle;
        }
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Activity act = findActivityFromContext(parent.getContext());
        View child = LayoutInflater.from(parent.getContext()).inflate(R.layout.window_list_item, parent, false);
        MaterialButton close = child.findViewById(R.id.window_list_close);

        TextView label = child.findViewById(R.id.window_list_label);
        String defaultTitle = act.getString(R.string.window_title, position + 1);
        label.setText(getSessionTitle(position, defaultTitle));

        final SessionList sessions = mSessions;
        final int closePosition = position;
        close.setOnClickListener(v -> {
            TermSession session = sessions.remove(closePosition);
            if (session != null) {
                session.finish();
                notifyDataSetChanged();
            }
        });
        if (position == displayedChild)
            child.<MaterialRadioButton>findViewById(R.id.radio_button).setChecked(true);
        return child;
    }

    public void onUpdate() {
        displayedChild = mViewFlipper == null ? -1 : mViewFlipper.getDisplayedChild();
        notifyDataSetChanged();
    }

    private static Activity findActivityFromContext(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof Activity) {
            return (Activity) context;
        } else if (context instanceof ContextWrapper cw) {
            return findActivityFromContext(cw.getBaseContext());
        }
        return null;
    }
}
