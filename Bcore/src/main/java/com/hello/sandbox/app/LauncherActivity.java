package com.hello.sandbox.app;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import com.hello.sandbox.R;
import com.hello.sandbox.SandBoxCore;
import com.hello.sandbox.utils.Slog;

/** Created by BlackBox on 2022/2/24. */
public class LauncherActivity extends Activity {
  public static final String TAG = "SplashScreen";

  public static final String KEY_INTENT = "launch_intent";
  public static final String KEY_PKG = "launch_pkg";
  public static final String KEY_USER_ID = "launch_user_id";
  private boolean isRunning = false;

  public static void launch(Intent intent, int userId) {
    Intent splash = new Intent();
    splash.setClass(SandBoxCore.getContext(), LauncherActivity.class);
    splash.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

    splash.putExtra(LauncherActivity.KEY_INTENT, intent);
    splash.putExtra(LauncherActivity.KEY_PKG, intent.getPackage());
    splash.putExtra(LauncherActivity.KEY_USER_ID, userId);
    SandBoxCore.getContext().startActivity(splash);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent intent = getIntent();
    if (intent == null) {
      finish();
      return;
    }
    Intent launchIntent = intent.getParcelableExtra(KEY_INTENT);
    String packageName = intent.getStringExtra(KEY_PKG);
    int userId = intent.getIntExtra(KEY_USER_ID, 0);

    PackageInfo packageInfo =
        SandBoxCore.getBPackageManager().getPackageInfo(packageName, 0, userId);
    if (packageInfo == null) {
      Slog.loge(TAG, packageName + " not installed!");
      finish();
      return;
    }
    Drawable drawable = packageInfo.applicationInfo.loadIcon(SandBoxCore.getPackageManager());
    setContentView(R.layout.activity_launcher);
    findViewById(R.id.iv_icon).setBackgroundDrawable(drawable);
    new Thread(() -> SandBoxCore.getBActivityManager().startActivity(launchIntent, userId)).start();
  }

  @Override
  protected void onPause() {
    super.onPause();
    isRunning = true;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (isRunning) {
      finish();
    }
  }
}
