package com.example.chirpplayer2;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
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
import com.jlibrosa.audio.JLibrosa;
import com.jlibrosa.audio.exception.FileFormatNotSupportedException;
import com.jlibrosa.audio.wavFile.WavFileException;

import org.apache.commons.math3.complex.Complex;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    int duration = 5;
    long subChunk1Size = 16;
    int bitsPerSample = 16;
    int format = 1;
    long channels = 1;
    int sampleRate = 44100;
    int numSample = duration * sampleRate;
    double[] sample = new double[numSample];
    double freq1 = 20;
    double freq2 = 1900;
    byte[] generatedSnd = new byte[2 * numSample];
    long byteRate = sampleRate * channels * bitsPerSample / 8;
    int blockAlign = (int) (channels * bitsPerSample / 8);

    AudioTrack audioTrack;
    LineVisualizer lineVisualizer;
    String filename = "audio.wav";


    // Recording Functionality Variables

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION = 200;
    private static String saveFileName = null;
    private Button recordButton = null;


    // Recording

    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private Boolean isRecording = false;
    private int bufferSize;




    // PERMISSIONS
    private String [] permissions = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    DatabaseHelper myDatabase;

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, BufferElements2Rec * BytesPerElement);

        recorder.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private byte[] short2byte(short[] sData) {
        int shortArrSize = sData.length;
        byte[] bytes = new byte[shortArrSize * 2];
        for (int i = 0; i < shortArrSize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;

    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte
        File file = new File(this.getFilesDir(), filename);
        String filePath = file.getAbsolutePath();
        Log.e("path",filePath);
        short sData[] = new short[BufferElements2Rec];
        FileOutputStream os;
        os = null;
        try {
            os = new FileOutputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format
            recorder.read(sData, 0, BufferElements2Rec);
            try {
                byte bData[] = short2byte(sData);
                os.write(bData, 0, BufferElements2Rec * BytesPerElement);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
        }
    }
    @SuppressLint("SetTextI18n")
    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Record to the external cache directory for visibility
        saveFileName = getExternalCacheDir().getAbsolutePath();
        saveFileName += "/current_audio.pcm";


        ActivityCompat.requestPermissions(this,
                permissions,
                REQUEST_RECORD_AUDIO_PERMISSION);
        ActivityCompat.requestPermissions(this,
                permissions,
                WRITE_EXTERNAL_STORAGE_PERMISSION);

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

            freq1 = Double.parseDouble(start);
            freq2 = Double.parseDouble(end);

            genTone();
            playSound();
            // writeWAV();
            //getFeatures();

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
            System.out.println(isRecording);
            if(!isRecording) {
                startRecording();
                recordButton.setText("STOP");
                isRecording = true;
            } else {
                isRecording = false;
                stopRecording();
                recordButton.setText("RECORD");
            }
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
        if (audioTrack != null)
            audioTrack.release();
    }

    void genTone(){

        double c;
        for (int i = 0; i < numSample; i++) {
            c = (freq2 - freq1) / (double) duration;

            sample[i] = Math.sin(2 * Math.PI * (c/2 * i/sampleRate + freq1) * i/sampleRate);
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

    private void toastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    void writeWAV(){
        long dataSize = generatedSnd.length/2;
        long chunk2Size =  dataSize * channels * bitsPerSample/8;
        long chunkSize = 36 + chunk2Size;

        try {

            OutputStream os;
            os = new FileOutputStream(new File(this.getFilesDir(), filename));
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream outFile = new DataOutputStream(bos);

            outFile.writeBytes("RIFF");                                 // 00 - RIFF
            outFile.write(intToByteArray((int) chunkSize), 0, 4);      // 04 - how big is the rest of this file?
            outFile.writeBytes("WAVE");                                 // 08 - WAVE
            outFile.writeBytes("fmt ");                                 // 12 - fmt
            outFile.write(intToByteArray((int) subChunk1Size), 0, 4);  // 16 - size of this chunk
            outFile.write(shortToByteArray((short) format), 0, 2);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
            outFile.write(shortToByteArray((short) channels), 0, 2);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
            outFile.write(intToByteArray(sampleRate), 0, 4);     // 24 - samples per second (numbers per second)
            outFile.write(intToByteArray((int) byteRate), 0, 4);       // 28 - bytes per second
            outFile.write(shortToByteArray((short) blockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray((short) bitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                                 // 36 - data
            outFile.write(intToByteArray((int) dataSize), 0, 4);       // 40 - how big is this data chunk
            outFile.write(generatedSnd);                                    // 44 - the actual data itself - just a long string of numbers

            outFile.flush();
            outFile.close();
        }
        catch (IOException e){
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    private static byte[] intToByteArray(int i)
    {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0x00FF);
        b[1] = (byte) ((i >> 8) & 0x000000FF);
        b[2] = (byte) ((i >> 16) & 0x000000FF);
        b[3] = (byte) ((i >> 24) & 0x000000FF);
        return b;
    }

    public static byte[] shortToByteArray(short data)
    {
        return new byte[]{(byte)(data & 0xff),(byte)((data >>> 8) & 0xff)};
    }

    void getFeatures(){
        int defaultSampleRate = -1;		//-1 value implies the method to use default sample rate
        int defaultAudioDuration = -1;	//-1 value implies the method to process complete audio duration

        JLibrosa jLibrosa = new JLibrosa();

        /* To read the magnitude values of audio files - equivalent to librosa.load('../audioFiles/1995-1826-0003.wav', sr=None) function */

        try {

            File file = new File(this.getFilesDir(),filename);
            Log.e("path", String.format("%s",file.getAbsolutePath()));
            float audioFeatureValues[] = jLibrosa.loadAndRead(file.getAbsolutePath(), defaultSampleRate, defaultAudioDuration);

            ArrayList<Float> audioFeatureValuesList = jLibrosa.loadAndReadAsList(file.getAbsolutePath(), defaultSampleRate, defaultAudioDuration);


            for (int i = 0; i < 10; i++) {
                Log.e("AudioFeature", String.format("%f", audioFeatureValues[i]));
            }


            /* To read the no of frames present in audio file*/
            int nNoOfFrames = jLibrosa.getNoOfFrames();


            /* To read sample rate of audio file */
            int sampleRate = jLibrosa.getSampleRate();

            /* To read number of channels in audio file */
            int noOfChannels = jLibrosa.getNoOfChannels();

            Complex[][] stftComplexValues = jLibrosa.generateSTFTFeatures(audioFeatureValues, sampleRate, 40);


            float[] invSTFTValues = jLibrosa.generateInvSTFTFeatures(stftComplexValues, sampleRate, 40);
            Log.e("invSTFT value", String.format("%f", invSTFTValues[0]));

            float[][] melSpectrogram = jLibrosa.generateMelSpectroGram(audioFeatureValues, sampleRate, 2048, 128, 256);
            Log.e("melSpectrogram value", String.format("%f", melSpectrogram[0][0]));

            /* To read the MFCC values of an audio file
             *equivalent to librosa.feature.mfcc(x, sr, n_mfcc=40) in python
             * */

            float[][] mfccValues = jLibrosa.generateMFCCFeatures(audioFeatureValues, sampleRate, 40);

            float[] meanMFCCValues = jLibrosa.generateMeanMFCCFeatures(mfccValues, mfccValues.length, mfccValues[0].length);
            Log.e("meanMFCC value", String.format("%f", meanMFCCValues[0]));
            //System.out.println(".......");
            //System.out.println("Size of MFCC Feature Values: (" + mfccValues.length + " , " + mfccValues[0].length + " )");

           /* for (int i = 0; i < 1; i++) {
                for (int j = 0; j < 10; j++) {
                    System.out.printf("%.6f%n", mfccValues[i][j]);
                }
            }*/



            /* To read the STFT values of an audio file
             *equivalent to librosa.core.stft(x, sr, n_mfcc=40) in python
             *Note STFT values return would be complex in nature with real and imaginary values.
             * */

            Complex[][] stftComplexValues1 = jLibrosa.generateSTFTFeatures(audioFeatureValues, sampleRate, 40);


            float[] invSTFTValues1 = jLibrosa.generateInvSTFTFeatures(stftComplexValues, sampleRate, 40);

           /* System.out.println(".......");
            System.out.println("Size of STFT Feature Values: (" + stftComplexValues.length + " , " + stftComplexValues[0].length + " )");


            for (int i = 0; i < 1; i++) {
                for (int j = 0; j < 10; j++) {
                    double realValue = stftComplexValues[i][j].getReal();
                    double imageValue = stftComplexValues[i][j].getImaginary();
                    System.out.println("Real and Image values of STFT are " + realValue + "," + imageValue);
                }

            }*/
        }
        catch (IOException | WavFileException | FileFormatNotSupportedException e){
            Log.e("Exception", "File write failed: " + e.toString());
        }

    }
}