package com.hello.sandbox;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import black.android.app.BRActivityThread;
import black.android.os.BRUserHandle;
import com.hello.sandbox.app.LauncherActivity;
import com.hello.sandbox.app.configuration.AppLifecycleCallback;
import com.hello.sandbox.app.configuration.ClientConfiguration;
import com.hello.sandbox.core.GmsCore;
import com.hello.sandbox.core.env.BEnvironment;
import com.hello.sandbox.core.system.DaemonService;
import com.hello.sandbox.core.system.ServiceManager;
import com.hello.sandbox.core.system.user.BUserHandle;
import com.hello.sandbox.core.system.user.BUserInfo;
import com.hello.sandbox.entity.pm.InstallOption;
import com.hello.sandbox.entity.pm.InstallResult;
import com.hello.sandbox.entity.pm.InstalledModule;
import com.hello.sandbox.fake.delegate.ContentProviderDelegate;
import com.hello.sandbox.fake.frameworks.BActivityManager;
import com.hello.sandbox.fake.frameworks.BJobManager;
import com.hello.sandbox.fake.frameworks.BPackageManager;
import com.hello.sandbox.fake.frameworks.BStorageManager;
import com.hello.sandbox.fake.frameworks.BUserManager;
import com.hello.sandbox.fake.frameworks.BXposedManager;
import com.hello.sandbox.fake.hook.HookManager;
import com.hello.sandbox.proxy.ProxyManifest;
import com.hello.sandbox.utils.FileUtils;
import com.hello.sandbox.utils.ShellUtils;
import com.hello.sandbox.utils.Slog;
import com.hello.sandbox.utils.compat.BuildCompat;
import com.hello.sandbox.utils.compat.BundleCompat;
import com.hello.sandbox.utils.compat.XposedParserCompat;
import com.hello.sandbox.utils.provider.ProviderCall;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.weishu.reflection.Reflection;
import top.canyie.pine.PineConfig;
import top.niunaijun.blackreflection.BlackReflection;

/** Created by Milk on 3/30/21. * ∧＿∧ (`･ω･∥ 丶　つ０ しーＪ 此处无Bug */
@SuppressLint({"StaticFieldLeak", "NewApi"})
public class SandBoxCore extends ClientConfiguration {
  public static final String TAG = "SandBoxCore";

  private static final SandBoxCore sSandBoxCore = new SandBoxCore();
  private static Context sContext;
  private ProcessType mProcessType;
  private final Map<String, IBinder> mServices = new HashMap<>();
  private Thread.UncaughtExceptionHandler mExceptionHandler;
  private ClientConfiguration mClientConfiguration;
  private final List<AppLifecycleCallback> mAppLifecycleCallbacks = new ArrayList<>();
  private final Handler mHandler = new Handler(Looper.getMainLooper());
  private final int mHostUid = Process.myUid();
  private final int mHostUserId = BRUserHandle.get().myUserId();

  public static SandBoxCore get() {
    return sSandBoxCore;
  }

  public Handler getHandler() {
    return mHandler;
  }

  public static PackageManager getPackageManager() {
    return sContext.getPackageManager();
  }

  public static String getHostPkg() {
    return get().getHostPackageName();
  }

  public static int getHostUid() {
    return get().mHostUid;
  }

  public static int getHostUserId() {
    return get().mHostUserId;
  }

  public static Context getContext() {
    return sContext;
  }

  public Thread.UncaughtExceptionHandler getExceptionHandler() {
    return mExceptionHandler;
  }

  public void setExceptionHandler(Thread.UncaughtExceptionHandler exceptionHandler) {
    mExceptionHandler = exceptionHandler;
  }

  public void doAttachBaseContext(Context context, ClientConfiguration clientConfiguration) {
    if (clientConfiguration == null) {
      throw new IllegalArgumentException("ClientConfiguration is null!");
    }
    BlackReflection.DEBUG = BuildConfig.DEBUG;
    Reflection.unseal(context);//load dex with prop file as readonly
    sContext = context;
    mClientConfiguration = clientConfiguration;

    String processName = getProcessName(getContext());
    if (processName.equals(SandBoxCore.getHostPkg())) {
      mProcessType = ProcessType.Main;
      startLogcat();
    } else if (processName.endsWith(getContext().getString(R.string.black_box_service_name))) {
      mProcessType = ProcessType.Server;
    } else {
      mProcessType = ProcessType.BAppClient;
    }
    if (SandBoxCore.get().isBlackProcess()) {
      BEnvironment.load();
      if (processName.endsWith("p0")) {
        //                android.os.Debug.waitForDebugger();
      }
      //            android.os.Debug.waitForDebugger();
    }
    if (isServerProcess()) {
      if (clientConfiguration.isEnableDaemonService()) {
        Intent intent = new Intent();
        intent.setClass(getContext(), DaemonService.class);
        if (BuildCompat.isOreo()) {
          getContext().startForegroundService(intent);
        } else {
          getContext().startService(intent);
        }
      }
    }
    PineConfig.debug = true;
    PineConfig.debuggable = true;
    HookManager.get().init();
  }

