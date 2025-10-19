/*
 * Copyright (C) 2007 The Android Open Source Project
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

package yangfentuozi.runner.app.ui.fragment.terminal;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import jackpal.androidterm.emulatorview.TermSession;
import yangfentuozi.runner.app.ui.fragment.terminal.util.SessionList;

public class TermService extends Service implements TermSession.FinishCallback {

    private static final String NOTIFICATION_CHANNEL_ID = "terminal_service_channel";

    private static final int RUNNING_NOTIFICATION = 1;

    private SessionList mTermSessions;

    public class TSBinder extends Binder {
        TermService getService() {
            Log.i("TermService", "Activity binding to service");
            return TermService.this;
        }
    }

    private final IBinder mTSBinder = new TSBinder();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i("TermService", "Activity called onBind()");
        return mTSBinder;
    }

    @Override
    public void onCreate() {
        mTermSessions = new SessionList();

        // Create a notification channel for the service.
        getSystemService(NotificationManager.class)
                .createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID, getText(R.string.application_terminal), NotificationManager.IMPORTANCE_LOW));

        // Create a notification to show that the service is running.
        var nb = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        nb.setSmallIcon(R.drawable.ic_stat_service_notification_icon);
        nb.setContentTitle(getText(R.string.application_terminal));
        nb.setContentText(getText(R.string.service_notify_text));
        nb.setOngoing(true);
        nb.setPriority(NotificationCompat.PRIORITY_LOW);
        nb.setCategory(NotificationCompat.CATEGORY_SERVICE);

        Intent notifyIntent = new Intent(this, Term.class);
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, PendingIntent.FLAG_IMMUTABLE);

        nb.setContentIntent(pendingIntent);

        /* Put the service in the foreground. */
        startForeground(RUNNING_NOTIFICATION, nb.build());

        Log.d(TermDebug.LOG_TAG, "TermService started");
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        for (TermSession session : mTermSessions) {
            /* Don't automatically remove from list of sessions -- we clear the
             * list below anyway and we could trigger
             * ConcurrentModificationException if we do */
            session.setFinishCallback(null);
            session.finish();
        }
        mTermSessions.clear();
    }

    public SessionList getSessions() {
        return mTermSessions;
    }

    public void onSessionFinish(TermSession session) {
        mTermSessions.remove(session);
    }
}
