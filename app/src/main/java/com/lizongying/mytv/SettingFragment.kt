package com.lizongying.mytv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.databinding.DialogBinding


class SettingFragment(
    private val versionName: String,
    private val versionCode: Long,
    private val channelReversal: Boolean,
    private val channelNum: Boolean,
    private val bootStartup: Boolean,
) :
    DialogFragment() {

    private var _binding: DialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogBinding.inflate(inflater, container, false)
        _binding?.version?.text =
            "当前版本: $versionName\n获取最新: https://github.com/lizongying/my-tv/releases/"

        val switchChannelReversal = _binding?.switchChannelReversal
        switchChannelReversal?.isChecked = channelReversal
        switchChannelReversal?.setOnCheckedChangeListener { _, isChecked ->
            (activity as MainActivity).saveChannelReversal(isChecked)
            (activity as MainActivity).settingActive()
        }

        val switchChannelNum = _binding?.switchChannelNum
        switchChannelNum?.isChecked = channelNum
        switchChannelNum?.setOnCheckedChangeListener { _, isChecked ->
            (activity as MainActivity).saveChannelNum(isChecked)
            (activity as MainActivity).settingActive()
        }

        val switchBootStartup = _binding?.switchBootStartup
        switchBootStartup?.isChecked = bootStartup
        switchBootStartup?.setOnCheckedChangeListener { _, isChecked ->
            (activity as MainActivity).saveBootStartup(isChecked)
            (activity as MainActivity).settingActive()
        }

        updateManager = UpdateManager(context, this, versionCode)
        _binding?.checkVersion?.setOnClickListener(OnClickListenerCheckVersion(updateManager))

        return binding.root
    }

    fun setVersionName(versionName: String) {
        binding.versionName.text = versionName
    }

    internal class OnClickListenerCheckVersion(private val updateManager: UpdateManager) :
        View.OnClickListener {
        override fun onClick(view: View?) {
            updateManager.checkAndUpdate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
//        updateManager.destroy()
    }

    companion object {
        const val TAG = "SettingFragment"
    }
}

