package com.neal.xposed.ui.alipay

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

class AlipayFragment : Fragment() {

    private lateinit var alipayViewModel: AlipayViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        alipayViewModel =
            ViewModelProviders.of(this).get(AlipayViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_alipay, container, false)
        val plus5: AppCompatButton = root.findViewById(R.id.plus5)
        val minus5: AppCompatButton = root.findViewById(R.id.minus5)
        val autoCollectOpen: SwitchCompat = root.findViewById(R.id.autoCollectOpen)
        val autoCollectInterval: AppCompatTextView = root.findViewById(R.id.autoCollectInterval)
        val autoCollectIntervalOpen: SwitchCompat = root.findViewById(R.id.autoCollectIntervalOpen)
        val autoCollectWhitelist: SwitchCompat = root.findViewById(R.id.autoCollectWhitelist)
        val autoFeed: SwitchCompat = root.findViewById(R.id.autoFeed)

        context?.apply {
            val lifecycleOwner = this@AlipayFragment
            val ctx = this
            alipayViewModel.autoCollectOpen.observe(lifecycleOwner, Observer {
                autoCollectOpen.isChecked = it
            })

            autoCollectOpen.setOnCheckedChangeListener { _, isChecked ->
                alipayViewModel.autoCollect(ctx, isChecked)
            }

            alipayViewModel.interval.observe(lifecycleOwner, Observer {
                autoCollectInterval.text = "循环时间间隔: ${it}s"
            })

            alipayViewModel.autoCollectWhitelist.observe(lifecycleOwner, Observer {
                autoCollectWhitelist.isChecked = it
            })

            autoCollectWhitelist.setOnCheckedChangeListener { _, isChecked ->
                alipayViewModel.autoCollectWhitelist(ctx, isChecked)
            }

            alipayViewModel.autoFeed.observe(lifecycleOwner, Observer {
                autoFeed.isChecked = it
            })

            autoFeed.setOnCheckedChangeListener { _, isChecked ->
                alipayViewModel.autoFeed(ctx, isChecked)
            }

            alipayViewModel.autoCollectIntervalOpen.observe(lifecycleOwner, Observer {
                autoCollectIntervalOpen.isChecked = it
            })

            autoCollectIntervalOpen.setOnCheckedChangeListener { _, isChecked ->
                alipayViewModel.intervalCollect(ctx, isChecked)
            }

            plus5.setOnClickListener {
                alipayViewModel.plus5Seconds(ctx)

            }
            minus5.setOnClickListener {
                alipayViewModel.minus5Seconds(ctx)
            }
            alipayViewModel.setup(ctx)
        }
        return root
    }

}