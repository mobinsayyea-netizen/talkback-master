package android.widget;

import android.R;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import com.luajava.LuaException;
import com.luajava.LuaFunction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ArrayListAdapter<T> extends BaseAdapter implements Filterable {
    private Context mContext;
    private int mDropDownResource;
    private ArrayListAdapter<T>.ArrayFilter mFilter;
    private LayoutInflater mInflater;
    private boolean mIsFiler;
    private LuaFunction mLuaFilter;
    private ArrayList<T> mObjects;
    private ArrayList<T> mOriginalValues;
    private int mResource;
    private final Object mLock = new Object();
    private int mFieldId = 0;
    private boolean mNotifyOnChange = true;

    private class ArrayFilter extends Filter {
        private ArrayFilter() {
        }

        @Override // android.widget.Filter
        protected Filter.FilterResults performFiltering(CharSequence charSequence) {
            ArrayList arrayList;
            int size;
            ArrayList arrayList2;
            Filter.FilterResults filterResults = new Filter.FilterResults();
            if (ArrayListAdapter.this.mOriginalValues == null) {
                synchronized (ArrayListAdapter.this.mLock) {
                    ArrayListAdapter.this.mOriginalValues = new ArrayList(ArrayListAdapter.this.mObjects);
                }
            } else if (TextUtils.isEmpty(charSequence)) {
                filterResults.values = new ArrayList(ArrayListAdapter.this.mOriginalValues);
                filterResults.count = ArrayListAdapter.this.mOriginalValues.size();
                ArrayListAdapter.this.mOriginalValues = null;
                return filterResults;
            }
            if (ArrayListAdapter.this.mLuaFilter != null) {
                ArrayList arrayList3 = new ArrayList();
                try {
                    ArrayListAdapter.this.mLuaFilter.call(new ArrayList(ArrayListAdapter.this.mOriginalValues), arrayList3, charSequence);
                } catch (LuaException e5) {
                    e5.printStackTrace();
                }
                filterResults.values = arrayList3;
                filterResults.count = arrayList3.size();
                return filterResults;
            }
            if (charSequence == null || charSequence.length() == 0) {
                synchronized (ArrayListAdapter.this.mLock) {
                    arrayList = new ArrayList(ArrayListAdapter.this.mOriginalValues);
                }
                filterResults.values = arrayList;
                size = arrayList.size();
            } else {
                String lowerCase = charSequence.toString().toLowerCase();
                synchronized (ArrayListAdapter.this.mLock) {
                    arrayList2 = new ArrayList(ArrayListAdapter.this.mOriginalValues);
                }
                int size2 = arrayList2.size();
                ArrayList arrayList4 = new ArrayList();
                for (int i5 = 0; i5 < size2; i5++) {
                    Object obj = arrayList2.get(i5);
                    if (obj.toString().toLowerCase().contains(lowerCase)) {
                        arrayList4.add(obj);
                    }
                }
                filterResults.values = arrayList4;
                size = arrayList4.size();
            }
            filterResults.count = size;
            return filterResults;
        }

        @Override // android.widget.Filter
        protected void publishResults(CharSequence charSequence, Filter.FilterResults filterResults) {
            ArrayListAdapter.this.mObjects = (ArrayList) filterResults.values;
            if (filterResults.count > 0) {
                ArrayListAdapter.this.notifyDataSetChanged();
            } else {
                ArrayListAdapter.this.notifyDataSetInvalidated();
            }
        }
    }

    public ArrayListAdapter(Context context) {
        init(context, R.layout.simple_list_item_1, 0, new ArrayList());
    }

    public ArrayListAdapter(Context context, int i5) {
        init(context, i5, 0, new ArrayList());
    }

    public ArrayListAdapter(Context context, int i5, int i6) {
        init(context, i5, i6, new ArrayList());
    }

    public ArrayListAdapter(Context context, int i5, int i6, List<T> list) {
        init(context, i5, i6, list);
    }

    public ArrayListAdapter(Context context, int i5, int i6, T[] tArr) {
        init(context, i5, i6, Arrays.asList(tArr));
    }

    public ArrayListAdapter(Context context, int i5, List<T> list) {
        init(context, i5, 0, list);
    }

    public ArrayListAdapter(Context context, int i5, T[] tArr) {
        init(context, i5, 0, Arrays.asList(tArr));
    }

    public ArrayListAdapter(Context context, List<T> list) {
        init(context, R.layout.simple_list_item_1, 0, list);
    }

    public ArrayListAdapter(Context context, T[] tArr) {
        init(context, R.layout.simple_list_item_1, 0, Arrays.asList(tArr));
    }

    public static ArrayListAdapter<CharSequence> createFromResource(Context context, int i5, int i6) {
        return new ArrayListAdapter<>(context, i6, context.getResources().getTextArray(i5));
    }

    private View createViewFromResource(int i5, View view, ViewGroup viewGroup, int i6) {
        String obj;
        if (view == null) {
            view = this.mInflater.inflate(i6, viewGroup, false);
        }
        try {
            int i7 = this.mFieldId;
            TextView textView = i7 == 0 ? (TextView) view : (TextView) view.findViewById(i7);
            T item = getItem(i5);
            if (item == null) {
                obj = Integer.toString(i5);
            } else {
                if (item instanceof CharSequence) {
                    textView.setText((CharSequence) item);
                    return view;
                }
                obj = item.toString();
            }
            textView.setText(obj);
            return view;
        } catch (ClassCastException e5) {
            Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException("ArrayAdapter requires the resource ID to be a TextView", e5);
        }
    }

    private void init(Context context, int i5, int i6, List<T> list) {
        this.mContext = context;
        this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        this.mDropDownResource = i5;
        this.mResource = i5;
        this.mObjects = new ArrayList<>(list);
        this.mFieldId = i6;
    }

    public void add(T t4) {
        synchronized (this.mLock) {
            ArrayList<T> arrayList = this.mOriginalValues;
            if (arrayList == null) {
                arrayList = this.mObjects;
            }
            arrayList.add(t4);
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void addAll(Collection<? extends T> collection) {
        synchronized (this.mLock) {
            ArrayList<T> arrayList = this.mOriginalValues;
            if (arrayList == null) {
                arrayList = this.mObjects;
            }
            arrayList.addAll(collection);
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void addAll(T... tArr) {
        synchronized (this.mLock) {
            ArrayList<T> arrayList = this.mOriginalValues;
            if (arrayList == null) {
                arrayList = this.mObjects;
            }
            Collections.addAll(arrayList, tArr);
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void clear() {
        synchronized (this.mLock) {
            ArrayList<T> arrayList = this.mOriginalValues;
            if (arrayList == null) {
                arrayList = this.mObjects;
            }
            arrayList.clear();
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void filter(CharSequence charSequence) {
        this.mIsFiler = !TextUtils.isEmpty(charSequence);
        getFilter().filter(charSequence);
    }

    public Context getContext() {
        return this.mContext;
    }

    @Override // android.widget.Adapter
    public int getCount() {
        return this.mObjects.size();
    }

    public ArrayList<T> getData() {
        return this.mObjects;
    }

    @Override // android.widget.BaseAdapter, android.widget.SpinnerAdapter
    public View getDropDownView(int i5, View view, ViewGroup viewGroup) {
        return createViewFromResource(i5, view, viewGroup, this.mDropDownResource);
    }

    @Override // android.widget.Filterable
    public Filter getFilter() {
        if (this.mFilter == null) {
            this.mFilter = new ArrayFilter();
        }
        return this.mFilter;
    }

    @Override // android.widget.Adapter
    public T getItem(int i5) {
        return this.mObjects.get(i5);
    }

    @Override // android.widget.Adapter
    public long getItemId(int i5) {
        return i5 + 1;
    }

    public int getPosition(T t4) {
        return this.mObjects.indexOf(t4);
    }

    @Override // android.widget.Adapter
    public View getView(int i5, View view, ViewGroup viewGroup) {
        return createViewFromResource(i5, view, viewGroup, this.mResource);
    }

    public void insert(int i5, T t4) {
        synchronized (this.mLock) {
            ArrayList<T> arrayList = this.mOriginalValues;
            if (arrayList == null) {
                arrayList = this.mObjects;
            }
            arrayList.add(i5, t4);
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public boolean isFilter() {
        return this.mIsFiler;
    }

    @Override // android.widget.BaseAdapter
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
        this.mNotifyOnChange = true;
    }

    public void remove(int i5) {
        synchronized (this.mLock) {
            ArrayList<T> arrayList = this.mOriginalValues;
            if (arrayList == null) {
                arrayList = this.mObjects;
            }
            arrayList.remove(i5);
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void remove(T t4) {
        synchronized (this.mLock) {
            ArrayList<T> arrayList = this.mOriginalValues;
            if (arrayList == null) {
                arrayList = this.mObjects;
            }
            arrayList.remove(t4);
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }

    public void setDropDownViewResource(int i5) {
        this.mDropDownResource = i5;
    }

    public void setFilter(LuaFunction luaFunction) {
        this.mLuaFilter = luaFunction;
    }

    public void setNotifyOnChange(boolean z4) {
        this.mNotifyOnChange = z4;
    }

    public void sort(Comparator<? super T> comparator) {
        synchronized (this.mLock) {
            ArrayList<T> arrayList = this.mOriginalValues;
            if (arrayList == null) {
                arrayList = this.mObjects;
            }
            Collections.sort(arrayList, comparator);
        }
        if (this.mNotifyOnChange) {
            notifyDataSetChanged();
        }
    }
}
