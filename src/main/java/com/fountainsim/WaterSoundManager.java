package com.fountainsim;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class WaterSoundManager {
    private SoundPool soundPool;
    private int streamId = 0;
    private int soundId;
    private boolean loaded = false;
    private float volume = 0.7f;

    public WaterSoundManager(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(3)
                .setAudioAttributes(attrs)
                .build();
        soundId = soundPool.load(context, R.raw.water_fountain, 1);
        soundPool.setOnLoadCompleteListener((sp, sampleId, status) -> {
            if (status == 0 && sampleId == soundId) {
                loaded = true;
                streamId = sp.play(soundId, volume, volume, 1, -1, 1f);
            }
        });
    }

    public void setVolume(float vol) {
        volume = Math.max(0, Math.min(1, vol));
        if (streamId != 0) {
            soundPool.setVolume(streamId, volume, volume);
        }
    }

    public void pause() {
        if (streamId != 0) {
            soundPool.pause(streamId);
        }
    }

    public void resume() {
        if (streamId != 0 && loaded) {
            soundPool.resume(streamId);
        } else if (loaded && streamId == 0) {
            streamId = soundPool.play(soundId, volume, volume, 1, -1, 1f);
        }
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
