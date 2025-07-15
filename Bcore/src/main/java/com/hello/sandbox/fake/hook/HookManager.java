package com.hello.sandbox.fake.hook;

import android.util.Log;
import com.hello.sandbox.SandBoxCore;
import com.hello.sandbox.fake.delegate.AppInstrumentation;
import com.hello.sandbox.fake.service.HCallbackProxy;
import com.hello.sandbox.fake.service.IAccessibilityManagerProxy;
import com.hello.sandbox.fake.service.IAccountManagerProxy;
import com.hello.sandbox.fake.service.IActivityClientProxy;
import com.hello.sandbox.fake.service.IActivityManagerProxy;
import com.hello.sandbox.fake.service.IActivityTaskManagerProxy;
import com.hello.sandbox.fake.service.IAlarmManagerProxy;
import com.hello.sandbox.fake.service.IAppOpsManagerProxy;
import com.hello.sandbox.fake.service.IAppWidgetManagerProxy;
import com.hello.sandbox.fake.service.IAutofillManagerProxy;
import com.hello.sandbox.fake.service.IClipboardManagerProxy;
import com.hello.sandbox.fake.service.IConnectivityManagerProxy;
import com.hello.sandbox.fake.service.IContextHubServiceProxy;
import com.hello.sandbox.fake.service.IDeviceIdentifiersPolicyProxy;
import com.hello.sandbox.fake.service.IDevicePolicyManagerProxy;
import com.hello.sandbox.fake.service.IDisplayManagerProxy;
import com.hello.sandbox.fake.service.IFingerprintManagerProxy;
import com.hello.sandbox.fake.service.IGraphicsStatsProxy;
import com.hello.sandbox.fake.service.IJobServiceProxy;
import com.hello.sandbox.fake.service.ILauncherAppsProxy;
import com.hello.sandbox.fake.service.ILocationManagerProxy;
import com.hello.sandbox.fake.service.IMediaRouterServiceProxy;
import com.hello.sandbox.fake.service.IMediaSessionManagerProxy;
import com.hello.sandbox.fake.service.INetworkManagementServiceProxy;
import com.hello.sandbox.fake.service.INotificationManagerProxy;
import com.hello.sandbox.fake.service.IPackageManagerProxy;
import com.hello.sandbox.fake.service.IPermissionManagerProxy;
import com.hello.sandbox.fake.service.IPersistentDataBlockServiceProxy;
import com.hello.sandbox.fake.service.IPhoneSubInfoProxy;
import com.hello.sandbox.fake.service.IPowerManagerProxy;
import com.hello.sandbox.fake.service.IShortcutManagerProxy;
import com.hello.sandbox.fake.service.IStorageManagerProxy;
import com.hello.sandbox.fake.service.IStorageStatsManagerProxy;
import com.hello.sandbox.fake.service.ISystemUpdateProxy;
import com.hello.sandbox.fake.service.ITelephonyManagerProxy;
import com.hello.sandbox.fake.service.ITelephonyRegistryProxy;
import com.hello.sandbox.fake.service.IUserManagerProxy;
import com.hello.sandbox.fake.service.IVibratorServiceProxy;
import com.hello.sandbox.fake.service.IVpnManagerProxy;
import com.hello.sandbox.fake.service.IWifiManagerProxy;
import com.hello.sandbox.fake.service.IWifiScannerProxy;
import com.hello.sandbox.fake.service.IWindowManagerProxy;
import com.hello.sandbox.fake.service.context.ContentServiceStub;
import com.hello.sandbox.fake.service.context.RestrictionsManagerStub;
import com.hello.sandbox.fake.service.libcore.OsStub;
import com.hello.sandbox.utils.Slog;
import com.hello.sandbox.utils.compat.BuildCompat;
import java.util.HashMap;
import java.util.Map;

/** Created by Milk on 3/30/21. * ∧＿∧ (`･ω･∥ 丶　つ０ しーＪ 此处无Bug */
public class HookManager {
  public static final String TAG = "HookManager";

  private static final HookManager sHookManager = new HookManager();

  private final Map<Class<?>, IInjectHook> mInjectors = new HashMap<>();

  public static HookManager get() {
    return sHookManager;
  }

