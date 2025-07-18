/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hello.sandbox.utils;

import android.util.Log;

/** @hide */
public final class Slog {
  /** @hide */
  public static final int LOG_ID_SYSTEM = 3;

  private Slog() {}

  public static int v(String tag, String msg) {
    return Log.println(Log.VERBOSE, tag, msg);
  }

  public static int v(String tag, String msg, Throwable tr) {
    return Log.println(Log.VERBOSE, tag, msg + '\n' + Log.getStackTraceString(tr));
  }

  public static int d(String tag, String msg) {
    return Log.println(Log.DEBUG, tag, msg);
  }

  public static int d(String tag, String msg, Throwable tr) {
    return Log.println(Log.DEBUG, tag, msg + '\n' + Log.getStackTraceString(tr));
  }

  public static int i(String tag, String msg) {
    return Log.println(Log.INFO, tag, msg);
  }

  public static int i(String tag, String msg, Throwable tr) {
    return Log.println(Log.INFO, tag, msg + '\n' + Log.getStackTraceString(tr));
  }

  public static int w(String tag, String msg) {
    return Log.println(Log.WARN, tag, msg);
  }

  public static int w(String tag, String msg, Throwable tr) {
    return Log.println(Log.WARN, tag, msg + '\n' + Log.getStackTraceString(tr));
  }

  public static int w(String tag, Throwable tr) {
    return Log.println(Log.WARN, tag, Log.getStackTraceString(tr));
  }

  public static int e(String tag, String msg) {
    return Log.println(Log.ERROR, tag, msg);
  }

  public static int e(String tag, String msg, Throwable tr) {
    return Log.println(Log.ERROR, tag, msg + '\n' + Log.getStackTraceString(tr));
  }

  public static int println(int priority, String tag, String msg) {
    return Log.println(priority, tag, msg);
  }

  public static void log(String mod, String msg) {
    d("vplay", mod + " -> " + msg);
  }

  public static void logw(String mod, String msg) {
    v("vplay", mod + " -> " + msg);
  }

  public static void loge(String mod, String msg) {
    e("vplay", mod + " -> " + msg);
  }

  public static void loge(String mod, String msg, Throwable e) {
    e("vplay", mod + " -> " + msg, e);
  }

  public static String stacks(String label) {
    StackTraceElement[] el = new Throwable().getStackTrace();
    String text = label + "|stack|";

    if (el != null) {
      for (int i = 0; i < el.length; i++) {
        text = text + i + ": " + el[i].getClassName() + "." + el[i].getMethodName() + "() @" + el[i].getFileName() + "#" + el[i].getLineNumber() + "\n";
      }
    }
    return text;
  }
}
