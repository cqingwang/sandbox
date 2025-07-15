package com.hello.sandbox.ui.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.hello.sandbox.ui.base.BaseAct
import com.hello.sandbox.ui.guide.GuideHelper
import com.hello.sandbox.ui.search.SearchListActivity
import com.hello.sandbox.view.NoScrollViewPager
import com.hello.sandbox.common.util.ViewUtil
import top.niunaijun.blackboxa.R
import top.niunaijun.blackboxa.databinding.ActivityHomeBinding
import top.niunaijun.blackboxa.util.InjectionUtil
import top.niunaijun.blackboxa.view.apps.AppsViewModel

class HomeAct : BaseAct() {
  private lateinit var binding: ActivityHomeBinding
  private lateinit var viewPager: NoScrollViewPager
//  private lateinit var imageHome: ImageView
//  private lateinit var imageAdd: ImageView
//  private lateinit var imageMe: ImageView
//  private lateinit var rlHome: RelativeLayout
//  private lateinit var rlMe: RelativeLayout

  private val fragments: MutableList<Fragment> = ArrayList()

  lateinit var viewModel: AppsViewModel

  companion object {
    const val userID = 0
    fun start(context: Context) {
      val intent = Intent(context, HomeAct::class.java)
      context.startActivity(intent)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityHomeBinding.inflate(layoutInflater)
    setContentView(binding.root)
    initToolbar(binding.toolbarLayout.toolbar, R.string.app_name)
    initViews()
//    initClickListener()
    initViewModel()
    if (GuideHelper.isNeedShowGuideDlg(this)) {
      GuideHelper.showGuideDlg(this)
      GuideHelper.updateShowGuide(this, false)
    }
  }

  private fun initViewModel() {
    viewModel =
      ViewModelProvider(this, InjectionUtil.getAppsFactory()).get(AppsViewModel::class.java)
    viewModel.getInstalledApps(userID)
  }

  private fun initViews() {
//    imageHome = binding.imageHome
//    imageAdd = binding.imageAdd
//    imageMe = binding.imageMe
//    rlHome = binding.rlHome
//    rlMe = binding.rlMe
    viewPager = binding.viewPager
    viewPager.offscreenPageLimit = 2
    fragments.run {
      add(HomeFrag.getInstance())
//      add(MeFrag.getInstance())
    }
    viewPager.adapter = PageAdapter(supportFragmentManager, fragments)
    viewPager.currentItem = 0
//    imageHome.setImageResource(R.drawable.bottom_tab_home_selected)
//    imageMe.setImageResource(R.drawable.bottom_tab_me_unselected)
  }

  private val apkPathResult =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == RESULT_OK) {
        it.data?.let { data ->
          val userId = data.getIntExtra("userID", 0)
          val source = data.getStringExtra("source")
          val fromSystem = data.getBooleanExtra("fromSystem", true)
          val appName = data.getStringExtra("appName")
          if (source != null) {
            (fragments[0] as HomeFrag).installApk(source, fromSystem, appName)
          }
        }
      }
    }

//  private fun initClickListener() {
//    ViewUtil.singleClickListener(rlHome) { changeToHome() }
//
//    ViewUtil.singleClickListener(imageAdd) { jumpSelectApkList() }
//
//    ViewUtil.singleClickListener(rlMe) { changeToMe() }
//  }

  fun jumpSelectApkList() {
    val intent = Intent(this, SearchListActivity::class.java)
    apkPathResult.launch(intent)
  }

//  private fun changeToHome() {
//    viewPager.currentItem = 0
//    imageHome.setImageResource(R.drawable.bottom_tab_home_selected)
//    imageMe.setImageResource(R.drawable.bottom_tab_me_unselected)
//    updateToolBarTitle(binding.toolbarLayout.toolbar, getString(R.string.app_name))
//  }

//  private fun changeToMe() {
//    viewPager.currentItem = 1
//    imageMe.setImageResource(R.drawable.bottom_tab_me_selected)
//    imageHome.setImageResource(R.drawable.bottom_tab_home_unselected)
//    updateToolBarTitle(binding.toolbarLayout.toolbar, "我的")
//  }

  override fun onBackPressed() {
//    if (viewPager.currentItem != 0) {
//      changeToHome()
//    } else {
//      moveTaskToBack(true)
//    }
    moveTaskToBack(true)
  }
}
