package com.example.audiorecorder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AudioRecorder {

    private static final String TAG = "AudioRecorder";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private File outputFile;
    private Thread recordThread;

    public AudioRecorder(File outputFile) {
        this.outputFile = outputFile;
    }

    public boolean startRecord() {
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
        int bufferSize = Math.max(minBufferSize, 4096);

        Log.d(TAG, "MinBufferSize: " + minBufferSize + ", using: " + bufferSize);

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord 初始化失败");
            return false;
        }

        audioRecord.startRecording();
        isRecording = true;

        recordThread = new Thread(new RecordTask(bufferSize));
        recordThread.start();

        Log.d(TAG, "录音开始: " + outputFile.getAbsolutePath());
        return true;
    }

    public void stopRecord() {
        isRecording = false;

        if (recordThread != null) {
            try {
                recordThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            recordThread = null;
        }

        if (audioRecord != null) {
            if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord.stop();
            }
            audioRecord.release();
            audioRecord = null;
        }

        Log.d(TAG, "录音停止");
    }

    private class RecordTask implements Runnable {
        private int bufferSize;

        public RecordTask(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public void run() {
            short[] audioBuffer = new short[bufferSize / 2];
            DataOutputStream dos = null;

            try {
                FileOutputStream fos = new FileOutputStream(outputFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                dos = new DataOutputStream(bos);

                long totalSamples = 0;

                while (isRecording) {
                    int read = audioRecord.read(audioBuffer, 0, audioBuffer.length);

                    if (read > 0) {
                        for (int i = 0; i < read; i++) {
                            dos.writeShort(audioBuffer[i]);
                        }
                        totalSamples += read;
                    }
                }

                dos.flush();
                Log.d(TAG, "录音完成，总采样数: " + totalSamples);

            } catch (IOException e) {
                Log.e(TAG, "录音文件写入错误", e);
            } finally {
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}