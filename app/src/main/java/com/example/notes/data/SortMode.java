package com.example.notes.data;

/** Режимы сортировки списка заметок. */
public enum SortMode {
    MANUAL,      // порядок, заданный drag&drop
    DATE_NEW,    // сначала новые
    DATE_OLD,    // сначала старые
    TITLE        // по заголовку А–Я
}
