package database;

public class SQLDbSchema {
    public static final class SearchHistoryTable{
        public static final String NAME = "searchhistory";

        public static final class Cols{
            public static final String VALUE = "value";
        }
    }

    public static final class SearchFilterTable{
        public static final String NAME = "searchfilters";

        public static final class Cols{
            public static final String NAME = "name";
            public static final String CATEGORY = "category";
            public static final String PRICEMIN = "pricemin";
            public static final String PRICEMAX = "pricemax";
            public static final String CHECKEDCODE = "checkedcode";
        }
    }
}
