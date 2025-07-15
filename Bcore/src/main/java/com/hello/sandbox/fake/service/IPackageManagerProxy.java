package com.hello.sandbox.fake.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;
import black.android.app.BRActivityThread;
import black.android.app.BRContextImpl;
import com.hello.sandbox.SandBoxCore;
import com.hello.sandbox.app.BActivityThread;
import com.hello.sandbox.core.env.AppSystemEnv;
import com.hello.sandbox.fake.hook.BinderInvocationStub;
import com.hello.sandbox.fake.hook.MethodHook;
import com.hello.sandbox.fake.hook.ProxyMethod;
import com.hello.sandbox.fake.service.base.PkgMethodProxy;
import com.hello.sandbox.fake.service.base.ValueMethodProxy;
import com.hello.sandbox.utils.MethodParameterUtils;
import com.hello.sandbox.utils.Reflector;
import com.hello.sandbox.utils.Slog;
import com.hello.sandbox.utils.compat.BuildCompat;
import com.hello.sandbox.utils.compat.ParceledListSliceCompat;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Created by Milk on 3/30/21. * ∧＿∧ (`･ω･∥ 丶　つ０ しーＪ 此处无Bug */
public class IPackageManagerProxy extends BinderInvocationStub {
  public static final String TAG = "PackageManagerStub";

  public IPackageManagerProxy() {
    super(BRActivityThread.get().sPackageManager().asBinder());
  }

  @Override
  protected Object getWho() {
    return BRActivityThread.get().sPackageManager();
  }

  @Override
  protected void inject(Object baseInvocation, Object proxyInvocation) {
    BRActivityThread.get()._set_sPackageManager(proxyInvocation);
    replaceSystemService("package");
    Object systemContext = BRActivityThread.get(SandBoxCore.mainThread()).getSystemContext();
    PackageManager packageManager = BRContextImpl.get(systemContext).mPackageManager();
    if (packageManager != null) {
      try {
        Reflector.on("android.app.ApplicationPackageManager")
            .field("mPM")
            .set(packageManager, proxyInvocation);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  public boolean isBadEnv() {
    return false;
  }

  @Override
  protected void onBindMethod() {
    super.onBindMethod();
    addMethodHook(new ValueMethodProxy("addOnPermissionsChangeListener", 0));
    addMethodHook(new ValueMethodProxy("removeOnPermissionsChangeListener", 0));
    addMethodHook(new PkgMethodProxy("shouldShowRequestPermissionRationale"));
  }

  @ProxyMethod("resolveIntent")
  public static class ResolveIntent extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      Intent intent = (Intent) args[0];
      String resolvedType = (String) args[1];
      int flags = Integer.parseInt(args[2] + "");
      ResolveInfo resolveInfo =
          SandBoxCore.getBPackageManager()
              .resolveIntent(intent, resolvedType, flags, BActivityThread.getUserId());
      if (resolveInfo != null) {
        return resolveInfo;
      }
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("resolveService")
  public static class ResolveService extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      Intent intent = (Intent) args[0];
      String resolvedType = (String) args[1];
      int flags = Integer.parseInt(args[2] + "");
      ResolveInfo resolveInfo =
          SandBoxCore.getBPackageManager()
              .resolveService(intent, flags, resolvedType, BActivityThread.getUserId());
      if (resolveInfo != null) {
        return resolveInfo;
      }
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("setComponentEnabledSetting")
  public static class SetComponentEnabledSetting extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      return 0;
    }
  }

  @ProxyMethod("getPackageInfo")
  public static class GetPackageInfo extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      String packageName = (String) args[0];
      for (Object o : args) {
        Slog.log(TAG, "GetPackageInfo o.class=" + o.getClass() + ", value=" + o);
      }
      int flag = Integer.parseInt(args[1] + "");
      //            if (ClientSystemEnv.isFakePackage(packageName)) {
      //                packageName = SandBoxCore.getHostPkg();
      //            }
      PackageInfo packageInfo =
          SandBoxCore.getBPackageManager()
              .getPackageInfo(packageName, flag, BActivityThread.getUserId());
      if (packageInfo != null) {
        return packageInfo;
      }
      if (AppSystemEnv.isOpenPackage(packageName)) {
        return method.invoke(who, args);
      }
      return null;
    }
  }

  @ProxyMethod("getPackageUid")
  public static class GetPackageUid extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      MethodParameterUtils.replaceFirstAppPkg(args);
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("getProviderInfo")
  public static class GetProviderInfo extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      ComponentName componentName = (ComponentName) args[0];
      int flags = Integer.parseInt(args[1] + "");
      ProviderInfo providerInfo =
          SandBoxCore.getBPackageManager()
              .getProviderInfo(componentName, flags, BActivityThread.getUserId());
      if (providerInfo != null) return providerInfo;
      if (AppSystemEnv.isOpenPackage(componentName)) {
        return method.invoke(who, args);
      }
      return null;
    }
  }

  @ProxyMethod("getReceiverInfo")
  public static class GetReceiverInfo extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      ComponentName componentName = (ComponentName) args[0];
      int flags = Integer.parseInt(args[1] + "");
      ActivityInfo receiverInfo =
          SandBoxCore.getBPackageManager()
              .getReceiverInfo(componentName, flags, BActivityThread.getUserId());
      if (receiverInfo != null) return receiverInfo;
      if (AppSystemEnv.isOpenPackage(componentName)) {
        return method.invoke(who, args);
      }
      return null;
    }
  }

  @ProxyMethod("getActivityInfo")
  public static class GetActivityInfo extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      ComponentName componentName = (ComponentName) args[0];
      int flags = Integer.parseInt(args[1] + "");
      ActivityInfo activityInfo =
          SandBoxCore.getBPackageManager()
              .getActivityInfo(componentName, flags, BActivityThread.getUserId());
      if (activityInfo != null) return activityInfo;
      if (AppSystemEnv.isOpenPackage(componentName)) {
        return method.invoke(who, args);
      }
      return null;
    }
  }

  @ProxyMethod("getServiceInfo")
  public static class GetServiceInfo extends MethodHook {

    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      ComponentName componentName = (ComponentName) args[0];
      int flags = Integer.parseInt(args[1] + "");
      ServiceInfo serviceInfo =
          SandBoxCore.getBPackageManager()
              .getServiceInfo(componentName, flags, BActivityThread.getUserId());
      if (serviceInfo != null) return serviceInfo;
      if (AppSystemEnv.isOpenPackage(componentName)) {
        return method.invoke(who, args);
      }
      return null;
    }
  }

  @ProxyMethod("getInstalledApplications")
  public static class GetInstalledApplications extends MethodHook {

    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      int flags = Integer.parseInt(args[0] + "");
      List<ApplicationInfo> installedApplications =
          SandBoxCore.getBPackageManager()
              .getInstalledApplications(flags, BActivityThread.getUserId());
      return ParceledListSliceCompat.create(installedApplications);
    }
  }

  @ProxyMethod("getInstalledPackages")
  public static class GetInstalledPackages extends MethodHook {

    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      int flags = Integer.parseInt(args[0] + "");
      List<PackageInfo> installedPackages =
          SandBoxCore.getBPackageManager().getInstalledPackages(flags, BActivityThread.getUserId());
      return ParceledListSliceCompat.create(installedPackages);
    }
  }

  @ProxyMethod("getApplicationInfo")
  public static class GetApplicationInfo extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      String packageName = (String) args[0];
      for (Object o : args) {
        Slog.log(TAG, "getApplicationInfo o.class=" + o.getClass() + ", value=" + o);
      }
      int flags = Integer.parseInt(args[1] + "");
      //            if (ClientSystemEnv.isFakePackage(packageName)) {
      //                packageName = SandBoxCore.getHostPkg();
      //            }
      ApplicationInfo applicationInfo =
          SandBoxCore.getBPackageManager()
              .getApplicationInfo(packageName, flags, BActivityThread.getUserId());
      if (applicationInfo != null) {
        Slog.log(TAG, "getApplicationInfo metaData=" + applicationInfo.metaData);
        if (applicationInfo.metaData != null) {
          Slog.log(
              TAG,
              "getApplicationInfo applicationInfo.metaData.get(\"com.facebook.sdk.ApplicationId\")"
                  + applicationInfo.metaData.get("com.facebook.sdk.ApplicationId"));
          Slog.log(
              TAG,
              "getApplicationInfo applicationInfo.metaData.get(\"com.facebook.sdk.ClientToken\")"
                  + applicationInfo.metaData.getString("com.facebook.sdk.ClientToken"));
          if ("com.p1.mobile.putong".equals(args[0])
              && TextUtils.isEmpty(
                  applicationInfo.metaData.getString("com.facebook.sdk.ClientToken"))) {
            // FIXME: 2023/4/14 这里很神奇，id可以获取到，token就是null，用这种方式给加上，实际上不加也行的，异常不影响后续使用
            applicationInfo.metaData.putString(
                "com.facebook.sdk.ClientToken", "6d8a6e54c2e859bfb2dbe047ec973ead");
          }
        }
        return applicationInfo;
      }
      if (AppSystemEnv.isOpenPackage(packageName)) {
        return method.invoke(who, args);
      }
      return null;
    }
  }

  @ProxyMethod("queryContentProviders")
  public static class QueryContentProviders extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      int flags = Integer.parseInt(args[2] + "");
      List<ProviderInfo> providers =
          SandBoxCore.getBPackageManager()
              .queryContentProviders(
                  BActivityThread.getAppProcessName(),
                  BActivityThread.getBUid(),
                  flags,
                  BActivityThread.getUserId());
      return ParceledListSliceCompat.create(providers);
    }
  }

  @ProxyMethod("queryIntentReceivers")
  public static class QueryBroadcastReceivers extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      Intent intent = MethodParameterUtils.getFirstParam(args, Intent.class);
      String type = MethodParameterUtils.getFirstParam(args, String.class);
      Integer flags = MethodParameterUtils.getFirstParam(args, Integer.class);
      List<ResolveInfo> resolves =
          SandBoxCore.getBPackageManager()
              .queryBroadcastReceivers(intent, flags, type, BActivityThread.getUserId());
      Slog.log(TAG, "queryIntentReceivers: " + resolves);

      // http://androidxref.com/7.0.0_r1/xref/frameworks/base/core/java/android/app/ApplicationPackageManager.java#872
      if (BuildCompat.isN()) {
        return ParceledListSliceCompat.create(resolves);
      }

      // http://androidxref.com/6.0.1_r10/xref/frameworks/base/core/java/android/app/ApplicationPackageManager.java#699
      return resolves;
    }
  }

  @ProxyMethod("resolveContentProvider")
  public static class ResolveContentProvider extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      String authority = (String) args[0];
      int flags = Integer.parseInt(args[1] + "");
      ProviderInfo providerInfo =
          SandBoxCore.getBPackageManager()
              .resolveContentProvider(authority, flags, BActivityThread.getUserId());
      if (providerInfo == null) {
        return method.invoke(who, args);
      }
      return providerInfo;
    }
  }

  @ProxyMethod("canRequestPackageInstalls")
  public static class CanRequestPackageInstalls extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      MethodParameterUtils.replaceFirstAppPkg(args);
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("getPackagesForUid")
  public static class GetPackagesForUid extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      int uid = (Integer) args[0];
      if (uid == SandBoxCore.getHostUid()) {
        args[0] = BActivityThread.getBUid();
        uid = (int) args[0];
      }
      String[] packagesForUid = SandBoxCore.getBPackageManager().getPackagesForUid(uid);
      Slog.log(
          TAG,
          args[0]
              + " , "
              + BActivityThread.getAppProcessName()
              + " GetPackagesForUid: "
              + Arrays.toString(packagesForUid));
      return packagesForUid;
    }
  }

  @ProxyMethod("getInstallerPackageName")
  public static class GetInstallerPackageName extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      // fake google play
      return "com.android.vending";
    }
  }

  @ProxyMethod("getSharedLibraries")
  public static class GetSharedLibraries extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      // todo
      return ParceledListSliceCompat.create(new ArrayList<>());
    }
  }

  @ProxyMethod("getComponentEnabledSetting")
  public static class getComponentEnabledSetting extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
    }
  }
}
