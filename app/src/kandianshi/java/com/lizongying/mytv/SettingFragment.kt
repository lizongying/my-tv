package com.lizongying.mytv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.databinding.SettingBinding

class SettingFragment : DialogFragment() {
    private var binding: SettingBinding? = null
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
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = SettingBinding.inflate(inflater, container, false)
        binding = view
        view.versionName.text = "v" + requireContext().appVersionName
        view.version.text = "内置央视、卫视、地方台\n每 6 小时检查源更新\n来源：iptv-org / vbskycn / YueChan"
        view.switchChannelReversal.apply {
            isChecked = SP.channelReversal
            setOnCheckedChangeListener { _, value -> SP.channelReversal = value }
        }
        view.switchChannelNum.apply {
            isChecked = SP.channelNum
            setOnCheckedChangeListener { _, value -> SP.channelNum = value }
        }
        view.switchTime.apply {
            isChecked = SP.time
            setOnCheckedChangeListener { _, value -> SP.time = value; (activity as MainActivity).settingDelayHide() }
        }
        view.switchBootStartup.apply {
            isChecked = SP.bootStartup
            setOnCheckedChangeListener { _, value -> SP.bootStartup = value }
        }
        view.checkVersion.text = "更新直播源"
        view.checkVersion.setOnClickListener { (activity as MainActivity).refreshSources() }
        view.agentSettings.setOnClickListener {
            val owner = activity as MainActivity
            owner.settingNeverHide()
            startActivity(android.content.Intent(requireContext(), com.lizongying.mytv.agent.AgentActivity::class.java)
                .putExtra("currentUrl", owner.currentStreamUrl()))
            dismiss()
        }
        view.exit.setOnClickListener { requireActivity().finishAffinity() }
        return view.root
    }
    override fun onDestroyView() { binding = null; super.onDestroyView() }
}
