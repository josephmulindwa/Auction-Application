package database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import database.SQLDbSchema.SearchHistoryTable;
import database.SQLDbSchema.SearchFilterTable;
import model.SearchFilter;

public class StaucBaseHelper extends SQLiteOpenHelper{
    public static final int VERSION = 1;
    private static final String DATABASE_NAME = "staucBase.db";

    public StaucBaseHelper(Context context){
        super(context, DATABASE_NAME, null, VERSION);
    }

    public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE " + SearchHistoryTable.NAME +"("+
                " _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SearchHistoryTable.Cols.VALUE + ")"
        );

        db.execSQL("CREATE TABLE " + SearchFilterTable.NAME + "("+
                " _id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                SearchFilterTable.Cols.NAME + ", " +
                SearchFilterTable.Cols.CATEGORY + ", " +
                SearchFilterTable.Cols.PRICEMIN + ", " +
                SearchFilterTable.Cols.PRICEMAX + ", " +
                SearchFilterTable.Cols.CHECKEDCODE + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){

    }

}
