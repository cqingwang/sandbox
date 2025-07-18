package com.hello.sandbox.core.system.pm;

import java.util.ArrayList;
import java.util.List;
import com.hello.sandbox.core.system.ISystemService;
import com.hello.sandbox.core.system.pm.installer.CopyExecutor;
import com.hello.sandbox.core.system.pm.installer.CreatePackageExecutor;
import com.hello.sandbox.core.system.pm.installer.CreateUserExecutor;
import com.hello.sandbox.core.system.pm.installer.Executor;
import com.hello.sandbox.core.system.pm.installer.RemoveAppExecutor;
import com.hello.sandbox.core.system.pm.installer.RemoveUserExecutor;
import com.hello.sandbox.entity.pm.InstallOption;
import com.hello.sandbox.utils.Slog;

/** Created by Milk on 4/21/21. * ∧＿∧ (`･ω･∥ 丶　つ０ しーＪ 此处无Bug */
public class BPackageInstallerService extends IBPackageInstallerService.Stub
    implements ISystemService {
  private static final BPackageInstallerService sService = new BPackageInstallerService();

  public static BPackageInstallerService get() {
    return sService;
  }

  public static final String TAG = "BPackageInstallerService";

  @Override
  public int installPackageAsUser(BPackageSettings ps, int userId) {
    List<Executor> executors = new ArrayList<>();
    // 创建用户环境相关操作
    executors.add(new CreateUserExecutor());
    // 创建应用环境相关操作
    executors.add(new CreatePackageExecutor());
    // 拷贝应用相关文件
    executors.add(new CopyExecutor());
    InstallOption option = ps.installOption;
    for (Executor executor : executors) {
      int exec = executor.exec(ps, option, userId);
      Slog.log(
          TAG, "installPackageAsUser: " + executor.getClass().getSimpleName() + " exec: " + exec);
      if (exec != 0) {
        return exec;
      }
    }
    return 0;
  }

  @Override
  public int uninstallPackageAsUser(BPackageSettings ps, boolean removeApp, int userId) {
    List<Executor> executors = new ArrayList<>();
    if (removeApp) {
      // 移除App
      executors.add(new RemoveAppExecutor());
    }
    // 移除用户相关目录
    executors.add(new RemoveUserExecutor());
    InstallOption option = ps.installOption;
    for (Executor executor : executors) {
      int exec = executor.exec(ps, option, userId);
      Slog.log(
          TAG, "uninstallPackageAsUser: " + executor.getClass().getSimpleName() + " exec: " + exec);
      if (exec != 0) {
        return exec;
      }
    }
    return 0;
  }

  @Override
  public int clearPackage(BPackageSettings ps, int userId) {
    List<Executor> executors = new ArrayList<>();
    // 移除用户相关目录
    executors.add(new RemoveUserExecutor());
    // 创建用户环境相关操作
    executors.add(new CreateUserExecutor());
    InstallOption option = ps.installOption;
    for (Executor executor : executors) {
      int exec = executor.exec(ps, option, userId);
      Slog.log(
          TAG, "uninstallPackageAsUser: " + executor.getClass().getSimpleName() + " exec: " + exec);
      if (exec != 0) {
        return exec;
      }
    }
    return 0;
  }

  @Override
  public int updatePackage(BPackageSettings ps) {
    List<Executor> executors = new ArrayList<>();
    executors.add(new CreatePackageExecutor());
    executors.add(new CopyExecutor());
    InstallOption option = ps.installOption;
    for (Executor executor : executors) {
      int exec = executor.exec(ps, option, -1);
      if (exec != 0) {
        return exec;
      }
    }
    return 0;
  }

  @Override
  public void systemReady() {}
}
