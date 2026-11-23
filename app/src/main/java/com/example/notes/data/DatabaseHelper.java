package com.example.notes.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Работа с локальной базой данных SQLite через SQLiteOpenHelper. */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "notes.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE = "notes";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DATE = "date_modified";
    public static final String COL_POSITION = "position";
    /**
     * Заголовок + текст в нижнем регистре. SQLite умеет LIKE без учёта регистра
     * только для латиницы, поэтому для корректного поиска по кириллице
     * ищем по заранее приведённой к нижнему регистру строке.
     */
    public static final String COL_SEARCH = "search_key";

    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_TITLE + " TEXT NOT NULL, "
                + COL_CONTENT + " TEXT, "
                + COL_CATEGORY + " TEXT NOT NULL, "
                + COL_DATE + " INTEGER NOT NULL, "
                + COL_POSITION + " INTEGER NOT NULL, "
                + COL_SEARCH + " TEXT"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    // ------------------------------------------------------------------ CREATE

    /** Добавляет заметку в начало списка (ручной порядок). Возвращает id. */
    public long insert(Note note) {
        note.setPosition(minPosition() - 1);
        long id = getWritableDatabase().insert(TABLE, null, toValues(note));
        note.setId(id);
        return id;
    }

    /** Восстанавливает удалённую заметку с прежними id и позицией (для «Отменить»). */
    public void restore(Note note) {
        ContentValues values = toValues(note);
        values.put(COL_ID, note.getId());
        getWritableDatabase().insert(TABLE, null, values);
    }

    // -------------------------------------------------------------------- READ

    public Note getNote(long id) {
        try (Cursor cursor = getReadableDatabase().query(TABLE, null,
                COL_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null)) {
            if (cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
        }
        return null;
    }

    /**
     * Возвращает заметки с учётом поискового запроса и сортировки.
     *
     * @param query пустая строка — без фильтра
     */
    public List<Note> getNotes(String query, SortMode sort) {
        String selection = null;
        String[] selectionArgs = null;
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().toLowerCase(Locale.getDefault())
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            selection = COL_SEARCH + " LIKE ? ESCAPE '\\'";
            selectionArgs = new String[]{"%" + q + "%"};
        }

        String orderBy;
        switch (sort) {
            case DATE_NEW:
                orderBy = COL_DATE + " DESC, " + COL_ID + " DESC";
                break;
            case DATE_OLD:
                orderBy = COL_DATE + " ASC, " + COL_ID + " ASC";
                break;
            case TITLE:
            case MANUAL:
            default:
                orderBy = COL_POSITION + " ASC, " + COL_ID + " DESC";
                break;
        }

        List<Note> notes = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE, null,
                selection, selectionArgs, null, null, orderBy)) {
            while (cursor.moveToNext()) {
                notes.add(fromCursor(cursor));
            }
        }

        if (sort == SortMode.TITLE) {
            // ORDER BY в SQLite не умеет регистронезависимо сортировать кириллицу
            Collections.sort(notes, (a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        }
        return notes;
    }

    // ------------------------------------------------------------------ UPDATE

    public void update(Note note) {
        getWritableDatabase().update(TABLE, toValues(note),
                COL_ID + " = ?", new String[]{String.valueOf(note.getId())});
    }

    /** Сохраняет порядок заметок после drag&drop одной транзакцией. */
    public void updatePositions(List<Note> notes) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Note note : notes) {
                ContentValues values = new ContentValues();
                values.put(COL_POSITION, note.getPosition());
                db.update(TABLE, values, COL_ID + " = ?",
                        new String[]{String.valueOf(note.getId())});
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // ------------------------------------------------------------------ DELETE

    public void delete(long id) {
        getWritableDatabase().delete(TABLE, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public void deleteAll() {
        getWritableDatabase().delete(TABLE, null, null);
    }

    // ----------------------------------------------------------------- helpers

    private int minPosition() {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT MIN(" + COL_POSITION + ") FROM " + TABLE, null)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    private ContentValues toValues(Note note) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, note.getTitle());
        values.put(COL_CONTENT, note.getContent());
        values.put(COL_CATEGORY, note.getCategory());
        values.put(COL_DATE, note.getDate());
        values.put(COL_POSITION, note.getPosition());
        String content = note.getContent() == null ? "" : note.getContent();
        values.put(COL_SEARCH, (note.getTitle() + " " + content).toLowerCase(Locale.getDefault()));
        return values;
    }

    private Note fromCursor(Cursor cursor) {
        Note note = new Note();
        note.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        note.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITLE)));
        note.setContent(cursor.getString(cursor.getColumnIndexOrThrow(COL_CONTENT)));
        note.setCategory(cursor.getString(cursor.getColumnIndexOrThrow(COL_CATEGORY)));
        note.setDate(cursor.getLong(cursor.getColumnIndexOrThrow(COL_DATE)));
        note.setPosition(cursor.getInt(cursor.getColumnIndexOrThrow(COL_POSITION)));
        return note;
    }
}
