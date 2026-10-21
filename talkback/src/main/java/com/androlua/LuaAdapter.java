package com.androlua;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.luajava.LuaException;
import com.luajava.LuaFunction;
import com.luajava.LuaJavaAPI;
import com.luajava.LuaObject;
import com.luajava.LuaState;
import com.luajava.LuaTable;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Step 52: list adapter for loadlayout.lua / ListView scripts: LuaAdapter(context, data, layout).
 * Written for this repo's luajava API (ported from Jieshuo's LuaAdapter; getView rebuilt by hand).
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class LuaAdapter extends BaseAdapter implements Filterable {

    private LuaState L;
    private LuaFunction<View> loadlayout;
    private LuaFunction insert;
    private LuaFunction remove;
    private LuaContext mContext;
    private LuaTable mLayout;
    private LuaTable<Integer, LuaTable<String, Object>> mBaseData;
    private LuaTable<Integer, LuaTable<String, Object>> mData;
    private LuaFunction mLuaFilter;
    private LuaFunction<Animation> mAnimationUtil;
    private LuaTable<String, Object> mTheme;
    private final HashMap<View, Boolean> mStyleCache = new HashMap<View, Boolean>();
    private final HashMap<String, Boolean> loaded = new HashMap<String, Boolean>();
    private ArrayFilter mFilter;
    private CharSequence mPrefix;
    private Resources mRes;
    private boolean mNotifyOnChange = true;
    private boolean updateing;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                notifyDataSetChanged();
                return;
            }
            try {
                LuaTable<Integer, LuaTable<String, Object>> filtered =
                        new LuaTable<Integer, LuaTable<String, Object>>(mData.getLuaState());
                mLuaFilter.call(mBaseData, filtered, mPrefix);
                mData = filtered;
                notifyDataSetChanged();
            } catch (Exception e) {
                e.printStackTrace();
                mContext.sendError("performFiltering", e);
            }
        }
    };

    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence prefix) {
            FilterResults results = new FilterResults();
            mPrefix = prefix;
            if (mData == null) {
                return results;
            }
            if (mLuaFilter != null) {
                mHandler.sendEmptyMessage(1);
                return null;
            }
            results.values = mData;
            results.count = mData.length();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
        }
    }

    private class AsyncLoader extends Thread {
        private LuaContext ctx;
        private String path;

        public Drawable getBitmap(LuaContext luaContext, String str) throws IOException {
            this.ctx = luaContext;
            this.path = str;
            String lower = str.toLowerCase();
            if ((lower.startsWith("http://") || lower.startsWith("https://"))
                    && !LuaBitmap.checkCache(luaContext, str)) {
                if (!loaded.containsKey(path)) {
                    start();
                    loaded.put(path, Boolean.TRUE);
                }
                return new LoadingDrawable(ctx.getContext());
            }
            return new BitmapDrawable(mRes, LuaBitmap.getBitmap(luaContext, str));
        }

        @Override
        public void run() {
            try {
                LuaBitmap.getBitmap(ctx, path);
                mHandler.sendEmptyMessage(0);
            } catch (IOException e) {
                ctx.sendError("AsyncLoader", e);
            }
        }
    }

    public LuaAdapter(LuaContext luaContext, LuaTable layout) throws LuaException {
        this(luaContext, null, layout);
    }

    public LuaAdapter(LuaContext luaContext, LuaTable<Integer, LuaTable<String, Object>> data, LuaTable layout)
            throws LuaException {
        mContext = luaContext;
        mLayout = layout;
        mRes = luaContext.getContext().getResources();
        L = layout.getLuaState();
        if (data == null) {
            data = new LuaTable<Integer, LuaTable<String, Object>>(L);
        }
        mData = data;
        mBaseData = data;
        loadlayout = (LuaFunction<View>) L.getLuaObject("loadlayout").getFunction();
        LuaObject tableLib = L.getLuaObject("table");
        insert = tableLib.getField("insert").getFunction();
        remove = tableLib.getField("remove").getFunction();
        // Same warm-up pass Jieshuo does: builds the layout once so mistakes show up early.
        try {
            loadlayout.call(mLayout, new LuaTable<String, Object>(L), AbsListView.class);
        } catch (Exception e) {
            mContext.sendError("LuaAdapter layout", e);
        }
    }

    // ---------------------------------------------------------------- setting view properties

    private int javaSetListener(Object obj, String name, Object listener) throws LuaException {
        ArrayList<Method> methods =
                LuaJavaAPI.getMethod(obj.getClass(), "setOn" + name.substring(2) + "Listener", false);
        for (Method method : methods) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length == 1 && types[0].isInterface()) {
                L.newTable();
                try {
                    L.pushObjectValue(listener);
                    L.setField(-2, name);
                    Object proxy = L.getLuaObject(-1).createProxy(types[0]);
                    method.invoke(obj, proxy);
                    return 1;
                } catch (Exception e) {
                    throw new LuaException(e);
                } finally {
                    L.pop(1);
                }
            }
        }
        return 0;
    }

    private int javaSetMethod(Object obj, String name, Object value) throws LuaException {
        if (Character.isLowerCase(name.charAt(0))) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
        Class<?> valueClass = value.getClass();
        StringBuilder errors = new StringBuilder();
        ArrayList<Method> methods = LuaJavaAPI.getMethod(obj.getClass(), "set" + name, false);
        for (Method method : methods) {
            Class<?>[] types = method.getParameterTypes();
            if (types.length != 1) {
                continue;
            }
            try {
                if (types[0].isPrimitive()) {
                    if (value instanceof Double || value instanceof Float) {
                        method.invoke(obj, LuaState.convertLuaNumber(
                                Double.valueOf(((Number) value).doubleValue()), types[0]));
                        return 1;
                    }
                    if (value instanceof Long || value instanceof Integer) {
                        method.invoke(obj, LuaState.convertLuaNumber(
                                Long.valueOf(((Number) value).longValue()), types[0]));
                        return 1;
                    }
                    if (value instanceof Boolean) {
                        method.invoke(obj, value);
                        return 1;
                    }
                } else if (types[0].isAssignableFrom(valueClass)) {
                    method.invoke(obj, value);
                    return 1;
                }
            } catch (Exception e) {
                errors.append(e.getMessage());
                errors.append("\n");
            }
        }
        if (errors.length() <= 0) {
            throw new LuaException("Invalid setter " + name + " is not a method.\n");
        }
        throw new LuaException("Invalid setter " + name + ". Invalid Parameters.\n" + errors.toString()
                + valueClass.toString());
    }

    private int javaSetter(Object obj, String name, Object value) throws LuaException {
        if (name.length() > 2 && name.substring(0, 2).equals("on") && (value instanceof LuaFunction)) {
            return javaSetListener(obj, name, value);
        }
        return javaSetMethod(obj, name, value);
    }

    private void setFields(View view, LuaTable<String, Object> table) throws LuaException {
        for (Object o : table.entrySet()) {
            Map.Entry entry = (Map.Entry) o;
            String key = String.valueOf(entry.getKey());
            Object value = entry.getValue();
            if (key.toLowerCase().equals("src")) {
                setHelper(view, value);
            } else if (value != null) {
                javaSetter(view, key, value);
            }
        }
    }

    private void setHelper(View view, Object obj) {
        try {
            if (obj == null) {
                return;
            }
            if (obj instanceof LuaTable) {
                setFields(view, (LuaTable<String, Object>) obj);
                return;
            }
            if (view instanceof TextView) {
                ((TextView) view).setText(obj instanceof CharSequence ? (CharSequence) obj : obj.toString());
                return;
            }
            if (view instanceof ImageView) {
                ImageView imageView = (ImageView) view;
                if (obj instanceof Bitmap) {
                    imageView.setImageBitmap((Bitmap) obj);
                } else if (obj instanceof String) {
                    imageView.setImageDrawable(new AsyncLoader().getBitmap(mContext, (String) obj));
                } else if (obj instanceof Drawable) {
                    imageView.setImageDrawable((Drawable) obj);
                } else if (obj instanceof Number) {
                    imageView.setImageResource(((Number) obj).intValue());
                }
            }
        } catch (Exception e) {
            mContext.sendError("setHelper", e);
        }
    }

    // ---------------------------------------------------------------- data editing (called from Lua)

    public void add(LuaTable<String, Object> item) throws LuaException {
        insert.call(mBaseData, item);
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void addAll(LuaTable<Integer, LuaTable<String, Object>> items) throws LuaException {
        int n = items.length();
        for (int i = 1; i <= n; i++) {
            insert.call(mBaseData, items.get(Integer.valueOf(i)));
        }
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void insert(int index, LuaTable<String, Object> item) throws LuaException {
        insert.call(mBaseData, Integer.valueOf(index + 1), item);
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void remove(int index) throws LuaException {
        remove.call(mBaseData, Integer.valueOf(index + 1));
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void clear() {
        mBaseData.clear();
        if (mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void filter(CharSequence prefix) {
        getFilter().filter(prefix);
    }

    public LuaTable<Integer, LuaTable<String, Object>> getData() {
        return mData;
    }

    // ---------------------------------------------------------------- Adapter

    @Override
    public int getCount() {
        return mData.length();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(Integer.valueOf(position + 1));
    }

    @Override
    public long getItemId(int position) {
        return position + 1;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        LuaTable<String, Object> holder;
        try {
            if (view == null) {
                holder = new LuaTable<String, Object>(L);
                view = loadlayout.call(mLayout, holder, AbsListView.class);
                view.setTag(holder);
            } else {
                holder = (LuaTable<String, Object>) view.getTag();
            }
        } catch (Exception e) {
            mContext.sendError("getView", e);
            return convertView != null ? convertView : new View(mContext.getContext());
        }

        try {
            // Theme (setStyle): applied once per view.
            if (mTheme != null && !mStyleCache.containsKey(view)) {
                for (Object o : mTheme.entrySet()) {
                    Map.Entry entry = (Map.Entry) o;
                    Object target = holder.get(String.valueOf(entry.getKey()));
                    if (target instanceof View && entry.getValue() instanceof LuaTable) {
                        setFields((View) target, (LuaTable<String, Object>) entry.getValue());
                    }
                }
                mStyleCache.put(view, Boolean.TRUE);
            }

            // Row data: every key of the item table is the id of a view in the layout.
            LuaTable<String, Object> item = mData.get(Integer.valueOf(position + 1));
            if (item != null) {
                for (Object o : item.entrySet()) {
                    Map.Entry entry = (Map.Entry) o;
                    Object target = holder.get(String.valueOf(entry.getKey()));
                    if (target instanceof View) {
                        setHelper((View) target, entry.getValue());
                    }
                }
            }

            // Optional animation (setAnimation).
            if (mAnimationUtil != null && !updateing) {
                Animation animation = mAnimationUtil.call();
                if (animation != null) {
                    view.startAnimation(animation);
                }
            }
        } catch (Exception e) {
            mContext.sendError("getView", e);
        }
        return view;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        if (updateing) {
            return;
        }
        updateing = true;
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                updateing = false;
            }
        }, 500L);
    }

    @Override
    public Filter getFilter() {
        if (mFilter == null) {
            mFilter = new ArrayFilter();
        }
        return mFilter;
    }

    // ---------------------------------------------------------------- options

    public void setAnimation(LuaFunction<Animation> function) {
        setAnimationUtil(function);
    }

    public void setAnimationUtil(LuaFunction<Animation> function) {
        mAnimationUtil = function;
    }

    public void setFilter(LuaFunction function) {
        mLuaFilter = function;
    }

    public void setNotifyOnChange(boolean notify) {
        mNotifyOnChange = notify;
        if (notify) {
            notifyDataSetChanged();
        }
    }

    public void setStyle(LuaTable<String, Object> theme) {
        mStyleCache.clear();
        mTheme = theme;
    }
}