  public void doCreate() {
    // fix contentProvider
    if (isBlackProcess()) {
      ContentProviderDelegate.init();
    }
    if (!isServerProcess()) {
      ServiceManager.initBlackManager();
    }
  }

  public static Object mainThread() {
    return BRActivityThread.get().currentActivityThread();
  }

  public void startActivity(Intent intent, int userId) {
    if (mClientConfiguration.isEnableLauncherActivity()) {
      LauncherActivity.launch(intent, userId);
    } else {
      getBActivityManager().startActivity(intent, userId);
    }
  }

  public static BJobManager getBJobManager() {
    return BJobManager.get();
  }

  public static BPackageManager getBPackageManager() {
    return BPackageManager.get();
  }

  public static BActivityManager getBActivityManager() {
    return BActivityManager.get();
  }

  public static BStorageManager getBStorageManager() {
    return BStorageManager.get();
  }

  public boolean launchApk(String packageName, int userId) {
    Intent launchIntentForPackage =
        getBPackageManager().getLaunchIntentForPackage(packageName, userId);
    if (launchIntentForPackage == null) {
      return false;
    }
    startActivity(launchIntentForPackage, userId);
    return true;
  }

  public boolean isInstalled(String packageName, int userId) {
    return getBPackageManager().isInstalled(packageName, userId);
  }

  public void uninstallPackageAsUser(String packageName, int userId) {
    getBPackageManager().uninstallPackageAsUser(packageName, userId);
  }

  public void uninstallPackage(String packageName) {
    getBPackageManager().uninstallPackage(packageName);
  }

  public InstallResult installPackageAsUser(String packageName, int userId) {
    try {
      PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
      return getBPackageManager()
          .installPackageAsUser(
              packageInfo.applicationInfo.sourceDir, InstallOption.installBySystem(), userId);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
      return new InstallResult().installError(e.getMessage());
    }
  }

  public InstallResult installPackageAsUser(File apk, int userId) {
    return getBPackageManager()
        .installPackageAsUser(apk.getAbsolutePath(), InstallOption.installByStorage(), userId);
  }

  public InstallResult installPackageAsUser(Uri apk, int userId) {
    return getBPackageManager()
        .installPackageAsUser(
            apk.toString(), InstallOption.installByStorage().makeUriFile(), userId);
  }

  public InstallResult installXPModule(File apk) {
    return getBPackageManager()
        .installPackageAsUser(
            apk.getAbsolutePath(),
            InstallOption.installByStorage().makeXposed(),
            BUserHandle.USER_XPOSED);
  }

  public InstallResult installXPModule(Uri apk) {
    return getBPackageManager()
        .installPackageAsUser(
            apk.toString(),
            InstallOption.installByStorage().makeXposed().makeUriFile(),
            BUserHandle.USER_XPOSED);
  }

  public InstallResult installXPModule(String packageName) {
    try {
      PackageInfo packageInfo = getPackageManager().getPackageInfo(packageName, 0);
      String path = packageInfo.applicationInfo.sourceDir;
      return getBPackageManager()
          .installPackageAsUser(
              path, InstallOption.installBySystem().makeXposed(), BUserHandle.USER_XPOSED);
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
      return new InstallResult().installError(e.getMessage());
    }
  }

  public void uninstallXPModule(String packageName) {
    uninstallPackage(packageName);
  }

  public boolean isXPEnable() {
    return BXposedManager.get().isXPEnable();
  }

  public void setXPEnable(boolean enable) {
    BXposedManager.get().setXPEnable(enable);
  }

  public boolean isXposedModule(File file) {
    return XposedParserCompat.isXPModule(file.getAbsolutePath());
  }

  public boolean isInstalledXposedModule(String packageName) {
    return isInstalled(packageName, BUserHandle.USER_XPOSED);
  }

  public boolean isPackageNotInstalled(String packageName) {
    try {
      return getPackageManager().getPackageInfo(packageName, 0) == null;
    } catch (Throwable e) {
      return true;
    }
  }

  public boolean isModuleEnable(String packageName) {
    return BXposedManager.get().isModuleEnable(packageName);
  }

  public void setModuleEnable(String packageName, boolean enable) {
    BXposedManager.get().setModuleEnable(packageName, enable);
  }

  public List<InstalledModule> getInstalledXPModules() {
    return BXposedManager.get().getInstalledModules();
  }

