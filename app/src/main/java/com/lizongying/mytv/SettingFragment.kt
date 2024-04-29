package com.lizongying.mytv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.databinding.SettingBinding


class SettingFragment : DialogFragment() {

    private var _binding: SettingBinding? = null
    private val binding get() = _binding!!

    private lateinit var updateManager: UpdateManager

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val context = requireContext() // It‘s safe to get context here.
        _binding = SettingBinding.inflate(inflater, container, false)
        binding.versionName.text = "当前版本: v${context.appVersionName}"
        binding.version.text = "https://github.com/lizongying/my-tv"

        binding.switchChannelReversal.run {
            isChecked = SP.channelReversal
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelReversal = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchChannelNum.run {
            isChecked = SP.channelNum
            setOnCheckedChangeListener { _, isChecked ->
                SP.channelNum = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchTime.run {
            isChecked = SP.time
            setOnCheckedChangeListener { _, isChecked ->
                SP.time = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchBootStartup.run {
            isChecked = SP.bootStartup
            setOnCheckedChangeListener { _, isChecked ->
                SP.bootStartup = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        binding.switchGrid.run {
            isChecked = SP.grid
            setOnCheckedChangeListener { _, isChecked ->
                SP.grid = isChecked
                (activity as MainActivity).settingDelayHide()
            }
        }

        updateManager = UpdateManager(context, this, context.appVersionCode)
        binding.checkVersion.setOnClickListener(
            OnClickListenerCheckVersion(
                activity as MainActivity,
                updateManager
            )
        )

        val application = requireActivity().applicationContext as MyTvApplication

        binding.content.layoutParams.width =
            application.px2Px(binding.content.layoutParams.width)
        binding.content.setPadding(
            application.px2Px(binding.content.paddingLeft),
            application.px2Px(binding.content.paddingTop),
            application.px2Px(binding.content.paddingRight),
            application.px2Px(binding.content.paddingBottom)
        )
        binding.name.textSize = application.px2PxFont(binding.name.textSize)
        binding.version.textSize = application.px2PxFont(binding.version.textSize)
        val layoutParamsVersion = binding.version.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsVersion.topMargin = application.px2Px(binding.version.marginTop)
        binding.version.layoutParams = layoutParamsVersion

        binding.checkVersion.textSize = application.px2PxFont(binding.checkVersion.textSize)
        val layoutParamsCheckVersion =
            binding.checkVersion.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsCheckVersion.marginEnd = application.px2Px(binding.checkVersion.marginEnd)
        binding.checkVersion.layoutParams = layoutParamsCheckVersion

        binding.versionName.textSize = application.px2PxFont(binding.versionName.textSize)

        binding.exit.textSize = application.px2PxFont(binding.exit.textSize)

        binding.switchChannelReversal.textSize =
            application.px2PxFont(binding.switchChannelReversal.textSize)
        val layoutParamsChannelReversal =
            binding.switchChannelReversal.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsChannelReversal.topMargin =
            application.px2Px(binding.switchChannelReversal.marginTop)
        binding.switchChannelReversal.layoutParams = layoutParamsChannelReversal

        binding.switchChannelNum.textSize = application.px2PxFont(binding.switchChannelNum.textSize)
        val layoutParamsChannelNum =
            binding.switchChannelNum.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsChannelNum.topMargin = application.px2Px(binding.switchChannelNum.marginTop)
        binding.switchChannelNum.layoutParams = layoutParamsChannelNum

        binding.switchTime.textSize = application.px2PxFont(binding.switchTime.textSize)
        val layoutParamsTime = binding.switchTime.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsTime.topMargin = application.px2Px(binding.switchTime.marginTop)
        binding.switchTime.layoutParams = layoutParamsTime

        binding.switchBootStartup.textSize =
            application.px2PxFont(binding.switchBootStartup.textSize)
        val layoutParamsBootStartup =
            binding.switchBootStartup.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsBootStartup.topMargin = application.px2Px(binding.switchBootStartup.marginTop)
        binding.switchBootStartup.layoutParams = layoutParamsBootStartup

        binding.switchGrid.textSize = application.px2PxFont(binding.switchGrid.textSize)
        val layoutParamsGrid = binding.switchGrid.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsGrid.topMargin = application.px2Px(binding.switchGrid.marginTop)
        binding.switchGrid.layoutParams = layoutParamsGrid

        binding.appreciate.layoutParams.width =
            application.px2Px(binding.appreciate.layoutParams.width)

        binding.exit.setOnClickListener {
            requireActivity().finishAffinity()
        }

        return binding.root
    }

    fun setVersionName(versionName: String) {
        if (_binding != null) {
            binding.versionName.text = versionName
        }
    }

    internal class OnClickListenerCheckVersion(
        private val mainActivity: MainActivity,
        private val updateManager: UpdateManager
    ) :
        View.OnClickListener {
        override fun onClick(view: View?) {
            mainActivity.settingDelayHide()
            updateManager.checkAndUpdate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingFragment"
    }
}

