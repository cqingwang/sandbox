package com.hello.sandbox.fake.service;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import androidx.annotation.NonNull;
import black.android.app.ActivityThreadActivityClientRecordContext;
import black.android.app.BRActivityClient;
import black.android.app.BRActivityClientActivityClientControllerSingleton;
import black.android.app.BRActivityManagerNative;
import black.android.app.BRActivityThread;
import black.android.app.BRActivityThreadActivityClientRecord;
import black.android.app.BRActivityThreadCreateServiceData;
import black.android.app.BRActivityThreadH;
import black.android.app.BRIActivityManager;
import black.android.app.servertransaction.BRClientTransaction;
import black.android.app.servertransaction.BRLaunchActivityItem;
import black.android.app.servertransaction.LaunchActivityItemContext;
import black.android.os.BRHandler;
import com.hello.sandbox.SandBoxCore;
import com.hello.sandbox.app.BActivityThread;
import com.hello.sandbox.fake.hook.IInjectHook;
import com.hello.sandbox.proxy.ProxyManifest;
import com.hello.sandbox.proxy.record.ProxyActivityRecord;
import com.hello.sandbox.utils.Slog;
import com.hello.sandbox.utils.compat.BuildCompat;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Created by Milk on 3/31/21. * ∧＿∧ (`･ω･∥ 丶　つ０ しーＪ 此处无Bug */
public class HCallbackProxy implements IInjectHook, Handler.Callback {
  public static final String TAG = "HCallbackStub";
  private Handler.Callback mOtherCallback;
  private AtomicBoolean mBeing = new AtomicBoolean(false);

  private Handler.Callback getHCallback() {
    return BRHandler.get(getH()).mCallback();
  }

  private Handler getH() {
    Object currentActivityThread = SandBoxCore.mainThread();
    return BRActivityThread.get(currentActivityThread).mH();
  }

  @Override
  public void injectHook() {
    mOtherCallback = getHCallback();
    if (mOtherCallback != null
        && (mOtherCallback == this
            || mOtherCallback.getClass().getName().equals(this.getClass().getName()))) {
      mOtherCallback = null;
    }
    BRHandler.get(getH())._set_mCallback(this);
  }

  @Override
  public boolean isBadEnv() {
    Handler.Callback hCallback = getHCallback();
    return hCallback != null && hCallback != this;
  }

  @Override
  public boolean handleMessage(@NonNull Message msg) {
    if (!mBeing.getAndSet(true)) {
      try {
        if (BuildCompat.isPie()) {
          if (msg.what == BRActivityThreadH.get().EXECUTE_TRANSACTION()) {
            final Boolean a = handleLaunchActivity(msg);
            if (a != null && a) {
              getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
              return true;
            }
          }
        } else {
          if (msg.what == BRActivityThreadH.get().LAUNCH_ACTIVITY()) {
            final Boolean a = handleLaunchActivity(msg);
            if (a != null && a) {
              getH().sendMessageAtFrontOfQueue(Message.obtain(msg));
              return true;
            }
          }
        }
        if (msg.what == BRActivityThreadH.get().CREATE_SERVICE()) {
          return handleCreateService(msg.obj);
        }
        if (mOtherCallback != null) {
          return mOtherCallback.handleMessage(msg);
        }
        return false;
      } finally {
        mBeing.set(false);
      }
    }
    return false;
  }

  private Object getLaunchActivityItem(Object clientTransaction) {
    List<Object> mActivityCallbacks =
        BRClientTransaction.get(clientTransaction).mActivityCallbacks();

    for (Object obj : mActivityCallbacks) {
      if (BRLaunchActivityItem.getRealClass().getName().equals(obj.getClass().getCanonicalName())) {
        return obj;
      }
    }
    return null;
  }

  private Boolean handleLaunchActivity(Message msg) {
    Object client = msg.obj;
    Object r;
    if (BuildCompat.isPie()) {
      // ClientTransaction
      r = getLaunchActivityItem(client);
    } else {
      // ActivityClientRecord
      r = client;
    }
    if (r == null) return false;

    Intent intent;
    IBinder token;
    if (BuildCompat.isPie()) {
      intent = BRLaunchActivityItem.get(r).mIntent();
      token = BRClientTransaction.get(client).mActivityToken();
    } else {
      ActivityThreadActivityClientRecordContext clientRecordContext =
          BRActivityThreadActivityClientRecord.get(r);
      intent = clientRecordContext.intent();
      token = clientRecordContext.token();
    }

    if (intent == null) return false;

    ProxyActivityRecord stubRecord = ProxyActivityRecord.create(intent);
    ActivityInfo activityInfo = stubRecord.mActivityInfo;
    if (activityInfo != null) {
      if (BActivityThread.getAppConfig() == null) {
        SandBoxCore.getBActivityManager()
            .restartProcess(activityInfo.packageName, activityInfo.processName, stubRecord.mUserId);

        Intent launchIntentForPackage =
            SandBoxCore.getBPackageManager()
                .getLaunchIntentForPackage(activityInfo.packageName, stubRecord.mUserId);
        intent.setExtrasClassLoader(this.getClass().getClassLoader());
        ProxyActivityRecord.saveStub(
            intent,
            launchIntentForPackage,
            stubRecord.mActivityInfo,
            stubRecord.mActivityRecord,
            stubRecord.mUserId);
        if (BuildCompat.isPie()) {
          LaunchActivityItemContext launchActivityItemContext = BRLaunchActivityItem.get(r);
          launchActivityItemContext._set_mIntent(intent);
          launchActivityItemContext._set_mInfo(activityInfo);
        } else {
          ActivityThreadActivityClientRecordContext clientRecordContext =
              BRActivityThreadActivityClientRecord.get(r);
          clientRecordContext._set_intent(intent);
          clientRecordContext._set_activityInfo(activityInfo);
        }
        return true;
      }
      // bind
      if (!BActivityThread.currentActivityThread().isInit()) {
        BActivityThread.currentActivityThread()
            .bindApplication(activityInfo.packageName, activityInfo.processName);
        return true;
      }

      int taskId =
          BRIActivityManager.get(BRActivityManagerNative.get().getDefault())
              .getTaskForActivity(token, false);
      SandBoxCore.getBActivityManager()
          .onActivityCreated(taskId, token, stubRecord.mActivityRecord);

      if (BuildCompat.isT()) {
        // FIXME: 13系统的LaunchActivityItem的preExecute中不再插入ActivityClientRecord，改在execute中了
        // 所以需要改一下处理逻辑，但是似乎直接下面这样改也可以了，后续看看有没有问题
        LaunchActivityItemContext launchActivityItemContext = BRLaunchActivityItem.get(r);
        launchActivityItemContext._set_mIntent(stubRecord.mTarget);
        launchActivityItemContext._set_mInfo(activityInfo);
        checkActivityClient();
      } else if (BuildCompat.isS()) {
        Object record = BRActivityThread.get(SandBoxCore.mainThread()).getLaunchingActivity(token);
        ActivityThreadActivityClientRecordContext clientRecordContext =
            BRActivityThreadActivityClientRecord.get(record);
        clientRecordContext._set_intent(stubRecord.mTarget);
        clientRecordContext._set_activityInfo(activityInfo);
        clientRecordContext._set_packageInfo(
            BActivityThread.currentActivityThread().getPackageInfo());

        checkActivityClient();
      } else if (BuildCompat.isPie()) {
        LaunchActivityItemContext launchActivityItemContext = BRLaunchActivityItem.get(r);
        launchActivityItemContext._set_mIntent(stubRecord.mTarget);
        launchActivityItemContext._set_mInfo(activityInfo);
      } else {
        ActivityThreadActivityClientRecordContext clientRecordContext =
            BRActivityThreadActivityClientRecord.get(r);
        clientRecordContext._set_intent(stubRecord.mTarget);
        clientRecordContext._set_activityInfo(activityInfo);
      }
    }
    return false;
  }

  private boolean handleCreateService(Object data) {
    if (BActivityThread.getAppConfig() != null) {
      String appPackageName = BActivityThread.getAppPackageName();
      assert appPackageName != null;

      ServiceInfo serviceInfo = BRActivityThreadCreateServiceData.get(data).info();
      if (!serviceInfo.name.equals(ProxyManifest.getProxyService(BActivityThread.getAppPid()))
          && !serviceInfo.name.equals(
              ProxyManifest.getProxyJobService(BActivityThread.getAppPid()))) {
        Slog.log(TAG, "handleCreateService: " + data);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(appPackageName, serviceInfo.name));
        SandBoxCore.getBActivityManager()
            .startService(intent, null, false, BActivityThread.getUserId());
        return true;
      }
    }
    return false;
  }

  private void checkActivityClient() {
    try {
      Object activityClientController = BRActivityClient.get().getActivityClientController();
      if (!(activityClientController instanceof Proxy)) {
        IActivityClientProxy iActivityClientProxy =
            new IActivityClientProxy(activityClientController);
        iActivityClientProxy.onlyProxy(true);
        iActivityClientProxy.injectHook();
        Object instance = BRActivityClient.get().getInstance();
        Object o = BRActivityClient.get(instance).INTERFACE_SINGLETON();
        BRActivityClientActivityClientControllerSingleton.get(o)
            ._set_mKnownInstance(iActivityClientProxy.getProxyInvocation());
      }
    } catch (Throwable t) {
      t.printStackTrace();
    }
  }
}
