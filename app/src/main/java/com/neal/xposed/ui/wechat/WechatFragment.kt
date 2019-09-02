package com.neal.xposed.ui.wechat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.neal.xposed.R

class WechatFragment : Fragment() {

    private lateinit var wechatViewModel: WechatViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        wechatViewModel =
            ViewModelProviders.of(this).get(WechatViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_wechat, container, false)
        val textView: TextView = root.findViewById(R.id.text_home)
        wechatViewModel.text.observe(this, Observer {
            textView.text = it
        })
        return root
    }
}