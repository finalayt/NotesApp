package com.example.notes.ui;

import android.os.Bundle;
import android.widget.Toast;

import com.example.notes.R;
import com.example.notes.data.Categories;
import com.example.notes.data.Note;

/** Фрагмент редактирования существующей заметки (открывается по нажатию на заметку). */
public class EditNoteFragment extends NoteFormFragment {

    private static final String ARG_NOTE_ID = "note_id";

    private Note note;

    public static EditNoteFragment newInstance(long noteId) {
        EditNoteFragment fragment = new EditNoteFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_NOTE_ID, noteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getScreenTitle() {
        return R.string.title_edit_note;
    }

    @Override
    protected boolean hasDelete() {
        return true;
    }

    @Override
    protected void onFormReady() {
        note = db.getNote(requireArguments().getLong(ARG_NOTE_ID));
        if (note == null) {
            Toast.makeText(requireContext(), R.string.note_not_found, Toast.LENGTH_SHORT).show();
            getParentFragmentManager().popBackStack();
            return;
        }
        titleEdit.setText(note.getTitle());
        contentEdit.setText(note.getContent());
        int index = Categories.indexOf(categories, note.getCategory());
        categorySpinner.setSelection(Math.max(index, 0));
    }

    @Override
    protected void onSave(String title, String content, String category) {
        if (note == null) {
            return;
        }
        String oldContent = note.getContent() == null ? "" : note.getContent();
        boolean changed = !title.equals(note.getTitle())
                || !content.equals(oldContent)
                || !category.equals(note.getCategory());
        if (changed) {
            note.setTitle(title);
            note.setContent(content);
            note.setCategory(category);
            note.setDate(System.currentTimeMillis());
            db.update(note);
        }
    }

    @Override
    protected void onDelete() {
        if (note != null) {
            db.delete(note.getId());
        }
    }
}
