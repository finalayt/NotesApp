package com.example.notes.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.example.notes.R;
import com.example.notes.data.Categories;
import com.example.notes.data.DatabaseHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** Общая часть экранов создания и редактирования заметки. */
public abstract class NoteFormFragment extends Fragment {

    protected DatabaseHelper db;
    protected String[] categories;
    protected TextInputLayout titleLayout;
    protected TextInputEditText titleEdit;
    protected TextInputEditText contentEdit;
    protected Spinner categorySpinner;

    @StringRes
    protected abstract int getScreenTitle();

    protected abstract void onSave(String title, String content, String category);

    /** Вызывается после инициализации полей (здесь экран редактирования заполняет поля). */
    protected void onFormReady() {
    }

    protected boolean hasDelete() {
        return false;
    }

    protected void onDelete() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = DatabaseHelper.getInstance(requireContext());
        categories = Categories.all(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_note_form, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleLayout = view.findViewById(R.id.layout_title);
        titleEdit = view.findViewById(R.id.edit_title);
        contentEdit = view.findViewById(R.id.edit_content);
        categorySpinner = view.findViewById(R.id.spinner_category);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categories);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(spinnerAdapter);

        onFormReady();

        requireActivity().addMenuProvider(menuProvider, getViewLifecycleOwner(),
                Lifecycle.State.RESUMED);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().setTitle(getScreenTitle());
    }

    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
            menuInflater.inflate(R.menu.menu_form, menu);
        }

        @Override
        public void onPrepareMenu(@NonNull Menu menu) {
            MenuItem delete = menu.findItem(R.id.action_delete);
            if (delete != null) {
                delete.setVisible(hasDelete());
            }
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_save) {
                save();
                return true;
            } else if (id == R.id.action_delete) {
                confirmDelete();
                return true;
            }
            return false;
        }
    };

    private void save() {
        String title = String.valueOf(titleEdit.getText()).trim();
        String content = String.valueOf(contentEdit.getText()).trim();
        if (title.isEmpty()) {
            titleLayout.setError(getString(R.string.title_required));
            return;
        }
        titleLayout.setError(null);

        int selected = categorySpinner.getSelectedItemPosition();
        String category = categories[selected < 0 ? categories.length - 1 : selected];

        onSave(title, content, category);
        close();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.dialog_delete_title)
                .setPositiveButton(R.string.yes, (dialog, which) -> {
                    onDelete();
                    close();
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    protected void close() {
        InputMethodManager imm = (InputMethodManager)
                requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && getView() != null) {
            imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
        }
        getParentFragmentManager().popBackStack();
    }
}
