package com.example.audiorecorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PcmPlayer {

    private static final String TAG = "PcmPlayer";

    // 播放参数：必须与录音参数一致
    private static final int SAMPLE_RATE = 16000;  // 16kHz
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO;  // 单通道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;  // 16bit

    private AudioTrack audioTrack;
    private boolean isPlaying = false;
    private Thread playThread;

    public boolean startPlay(File pcmFile) {
        if (!pcmFile.exists()) {
            Log.e(TAG, "PCM 文件不存在: " + pcmFile.getAbsolutePath());
            return false;
        }

        // 计算最小缓冲区大小
        int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        int bufferSize = Math.max(minBufferSize, 4096);

        Log.d(TAG, "播放缓冲区大小: " + bufferSize);

        // 创建 AudioTrack 实例
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,  // 音乐流类型
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize,
                AudioTrack.MODE_STREAM  // 流模式
        );

        // 检查初始化状态
        if (audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
            Log.e(TAG, "AudioTrack 初始化失败");
            return false;
        }

        // 开始播放
        audioTrack.play();
        isPlaying = true;

        // 启动播放线程
        playThread = new Thread(new PlayTask(pcmFile, bufferSize));
        playThread.start();

        Log.d(TAG, "播放开始: " + pcmFile.getAbsolutePath());
        return true;
    }

    public void stopPlay() {
        isPlaying = false;

        if (playThread != null) {
            try {
                playThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            playThread = null;
        }

        if (audioTrack != null) {
            if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack.stop();
            }
            audioTrack.release();
            audioTrack = null;
        }

        Log.d(TAG, "播放停止");
    }

    private class PlayTask implements Runnable {
        private File pcmFile;
        private int bufferSize;

        public PlayTask(File pcmFile, int bufferSize) {
            this.pcmFile = pcmFile;
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            // 16bit 采样，short 数组
            short[] audioBuffer = new short[bufferSize / 2];

            DataInputStream dis = null;

            try {
                FileInputStream fis = new FileInputStream(pcmFile);
                BufferedInputStream bis = new BufferedInputStream(fis);
                dis = new DataInputStream(bis);

                long totalSamples = 0;

                while (isPlaying && dis.available() > 0) {
                    int samplesRead = 0;

                    // 读取 short 数据（16bit）
                    for (int i = 0; i < audioBuffer.length && dis.available() >= 2; i++) {
                        try {
                            audioBuffer[i] = dis.readShort();
                            samplesRead++;
                        } catch (IOException e) {
                            break;
                        }
                    }

                    if (samplesRead > 0) {
                        // 写入 AudioTrack 播放
                        int written = audioTrack.write(audioBuffer, 0, samplesRead);
                        totalSamples += samplesRead;

                        if (written < 0) {
                            Log.e(TAG, "写入 AudioTrack 失败: " + written);
                            break;
                        }
                    }
                }

                Log.d(TAG, "播放完成，总采样数: " + totalSamples +
                        ", 时长: " + (totalSamples / SAMPLE_RATE) + "秒");

            } catch (IOException e) {
                Log.e(TAG, "播放文件读取错误", e);
            } finally {
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}