package com.example.chirpplayer2;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class DatabaseHelper extends SQLiteOpenHelper {

    private static final  String TAG = "DatabaseHelper";
    private static final String TABLE_NAME = "people_table";


    public DatabaseHelper(Context context) {
        // creating a single table with name TABLE name
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String createTablePeople = "CREATE TABLE people_table (ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                " name TEXT)";
        sqLiteDatabase.execSQL(createTablePeople);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS people_table");
        onCreate(sqLiteDatabase);
    }

    public boolean addData(String name) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("name", name);

        Log.d(TAG, "addData: Adding " + name + " to " + TABLE_NAME);

        long result = sqLiteDatabase.insert(TABLE_NAME, null, contentValues);
        if(result == -1) {
            return false;
        } else {
            return true;
        }
    }

    public Cursor getData(){
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT * FROM " + TABLE_NAME;
        return db.rawQuery(query, null);
    }


}
