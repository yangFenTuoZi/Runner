package yangFenTuoZi.runner.plus.ui.fragment.home

import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseFragment
import yangFenTuoZi.runner.plus.databinding.FragmentHomeBinding

class HomeFragment : BaseFragment() {
    var binding: FragmentHomeBinding? = null
        private set
    private var recyclerView: BorderRecyclerView? = null
    private val adapter = HomeAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        recyclerView = binding!!.list.apply {
            adapter = this@HomeFragment.adapter
            fixEdgeEffect(true, true)
            addItemSpacing(0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP)
            addEdgeSpacing(16f, 4f, 16f, 4f, TypedValue.COMPLEX_UNIT_DIP)
        }

        return binding!!.getRoot()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onStart() {
        super.onStart()
        val l = View.OnClickListener { v: View? -> recyclerView!!.smoothScrollToPosition(0) }
        getToolbar().setOnClickListener(l)
        Runner.refreshStatus()
        adapter.updateData()
        adapter.registerListeners()
    }

    override fun onStop() {
        super.onStop()
        adapter.unregisterListeners()
    }
}