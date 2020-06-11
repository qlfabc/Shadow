package com.tencent.shadow.sample.plugin.app.lib;

import android.text.TextUtils;
import com.tencent.shadow.sample.host.lib.inter.ObjectFactory;

/**
 * @description : ObjectFactoryImpl 
 * @date : 2020/6/9 11:43 AM 
 * @author : qilufei 
 * @version :
 */
public final class ObjectFactoryImpl implements ObjectFactory {

    @Override
    public Object getObject(String className) {
        if (TextUtils.equals(className, "com.tencent.shadow.sample.plugin.app.lib.DemoFragment")) {
            return DemoFragment.newInstance();
        }
        return "";
    }
}
