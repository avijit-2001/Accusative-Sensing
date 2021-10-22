package com.example.chirpplayer2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final  String TAG = "DatabaseHelper";
    private static final String TABLE_NAME = "features";


    public DatabaseHelper(Context context) {
        // creating a single table with name TABLE name
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTablePeople = "CREATE TABLE " + TABLE_NAME + "(ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                " noOfFrames TEXT, sampleRate TEXT, noOfChannels TEXT, stftComplexValues TEXT, invSTFTValues," +
                "melSpectrogram TEXT, mfccValues TEXT, meanMFCCValues TEXT)";
        sqLiteDatabase.execSQL(createTablePeople);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS people_table");
        onCreate(sqLiteDatabase);
    }

    public boolean addData(String noOfFrames, String sampleRate, String noOfChannels, String stftComplexValues, String invSTFTValues, String melSpectrogram, String mfccValues, String meanMFCCValues) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("noOfFrames",noOfFrames);
        contentValues.put("sampleRate",sampleRate);
        contentValues.put("noOfChannels",noOfChannels);
        contentValues.put("stftComplexValues",stftComplexValues);
        contentValues.put("invSTFTValues",invSTFTValues);
        contentValues.put("melSpectrogram",melSpectrogram);
        contentValues.put("mfccValues",mfccValues);
        contentValues.put("meanMFCCValues",meanMFCCValues);
        Log.d(TAG, "addData: Adding " + sampleRate + " to " + TABLE_NAME);

        long result = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
        return result != -1;
    }

    public Cursor getData(){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        return db.rawQuery(query, null);
    }


}
