package yangFenTuoZi.runner.plus.ui.fragment.home

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.base.BaseFragment
import yangFenTuoZi.runner.plus.databinding.FragmentHomeBinding
import yangFenTuoZi.runner.plus.ui.activity.InstallTermExtActivity

class HomeFragment : BaseFragment() {
    var binding: FragmentHomeBinding? = null
        private set
    private var recyclerView: BorderRecyclerView? = null
    private val adapter = HomeAdapter(this)

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

    fun installTermExt() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/zip"
        }
        pickFileLauncher.launch(Intent.createChooser(intent, getString(R.string.pick_term_ext)))
    }

    private var pickFileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.data?.let { uri ->
                    val mimeType = mContext.contentResolver.getType(uri)
                    if (mimeType != "application/zip") {
                        return@let
                    }

                    val installIntent = Intent(mContext, InstallTermExtActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                        setDataAndType(uri, "application/zip")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }

                    startActivity(installIntent)
                }
            }
        }
}