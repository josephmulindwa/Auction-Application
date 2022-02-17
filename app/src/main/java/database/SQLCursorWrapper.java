package database;

import android.database.Cursor;
import android.database.CursorWrapper;

import database.SQLDbSchema.SearchFilterTable;
import model.SearchFilter;
import model.SearchHistory;

public class SQLCursorWrapper extends CursorWrapper {
    public SQLCursorWrapper(Cursor cursor){
        super(cursor);
    }

    public SearchFilter getSearchFilter(){
        String name = getString(getColumnIndex(SearchFilterTable.Cols.NAME));
        String category = getString(getColumnIndex(SearchFilterTable.Cols.CATEGORY));
        int priceMin = getInt(getColumnIndex(SearchFilterTable.Cols.PRICEMIN));
        int priceMax = getInt(getColumnIndex(SearchFilterTable.Cols.PRICEMAX));
        int checkedCode = getInt(getColumnIndex(SearchFilterTable.Cols.CHECKEDCODE));

        SearchFilter filter = new SearchFilter(name, category, priceMin, priceMax);
        filter.setCheckedCode((byte) checkedCode);
        return filter;
    }

    public SearchHistory getSearchHistory(){
        String value = getString(getColumnIndex(SQLDbSchema.SearchHistoryTable.Cols.VALUE));
        return new SearchHistory(value);
    }

}
