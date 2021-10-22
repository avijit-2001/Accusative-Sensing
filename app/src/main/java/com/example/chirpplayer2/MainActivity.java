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
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    int duration = 5;
    int sampleRate = 44100;
    int numSample = duration * sampleRate;
    double[] sample = new double[numSample];
    double freq1 = 20;
    double freq2 = 1900;
    byte[] generatedSnd = new byte[2 * numSample];

    AudioTrack audioTrack;
    LineVisualizer lineVisualizer;
    String pcmFilename = "audio.pcm";
    String wavFilename = "audio.wav";

    // Recording Functionality Variables

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int WRITE_EXTERNAL_STORAGE_PERMISSION = 200;
    private Button recordButton = null;


    // Recording

    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord recorder = null;
    private Thread recordingThread = null;
    private Boolean isRecording = false;
    private int bufferSize;
    File pcmFile;
    File wavFile;



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
        pcmFile = new File(this.getFilesDir(), pcmFilename);
        String filePath = pcmFile.getAbsolutePath();
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
            float[] dData = new float[sData.length];
            Filter filter = new Filter(20000,44100, Filter.PassType.Highpass,1);
            for(int i=0; i< sData.length; i++){
                dData[i] = sData[i] / (float)32768 ;
                filter.Update(dData[i]);
                dData[i] = filter.getValue();
                sData[i] = (short) (dData[i] * 32767);
            }

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
            wavFile = new File(this.getFilesDir(), wavFilename);
            try {
                rawToWave(pcmFile, wavFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            getFeatures();
        }
    }
    @SuppressLint("SetTextI18n")
    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


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

    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 1); // number of channels
            writeInt(output, 44100); // sample rate
            writeInt(output, RECORDER_SAMPLERATE * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis= new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        }  catch (IOException e){
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }
    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    void getFeatures(){
        int defaultSampleRate = -1;		//-1 value implies the method to use default sample rate
        int defaultAudioDuration = -1;	//-1 value implies the method to process complete audio duration

        JLibrosa jLibrosa = new JLibrosa();

        /* To read the magnitude values of audio files - equivalent to librosa.load('../audioFiles/1995-1826-0003.wav', sr=None) function */

        try {

            float audioFeatureValues[] = jLibrosa.loadAndRead(wavFile.getAbsolutePath(), defaultSampleRate, defaultAudioDuration);

            ArrayList<Float> audioFeatureValuesList = jLibrosa.loadAndReadAsList(wavFile.getAbsolutePath(), defaultSampleRate, defaultAudioDuration);


            for (int i = 0; i < 10; i++) {
                Log.e("AudioFeature", String.format("%f", audioFeatureValues[i]));
            }


            /* To read the no of frames present in audio file*/
            int noOfFrames = jLibrosa.getNoOfFrames();
            String sNoOfFrames = Integer.toString(noOfFrames);

            /* To read sample rate of audio file */
            int sampleRate = jLibrosa.getSampleRate();
            String sSampleRate = Integer.toString(sampleRate);

            /* To read number of channels in audio file */
            int noOfChannels = jLibrosa.getNoOfChannels();
            String sNoOfChannels = Integer.toString(noOfChannels);

            Complex[][] stftComplexValues = jLibrosa.generateSTFTFeatures(audioFeatureValues, sampleRate, 40);
            String sStftComplexValues = Arrays.deepToString(stftComplexValues);

            float[] invSTFTValues = jLibrosa.generateInvSTFTFeatures(stftComplexValues, sampleRate, 40);
            String sInvSTFTValues = Arrays.toString(invSTFTValues);
            Log.e("invSTFT value", String.format("%f", invSTFTValues[0]));

            float[][] melSpectrogram = jLibrosa.generateMelSpectroGram(audioFeatureValues, sampleRate, 2048, 128, 256);
            String sMelSpectrogram = Arrays.deepToString(melSpectrogram);
            Log.e("melSpectrogram value", String.format("%f", melSpectrogram[0][0]));

            /* To read the MFCC values of an audio file
             *equivalent to librosa.feature.mfcc(x, sr, n_mfcc=40) in python
             * */

            float[][] mfccValues = jLibrosa.generateMFCCFeatures(audioFeatureValues, sampleRate, 40);
            String sMfccValues = Arrays.deepToString(mfccValues);

            float[] meanMFCCValues = jLibrosa.generateMeanMFCCFeatures(mfccValues, mfccValues.length, mfccValues[0].length);
            String sMeanMFCCValues = Arrays.toString(meanMFCCValues);
            Log.e("meanMFCC value", String.format("%f", meanMFCCValues[0]));
            //System.out.println(".......");
            //System.out.println("Size of MFCC Feature Values: (" + mfccValues.length + " , " + mfccValues[0].length + " )");

           /* for (int i = 0; i < 1; i++) {
                for (int j = 0; j < 10; j++) {
                    System.out.printf("%.6f%n", mfccValues[i][j]);
                }
            }*/

            myDatabase.addData(sNoOfFrames, sSampleRate, sNoOfChannels, sStftComplexValues, sInvSTFTValues, sMelSpectrogram, sMfccValues, sMeanMFCCValues);

            /* To read the STFT values of an audio file
             *equivalent to librosa.core.stft(x, sr, n_mfcc=40) in python
             *Note STFT values return would be complex in nature with real and imaginary values.
             * */

            //Complex[][] stftComplexValues1 = jLibrosa.generateSTFTFeatures(audioFeatureValues, sampleRate, 40);


            //float[] invSTFTValues1 = jLibrosa.generateInvSTFTFeatures(stftComplexValues, sampleRate, 40);
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