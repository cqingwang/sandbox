package com.hello.sandbox.ui.home

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager2.widget.ViewPager2
import cbfg.rvadapter.RVAdapter
import com.hello.sandbox.Constant
import com.hello.sandbox.SandBoxCore
import com.hello.sandbox.common.ui.Toast
import com.hello.sandbox.common.util.Vu
import com.hello.sandbox.ui.appIcon.ChangeAppIconActivity
import com.hello.sandbox.ui.guide.GuideHelper
import com.hello.sandbox.ui.home.HomeAct.Companion.userID
import com.hello.sandbox.ui.password.SettingPasswordActivity
import com.hello.sandbox.ui.screen.ScreenOrientationActivity
import com.hello.sandbox.ui.upgrade.UpgradeAppViewModel
import com.hello.sandbox.ui.upgrade.UpgradeChecker
import com.hello.sandbox.util.*
import com.hello.sandbox.view.HandleAppPopup
import com.hello.sandbox.view.pager2banner.Banner
import com.hello.sandbox.view.pager2banner.IndicatorView
import com.hello.sandbox.view.pager2banner.IndicatorView.IndicatorStyle.INDICATOR_CIRCLE
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.util.XPopupUtils
import com.zhpan.indicator.utils.IndicatorUtils
import java.util.Collections
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.bean.AppInfo
import top.niunaijun.blackboxa.databinding.FragmentHomeBinding
import top.niunaijun.blackboxa.util.toast
import top.niunaijun.blackboxa.view.apps.AppsViewModel

class HomeFrag : Fragment() {
  private lateinit var binding: FragmentHomeBinding
  private lateinit var viewPager2: ViewPager2
  private lateinit var indicatorView: IndicatorView

  @androidx.annotation.ColorInt private var normalColor: Int = 0

  @androidx.annotation.ColorInt private var checkedColor: Int = 0

  private lateinit var mAdapter: RVAdapter<AppInfo>

  private lateinit var appInstallViewModel: AppInstallViewModel

  private var popupMenu: PopupMenu? = null

  private var banner: Banner? = null

  private lateinit var appInfos: ArrayList<AppInfo>

  companion object {
    const val KEY_UPGRADE_REQUEST_TIME = "upgrade_request_time"
    @JvmStatic
    fun getInstance(): HomeFrag {
      return HomeFrag()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = FragmentHomeBinding.inflate(layoutInflater)
    //    viewPager2 = binding.viewPager2
    //    viewPager2.adapter = ViewPager2Adapter(getData())
    //    indicatorView = binding.indicatorView
    //    indicatorView.apply {
    //      setIndicatorStyle(IndicatorStyle.ROUND_RECT)
    //      setSliderGap(IndicatorUtils.dp2px(4f).toFloat())
    //      setSliderHeight(resources.getDimensionPixelOffset(R.dimen.dp_6).toFloat())
    //      setSlideMode(IndicatorSlideMode.SMOOTH)
    //      setSliderColor(normalColor, checkedColor)
    //      setOrientation(IndicatorOrientation.INDICATOR_HORIZONTAL)
    //      setupWithViewPager(viewPager2)
    //    }
    normalColor = ContextCompat.getColor(context!!, R.color.indicator_normal)
    checkedColor = ContextCompat.getColor(context!!, R.color.indicator_selected)
    banner = binding.banner
    banner!!.post {
      banner!!.layoutParams.width = Vu.screenWidth()
      banner!!.layoutParams.height = (Vu.screenWidth() / 2.5).toInt()
      banner!!.requestLayout()
    }
    banner!!.setIndicator(
      IndicatorView(requireContext())
        .setIndicatorColor(normalColor)
        .setIndicatorSpacing(IndicatorUtils.dp2px(4f).toFloat())
        .setIndicatorSelectorColor(checkedColor)
        .setIndicatorStyle(INDICATOR_CIRCLE)
    )

    banner!!.adapter = ViewPager2Adapter(getData())
    (banner!!.adapter as ViewPager2Adapter).setOnItemClickListener {
      if (TextUtils.isEmpty(it.url)) {
        GuideHelper.showGuideDlg(requireContext())
      } else {
        MarketHelper.goToAppMarket(requireActivity(), it.url)
      }
    }

    appInstallViewModel = ViewModelProvider(this).get(AppInstallViewModel::class.java)
    appInstallViewModel.registerReceiver(requireContext())
    mAdapter = RVAdapter<AppInfo>(requireContext(), AppsAdapter()).bind(binding.recyclerView)
    binding.recyclerView.adapter = mAdapter
    binding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 4)
    setOnLongClick()
    mAdapter.setItemClickListener { _, data, _ ->
      if (data.isDefault) {
        (requireActivity() as HomeAct).jumpSelectApkList()
      } else {
        showLoading()
        ViewModelProvider(requireActivity())
          .get(AppsViewModel::class.java)
          .launchApk(data.packageName, userID)
      }
    }
    binding.ilHomeCenterLayout.imageSetSecret.singleClickListener {
      startActivity(Intent(it.context, SettingPasswordActivity::class.java))
    }
    binding.ilHomeCenterLayout.imageChangeIcon.singleClickListener {
      startActivity(Intent(it.context, ChangeAppIconActivity::class.java))
    }
    binding.ilHomeCenterLayout.imageRotateScreen.singleClickListener {
      //      startActivity(Intent(it.context, RotationVectorDemo::class.java))
      startActivity(Intent(it.context, ScreenOrientationActivity::class.java))
    }
    if (!ChannelHelper.isXiaomi()) {
      binding.ilHomeCenterLayout.rlChangeIcon.visibility = View.VISIBLE
      binding.ilHomeCenterLayout.vEmpty.visibility = View.GONE
    }
    checkNewVersionAndShowDialog()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    initData()
  }

