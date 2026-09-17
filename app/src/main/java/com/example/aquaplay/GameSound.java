package com.example.aquaplay;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

public class GameSound {
    private SoundPool soundPool;
    private int soundInicio, soundVitoria, soundDerrota;
    private MediaPlayer mediaPlayer;
    private boolean loaded = false;

    public GameSound(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (sampleId == soundInicio && status == 0) {
                loaded = true;
                playInicio();
            }
        });

        soundInicio = soundPool.load(context, R.raw.som_inicio, 1);
        soundVitoria = soundPool.load(context, R.raw.som_vitoria, 1);
        soundDerrota = soundPool.load(context, R.raw.som_derrota, 1);

        mediaPlayer = MediaPlayer.create(context, R.raw.musica_fundo);
        if (mediaPlayer != null) {
            mediaPlayer.setLooping(true);
            mediaPlayer.setVolume(0.5f, 0.5f);
            mediaPlayer.start();
        }
    }

    public void playInicio() {
        if (loaded) soundPool.play(soundInicio, 1, 1, 0, 0, 1);
    }

    public void playVitoria() {
        soundPool.play(soundVitoria, 1, 1, 0, 0, 1);
    }

    public void playDerrota() {
        soundPool.play(soundDerrota, 1, 1, 0, 0, 1);
    }

    public void release() {
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
