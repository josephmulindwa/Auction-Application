package database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import database.SQLDbSchema.SearchFilterTable;
import database.SQLDbSchema.SearchHistoryTable;

import java.util.ArrayList;

import model.SearchFilter;
import model.SearchHistory;
import util.AppUtils;


public class SQLGateway {
    private static SQLGateway gateway;
    private final Context mContext;
    private final SQLiteDatabase mDatabase;

    private SQLGateway(Context context){
        mContext = context.getApplicationContext();
        mDatabase = new StaucBaseHelper(mContext).getWritableDatabase();
    }

    public static SQLGateway get(Context context){
        if (gateway == null){
            gateway = new SQLGateway(context);
        }
        return gateway;
    }

    public boolean addSearchFilter(SearchFilter filter){
        SearchFilter searchFilter = getSearchFilter(filter.getName());
        if(searchFilter == null) {
            ContentValues values = getContentValues(filter);
            mDatabase.insert(SearchFilterTable.NAME, null, values);
            return true;
        }
        return false;
    }

    public void addSearchHistory(SearchHistory history){
        ArrayList<SearchHistory> histories = getSearchHistory();
        if(histories.size() >= AppUtils.MAX_CACHABLE_HISTORY){
            SearchHistory last = histories.get(0);
            deleteSearchHistory(last);
        }
        SearchHistory searchHistory = getSearchHistory(history.value);
        if(searchHistory == null){
            ContentValues values = getContentValues(history);
            mDatabase.insert(SearchHistoryTable.NAME, null, values);
        }
    }

    public void deleteSearchFilter(SearchFilter filter){
        mDatabase.delete(SearchFilterTable.NAME,
                SearchFilterTable.Cols.NAME + " = ?",
                new String[] { filter.getName() }
                );
    }

    public void deleteSearchHistory(SearchHistory history){
        mDatabase.delete(SearchHistoryTable.NAME,
                SearchHistoryTable.Cols.VALUE + " = ?",
                new String[]{ history.value });
    }

    public void updateSearchFilter(SearchFilter filter){
        if(addSearchFilter(filter)){
            return;
        }
        String name= filter.getName();
        ContentValues values = getContentValues(filter);

        mDatabase.update(SearchFilterTable.NAME, values,
                SearchFilterTable.Cols.NAME + " = ?",
                new String[]{ name });
    }

    public ArrayList<SearchFilter> getSearchFilters(){
        ArrayList<SearchFilter> filters = new ArrayList<>();
        SQLCursorWrapper cursor = querySearchFilters(null, null);
        try{
            cursor.moveToFirst();
            while(!cursor.isAfterLast()){
                filters.add(cursor.getSearchFilter());
                cursor.moveToNext();
            }
        }finally{
            cursor.close();
        }
        return filters;
    }

    public ArrayList<SearchHistory> getSearchHistory(){
        ArrayList<SearchHistory> histories = new ArrayList<>();
        SQLCursorWrapper cursor = querySearchHistory(null, null);
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()){
                histories.add(cursor.getSearchHistory());
                cursor.moveToNext();
            }
        }finally {
            cursor.close();
        }
        return histories;
    }

    public SearchHistory getSearchHistory(String value){
        SQLCursorWrapper cursor = querySearchHistory(SearchHistoryTable.Cols.VALUE + " = ?",
                new String[]{ value });
        try{
            if (cursor.getCount() == 0){
                return null;
            }
            cursor.moveToFirst();
            return cursor.getSearchHistory();
        }finally{
            cursor.close();
        }
    }


    public SearchFilter getSearchFilter(String name){
        SQLCursorWrapper cursor = querySearchFilters(SearchFilterTable.Cols.NAME + " = ?",
                new String[]{ name });
        try{
            if (cursor.getCount() == 0){
                return null;
            }
            cursor.moveToFirst();
            return cursor.getSearchFilter();
        }finally{
            cursor.close();
        }
    }

    private ContentValues getContentValues(SearchFilter filter){
        ContentValues values = new ContentValues();
        values.put(SearchFilterTable.Cols.NAME, filter.getName());
        values.put(SearchFilterTable.Cols.CATEGORY, filter.getCategory());
        values.put(SearchFilterTable.Cols.PRICEMIN, filter.getPriceMin());
        values.put(SearchFilterTable.Cols.PRICEMAX, filter.getPriceMax());
        values.put(SearchFilterTable.Cols.CHECKEDCODE, filter.getCheckedCode());
        return values;
    }

    private ContentValues getContentValues(SearchHistory history){
        ContentValues values = new ContentValues();
        values.put(SearchHistoryTable.Cols.VALUE, history.value);
        return values;
    }

    private SQLCursorWrapper querySearchFilters(String whereClause, String[] whereArgs){
        Cursor cursor = mDatabase.query(SearchFilterTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null);
        return new SQLCursorWrapper(cursor);
    }

    private SQLCursorWrapper querySearchHistory(String whereClause, String[] whereArgs){
        Cursor cursor = mDatabase.query(SearchHistoryTable.NAME,
                null,
                whereClause,
                whereArgs,
                null,
                null,
                null);
        return new SQLCursorWrapper(cursor);
    }
}
