package com.junyou.hbks.Utils;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.junyou.hbks.R;

import java.util.HashMap;

public class SoundUtil {

    HashMap soundPoolMap;
    private SoundPool soundPool;
    private Activity _activity;

    public void initSound(Activity context) {
        _activity = context;
        if (null != _activity){
            soundPoolMap = new HashMap();
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            soundPoolMap.put(1, soundPool.load(_activity, R.raw.qhb, 1));
        }
    }

    public void playSounds(int count) {
        if (null != _activity){
            AudioManager am = (AudioManager) _activity.getSystemService(_activity.AUDIO_SERVICE);
            float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);
            float volumnRatio = audioCurrentVolumn / audioMaxVolumn;
            soundPool.play((int)soundPoolMap.get(count), audioCurrentVolumn, audioCurrentVolumn, 1, 0, (float) 2.0);
        }

        //实例化AudioManager对象，控制声音
//        AudioManager am = (AudioManager)_activity.getSystemService(_activity.AUDIO_SERVICE);
        //最大音量
//        float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //当前音量
//        float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_MUSIC);
    }
}
