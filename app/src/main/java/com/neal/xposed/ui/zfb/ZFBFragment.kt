package com.neal.xposed.ui.zfb

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.neal.xposed.R

class ZFBFragment : Fragment() {

    private lateinit var zfbViewModel: ZFBViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        zfbViewModel =
            ViewModelProviders.of(this).get(ZFBViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_gallery, container, false)
        val textView: AppCompatTextView = root.findViewById(R.id.text_gallery)
        val plus5: AppCompatButton = root.findViewById(R.id.plus5)
        val minus5: AppCompatButton = root.findViewById(R.id.minus5)
        val autoCollectOpen: SwitchCompat = root.findViewById(R.id.autoCollectOpen)
        val autoCollectInterval: AppCompatTextView = root.findViewById(R.id.autoCollectInterval)
        val autoCollectIntervalOpen: SwitchCompat = root.findViewById(R.id.autoCollectIntervalOpen)

        zfbViewModel.autoCollectOpen.observe(this, Observer {
            autoCollectOpen.isChecked = it
        })

        autoCollectOpen.setOnCheckedChangeListener { _, isChecked ->
            zfbViewModel.autoCollect(context!!, isChecked)
        }

        zfbViewModel.text.observe(this, Observer {
            textView.text = it
        })

        zfbViewModel.interval.observe(this, Observer {
            autoCollectInterval.text = "时间间隔: ${it}s"
        })

        zfbViewModel.autoCollectIntervalOpen.observe(this, Observer {
            autoCollectIntervalOpen.isChecked = it
        })

        autoCollectIntervalOpen.setOnCheckedChangeListener { _, isChecked ->
            zfbViewModel.intervalCollect(context!!, isChecked)
        }

        plus5.setOnClickListener {
            zfbViewModel.plus5Seconds(context!!)

        }
        minus5.setOnClickListener {
            zfbViewModel.minus5Seconds(context!!)
        }
        zfbViewModel.setup(context!!)
        return root
    }

}