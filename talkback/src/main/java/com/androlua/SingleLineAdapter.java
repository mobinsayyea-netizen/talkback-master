package com.androlua;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.List;

/**
 * Step 46: list adapter for Lua scripts, one line per item (written for Advanced Screen Reader).
 *
 * <p>Used like {@code SingleLineAdapter(service, String(items))}. Items longer than 5000
 * characters are cut so a huge text cannot freeze the list.
 */
public class SingleLineAdapter extends ArrayAdapter<String> {
  private static final int MAX_ITEM_LENGTH = 5000;

  public SingleLineAdapter(Context context, int resource, List<String> items) {
    super(context, resource, items);
  }

  public SingleLineAdapter(Context context, int resource, String[] items) {
    super(context, resource, items);
  }

  public SingleLineAdapter(Context context, List<String> items) {
    super(context, android.R.layout.simple_list_item_1, items);
  }

  public SingleLineAdapter(Context context, String[] items) {
    super(context, android.R.layout.simple_list_item_1, items);
  }

  @Override
  public String getItem(int position) {
    String s = super.getItem(position);
    return (s == null || s.length() <= MAX_ITEM_LENGTH) ? s : s.substring(0, MAX_ITEM_LENGTH);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    try {
      View v = super.getView(position, convertView, parent);
      if (convertView == null && v instanceof TextView) {
        ((TextView) v).setSingleLine(true);
      }
      return v;
    } catch (Exception e) {
      TextView t = convertView instanceof TextView ? (TextView) convertView : new TextView(getContext());
      t.setText("");
      return t;
    }
  }
}