  override fun setUserVisibleHint(isVisibleToUser: Boolean) {
    super.setUserVisibleHint(isVisibleToUser)
    if (isVisibleToUser) {
      startBannerTurning()
    } else {
      stopBannerTurning()
    }
  }

  override fun onResume() {
    super.onResume()
    if (userVisibleHint) {
      startBannerTurning()
    }
  }

  override fun onPause() {
    super.onPause()
    stopBannerTurning()
  }

  private fun startBannerTurning() {
    banner?.startTurning()
  }

  private fun stopBannerTurning() {
    banner?.stopTurning()
  }

  private fun initData() {
    appInfos = ArrayList()
    (requireActivity() as HomeAct).viewModel.appsLiveData.observe(viewLifecycleOwner) {
      if (it != null) {
        appInfos.clear()
        appInfos.addAll(it)
        addDefaultIcon()
        mAdapter.setItems(appInfos)
      } else {
        addDefaultIcon()
      }
    }
    (requireActivity() as HomeAct).viewModel.resultLiveData.observe(viewLifecycleOwner) {
      if (!TextUtils.isEmpty(it)) {
        hideLoading()
        val message = it.split(":")[0]
        Toast.message(message)
        var appName = ""
        if (it.split(":").size > 1) {
          appName = it.split(":")[1]
        }
        ViewModelProvider(requireActivity()).get(AppsViewModel::class.java).getInstalledApps(userID)
      }
    }
    (requireActivity() as HomeAct).viewModel.launchLiveData.observe(viewLifecycleOwner) {
      it?.run {
        hideLoading()
        if (!it) {
          Toast.message(R.string.start_fail)
        }
      }
    }
    appInstallViewModel.appinstallOrUninstallData.observe(this) {
      if (appInfos.size > 1) {
        appInfos.onEach {
          it
          try {
            val info = SandBoxCore.getPackageManager().getPackageInfo(it.packageName, 0)
            it.isHide = info == null
          } catch (e: Throwable) {
            it.isHide = true
          }
        }
        mAdapter.setItems(appInfos)
        mAdapter.notifyDataSetChanged()
      }
    }
  }

  private fun addDefaultIcon() {
    val drawable: Drawable = resources.getDrawable(R.drawable.home_icon_add)
    val info = AppInfo("添加应用", drawable, "", "", false, true)
    appInfos.add(info)
  }

  fun getData(): List<HomeBannerInfo> {
    val list = ArrayList<HomeBannerInfo>()
    var info1 = HomeBannerInfo("", resources.getDrawable(R.drawable.home_banner_1))
//    var info2 =
//      HomeBannerInfo(
//        Constant.RECOMMEND_MOMO_APP_URL,
//        resources.getDrawable(R.drawable.home_banner_2)
//      )
//    var info3 =
//      HomeBannerInfo(
//        Constant.RECOMMEND_TANTAN_APP_URL,
//        resources.getDrawable(R.drawable.home_banner_3)
//      )
    list.add(info1)
//    list.add(info2)
//    list.add(info3)
    return list
  }

