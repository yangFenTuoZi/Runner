package yangfentuozi.runner.ui.fragment.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import rikka.recyclerview.BaseViewHolder
import yangfentuozi.runner.Runner
import yangfentuozi.runner.databinding.HomeItemContainerBinding
import yangfentuozi.runner.databinding.HomeShizukuPermRequestBinding

class GrantShizukuPermViewHolder(binding: HomeShizukuPermRequestBinding, root: View) :
    BaseViewHolder<Any?>(root) {
    init {
        binding.button1.setOnClickListener { v ->
            Runner.requestPermission()
        }
    }

    companion object {
        val CREATOR: Creator<Any?> = Creator { inflater: LayoutInflater?, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(
                inflater!!, parent, false
            )
            val inner = HomeShizukuPermRequestBinding.inflate(inflater, outer.getRoot(), true)
            GrantShizukuPermViewHolder(inner, outer.getRoot())
        }
    }
}