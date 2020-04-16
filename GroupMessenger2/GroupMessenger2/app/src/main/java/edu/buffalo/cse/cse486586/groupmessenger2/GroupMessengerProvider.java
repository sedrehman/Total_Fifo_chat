package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    static final String PROVIDER_NAME = "edu.buffalo.cse.cse486586.groupmessenger2.provider";
    static final String URL =  "content://" + PROVIDER_NAME;
    static final Uri PROVIDER_URI = Uri.parse(URL);

    private DbHandler handler= new DbHandler(getContext());

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

        Log.e("insert~~~~~~~~~~~", values.toString());
        SQLiteDatabase db = handler.getReadableDatabase();

        db.insert("data", null , values);
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        handler = new DbHandler(getContext());
        return true;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }


    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        Log.e("query~~~~~~~~~~~~~~~: ", selection);
        final Cursor c = handler.getReadableDatabase().rawQuery("SELECT *  FROM data", null);
        //Log.e("query", DatabaseUtils.dumpCursorToString(c));
        return find_right_curson(c, selection);
    }

    private Cursor find_right_curson(Cursor cursor, String find_key){
        Cursor c = null;
        while(cursor.moveToNext()){
            String key = cursor.getString(cursor.getColumnIndex("key"));
            if(key.equals(find_key)){
                c = cursor;
                String value = cursor.getString(cursor.getColumnIndex("value"));
                Log.e("cursor found ", key + " , " + value);
                MatrixCursor cursor1 = new MatrixCursor(new String[] {"key", "value"});
                cursor1.newRow().add("key", key)
                        .add("value", value);
                c= cursor1;
                break;
            }
        }
        if(c == null){
            Log.e("cursor :", find_key + " not found");
        }
        return c;
    }
}
/*
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(handler.getTableName());
        Cursor c = getContext().getContentResolver().query(
                uri,
                null,
                selection,
                null,
                null
        );
        return c;
 */