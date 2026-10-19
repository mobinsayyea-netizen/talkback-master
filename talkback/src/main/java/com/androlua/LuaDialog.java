package com.androlua;

import android.accessibilityservice.AccessibilityService;
import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Step 46: dialog class for Lua extensions and tools, written for MS Screen Reader.
 *
 * <p>Same public names that Jieshuo scripts use: {@code LuaDialog(this)}, {@code dlg.View = ...},
 * {@code setTitle}, {@code setMessage}, {@code setButton(text)}, {@code setItems}, {@code setAdapter},
 * {@code show}, {@code hide}, {@code dismiss}. Written from how the scripts call it, not copied.
 *
 * <p>When the context comes from the accessibility service, the window is an accessibility overlay,
 * so the dialog can open on top of any app.
 */
public class LuaDialog extends AlertDialog implements DialogInterface.OnClickListener {

  /** Called when one of the dialog buttons is pressed. */
  public interface OnClickListener {
    void onClick(LuaDialog dialog, Button button);
  }

  private final Context mContext;
  private final ListView mListView;
  private String mMessage;
  private String mTitle;
  private View mView;
  private OnClickListener mOnClickListener;

  public LuaDialog(Context context) {
    super(context, android.R.style.Theme_DeviceDefault_Dialog_Alert);
    mContext = context;
    mListView = new ListView(context);
  }

  public LuaDialog(Context context, int themeResId) {
    super(context, themeResId);
    mContext = context;
    mListView = new ListView(context);
  }

  // ------------------------------------------------------------------ getters

  @Override
  public ListView getListView() {
    return mListView;
  }

  public String getMessage() {
    return mMessage;
  }

  public String getTitle() {
    return mTitle;
  }

  public View getView() {
    return mView;
  }

  // ------------------------------------------------------------------ content

  @Override
  public void setTitle(CharSequence title) {
    if (title != null) {
      mTitle = title.toString();
    }
    super.setTitle(title);
  }

  @Override
  public void setMessage(CharSequence message) {
    mMessage = message == null ? null : message.toString();
    super.setMessage(message);
  }

  @Override
  public void setIcon(Drawable icon) {
    super.setIcon(icon);
  }

  @Override
  public void setView(View view) {
    mView = view;
    if (view != null) {
      view.setLayoutParams(
          new ViewGroup.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }
    super.setView(view);
  }

  public void setAdapter(ListAdapter adapter) {
    if (mView != mListView) {
      setView(mListView);
    }
    mListView.setAdapter(adapter);
  }

  public void setItems(List<String> items) {
    setAdapter(new ArrayAdapter<String>(mContext, android.R.layout.simple_list_item_1, items));
    mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
  }

  public void setItems(String[] items) {
    setItems(new ArrayList<String>(Arrays.asList(items)));
  }

  public void setSingleChoiceItems(CharSequence[] items) {
    setSingleChoiceItems(items, -1);
  }

  public void setSingleChoiceItems(CharSequence[] items, int checked) {
    setAdapter(
        new ArrayAdapter<CharSequence>(
            mContext, android.R.layout.simple_list_item_single_choice, items));
    mListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    if (checked >= 0 && checked < items.length) {
      mListView.setItemChecked(checked, true);
    }
  }

  public void setMultiChoiceItems(CharSequence[] items) {
    setMultiChoiceItems(items, new int[0]);
  }

  public void setMultiChoiceItems(CharSequence[] items, int[] checked) {
    setAdapter(
        new ArrayAdapter<CharSequence>(
            mContext, android.R.layout.simple_list_item_multiple_choice, items));
    mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
    if (checked != null) {
      for (int i : checked) {
        if (i >= 0 && i < items.length) {
          mListView.setItemChecked(i, true);
        }
      }
    }
  }

  // ------------------------------------------------------------------ buttons

  public void setButton(CharSequence text) {
    setOkButton(text);
  }

  public void setButton1(CharSequence text) {
    setButton(DialogInterface.BUTTON_POSITIVE, text, this);
  }

  public void setButton2(CharSequence text) {
    setButton(DialogInterface.BUTTON_NEGATIVE, text, this);
  }

  public void setButton3(CharSequence text) {
    setButton(DialogInterface.BUTTON_NEUTRAL, text, this);
  }

  public void setOkButton(CharSequence text) {
    setButton1(text);
  }

  public void setPosButton(CharSequence text) {
    setButton1(text);
  }

  public void setCancelButton(CharSequence text) {
    setButton2(text);
  }

  public void setNegButton(CharSequence text) {
    setButton2(text);
  }

  public void setNeuButton(CharSequence text) {
    setButton3(text);
  }

  public void setPositiveButton(CharSequence text, DialogInterface.OnClickListener listener) {
    setButton(DialogInterface.BUTTON_POSITIVE, text, listener);
  }

  public void setNegativeButton(CharSequence text, DialogInterface.OnClickListener listener) {
    setButton(DialogInterface.BUTTON_NEGATIVE, text, listener);
  }

  public void setNeutralButton(CharSequence text, DialogInterface.OnClickListener listener) {
    setButton(DialogInterface.BUTTON_NEUTRAL, text, listener);
  }

  public void setOnClickListener(OnClickListener listener) {
    mOnClickListener = listener;
  }

  @Override
  public void onClick(DialogInterface dialog, int which) {
    if (mOnClickListener != null) {
      mOnClickListener.onClick(this, getButton(which));
    }
  }

  // ------------------------------------------------------------------ listeners

  public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
    mListView.setOnItemClickListener(listener);
  }

  public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
    mListView.setOnItemLongClickListener(listener);
  }

  public void setOnItemSelectedListener(AdapterView.OnItemSelectedListener listener) {
    mListView.setOnItemSelectedListener(listener);
  }

  @Override
  public void setOnCancelListener(OnCancelListener listener) {
    super.setOnCancelListener(listener);
  }

  @Override
  public void setOnDismissListener(OnDismissListener listener) {
    super.setOnDismissListener(listener);
  }

  // ------------------------------------------------------------------ show / hide

  @Override
  public void hide() {
    super.hide();
  }

  @Override
  public boolean isShowing() {
    return super.isShowing();
  }

  @Override
  public void show() {
    Window window = getWindow();
    if (window != null && isFromAccessibilityService(mContext)) {
      window.setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
    }
    try {
      super.show();
    } catch (Exception e) {
      // A script must never crash the screen reader because a dialog could not open.
      android.util.Log.w("LuaDialog", "show failed", e);
    }
  }

  private static boolean isFromAccessibilityService(Context context) {
    Context c = context;
    for (int i = 0; i < 10 && c != null; i++) {
      if (c instanceof AccessibilityService) {
        return true;
      }
      if (!(c instanceof ContextWrapper)) {
        return false;
      }
      c = ((ContextWrapper) c).getBaseContext();
    }
    return false;
  }
}
