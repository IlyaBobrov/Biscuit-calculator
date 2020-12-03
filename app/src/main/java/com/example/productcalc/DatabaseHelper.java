package com.example.productcalc;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class DatabaseHelper extends SQLiteOpenHelper {

    public static int DB_VERSION = 1;
    public static final String DB_NAME = "productsDB";
    public static final String TABLE_PRODUCTS = "products";

    public static final String KEY_ID = "_id";
    public static final String KEY_NAME = "name";
    public static final String KEY_WEIGHT = "weight";
    public static final String KEY_PRICE = "price";
    public static final String KEY_RES_UNIT = "res_unit";
    public static final String KEY_UNITS = "units";
    public static final String KEY_RES_UNITS = "res_units";
    public static final String KEY_MEASURE = "measure";


    public DatabaseHelper(@Nullable Context context, @Nullable SQLiteDatabase.CursorFactory factory) {
        super(context, DB_NAME, factory, DB_VERSION);
    }

    public DatabaseHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + "(" +
                KEY_ID + " INTEGER PRIMARY KEY, " +
                KEY_NAME     + " TEXT, " +
                KEY_WEIGHT   + " REAL, " +
                KEY_PRICE    + " REAL, " +
                KEY_RES_UNIT + " REAL, " +
                KEY_UNITS    + " INTEGER," +
                KEY_RES_UNITS+ " REAL, " +
                KEY_MEASURE  + " TEXT " +
                ")");

        db.execSQL("INSERT INTO "+ TABLE_PRODUCTS +" (" +
                KEY_NAME + ", " +  KEY_WEIGHT  + ", " + KEY_PRICE  + ", " +  KEY_RES_UNIT  + ", " + KEY_UNITS  + ", " + KEY_RES_UNITS  + ", " + KEY_MEASURE +
                ") VALUES ('Рулет', 200, 60, 0.3, 100, 30.0, 'г');");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

}
