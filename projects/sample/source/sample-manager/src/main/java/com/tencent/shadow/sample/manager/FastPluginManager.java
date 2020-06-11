/*
 * Tencent is pleased to support the open source community by making Tencent Shadow available.
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.tencent.shadow.sample.manager;

import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;
import com.tencent.shadow.core.common.InstalledApk;
import com.tencent.shadow.core.common.Logger;
import com.tencent.shadow.core.common.LoggerFactory;
import com.tencent.shadow.core.manager.installplugin.InstalledPlugin;
import com.tencent.shadow.core.manager.installplugin.InstalledPlugin.Part;
import com.tencent.shadow.core.manager.installplugin.InstalledType;
import com.tencent.shadow.core.manager.installplugin.PluginConfig;
import com.tencent.shadow.dynamic.host.FailedException;
import com.tencent.shadow.dynamic.manager.PluginManagerThatUseDynamicLoader;
import com.tencent.shadow.sample.host.lib.inter.ObjectFactory;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.json.JSONException;

public abstract class FastPluginManager extends PluginManagerThatUseDynamicLoader {

    private static final Logger mLogger = LoggerFactory.getLogger(FastPluginManager.class);

    private ExecutorService mFixedPool = Executors.newFixedThreadPool(4);

    public FastPluginManager(Context context) {
        super(context);
    }


    public InstalledPlugin installPlugin(String zip, String hash , boolean odex) throws IOException, JSONException, InterruptedException, ExecutionException {
        final PluginConfig pluginConfig = installPluginFromZip(new File(zip), hash);
        final String uuid = pluginConfig.UUID;
        List<Future> futures = new LinkedList<>();
        if (pluginConfig.runTime != null && pluginConfig.pluginLoader != null) {
            Future odexRuntime = mFixedPool.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    oDexPluginLoaderOrRunTime(uuid, InstalledType.TYPE_PLUGIN_RUNTIME,
                            pluginConfig.runTime.file);
                    return null;
                }
            });
            futures.add(odexRuntime);
            Future odexLoader = mFixedPool.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    oDexPluginLoaderOrRunTime(uuid, InstalledType.TYPE_PLUGIN_LOADER,
                            pluginConfig.pluginLoader.file);
                    return null;
                }
            });
            futures.add(odexLoader);
        }
        for (Map.Entry<String, PluginConfig.PluginFileInfo> plugin : pluginConfig.plugins.entrySet()) {
            final String partKey = plugin.getKey();
            final File apkFile = plugin.getValue().file;
            Future extractSo = mFixedPool.submit(new Callable() {
                @Override
                public Object call() throws Exception {
                    extractSo(uuid, partKey, apkFile);
                    return null;
                }
            });
            futures.add(extractSo);
            if (odex) {
                Future odexPlugin = mFixedPool.submit(new Callable() {
                    @Override
                    public Object call() throws Exception {
                        oDexPlugin(uuid, partKey, apkFile);
                        return null;
                    }
                });
                futures.add(odexPlugin);
            }
        }

        for (Future future : futures) {
            future.get();
        }
        onInstallCompleted(pluginConfig);

        return getInstalledPlugins(1).get(0);
    }


    public void startPluginActivity(InstalledPlugin installedPlugin, String partKey, Intent pluginIntent)
        throws RemoteException, TimeoutException, FailedException {
        Intent intent = convertActivityIntent(installedPlugin, partKey, pluginIntent);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mPluginLoader.startActivityInPluginProcess(intent);

    }

    private static final String OBJECT_FACTORY_CLASS_NAME = "com.tencent.shadow.sample.plugin.app.lib.ObjectFactoryImpl";

    public Object getPluginObject(InstalledPlugin installedPlugin, String partKey,
        String className) throws RemoteException, TimeoutException, FailedException {

        preparePlugin(installedPlugin, partKey);

        Part part = installedPlugin.getPart(partKey);
        InstalledApk installedApk = new InstalledApk(part.pluginFile.getAbsolutePath(),
            part.oDexDir == null ? null : part.oDexDir.getAbsolutePath(),
            part.libraryDir == null ? null : part.libraryDir.getAbsolutePath());

        DexClassLoader apkClassLoader = new DexClassLoader(
            installedApk.apkFilePath,
            installedApk.oDexPath,
            installedApk.libraryPath,
            ObjectFactory.class.getClassLoader()
        );

        try {
            ObjectFactory objectFactory = getInterface(
                apkClassLoader,
                ObjectFactory.class,
                OBJECT_FACTORY_CLASS_NAME
            );
            return objectFactory.getObject(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从apk中读取接口的实现
     *
     * @param clazz     接口类
     * @param className 实现类的类名
     * @param <T>       接口类型
     * @return 所需接口
     * @throws Exception
     */
    <T> T getInterface(DexClassLoader loader, Class<T> clazz, String className) throws Exception {
        try {
            Class<?> interfaceImplementClass = loader.loadClass(className);
            Object interfaceImplement = interfaceImplementClass.newInstance();
            return clazz.cast(interfaceImplement);
        } catch (ClassNotFoundException | InstantiationException
            | ClassCastException | IllegalAccessException e) {
            throw new Exception(e);
        }
    }

    public Intent convertActivityIntent(InstalledPlugin installedPlugin, String partKey, Intent pluginIntent) throws RemoteException, TimeoutException, FailedException {
        preparePlugin(installedPlugin, partKey);
        return mPluginLoader.convertActivityIntent(pluginIntent);
    }

    public void preparePlugin(InstalledPlugin installedPlugin, String partKey)
        throws RemoteException, TimeoutException, FailedException {
        loadPlugin(installedPlugin.UUID, partKey);
        Map map = mPluginLoader.getLoadedPlugin();
        Boolean isCall = (Boolean) map.get(partKey);
        if (isCall == null || !isCall) {
            mPluginLoader.callApplicationOnCreate(partKey);
        }
    }

    private void loadPluginLoaderAndRuntime(String uuid, String partKey) throws RemoteException, TimeoutException, FailedException {
        if (mPpsController == null) {
            bindPluginProcessService(getPluginProcessServiceName(partKey));
            waitServiceConnected(10, TimeUnit.SECONDS);
        }
        loadRunTime(uuid);
        loadPluginLoader(uuid);
    }

    private void loadPlugin(String uuid, String partKey) throws RemoteException, TimeoutException, FailedException {
        loadPluginLoaderAndRuntime(uuid, partKey);
        Map map = mPluginLoader.getLoadedPlugin();
        if (!map.containsKey(partKey)) {
            mPluginLoader.loadPlugin(partKey);
        }
    }


    protected abstract String getPluginProcessServiceName(String partKey);

}
