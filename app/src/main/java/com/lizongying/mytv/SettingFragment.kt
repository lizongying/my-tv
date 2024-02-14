package com.lizongying.mytv

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.lizongying.mytv.databinding.DialogBinding


class SettingFragment(private val versionName: String,
                      private val channelReversal: Boolean,
                      private val channelNum: Boolean,
                      private val bootStartup: Boolean,
                      private val selectedProvince: String,
    ) :
    DialogFragment() {

    private var _binding: DialogBinding? = null
    private val binding get() = _binding!!

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
        }

        val switchChannelNum = _binding?.switchChannelNum
        switchChannelNum?.isChecked = channelNum
        switchChannelNum?.setOnCheckedChangeListener { _, isChecked ->
            (activity as MainActivity).saveChannelNum(isChecked)
        }

        val switchBootStartup = _binding?.switchBootStartup
        switchBootStartup?.isChecked = bootStartup
        switchBootStartup?.setOnCheckedChangeListener { _, isChecked ->
            (activity as MainActivity).saveBootStartup(isChecked)
        }

        // 设置省份Spinner
        val provinceSpinner = binding.provinceSpinner
        val provinces = arrayOf("北京", "上海", "天津", "重庆", "河北", "山西", "内蒙古", "辽宁", "吉林", "黑龙江", "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南", "广东", "广西", "海南", "四川", "贵州", "云南", "西藏", "陕西", "甘肃", "青海", "宁夏", "新疆", "台湾", "香港", "澳门")
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, provinces)
        provinceSpinner.adapter = adapter

        // 设置默认选择为“湖南”
        val defaultIndex = provinces.indexOf("湖南")
        if (defaultIndex != -1) {
            provinceSpinner.setSelection(defaultIndex)
        }

        // 设置Spinner的监听器
        provinceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedProvince = provinces[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedProvince = null
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // 取消Spinner的监听器(看是否需要吧)
        binding.provinceSpinner.onItemSelectedListener = null
    }

    companion object {
        const val TAG = "SettingFragment"
    }
}

