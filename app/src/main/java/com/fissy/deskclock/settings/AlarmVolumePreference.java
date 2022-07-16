/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.fissy.deskclock.settings;

import static android.content.Context.AUDIO_SERVICE;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.media.AudioManager.STREAM_ALARM;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.fissy.deskclock.R;
import com.fissy.deskclock.RingtonePreviewKlaxon;
import com.fissy.deskclock.Utils;
import com.fissy.deskclock.data.DataModel;
import com.google.android.material.slider.Slider;

public class AlarmVolumePreference extends Preference {

    private static final long ALARM_PREVIEW_DURATION_MS = 2000;

    private Slider mSeekbar;
    private ImageView mAlarmIcon;
    private boolean mPreviewPlaying;

    public AlarmVolumePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        final Context context = getContext();
        final AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);

        // Disable click feedback for this preference.
        holder.itemView.setClickable(false);

        mSeekbar = (Slider) holder.findViewById(R.id.seekbar);
        mSeekbar.setStepSize(1f);
        mSeekbar.setTickVisible(false);
        mSeekbar.setValueFrom(0f);
        mSeekbar.setValueTo(Math.round(audioManager.getStreamMaxVolume(STREAM_ALARM)));
        mSeekbar.setValue(Math.round(audioManager.getStreamVolume(STREAM_ALARM)));
        mAlarmIcon = (ImageView) holder.findViewById(R.id.alarm_icon);
        onSeekbarChanged();


        mSeekbar.addOnChangeListener((slider, value, fromUser) -> {
            if (fromUser) {
                audioManager.setStreamVolume(STREAM_ALARM, Math.round(value), 0);
            }
            onSeekbarChanged();
        });

        mSeekbar.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (!mPreviewPlaying && slider.getValue() != 0) {
                    // If we are not currently playing and progress is set to non-zero, start.
                    RingtonePreviewKlaxon.start(
                            context, DataModel.getDataModel().getDefaultAlarmRingtoneUri());
                    mPreviewPlaying = true;
                    slider.postDelayed((Runnable) () -> {
                            RingtonePreviewKlaxon.stop(context);
                            mPreviewPlaying = false;
                    }, ALARM_PREVIEW_DURATION_MS);
                }
            }
        });
    }

    private void onSeekbarChanged() {
        mSeekbar.setEnabled(doesDoNotDisturbAllowAlarmPlayback());
        mAlarmIcon.setImageResource(mSeekbar.getValue() == 0 ?
                R.drawable.ic_alarm_off_24dp : R.drawable.ic_alarm_small);
    }

    private boolean doesDoNotDisturbAllowAlarmPlayback() {
        return !Utils.isNOrLater() || doesDoNotDisturbAllowAlarmPlaybackNPlus();
    }

    @TargetApi(Build.VERSION_CODES.N)
    private boolean doesDoNotDisturbAllowAlarmPlaybackNPlus() {
        final NotificationManager notificationManager = (NotificationManager)
                getContext().getSystemService(NOTIFICATION_SERVICE);
        return notificationManager.getCurrentInterruptionFilter() !=
                NotificationManager.INTERRUPTION_FILTER_NONE;
    }
}
