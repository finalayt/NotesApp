package com.example.notes.data;

/** Модель заметки. */
public class Note {
    private long id;
    private String title;
    private String content;
    private String category;
    private long date;      // время последнего изменения (мс)
    private int position;   // порядок для ручной сортировки (drag&drop)

    public Note() {
    }

    public Note(String title, String content, String category, long date) {
        this.title = title;
        this.content = content;
        this.category = category;
        this.date = date;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public long getDate() { return date; }
    public void setDate(long date) { this.date = date; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
