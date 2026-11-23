package com.example.notes.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.notes.MainActivity;
import com.example.notes.R;
import com.example.notes.data.Categories;
import com.example.notes.data.DatabaseHelper;
import com.example.notes.data.Note;
import com.example.notes.data.SortMode;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Фрагмент со списком заметок (RecyclerView), поиском, сортировкой,
 * группировкой по категориям, drag&drop и удалением свайпом.
 */
public class NoteListFragment extends Fragment {

    private static final String PREFS = "notes_prefs";
    private static final String KEY_SORT = "sort";
    private static final String KEY_GROUP = "group";

    private DatabaseHelper db;
    private SharedPreferences prefs;
    private NotesAdapter adapter;
    private FloatingActionButton fab;
    private TextView emptyView;

    private SortMode sortMode;
    private boolean grouped;
    private String query = "";
    private boolean orderChanged = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());
        prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sortMode = SortMode.valueOf(prefs.getString(KEY_SORT, SortMode.MANUAL.name()));
        grouped = prefs.getBoolean(KEY_GROUP, false);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RecyclerView recyclerView = view.findViewById(R.id.recycler_notes);
        emptyView = view.findViewById(R.id.text_empty);
        fab = view.findViewById(R.id.fab_add);

        adapter = new NotesAdapter(note ->
                ((MainActivity) requireActivity()).openEditNote(note.getId()));
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        new ItemTouchHelper(new TouchCallback()).attachToRecyclerView(recyclerView);

        fab.setOnClickListener(v -> ((MainActivity) requireActivity()).openAddNote());

        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(),
                Lifecycle.State.RESUMED);

        loadNotes();
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(R.string.app_name);
    }

    // ------------------------------------------------------------------ данные

    private void loadNotes() {
        List<Note> notes = db.getNotes(query, sortMode);
        List<Object> items = new ArrayList<>();

        if (grouped) {
            final String[] categories = Categories.all(requireContext());
            // сортировка устойчивая: внутри группы сохраняется выбранный порядок
            Collections.sort(notes, (a, b) -> Integer.compare(
                    Categories.order(categories, a.getCategory()),
                    Categories.order(categories, b.getCategory())));
            String current = null;
            for (Note note : notes) {
                if (!note.getCategory().equals(current)) {
                    current = note.getCategory();
                    items.add(current);
                }
                items.add(note);
            }
        } else {
            items.addAll(notes);
        }

        adapter.setItems(items);
        emptyView.setText(query.isEmpty() ? R.string.empty_list : R.string.nothing_found);
        emptyView.setVisibility(notes.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private boolean isDragAllowed() {
        return sortMode == SortMode.MANUAL && query.isEmpty();
    }

    /**
     * Сохраняет новый порядок. Заметки получают позиции, которые уже были заняты
     * в их группе, поэтому порядок остальных групп (и режима без группировки) не ломается.
     */
    private void persistOrder() {
        List<Note> current = adapter.getNotes();
        Map<String, List<Note>> groups = new LinkedHashMap<>();
        for (Note note : current) {
            String key = grouped ? note.getCategory() : "";
            if (!groups.containsKey(key)) {
                groups.put(key, new ArrayList<Note>());
            }
            groups.get(key).add(note);
        }
        for (List<Note> group : groups.values()) {
            int[] positions = new int[group.size()];
            for (int i = 0; i < group.size(); i++) {
                positions[i] = group.get(i).getPosition();
            }
            Arrays.sort(positions);
            for (int i = 0; i < group.size(); i++) {
                group.get(i).setPosition(positions[i]);
            }
        }
        db.updatePositions(current);
    }

    private void setSortMode(SortMode mode) {
        sortMode = mode;
        prefs.edit().putString(KEY_SORT, mode.name()).apply();
        loadNotes();
    }

    // -------------------------------------------------------------------- меню

    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_list, menu);

            MenuItem searchItem = menu.findItem(R.id.action_search);
            final SearchView searchView = (SearchView) searchItem.getActionView();
            searchView.setQueryHint(getString(R.string.search_hint));
            if (!query.isEmpty()) {
                searchItem.expandActionView();
                searchView.setQuery(query, false);
            }
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String text) {
                    searchView.clearFocus();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String text) {
                    query = text == null ? "" : text;
                    loadNotes();
                    return true;
                }
            });
            searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(@NonNull MenuItem item) {
                    return true;
                }

                @Override
                public boolean onMenuItemActionCollapse(@NonNull MenuItem item) {
                    query = "";
                    loadNotes();
                    return true;
                }
            });
        }

        @Override
        public void onPrepareMenu(@NonNull Menu menu) {
            int checkedId;
            switch (sortMode) {
                case DATE_NEW:
                    checkedId = R.id.sort_date_new;
                    break;
                case DATE_OLD:
                    checkedId = R.id.sort_date_old;
                    break;
                case TITLE:
                    checkedId = R.id.sort_title;
                    break;
                case MANUAL:
                default:
                    checkedId = R.id.sort_manual;
                    break;
            }
            MenuItem sortItem = menu.findItem(checkedId);
            if (sortItem != null) {
                sortItem.setChecked(true);
            }
            MenuItem groupItem = menu.findItem(R.id.action_group);
            if (groupItem != null) {
                groupItem.setChecked(grouped);
            }
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.sort_manual) {
                item.setChecked(true);
                setSortMode(SortMode.MANUAL);
                return true;
            } else if (id == R.id.sort_date_new) {
                item.setChecked(true);
                setSortMode(SortMode.DATE_NEW);
                return true;
            } else if (id == R.id.sort_date_old) {
                item.setChecked(true);
                setSortMode(SortMode.DATE_OLD);
                return true;
            } else if (id == R.id.sort_title) {
                item.setChecked(true);
                setSortMode(SortMode.TITLE);
                return true;
            } else if (id == R.id.action_group) {
                grouped = !grouped;
                item.setChecked(grouped);
                prefs.edit().putBoolean(KEY_GROUP, grouped).apply();
                loadNotes();
                return true;
            } else if (id == R.id.action_delete_all) {
                confirmDeleteAll();
                return true;
            }
            return false;
        }
    };

    private void confirmDeleteAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_all_title)
                .setMessage(R.string.dialog_delete_all_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    db.deleteAll();
                    loadNotes();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    // ---------------------------------------------------- drag&drop и свайп

    private class TouchCallback extends ItemTouchHelper.SimpleCallback {

        TouchCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT);
        }

        @Override
        public int getDragDirs(@NonNull RecyclerView recyclerView,
                               @NonNull RecyclerView.ViewHolder viewHolder) {
            if (!isDragAllowed() || viewHolder.getItemViewType() != NotesAdapter.TYPE_NOTE) {
                return 0;
            }
            return super.getDragDirs(recyclerView, viewHolder);
        }

        @Override
        public int getSwipeDirs(@NonNull RecyclerView recyclerView,
                                @NonNull RecyclerView.ViewHolder viewHolder) {
            if (viewHolder.getItemViewType() != NotesAdapter.TYPE_NOTE) {
                return 0;
            }
            return super.getSwipeDirs(recyclerView, viewHolder);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder,
                              @NonNull RecyclerView.ViewHolder target) {
            int from = viewHolder.getBindingAdapterPosition();
            int to = target.getBindingAdapterPosition();
            if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) {
                return false;
            }
            if (target.getItemViewType() != NotesAdapter.TYPE_NOTE) {
                return false;
            }
            // при группировке перетаскивать можно только внутри своей категории
            if (grouped && !adapter.getNote(from).getCategory()
                    .equals(adapter.getNote(to).getCategory())) {
                return false;
            }
            adapter.move(from, to);
            orderChanged = true;
            return true;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            int position = viewHolder.getBindingAdapterPosition();
            if (position == RecyclerView.NO_POSITION) {
                return;
            }
            final Note note = adapter.getNote(position);
            db.delete(note.getId());
            loadNotes();

            Snackbar.make(requireView(), R.string.note_deleted, Snackbar.LENGTH_LONG)
                    .setAnchorView(fab)
                    .setAction(R.string.undo, v -> {
                        db.restore(note);
                        loadNotes();
                    })
                    .show();
        }

        @Override
        public void clearView(@NonNull RecyclerView recyclerView,
                              @NonNull RecyclerView.ViewHolder viewHolder) {
            super.clearView(recyclerView, viewHolder);
            if (orderChanged) {
                orderChanged = false;
                persistOrder();
            }
        }
    }
}
