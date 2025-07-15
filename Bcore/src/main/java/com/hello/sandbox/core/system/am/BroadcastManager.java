package com.hello.sandbox.core.system.am;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.hello.sandbox.SandBoxCore;
import com.hello.sandbox.core.system.pm.BPackage;
import com.hello.sandbox.core.system.pm.BPackageManagerService;
import com.hello.sandbox.core.system.pm.BPackageSettings;
import com.hello.sandbox.core.system.pm.PackageMonitor;
import com.hello.sandbox.entity.am.PendingResultData;
import com.hello.sandbox.proxy.ProxyBroadcastReceiver;
import com.hello.sandbox.utils.Slog;
import com.hello.sandbox.utils.compat.BuildCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Created by BlackBox on 2022/2/28. */
public class BroadcastManager implements PackageMonitor {
  public static final String TAG = "BroadcastManager";

  public static final int TIMEOUT = 9000;

  public static final int MSG_TIME_OUT = 1;

  private static BroadcastManager sBroadcastManager;

  public static final String ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED";
  public static final String ACTION_CHECKIN_NOW = "com.google.android.gms.permission.CHECKIN_NOW";
  public static final String ACTION_CHECKIN_NOW_SERVER = "android.server.checkin.CHECKIN_NOW";
  public static final String ACTION_MODE_CHANGED = "android.location.MODE_CHANGED";
  public static final String ACTION_PROVIDERS_CHANGED = "android.location.PROVIDERS_CHANGED";
  public static final String ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED";
  private static final Set<String> SYSTEM_ACTIONS = new HashSet<>();

  private final BActivityManagerService mAms;
  private final BPackageManagerService mPms;
  private final Map<String, List<BroadcastReceiver>> mReceivers = new HashMap<>();
  private final Map<String, PendingResultData> mReceiversData = new HashMap<>();


  static {
    SYSTEM_ACTIONS.add("android.accounts.LOGIN_ACCOUNTS_CHANGED");
    SYSTEM_ACTIONS.add(ACTION_BOOT_COMPLETED);
    SYSTEM_ACTIONS.add(ACTION_CHECKIN_NOW);
    SYSTEM_ACTIONS.add(ACTION_CHECKIN_NOW_SERVER);
    SYSTEM_ACTIONS.add(ACTION_MODE_CHANGED);
    SYSTEM_ACTIONS.add(ACTION_PROVIDERS_CHANGED);
    SYSTEM_ACTIONS.add(ACTION_SIM_STATE_CHANGED);
  }

  private final Handler mHandler =
      new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
          super.handleMessage(msg);
          switch (msg.what) {
            case MSG_TIME_OUT:
              try {
                PendingResultData data = (PendingResultData) msg.obj;
                data.build().finish();
                Slog.log(TAG, "Timeout Receiver: " + data);
              } catch (Throwable ignore) {
              }
              break;
          }
        }
      };

  public static BroadcastManager startSystem(
      BActivityManagerService ams, BPackageManagerService pms) {
    if (sBroadcastManager == null) {
      synchronized (BroadcastManager.class) {
        if (sBroadcastManager == null) {
          sBroadcastManager = new BroadcastManager(ams, pms);
        }
      }
    }
    return sBroadcastManager;
  }

  public BroadcastManager(BActivityManagerService ams, BPackageManagerService pms) {
    mAms = ams;
    mPms = pms;
  }

  public void startup() {
    mPms.addPackageMonitor(this);
    List<BPackageSettings> bPackageSettings = mPms.getBPackageSettings();
    for (BPackageSettings bPackageSetting : bPackageSettings) {
      BPackage bPackage = bPackageSetting.pkg;
      registerPackage(bPackage);
    }
  }

  public static boolean isBlackAction(String str) {
    return SYSTEM_ACTIONS.contains(str);
  }

  public static String proxySystemAction(String str) {
    return "fake.black." + str;
  }

  @SuppressLint("NewApi")
  private void registerPackage(BPackage bPackage) {
    synchronized (mReceivers) {
      Slog.log(TAG, "register: " + bPackage.packageName  + ", size: " + bPackage.receivers.size());
      for (BPackage.Activity receiver : bPackage.receivers)  {
        List<BPackage.ActivityIntentInfo> intents = receiver.intents;
        for (BPackage.ActivityIntentInfo intent : intents) {
          // 新增功能1：类型转换获取IntentFilter
          BPackage.IntentInfo intentInfo = (BPackage.IntentInfo) intent;
          IntentFilter intentFilter = intentInfo.intentFilter;
          // 新增功能2：黑名单Action处理
          if (intentFilter != null) {
            int countActions = intentFilter.countActions();
            for (int i = 0; i < countActions; i++) {
              String action = intentFilter.getAction(i);
              if (isBlackAction(action)) {
                intentFilter.addAction(proxySystemAction(action));
              }
            }
          }
          ProxyBroadcastReceiver proxyBroadcastReceiver = new ProxyBroadcastReceiver();
          // 新增功能3：系统版本判断逻辑
          if (BuildCompat.isT())  {
            SandBoxCore.getContext().registerReceiver(proxyBroadcastReceiver,  intentFilter, 2);
          } else {
            SandBoxCore.getContext().registerReceiver(proxyBroadcastReceiver,  intentFilter);
          }

          addReceiver(bPackage.packageName,  proxyBroadcastReceiver);
        }
      }
    }
  }

  private void addReceiver(String packageName, BroadcastReceiver receiver) {
    List<BroadcastReceiver> broadcastReceivers = mReceivers.get(packageName);
    if (broadcastReceivers == null) {
      broadcastReceivers = new ArrayList<>();
      mReceivers.put(packageName, broadcastReceivers);
    }
    broadcastReceivers.add(receiver);
  }

  public void sendBroadcast(PendingResultData pendingResultData) {
    synchronized (mReceiversData) {
      // Slog.log(TAG, "sendBroadcast: " + pendingResultData);
      mReceiversData.put(pendingResultData.mBToken, pendingResultData);
      Message obtain = Message.obtain(mHandler, MSG_TIME_OUT, pendingResultData);
      mHandler.sendMessageDelayed(obtain, TIMEOUT);
    }
  }

  public void finishBroadcast(PendingResultData data) {
    synchronized (mReceiversData) {
      // Slog.log(TAG, "finishBroadcast: " + data);
      mHandler.removeMessages(MSG_TIME_OUT, mReceiversData.get(data.mBToken));
    }
  }

  @Override
  public void onPackageUninstalled(String packageName, boolean removeApp, int userId) {
    if (removeApp) {
      synchronized (mReceivers) {
        List<BroadcastReceiver> broadcastReceivers = mReceivers.get(packageName);
        if (broadcastReceivers != null) {
          Slog.log(
              TAG,
              "unregisterReceiver Package: "
                  + packageName
                  + ", size: "
                  + broadcastReceivers.size());
          for (BroadcastReceiver broadcastReceiver : broadcastReceivers) {
            try {
              SandBoxCore.getContext().unregisterReceiver(broadcastReceiver);
            } catch (Throwable ignored) {
            }
          }
        }
        mReceivers.remove(packageName);
      }
    }
  }

  @Override
  public void onPackageInstalled(String packageName, int userId) {
    synchronized (mReceivers) {
      mReceivers.remove(packageName);
      BPackageSettings bPackageSetting = mPms.getBPackageSetting(packageName);
      if (bPackageSetting != null) {
        registerPackage(bPackageSetting.pkg);
      }
    }
  }
}