  public void init() {
    if (SandBoxCore.get().isBlackProcess() || SandBoxCore.get().isServerProcess()) {
      addInjector(new IDisplayManagerProxy());
      addInjector(new OsStub());
      addInjector(new IActivityManagerProxy());
      addInjector(new IPackageManagerProxy());
      addInjector(new ITelephonyManagerProxy());
      addInjector(new HCallbackProxy());
      addInjector(new IAppOpsManagerProxy());
      addInjector(new INotificationManagerProxy());
      addInjector(new IAlarmManagerProxy());
      addInjector(new IAppWidgetManagerProxy());
      addInjector(new ContentServiceStub());
      addInjector(new IWindowManagerProxy());
      addInjector(new IUserManagerProxy());
      addInjector(new RestrictionsManagerStub());
      addInjector(new IMediaSessionManagerProxy());
      addInjector(new ILocationManagerProxy());
      addInjector(new IStorageManagerProxy());
      addInjector(new ILauncherAppsProxy());
      addInjector(new IJobServiceProxy());
      addInjector(new IAccessibilityManagerProxy());
      addInjector(new ITelephonyRegistryProxy());
      addInjector(new IDevicePolicyManagerProxy());
      addInjector(new IAccountManagerProxy());
      addInjector(new IConnectivityManagerProxy());
      addInjector(new IClipboardManagerProxy());
      addInjector(new IPhoneSubInfoProxy());
      addInjector(new IMediaRouterServiceProxy());
      addInjector(new IPowerManagerProxy());
      addInjector(new IContextHubServiceProxy());
      addInjector(new IVibratorServiceProxy());
      addInjector(new IPersistentDataBlockServiceProxy());
      addInjector(AppInstrumentation.get());
      /*
       * It takes time to test and enhance the compatibility of WifiManager
       * (only tested in Android 10).
       * commented by BlackBoxing at 2022/03/08
       * */
      addInjector(new IWifiManagerProxy());
      addInjector(new IWifiScannerProxy());
      // 12.0
      if (BuildCompat.isS()) {
        addInjector(new IActivityClientProxy(null));
        addInjector(new IVpnManagerProxy());
      }
      // 11.0
      if (BuildCompat.isR()) {
        addInjector(new IPermissionManagerProxy());
      }
      // 10.0
      if (BuildCompat.isQ()) {
        addInjector(new IActivityTaskManagerProxy());
      }
      // 9.0
      if (BuildCompat.isPie()) {
        addInjector(new ISystemUpdateProxy());
      }
      // 8.0
      if (BuildCompat.isOreo()) {
        addInjector(new IAutofillManagerProxy());
        addInjector(new IDeviceIdentifiersPolicyProxy());
        addInjector(new IStorageStatsManagerProxy());
      }
      // 7.1
      if (BuildCompat.isN_MR1()) {
        addInjector(new IShortcutManagerProxy());
      }
      // 7.0
      if (BuildCompat.isN()) {
        addInjector(new INetworkManagementServiceProxy());
      }
      // 6.0
      if (BuildCompat.isM()) {
        addInjector(new IFingerprintManagerProxy());
        addInjector(new IGraphicsStatsProxy());
      }
      // 5.0
      if (BuildCompat.isL()) {
        addInjector(new IJobServiceProxy());
      }
    }
    injectAll();
  }

  public void checkEnv(Class<?> clazz) {
    IInjectHook iInjectHook = mInjectors.get(clazz);
    if (iInjectHook != null && iInjectHook.isBadEnv()) {
      Log.d(TAG, "checkEnv: " + clazz.getSimpleName() + " is bad env");
      iInjectHook.injectHook();
    }
  }

  public void checkAll() {
    for (Class<?> aClass : mInjectors.keySet()) {
      IInjectHook iInjectHook = mInjectors.get(aClass);
      if (iInjectHook != null && iInjectHook.isBadEnv()) {
        Log.d(TAG, "checkEnv: " + aClass.getSimpleName() + " is bad env");
        iInjectHook.injectHook();
      }
    }
  }

  void addInjector(IInjectHook injectHook) {
    mInjectors.put(injectHook.getClass(), injectHook);
  }

  void injectAll() {
    for (IInjectHook value : mInjectors.values()) {
      try {
        Slog.log(TAG, "hook: " + value);
        value.injectHook();
      } catch (Exception e) {
        Slog.loge(TAG, "hook error: " + value, e);
      }
    }
  }
}
