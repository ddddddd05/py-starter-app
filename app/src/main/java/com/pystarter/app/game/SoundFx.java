package com.pystarter.app.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

import com.pystarter.app.SettingsActivity;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 8-bit 像素音效与震动反馈：用纯代码合成方波/三角波音效，无需任何外部音频文件。
 */
public final class SoundFx {

    private static final int SAMPLE_RATE = 22050;
    private static final ExecutorService AUDIO = Executors.newSingleThreadExecutor();
    public static final String KEY_SOUND = "sound_fx";
    public static final String KEY_VIBRATE = "vibrate_fx";

    private SoundFx() {}

    public static boolean isSoundEnabled(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
        return p.getBoolean(KEY_SOUND, true);
    }

    public static boolean isVibrateEnabled(Context ctx) {
        SharedPreferences p = ctx.getSharedPreferences(SettingsActivity.PREFS, Context.MODE_PRIVATE);
        return p.getBoolean(KEY_VIBRATE, true);
    }

    public static void click(Context ctx) {
        if (isSoundEnabled(ctx)) {
            playNotes(new int[][]{{880, 35}});
        }
    }

    public static void step(Context ctx) {
        if (isSoundEnabled(ctx)) {
            playNotes(new int[][]{{330, 30}, {440, 25}});
        }
    }

    public static void attack(Context ctx) {
        if (isSoundEnabled(ctx)) {
            playNotes(new int[][]{{523, 45}, {659, 45}, {784, 65}});
        }
        vibrate(ctx, 30);
    }

    public static void hit(Context ctx) {
        if (isSoundEnabled(ctx)) {
            playNotes(new int[][]{{220, 60}, {165, 80}, {110, 100}});
        }
        vibrate(ctx, 80);
    }

    public static void star(Context ctx) {
        if (isSoundEnabled(ctx)) {
            playNotes(new int[][]{{784, 55}, {988, 55}, {1318, 110}});
        }
        vibrate(ctx, 40);
    }

    public static void win(Context ctx) {
        if (isSoundEnabled(ctx)) {
            playNotes(new int[][]{{523, 70}, {659, 70}, {784, 70}, {1046, 170}});
        }
        vibrate(ctx, 60);
    }

    public static void vibrate(Context ctx, long ms) {
        if (!isVibrateEnabled(ctx)) {
            return;
        }
        try {
            Vibrator v;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vm = (VibratorManager) ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                v = vm != null ? vm.getDefaultVibrator() : null;
            } else {
                v = (Vibrator) ctx.getSystemService(Context.VIBRATOR_SERVICE);
            }
            if (v == null || !v.hasVibrator()) {
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (Exception ignored) {
        }
    }

    private static void playNotes(int[][] notes) {
        AUDIO.execute(() -> {
            try {
                int totalSamples = 0;
                for (int[] n : notes) {
                    totalSamples += (int) (SAMPLE_RATE * (n[1] / 1000.0));
                }
                if (totalSamples <= 0) {
                    return;
                }
                short[] pcm = new short[totalSamples];
                int offset = 0;
                for (int[] n : notes) {
                    int freq = n[0];
                    int count = (int) (SAMPLE_RATE * (n[1] / 1000.0));
                    double period = freq > 0 ? (double) SAMPLE_RATE / freq : 0;
                    for (int i = 0; i < count; i++) {
                        double env = 1.0 - (double) i / count; // 轻微衰减，听起来像复古芯片音
                        short amp = (short) (7000 * (0.4 + 0.6 * env));
                        if (period > 0) {
                            pcm[offset + i] = ((i % period) < (period / 2.0)) ? amp : (short) -amp;
                        }
                    }
                    offset += count;
                }
                AudioTrack track = new AudioTrack.Builder()
                        .setAudioAttributes(new AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build())
                        .setAudioFormat(new AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build())
                        .setBufferSizeInBytes(pcm.length * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build();
                track.write(pcm, 0, pcm.length);
                track.play();
                int totalMs = 0;
                for (int[] n : notes) {
                    totalMs += n[1];
                }
                Thread.sleep(totalMs + 40L);
                track.release();
            } catch (Exception ignored) {
            }
        });
    }
}
