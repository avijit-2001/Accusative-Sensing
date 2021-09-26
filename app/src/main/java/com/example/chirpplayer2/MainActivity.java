package com.example.chirpplayer2;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.chibde.visualizer.LineVisualizer;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    int duration=10;
    int sampleRate=44100;
    int numSample=duration*sampleRate;
    double[] sample =new double[numSample];
    double freq1=20;
    double freq2=1900;
    byte[] generatedSnd= new byte[2*numSample];
    Handler handler = new Handler();
    AudioTrack audioTrack;
    LineVisualizer lineVisualizer;


    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;

    private MediaRecorder recorder = null;
    private Button recordButton = null;
    private Boolean recordingStatus = false;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }

    private void onRecord(boolean start) {

        if (start) {
            recordButton.setText("Stop & Save");
            startRecording();
        } else {
            recordButton.setText("Start Recording");
            stopRecording();
        }
    }


    private void startRecording() {
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        toastMessage(fileName);
        recorder.setOutputFile(fileName);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    DatabaseHelper myDatabase;

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Record to the external cache directory for visibility
        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/current_audio.3gp";


        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        Button bGenerate = findViewById(R.id.generate);
        Button pitchIncreaseButton = findViewById(R.id.increase);
        Button pitchDecreaseButton = findViewById(R.id.decrease);
        Button savePitchButton = findViewById(R.id.savePitch);
        recordButton = findViewById(R.id.recordAudio);
        lineVisualizer = findViewById(R.id.visualizerLine);

        EditText sFreq = findViewById(R.id.startFrequency);
        EditText eFreq = findViewById(R.id.endFrequency);

        TextView pitchValueTextView = findViewById(R.id.pitchValue);
        pitchValueTextView.setText("pitch: 1.00f");
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);

        myDatabase = new DatabaseHelper(this);


        bGenerate.setOnClickListener(view -> {
            String start = sFreq.getText().toString();
            String end = eFreq.getText().toString();

            freq1 = Double.valueOf(start);
            freq2 = Double.valueOf(end);

            genTone();
            playSound();

        });

        pitchIncreaseButton.setOnClickListener(view -> {
            PlaybackParams params = audioTrack.getPlaybackParams();
            float pitch = params.getPitch();
            pitch = pitch + 0.05f;
            params.setPitch(pitch);
            pitchValueTextView.setText("pitch: " + df.format(pitch));
            audioTrack.setPlaybackParams(params);
        });

        pitchDecreaseButton.setOnClickListener(view -> {
            PlaybackParams params = audioTrack.getPlaybackParams();
            float pitch = params.getPitch();
            if(pitch > 0.10f) {
                pitch = pitch - 0.05f;
            }
            pitchValueTextView.setText("pitch: " + df.format(pitch));
            params.setPitch(pitch);
            audioTrack.setPlaybackParams(params);
        });

        savePitchButton.setOnClickListener(view -> {
            String pitchValue = pitchValueTextView.getText().toString();
            myDatabase.addData(pitchValue);
        });

        recordButton.setOnClickListener(view -> {

            recordingStatus = !recordingStatus;
            onRecord(recordingStatus);
            toastMessage(fileName);
        });

    }

    protected void onResume()
    {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (lineVisualizer != null)
            lineVisualizer.release();
    }

    void genTone(){

        double instfreq = 0, numerator;
        for (int i = 0; i < numSample; i++) {
            numerator = (double) (i) / (double) numSample;
            instfreq = freq1 + (numerator * (freq2 - freq1));
            if ((i % 1000) == 0) {
                Log.e("Current Freq:", String.format("Freq is:  %f at loop %d of %d", instfreq, i, numSample));
            }
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / instfreq));

        }
        int idx = 0;
        for (final double dVal : sample) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767)); // max positive sample for signed 16 bit integers is 32767
            // in 16 bit wave PCM, first byte is the low order byte (pcm: pulse control modulation)
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);

        }
    }


    void playSound(){
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();

        lineVisualizer.setVisibility(View.VISIBLE);

        // set a custom color to the line.
        lineVisualizer.setColor(ContextCompat.getColor(this, R.color.purple_200));

        // set the line width for the visualizer between 1-10 default is  1.
        lineVisualizer.setStrokeWidth(1);

        // Setting the media player to the visualizer.
        lineVisualizer.setPlayer(audioTrack.getAudioSessionId());
    }

    /**
     * customizable toast
     * @param message
     */
    private void toastMessage(String message){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show();
    }
}