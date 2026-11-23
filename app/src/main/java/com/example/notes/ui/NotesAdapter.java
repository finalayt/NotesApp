package com.example.notes.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.R;
import com.example.notes.data.Note;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Адаптер списка. Элемент списка — либо Note (карточка), либо String (заголовок группы категории).
 */
public class NotesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_NOTE = 1;

    public interface OnNoteClickListener {
        void onNoteClick(Note note);
    }

    private final List<Object> items = new ArrayList<>();
    private final OnNoteClickListener listener;
    private final DateFormat dateFormat =
            new SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault());

    public NotesAdapter(OnNoteClickListener listener) {
        this.listener = listener;
    }

    // ---------------------------------------------------------------- ViewHolders

    class NoteViewHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView date;
        final TextView content;
        final TextView category;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
            date = itemView.findViewById(R.id.text_date);
            content = itemView.findViewById(R.id.text_content);
            category = itemView.findViewById(R.id.text_category);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION && items.get(position) instanceof Note) {
                    listener.onNoteClick((Note) items.get(position));
                }
            });
        }
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView header;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.text_header);
        }
    }

    // -------------------------------------------------------------------- Adapter

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof Note ? TYPE_NOTE : TYPE_HEADER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_header, parent, false));
        }
        return new NoteViewHolder(inflater.inflate(R.layout.item_note, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).header.setText((String) item);
        } else {
            Note note = (Note) item;
            NoteViewHolder h = (NoteViewHolder) holder;
            h.title.setText(note.getTitle());
            h.date.setText(dateFormat.format(new Date(note.getDate())));
            h.category.setText(note.getCategory());
            String content = note.getContent();
            if (content == null || content.isEmpty()) {
                h.content.setVisibility(View.GONE);
            } else {
                h.content.setVisibility(View.VISIBLE);
                h.content.setText(content);
            }
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // -------------------------------------------------------------------- helpers

    public void setItems(List<Object> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public Note getNote(int position) {
        return (Note) items.get(position);
    }

    /** Перемещение при drag&drop. */
    public void move(int from, int to) {
        Object item = items.remove(from);
        items.add(to, item);
        notifyItemMoved(from, to);
    }

    /** Все заметки в текущем порядке на экране (без заголовков групп). */
    public List<Note> getNotes() {
        List<Note> notes = new ArrayList<>();
        for (Object item : items) {
            if (item instanceof Note) {
                notes.add((Note) item);
            }
        }
        return notes;
    }
}
