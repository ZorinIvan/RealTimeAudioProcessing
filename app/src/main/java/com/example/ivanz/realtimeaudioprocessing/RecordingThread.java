package com.example.ivanz.realtimeaudioprocessing;


import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.Arrays;

public class RecordingThread {
    private static final String LOG_TAG = RecordingThread.class.getSimpleName();
    private static final int SAMPLE_RATE = 8000;
    public static int factor_delay = 1;
    AudioTrack audioTrack;
    int maxJitter;
    short[] audioBuffer;

    public RecordingThread(AudioDataReceivedListener listener) {
        mListener = listener;
    }

    public RecordingThread() {
    }

    private boolean mShouldContinue;
    private AudioDataReceivedListener mListener;
    private Thread mThread;

    public boolean recording() {
        return mThread != null;
    }

    public void startRecording() {
        if (mThread != null)
            return;
        audioBuffer = new short[128];
        mShouldContinue = true;
        mThread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        mThread.start();
    }
    public int getSize()
    {
        return audioBuffer.length;
    }
    public void stopRecording() {
        if (mThread == null)
            return;

        mShouldContinue = false;
        mThread = null;
    }

    public void setAudioTrack(float delay_factor) {
        if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
            audioTrack.stop();
            audioTrack.release();
        }
       maxJitter = AudioTrack.getMinBufferSize((int)(SAMPLE_RATE * delay_factor),
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioTrack = audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxJitter,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
     audioBuffer= Arrays.copyOf(audioBuffer, (int)(1024*delay_factor));

    }

    private void record() {
        Log.v(LOG_TAG, "Start");
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // buffer size in bytes
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        maxJitter = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }



        AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);
        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(LOG_TAG, "Audio Record can't initialize!");
            return;
        }
        long time1= System.currentTimeMillis();
        record.startRecording();
        long time2= System.currentTimeMillis();
        long diff = time2-time1;
        Log.d(LOG_TAG, "diff_start_record="+diff);
        Log.v(LOG_TAG, "Start recording");

        long shortsRead = 0;
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxJitter,
                AudioTrack.MODE_STREAM);
        audioTrack.play();
        while (mShouldContinue) {
            time1= System.currentTimeMillis();
            int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
            time2= System.currentTimeMillis();
            diff = time2-time1;
            Log.d(LOG_TAG, "diff_read="+diff);

            shortsRead += numberOfShort;
      //      for (int i=0;i<audioBuffer.length;i++)
      //          audioBuffer[i]=(short)(audioBuffer[i]+(short)(0.6*audioBuffer[i]));
            // Notify waveform
            if (numberOfShort> 0) {
                time1 = System.currentTimeMillis();
                audioTrack.write(audioBuffer, 0, numberOfShort);
                time2= System.currentTimeMillis();
                diff = time2-time1;
                Log.d(LOG_TAG, "diff_write="+diff);

            }
        }
        audioTrack.stop();
        audioTrack.release();
        record.stop();
        record.release();

        Log.v(LOG_TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }
}
