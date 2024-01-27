package com.lizongying.mytv

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.lizongying.mytv.databinding.InfoBinding
import com.lizongying.mytv.models.TVViewModel

class InfoFragment : Fragment() {
    private var _binding: InfoBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler()
    private val delay: Long = 3000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = InfoBinding.inflate(inflater, container, false)
        _binding!!.root.visibility = View.GONE
        (activity as MainActivity).fragmentReady()
        return binding.root
    }

    fun show(tvViewModel: TVViewModel) {
        binding.textView.text = tvViewModel.title.value

        when (tvViewModel.title.value) {
            "CCTV8K 超高清" -> Glide.with(this)
                .load(R.drawable.cctv8k)
                .into(binding.infoLogo)

            "天津卫视" -> Glide.with(this)
                .load(R.drawable.tianjin)
                .into(binding.infoLogo)

            "新疆卫视" -> Glide.with(this)
                .load(R.drawable.xinjiang)
                .into(binding.infoLogo)

            else -> Glide.with(this)
                .load(tvViewModel.logo.value)
                .into(binding.infoLogo)
        }

        val program = tvViewModel.getProgramOne()
        if (program != null) {
            binding.infoDesc.text = program.name
        }

        handler.removeCallbacks(removeRunnable)
        view?.visibility = View.VISIBLE
        handler.postDelayed(removeRunnable, delay)
    }

    override fun onResume() {
        super.onResume()
        handler.postDelayed(removeRunnable, delay)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(removeRunnable)
    }

    private val removeRunnable = Runnable {
        view?.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAG = "InfoFragment"
    }
}