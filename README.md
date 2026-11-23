# Заметки (ЛР №1) — Android, Java, SQLite

Открыть в Android Studio: File → Open → папка NotesApp (JDK 17, Android SDK 34).

Архитектура: одна Activity (MainActivity) + фрагменты
- NoteListFragment — список (RecyclerView), FAB «+», поиск, сортировка, группировка, drag&drop, удаление
- AddNoteFragment — создание заметки
- EditNoteFragment — редактирование (по нажатию на заметку), удаление
- NoteFormFragment — общая часть двух форм

БД: DatabaseHelper extends SQLiteOpenHelper (+ Cursor), таблица notes.
