package com.hello.sandbox.fake.service;

import static android.content.pm.PackageManager.GET_META_DATA;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import com.hello.sandbox.SandBoxCore;
import com.hello.sandbox.app.BActivityThread;
import com.hello.sandbox.fake.hook.MethodHook;
import com.hello.sandbox.fake.hook.ProxyMethod;
import com.hello.sandbox.fake.provider.FileProviderHandler;
import com.hello.sandbox.utils.ComponentUtils;
import com.hello.sandbox.utils.MethodParameterUtils;
import com.hello.sandbox.utils.Slog;
import com.hello.sandbox.utils.compat.BuildCompat;
import com.hello.sandbox.utils.compat.StartActivityCompat;
import java.io.File;
import java.lang.reflect.Method;

/** Created by Milk on 4/21/21. * ∧＿∧ (`･ω･∥ 丶　つ０ しーＪ 此处无Bug */
public class ActivityManagerCommonProxy {
  public static final String TAG = "CommonStub";

  @ProxyMethod("startActivity")
  public static class StartActivity extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      MethodParameterUtils.replaceFirstAppPkg(args);
      Intent intent = getIntent(args);
      Slog.log(TAG, "Hook in : " + intent);
      assert intent != null;
      if (intent.getParcelableExtra("_B_|_target_") != null) {
        return method.invoke(who, args);
      }
      if (ComponentUtils.isRequestInstall(intent)) {
        File file =
            FileProviderHandler.convertFile(BActivityThread.getApplication(), intent.getData());
        if (SandBoxCore.get().requestInstallPackage(file, BActivityThread.getUserId())) {
          return 0;
        }
        intent.setData(
            FileProviderHandler.convertFileUri(BActivityThread.getApplication(), intent.getData()));
        return method.invoke(who, args);
      }
      String dataString = intent.getDataString();
      if (dataString != null
          && dataString.equals("package:" + BActivityThread.getAppPackageName())) {
        intent.setData(Uri.parse("package:" + SandBoxCore.getHostPkg()));
      }

      ResolveInfo resolveInfo =
          SandBoxCore.getBPackageManager()
              .resolveActivity(
                  intent,
                  GET_META_DATA,
                  StartActivityCompat.getResolvedType(args),
                  BActivityThread.getUserId());
      if (resolveInfo == null) {
        String origPackage = intent.getPackage();
        if (intent.getPackage() == null && intent.getComponent() == null) {
          intent.setPackage(BActivityThread.getAppPackageName());
        } else {
          origPackage = intent.getPackage();
        }
        resolveInfo =
            SandBoxCore.getBPackageManager()
                .resolveActivity(
                    intent,
                    GET_META_DATA,
                    StartActivityCompat.getResolvedType(args),
                    BActivityThread.getUserId());
        if (resolveInfo == null) {
          intent.setPackage(origPackage);
          return method.invoke(who, args);
        }
      }

      intent.setExtrasClassLoader(who.getClass().getClassLoader());
      intent.setComponent(
          new ComponentName(resolveInfo.activityInfo.packageName, resolveInfo.activityInfo.name));
      SandBoxCore.getBActivityManager()
          .startActivityAms(
              BActivityThread.getUserId(),
              StartActivityCompat.getIntent(args),
              StartActivityCompat.getResolvedType(args),
              StartActivityCompat.getResultTo(args),
              StartActivityCompat.getResultWho(args),
              StartActivityCompat.getRequestCode(args),
              StartActivityCompat.getFlags(args),
              StartActivityCompat.getOptions(args));
      return 0;
    }

    private Intent getIntent(Object[] args) {
      int index;
      if (BuildCompat.isR()) {
        index = 3;
      } else {
        index = 2;
      }
      if (args[index] instanceof Intent) {
        return (Intent) args[index];
      }
      for (Object arg : args) {
        if (arg instanceof Intent) {
          return (Intent) arg;
        }
      }
      return null;
    }
  }

  @ProxyMethod("startActivities")
  public static class StartActivities extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      int index = getIntents();
      Intent[] intents = (Intent[]) args[index++];
      String[] resolvedTypes = (String[]) args[index++];
      IBinder resultTo = (IBinder) args[index++];
      Bundle options = (Bundle) args[index];
      // todo ??
      if (!ComponentUtils.isSelf(intents)) {
        return method.invoke(who, args);
      }

      for (Intent intent : intents) {
        intent.setExtrasClassLoader(who.getClass().getClassLoader());
      }
      return SandBoxCore.getBActivityManager()
          .startActivities(BActivityThread.getUserId(), intents, resolvedTypes, resultTo, options);
    }

    public int getIntents() {
      if (BuildCompat.isR()) {
        return 3;
      }
      return 2;
    }
  }

  @ProxyMethod("startIntentSenderForResult")
  public static class StartIntentSenderForResult extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("activityResumed")
  public static class ActivityResumed extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      SandBoxCore.getBActivityManager().onActivityResumed((IBinder) args[0]);
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("activityDestroyed")
  public static class ActivityDestroyed extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      SandBoxCore.getBActivityManager().onActivityDestroyed((IBinder) args[0]);
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("finishActivity")
  public static class FinishActivity extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      SandBoxCore.getBActivityManager().onFinishActivity((IBinder) args[0]);
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("getAppTasks")
  public static class GetAppTasks extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      MethodParameterUtils.replaceFirstAppPkg(args);
      return method.invoke(who, args);
    }
  }

  @ProxyMethod("getCallingPackage")
  public static class getCallingPackage extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      return SandBoxCore.getBActivityManager()
          .getCallingPackage((IBinder) args[0], BActivityThread.getUserId());
    }
  }

  @ProxyMethod("getCallingActivity")
  public static class getCallingActivity extends MethodHook {
    @Override
    protected Object hook(Object who, Method method, Object[] args) throws Throwable {
      return SandBoxCore.getBActivityManager()
          .getCallingActivity((IBinder) args[0], BActivityThread.getUserId());
    }
  }
}
