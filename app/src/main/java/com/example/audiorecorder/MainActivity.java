package com.example.audiorecorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_PERMISSIONS = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    private AudioRecorder audioRecorder;
    private PcmPlayer pcmPlayer;
    private Button btnRecord, btnStopRecord, btnPlay, btnStopPlay;
    private TextView tvStatus;
    private File pcmFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        checkPermissions();

        // 设置 PCM 文件保存路径
        File dir = new File(getExternalFilesDir(null), "AudioRecords");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        pcmFile = new File(dir, "record_16k_16bit_mono.pcm");
    }

    private void initViews() {
        btnRecord = findViewById(R.id.btnRecord);
        btnStopRecord = findViewById(R.id.btnStopRecord);
        btnPlay = findViewById(R.id.btnPlay);
        btnStopPlay = findViewById(R.id.btnStopPlay);
        tvStatus = findViewById(R.id.tvStatus);

        btnRecord.setOnClickListener(v -> startRecording());
        btnStopRecord.setOnClickListener(v -> stopRecording());
        btnPlay.setOnClickListener(v -> startPlaying());
        btnStopPlay.setOnClickListener(v -> stopPlaying());

        updateUI(false, false);
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean needPermissions = false;
            for (String permission : PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    needPermissions = true;
                    break;
                }
            }

            if (needPermissions) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                Toast.makeText(this, "需要录音和存储权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startRecording() {
        audioRecorder = new AudioRecorder(pcmFile);
        if (audioRecorder.startRecord()) {
            updateUI(true, false);
            tvStatus.setText("正在录音...\n文件: " + pcmFile.getAbsolutePath() +
                    "\n格式: 16kHz, 16bit, 单通道");
        } else {
            Toast.makeText(this, "录音启动失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopRecording() {
        if (audioRecorder != null) {
            audioRecorder.stopRecord();
            audioRecorder = null;
        }
        updateUI(false, false);
        tvStatus.setText("录音已保存\n文件: " + pcmFile.getAbsolutePath() +
                "\n大小: " + pcmFile.length() + " bytes");
    }

    private void startPlaying() {
        if (!pcmFile.exists()) {
            Toast.makeText(this, "没有找到录音文件", Toast.LENGTH_SHORT).show();
            return;
        }

        pcmPlayer = new PcmPlayer();
        if (pcmPlayer.startPlay(pcmFile)) {
            updateUI(false, true);
            tvStatus.setText("正在播放...\n文件: " + pcmFile.getAbsolutePath());
        } else {
            Toast.makeText(this, "播放启动失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopPlaying() {
        if (pcmPlayer != null) {
            pcmPlayer.stopPlay();
            pcmPlayer = null;
        }
        updateUI(false, false);
        tvStatus.setText("播放已停止");
    }

    private void updateUI(boolean isRecording, boolean isPlaying) {
        btnRecord.setEnabled(!isRecording && !isPlaying);
        btnStopRecord.setEnabled(isRecording);
        btnPlay.setEnabled(!isRecording && !isPlaying && pcmFile != null && pcmFile.exists());
        btnStopPlay.setEnabled(isPlaying);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRecording();
        stopPlaying();
    }
}