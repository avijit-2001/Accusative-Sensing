package com.example.chirpplayer2;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.PlaybackParams;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.chibde.visualizer.LineVisualizer;

import java.io.IOException;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {

    int duration=10;
    int sampleRate=44100;
    int numSample=duration*sampleRate;
    double sample[]=new double[numSample];
    double freq1=20;
    double freq2=1900;
    byte[] generatedSnd= new byte[2*numSample];
    Handler handler = new Handler();
    AudioTrack audioTrack;
    LineVisualizer lineVisualizer;

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        Button bGenerate = findViewById(R.id.generate);
        Button pitchIncreaseButton = findViewById(R.id.increase);
        Button pitchDecreaseButton = findViewById(R.id.decrease);
        lineVisualizer = findViewById(R.id.visualizerLine);

        EditText sFreq = findViewById(R.id.startFrequency);
        EditText eFreq = findViewById(R.id.endFrequency);

        TextView pitchValue = findViewById(R.id.pitchValue);
        pitchValue.setText("pitch: 1.00f");
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(2);


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
            pitchValue.setText("pitch: " + df.format(pitch));
            audioTrack.setPlaybackParams(params);
        });

        pitchDecreaseButton.setOnClickListener(view -> {
            PlaybackParams params = audioTrack.getPlaybackParams();
            float pitch = params.getPitch();
            if(pitch > 0.10f) {
                pitch = pitch - 0.05f;
            }
            pitchValue.setText("pitch: " + df.format(pitch));
            params.setPitch(pitch);
            audioTrack.setPlaybackParams(params);
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
}