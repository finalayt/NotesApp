package com.example.notes.ui;

import com.example.notes.R;
import com.example.notes.data.Note;

/** Фрагмент создания новой заметки. */
public class AddNoteFragment extends NoteFormFragment {

    @Override
    protected int getScreenTitle() {
        return R.string.title_new_note;
    }

    @Override
    protected void onSave(String title, String content, String category) {
        db.insert(new Note(title, content, category, System.currentTimeMillis()));
    }
}
