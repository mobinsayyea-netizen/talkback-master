package com.google.android.accessibility.talkback.clipboard;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

/**
 * Clipboard history screen. Shows every copied text. Double tap on an item pastes it into the edit
 * box the screen was opened from (or copies it when there is no edit box). Long press on an item
 * gives Add to favorites, Delete and Select several.
 */
public class ClipboardActivity extends Activity {

  private static final int MAX_SHOWN_LENGTH = 600;

  private final List<ClipboardStore.Entry> shown = new ArrayList<>();
  private ArrayAdapter<ClipboardStore.Entry> adapter;
  private ListView list;
  private TextView emptyView;
  private LinearLayout normalRow;
  private LinearLayout manageRow;
  private Button favoritesOnlyButton;

  private boolean manageMode = false;
  private boolean favoritesOnly = false;
  private ClipboardBridge.PasteTarget pasteTarget;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setTitle("Clipboard");
    pasteTarget = ClipboardBridge.takeTarget();

    LinearLayout layout = new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setBackgroundColor(Color.BLACK);
    int pad = (int) (16 * getResources().getDisplayMetrics().density);
    layout.setPadding(pad, pad, pad, pad);

    TextView heading = new TextView(this);
    heading.setText("Clipboard");
    heading.setTextColor(Color.WHITE);
    heading.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
    if (Build.VERSION.SDK_INT >= 28) {
      heading.setAccessibilityHeading(true);
    }
    layout.addView(
        heading,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    emptyView = new TextView(this);
    emptyView.setTextColor(Color.WHITE);
    emptyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
    emptyView.setPadding(0, pad, 0, pad);
    layout.addView(
        emptyView,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    list = new ListView(this);
    list.setOnItemClickListener(
        (parent, view, position, id) -> {
          if (!manageMode && position < shown.size()) {
            choose(shown.get(position));
          }
        });
    list.setOnItemLongClickListener(
        (parent, view, position, id) -> {
          if (position < shown.size()) {
            showItemMenu(shown.get(position));
          }
          return true;
        });
    layout.addView(
        list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

    normalRow = new LinearLayout(this);
    normalRow.setOrientation(LinearLayout.HORIZONTAL);
    normalRow.addView(button("Manage", v -> setManageMode(true)), rowParams());
    favoritesOnlyButton =
        button(
            "Favorites only",
            v -> {
              favoritesOnly = !favoritesOnly;
              favoritesOnlyButton.setText(favoritesOnly ? "Show all" : "Favorites only");
              refresh();
            });
    normalRow.addView(favoritesOnlyButton, rowParams());
    normalRow.addView(button("Delete all", v -> confirmDeleteAll()), rowParams());
    layout.addView(
        normalRow,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    manageRow = new LinearLayout(this);
    manageRow.setOrientation(LinearLayout.HORIZONTAL);
    manageRow.addView(button("Favorite", v -> favoriteSelected()), rowParams());
    manageRow.addView(button("Delete", v -> deleteSelected()), rowParams());
    manageRow.addView(button("Select all", v -> selectAll()), rowParams());
    manageRow.addView(button("Done", v -> setManageMode(false)), rowParams());
    manageRow.setVisibility(View.GONE);
    layout.addView(
        manageRow,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    setContentView(layout);
    installAdapter();
  }

  @Override
  protected void onResume() {
    super.onResume();
    refresh();
  }

  @Override
  public void onBackPressed() {
    if (manageMode) {
      setManageMode(false);
    } else {
      super.onBackPressed();
    }
  }

  private Button button(String label, View.OnClickListener listener) {
    Button b = new Button(this);
    b.setText(label);
    b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f);
    b.setOnClickListener(listener);
    return b;
  }

  private LinearLayout.LayoutParams rowParams() {
    return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
  }

  private void installAdapter() {
    final int layoutId =
        manageMode
            ? android.R.layout.simple_list_item_multiple_choice
            : android.R.layout.simple_list_item_1;
    adapter =
        new ArrayAdapter<ClipboardStore.Entry>(this, layoutId, android.R.id.text1, shown) {
          @Override
          public View getView(int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView t = (TextView) v.findViewById(android.R.id.text1);
            t.setText(label(getItem(position)));
            t.setTextColor(Color.WHITE);
            return v;
          }
        };
    list.clearChoices();
    list.setChoiceMode(manageMode ? ListView.CHOICE_MODE_MULTIPLE : ListView.CHOICE_MODE_NONE);
    list.setAdapter(adapter);
  }

  private static String label(ClipboardStore.Entry e) {
    String t = e.text;
    if (t.length() > MAX_SHOWN_LENGTH) {
      t = t.substring(0, MAX_SHOWN_LENGTH) + "... (long text)";
    }
    return e.favorite ? "Favorite. " + t : t;
  }

  private void refresh() {
    shown.clear();
    for (ClipboardStore.Entry e : ClipboardStore.all(this)) {
      if (!favoritesOnly || e.favorite) {
        shown.add(e);
      }
    }
    list.clearChoices();
    adapter.notifyDataSetChanged();
    emptyView.setText(favoritesOnly ? "No favorites yet." : "No copied text yet.");
    emptyView.setVisibility(shown.isEmpty() ? View.VISIBLE : View.GONE);
  }

  private void setManageMode(boolean on) {
    manageMode = on;
    normalRow.setVisibility(on ? View.GONE : View.VISIBLE);
    manageRow.setVisibility(on ? View.VISIBLE : View.GONE);
    installAdapter();
    refresh();
    list.announceForAccessibility(on ? "Select items" : "Selection finished");
  }

  /** Copies the item, and pastes it when the screen was opened from an edit box. */
  private void choose(ClipboardStore.Entry entry) {
    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
    clipboard.setPrimaryClip(ClipData.newPlainText("MS Screen Reader", entry.text));
    ClipboardBridge.PasteTarget target = pasteTarget;
    pasteTarget = null;
    finish();
    if (target != null) {
      target.pasteChosenText();
    } else {
      Toast.makeText(getApplicationContext(), "Copied", Toast.LENGTH_SHORT).show();
    }
  }

  private void showItemMenu(final ClipboardStore.Entry entry) {
    final String[] items = {
      entry.favorite ? "Remove from favorites" : "Add to favorites", "Delete", "Select several"
    };
    new AlertDialog.Builder(this)
        .setTitle("Clipboard item")
        .setItems(
            items,
            (dialog, which) -> {
              List<Long> ids = new ArrayList<>();
              ids.add(entry.id);
              if (which == 0) {
                ClipboardStore.setFavorite(this, ids, !entry.favorite);
                refresh();
                list.announceForAccessibility(
                    entry.favorite ? "Removed from favorites" : "Added to favorites");
              } else if (which == 1) {
                ClipboardStore.delete(this, ids);
                refresh();
                list.announceForAccessibility("Deleted");
              } else {
                setManageMode(true);
              }
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private void confirmDeleteAll() {
    new AlertDialog.Builder(this)
        .setTitle("Delete all")
        .setMessage("Delete all copied text? Favorites will stay.")
        .setPositiveButton(
            "Delete",
            (dialog, which) -> {
              ClipboardStore.deleteAllKeepFavorites(this);
              refresh();
              list.announceForAccessibility("Deleted. Favorites are kept");
            })
        .setNegativeButton("Cancel", null)
        .show();
  }

  private List<ClipboardStore.Entry> selectedEntries() {
    List<ClipboardStore.Entry> result = new ArrayList<>();
    SparseBooleanArray checked = list.getCheckedItemPositions();
    if (checked == null) {
      return result;
    }
    for (int i = 0; i < checked.size(); i++) {
      int position = checked.keyAt(i);
      if (checked.valueAt(i) && position >= 0 && position < shown.size()) {
        result.add(shown.get(position));
      }
    }
    return result;
  }

  private void selectAll() {
    for (int i = 0; i < shown.size(); i++) {
      list.setItemChecked(i, true);
    }
    list.announceForAccessibility("All selected");
  }

  private void favoriteSelected() {
    List<ClipboardStore.Entry> selected = selectedEntries();
    if (selected.isEmpty()) {
      list.announceForAccessibility("Nothing selected");
      return;
    }
    boolean makeFavorite = false;
    List<Long> ids = new ArrayList<>();
    for (ClipboardStore.Entry e : selected) {
      ids.add(e.id);
      if (!e.favorite) {
        makeFavorite = true;
      }
    }
    ClipboardStore.setFavorite(this, ids, makeFavorite);
    refresh();
    list.announceForAccessibility(makeFavorite ? "Added to favorites" : "Removed from favorites");
  }

  private void deleteSelected() {
    List<ClipboardStore.Entry> selected = selectedEntries();
    if (selected.isEmpty()) {
      list.announceForAccessibility("Nothing selected");
      return;
    }
    List<Long> ids = new ArrayList<>();
    for (ClipboardStore.Entry e : selected) {
      ids.add(e.id);
    }
    ClipboardStore.delete(this, ids);
    refresh();
    list.announceForAccessibility("Deleted");
  }
}
