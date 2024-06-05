package com.lizongying.mytv

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.marginEnd
import androidx.core.view.marginTop
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.api.YSP
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                setAttributes(attributes)
            }
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

        binding.switchUpdateUrl.run {
            isChecked = SP.grid
            setOnCheckedChangeListener { _, isChecked ->
                SP.grid = isChecked
                (activity as MainActivity).settingDelayHide()

                if (true == isChecked) {
                    binding.version.text = "https://gitee.com/lizongying/my-tv"
                } else {
                    binding.version.text = "https://github.com/lizongying/my-tv"
                }
            }

            if (true == isChecked) {
                binding.version.text = "https://gitee.com/lizongying/my-tv"
            } else {
                binding.version.text = "https://github.com/lizongying/my-tv"
            }
        }

        binding.checkVersion.setOnClickListener {
            (activity as MainActivity).settingDelayHide()
            requestInstallPermissions()
        }

        binding.clear.setOnClickListener {
            (requireActivity() as MainActivity).syncTime()
            SP.guid = ""
            YSP.getGuid()
        }

        val application = requireActivity().applicationContext as MyTVApplication
        val textSize = application.px2PxFont(binding.switchChannelReversal.textSize)

        binding.content.layoutParams.width =
            application.px2Px(binding.content.layoutParams.width)
        binding.content.setPadding(
            application.px2Px(binding.content.paddingLeft),
            application.px2Px(binding.content.paddingTop),
            application.px2Px(binding.content.paddingRight),
            application.px2Px(binding.content.paddingBottom)
        )
        binding.name.textSize = application.px2PxFont(binding.name.textSize)
        binding.version.textSize = textSize
        val layoutParamsVersion = binding.version.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsVersion.topMargin = application.px2Px(binding.version.marginTop)
        binding.version.layoutParams = layoutParamsVersion

        binding.checkVersion.textSize = textSize
        val layoutParamsCheckVersion =
            binding.checkVersion.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsCheckVersion.marginEnd = application.px2Px(binding.checkVersion.marginEnd)
        binding.checkVersion.layoutParams = layoutParamsCheckVersion

        binding.versionName.textSize = textSize

        binding.clear.textSize = textSize
        binding.exit.textSize = textSize

        val layoutParamsChannelSwitch =
            binding.switchChannelReversal.layoutParams as ViewGroup.MarginLayoutParams
        layoutParamsChannelSwitch.topMargin =
            application.px2Px(binding.switchChannelReversal.marginTop)

        binding.switchChannelReversal.textSize = textSize
        binding.switchChannelReversal.layoutParams = layoutParamsChannelSwitch

        binding.switchChannelNum.textSize = textSize
        binding.switchChannelNum.layoutParams = layoutParamsChannelSwitch

        binding.switchTime.textSize = textSize
        binding.switchTime.layoutParams = layoutParamsChannelSwitch

        binding.switchBootStartup.textSize = textSize
        binding.switchBootStartup.layoutParams = layoutParamsChannelSwitch

        binding.switchGrid.textSize = textSize
        binding.switchGrid.layoutParams = layoutParamsChannelSwitch

        binding.switchUpdateUrl.textSize = textSize
        binding.switchUpdateUrl.layoutParams = layoutParamsChannelSwitch

        binding.appreciate.layoutParams.width =
            application.px2Px(binding.appreciate.layoutParams.width)

        binding.exit.setOnClickListener {
            requireActivity().finishAffinity()
        }

        updateManager = UpdateManager(context, context.appVersionCode)

        return binding.root
    }

    private fun requestInstallPermissions() {
//        val context = requireContext()
//        val permissionsList = mutableListOf<String>()
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
//            permissionsList.add(Manifest.permission.REQUEST_INSTALL_PACKAGES)
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
//            ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.READ_EXTERNAL_STORAGE
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            permissionsList.add(Manifest.permission.READ_EXTERNAL_STORAGE)
//        }
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
//            ContextCompat.checkSelfPermission(
//                context,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
//        }
//
//        if (permissionsList.isNotEmpty()) {
//            ActivityCompat.requestPermissions(
//                requireActivity(),
//                permissionsList.toTypedArray(),
//                PERMISSIONS_REQUEST_CODE
//            )
//        } else {
//            updateManager.checkAndUpdate()
//        }
        updateManager.checkAndUpdate()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            var allPermissionsGranted = true
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }
            if (allPermissionsGranted) {
                updateManager.checkAndUpdate()
            } else {
                Toast.makeText(context, "权限授权失败", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingFragment"
        const val PERMISSIONS_REQUEST_CODE = 1
    }
}