  public List<ApplicationInfo> getInstalledApplications(int flags, int userId) {
    return getBPackageManager().getInstalledApplications(flags, userId);
  }

  public List<PackageInfo> getInstalledPackages(int flags, int userId) {
    return getBPackageManager().getInstalledPackages(flags, userId);
  }

  public void clearPackage(String packageName, int userId) {
    BPackageManager.get().clearPackage(packageName, userId);
  }

  public void stopPackage(String packageName, int userId) {
    BPackageManager.get().stopPackage(packageName, userId);
  }

  public List<BUserInfo> getUsers() {
    return BUserManager.get().getUsers();
  }

  public BUserInfo createUser(int userId) {
    return BUserManager.get().createUser(userId);
  }

  public void deleteUser(int userId) {
    BUserManager.get().deleteUser(userId);
  }

  public List<AppLifecycleCallback> getAppLifecycleCallbacks() {
    return mAppLifecycleCallbacks;
  }

  public void removeAppLifecycleCallback(AppLifecycleCallback appLifecycleCallback) {
    mAppLifecycleCallbacks.remove(appLifecycleCallback);
  }

  public void addAppLifecycleCallback(AppLifecycleCallback appLifecycleCallback) {
    mAppLifecycleCallbacks.add(appLifecycleCallback);
  }

  public boolean isSupportGms() {
    return GmsCore.isSupportGms();
  }

  public boolean isInstallGms(int userId) {
    return GmsCore.isInstalledGoogleService(userId);
  }

  public InstallResult installGms(int userId) {
    return GmsCore.installGApps(userId);
  }

  public boolean uninstallGms(int userId) {
    GmsCore.uninstallGApps(userId);
    return !GmsCore.isInstalledGoogleService(userId);
  }

  public IBinder getService(String name) {
    IBinder binder = mServices.get(name);
    if (binder != null && binder.isBinderAlive()) {
      return binder;
    }
    Bundle bundle = new Bundle();
    bundle.putString("_B_|_server_name_", name);
    Bundle vm = ProviderCall.callSafely(ProxyManifest.getBindProvider(), "VM", null, bundle);
    binder = BundleCompat.getBinder(vm, "_B_|_server_");
    Slog.log(TAG, "getService: " + name + ", " + binder);
    mServices.put(name, binder);
    return binder;
  }

  /** Process type */
  private enum ProcessType {
    /** Server process */
    Server,
    /** Black app process */
    BAppClient,
    /** Main process */
    Main,
  }

  public boolean isBlackProcess() {
    return mProcessType == ProcessType.BAppClient;
  }

  public boolean isMainProcess() {
    return mProcessType == ProcessType.Main;
  }

  public boolean isServerProcess() {
    return mProcessType == ProcessType.Server;
  }

  @Override
  public boolean isHideRoot() {
    return mClientConfiguration.isHideRoot();
  }

  @Override
  public boolean isHideXposed() {
    return mClientConfiguration.isHideXposed();
  }

  @Override
  public String getHostPackageName() {
    return mClientConfiguration.getHostPackageName();
  }

  @Override
  public boolean requestInstallPackage(File file, int userId) {
    return mClientConfiguration.requestInstallPackage(file, userId);
  }

  private void startLogcat() {
    new Thread(
            () -> {
              File file =
                  new File(
                      Environment.getExternalStoragePublicDirectory(
                          Environment.DIRECTORY_DOWNLOADS),
                      getContext().getPackageName() + "_logcat.txt");
              FileUtils.deleteDir(file);
              ShellUtils.execCommand("logcat -c", false);
              ShellUtils.execCommand("logcat -f " + file.getAbsolutePath(), false);
            })
        .start();
  }

  private static String getProcessName(Context context) {
    int pid = Process.myPid();
    String processName = null;
    ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
      if (info.pid == pid) {
        processName = info.processName;
        break;
      }
    }
    if (processName == null) {
      throw new RuntimeException("processName = null");
    }
    return processName;
  }

  public static boolean is64Bit() {
    if (BuildCompat.isM()) {
      return Process.is64Bit();
    } else {
      return Build.CPU_ABI.equals("arm64-v8a");
    }
  }

  public void initNotificationManager() {
    NotificationManager nm =
        (NotificationManager)
            SandBoxCore.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
    String CHANNEL_ONE_ID = SandBoxCore.getContext().getPackageName() + ".blackbox_core";
    String CHANNEL_ONE_NAME = "blackbox_core";
    if (BuildCompat.isOreo()) {
      NotificationChannel notificationChannel =
          new NotificationChannel(
              CHANNEL_ONE_ID, CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH);
      notificationChannel.enableLights(true);
      notificationChannel.setLightColor(Color.RED);
      notificationChannel.setShowBadge(true);
      notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
      nm.createNotificationChannel(notificationChannel);
    }
  }
}
