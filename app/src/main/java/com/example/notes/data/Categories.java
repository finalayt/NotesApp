package com.example.notes.data;

import android.content.Context;

import com.example.notes.R;

/** Список категорий хранится в res/values/strings.xml (string-array "categories"). */
public final class Categories {

    private Categories() {
    }

    public static String[] all(Context context) {
        return context.getResources().getStringArray(R.array.categories);
    }

    /** Индекс категории или -1, если такой нет. */
    public static int indexOf(String[] categories, String category) {
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(category)) {
                return i;
            }
        }
        return -1;
    }

    /** Порядок группы в списке: неизвестные категории уходят в конец. */
    public static int order(String[] categories, String category) {
        int index = indexOf(categories, category);
        return index < 0 ? categories.length : index;
    }
}