  private fun setOnLongClick() {
    mAdapter.setItemLongClickListener { view, data, _ ->
      if (!data.isDefault) {
        popupMenu =
          PopupMenu(requireContext(), view).also {
            it.inflate(R.menu.app_menu)
            it.setOnMenuItemClickListener { item ->
              when (item.itemId) {
                R.id.app_remove -> {
                  if (data.isXpModule) {
                    toast(R.string.uninstall_module_toast)
                  } else {
                    unInstallApk(data)
                  }
                }
                R.id.app_clear -> {
                  clearApk(data)
                }
                R.id.app_stop -> {
                  stopApk(data)
                }
              //                R.id.app_shortcut -> {
              //                  ShortcutUtil.createShortcut(requireContext(), userID, data)
              //                }
              }
              return@setOnMenuItemClickListener true
            }
            it.show()
          }
      }
    }
  }

  private fun unInstallApk(info: AppInfo) {
    var handleAppPopup =
      HandleAppPopup(
        requireContext(),
        info.name,
        info.icon,
        {
          showLoading()
          ViewModelProvider(requireActivity())
            .get(AppsViewModel::class.java)
            .unInstall(info.packageName, userID, info.name)
        },
        {},
        getString(R.string.uninstall_app_hint, info.name),
        getString(R.string.uninstall_confirm),
        getString(R.string.uninstall_cancel)
      )
    showXPopup(handleAppPopup)
  }

  fun installApk(source: String, fromSystem: Boolean, appName: String? = "") {
    showLoading("加载中")
    (requireActivity() as HomeAct).viewModel.install(source, userID, fromSystem, appName)
  }

  /**
   * 强行停止软件
   * @param info AppInfo
   */
  private fun stopApk(info: AppInfo) {
    var handleAppPopup =
      HandleAppPopup(
        requireContext(),
        info.name,
        info.icon,
        {
          SandBoxCore.get().stopPackage(info.packageName, userID)
          Toast.message(getString(R.string.is_stop, info.name))
        },
        {},
        getString(R.string.app_stop_hint, info.name),
        getString(R.string.stop_app_confirm),
        getString(R.string.stop_app_cancel)
      )
    showXPopup(handleAppPopup)
  }

  private fun clearApk(info: AppInfo) {
    var handleAppPopup =
      HandleAppPopup(
        requireContext(),
        info.name,
        info.icon,
        {
          showLoading()
          (requireActivity() as HomeAct).viewModel.clearApkData(info.packageName, userID)
        },
        {},
        getString(R.string.app_clear_hint, info.name),
        getString(R.string.clear_confirm),
        getString(R.string.clear_cancel)
      )
    showXPopup(handleAppPopup)
  }

  private fun showXPopup(popup: HandleAppPopup) {
    XPopup.Builder(requireContext())
      .moveUpToKeyboard(false)
      .maxWidth(
        XPopupUtils.getScreenWidth(requireContext()) - XPopupUtils.dp2px(requireContext(), 40f)
      )
      .isDestroyOnDismiss(true)
      .dismissOnTouchOutside(false)
      .dismissOnBackPressed(false)
      .asCustom(this.let { popup })
      .show()
  }

  private fun showLoading() {
    (requireActivity() as HomeAct).showLoading()
  }

  private fun showLoading(message: String) {
    (requireActivity() as HomeAct).showLoading(message)
  }

  private fun hideLoading() {
    (requireActivity() as HomeAct).hideLoading()
  }

  private fun onItemMove(fromPosition: Int, toPosition: Int) {
    if (fromPosition < toPosition) {
      for (i in fromPosition until toPosition) {
        Collections.swap(mAdapter.getItems(), i, i + 1)
      }
    } else {
      for (i in fromPosition downTo toPosition + 1) {
        Collections.swap(mAdapter.getItems(), i, i - 1)
      }
    }
    mAdapter.notifyItemMoved(fromPosition, toPosition)
  }

  override fun onDestroy() {
    super.onDestroy()
    appInstallViewModel.unregister()
  }

  private fun checkNewVersionAndShowDialog() {
    val lastTime = SharedPrefUtils.getLongData(requireContext(), KEY_UPGRADE_REQUEST_TIME)
    if (!TimeHelper.isSameDay(System.currentTimeMillis(), lastTime)) {
      val upgradeAppViewModel = ViewModelProvider(this).get(UpgradeAppViewModel::class.java)
      upgradeAppViewModel.checkAppUpgradeInfo()
      upgradeAppViewModel.upgradeModel.observe(this) {
        it?.apply {
          SharedPrefUtils.saveData(
            requireContext(),
            KEY_UPGRADE_REQUEST_TIME,
            System.currentTimeMillis()
          )
          if (UpgradeChecker.hasNewVersion(it)) {
            UpgradeChecker.showAppUpgradePopUp(requireContext(), it)
          }
        }
      }
    }
  }
}
