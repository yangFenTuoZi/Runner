package yangfentuozi.runner.ui.fragment.terminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import yangfentuozi.runner.base.BaseFragment
import yangfentuozi.runner.databinding.FragmentTerminalBinding

class TerminalFragment : BaseFragment() {
    private var binding: FragmentTerminalBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTerminalBinding.inflate(inflater, container, false)
        val root: View? = binding!!.getRoot()
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }


    override fun onStart() {
        super.onStart()
    }
}