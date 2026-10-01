package com.androlua;

import com.android.cglib.proxy.Enhancer;
import com.android.cglib.proxy.EnhancerInterface;
import com.android.cglib.proxy.MethodFilter;
import com.android.cglib.proxy.MethodInterceptor;

/**
 * Created by nirenr on 2018/12/19.
 */

public class LuaEnhancer {

    // Set once by the MS Screen Reader Lua manager (replaces AndroLua's LuaApplication).
    public static android.content.Context sContext;

    private Enhancer mEnhancer;

    public LuaEnhancer(String cls) throws ClassNotFoundException {
        this(Class.forName(cls));
    }

    public LuaEnhancer(Class<?> cls) {
        mEnhancer = new Enhancer(sContext);
        mEnhancer.setSuperclass(cls);
    }

    public void setInterceptor(EnhancerInterface obj, MethodInterceptor interceptor) {
        obj.setMethodInterceptor_Enhancer(interceptor);
    }

    public Class<?> create() {
        try {
            return mEnhancer.create();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Class<?> create(MethodFilter filer) {
        try {
            mEnhancer.setMethodFilter(filer);
            return mEnhancer.create();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
