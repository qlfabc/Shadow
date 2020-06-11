package com.tencent.shadow.sample.host;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import com.tencent.shadow.dynamic.host.ObjectCallBack;
import com.tencent.shadow.sample.constant.Constant;

/**
 * @description : 使用宿主activity加载插件fragment
 * @date : 2020/6/9 9:23 AM 
 * @version :
 */
public class DemoFragmentActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plugin_fragment);

        initPluginFragment();

    }

    private void initPluginFragment() {
        PluginHelper.getInstance().singlePool.execute(new Runnable() {
            @Override
            public void run() {
                HostApplication.getApp().loadPluginManager(PluginHelper.getInstance().pluginManagerFile);
                Bundle bundle = new Bundle();
                bundle.putString(Constant.KEY_PLUGIN_ZIP_PATH, PluginHelper.getInstance().pluginZipFile.getAbsolutePath());
                bundle.putString(Constant.KEY_PLUGIN_PART_KEY, getIntent().getStringExtra(Constant.KEY_PLUGIN_PART_KEY));
                bundle.putString(Constant.KEY_FRAGMENT_CLASSNAME, getIntent().getStringExtra(Constant.KEY_FRAGMENT_CLASSNAME));
                HostApplication.getApp().getPluginManager()
                    .getObject(DemoFragmentActivity.this, Constant.FROM_ID_GET_OBJECT, bundle,
                        new ObjectCallBack() {
                            @Override
                            public void onGetObject(Object obj) {
                                showDemoFragment(obj);
                            }
                        });
            }
        });
    }

    private void showDemoFragment(final Object obj) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (obj instanceof Fragment) {
                    Fragment fragment = (Fragment) obj;
                    getFragmentManager().beginTransaction()
                        .replace(R.id.content_layout, fragment).commit();
                }
            }
        });
    }
}
