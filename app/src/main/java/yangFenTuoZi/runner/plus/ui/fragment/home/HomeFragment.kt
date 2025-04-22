package yangFenTuoZi.runner.plus.ui.fragment.home

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.widget.borderview.BorderRecyclerView
import yangFenTuoZi.runner.plus.R
import yangFenTuoZi.runner.plus.Runner
import yangFenTuoZi.runner.plus.adapters.CmdAdapter
import yangFenTuoZi.runner.plus.base.BaseFragment
import yangFenTuoZi.runner.plus.databinding.DialogEditBinding
import yangFenTuoZi.runner.plus.databinding.FragmentHomeBinding
import yangFenTuoZi.runner.plus.service.CommandInfo
import java.lang.String
import kotlin.Int
import kotlin.toString

class HomeFragment : BaseFragment() {
    var binding: FragmentHomeBinding? = null
        private set
    private var recyclerView: RecyclerView? = null
    private val statusAdapter = HomeAdapter()
    private val adapter: CmdAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View? = binding!!.getRoot()
        recyclerView = binding!!.recyclerView
        recyclerView!!.setLayoutManager(GridLayoutManager(mContext, 2))
        recyclerView!!.fixEdgeEffect(false, true)
        binding!!.swipeRefreshLayout.setOnRefreshListener {
            Handler().postDelayed(Runnable {
                initList()
                binding!!.swipeRefreshLayout.isRefreshing = false
            }, 1000)
        }

        binding!!.add.setOnClickListener { view ->
            if (mContext.isDialogShow) return@setOnClickListener
            val binding = DialogEditBinding.inflate(LayoutInflater.from(mContext))

            binding.dialogUidGid.visibility = View.GONE
            binding.dialogChid.setOnCheckedChangeListener { buttonView, isChecked ->
                binding.dialogUidGid.visibility = if (isChecked) View.VISIBLE else View.GONE
            }

            binding.dialogName.requestFocus()
            binding.dialogName.postDelayed({
                (mContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(
                    binding.dialogName,
                    0
                )
            }, 200)
            mContext.isDialogShow = true
            MaterialAlertDialogBuilder(mContext).setTitle(mContext.getString(R.string.dialog_edit))
                .setView(binding.getRoot()).setPositiveButton(
                    mContext.getString(R.string.dialog_finish),
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        if (!Runner.pingServer()) {
                            Toast.makeText(
                                mContext,
                                R.string.home_status_service_not_running,
                                Toast.LENGTH_SHORT
                            ).show()
                            true
                        }
                        val info = CommandInfo()
                        info.command = String.valueOf(binding.dialogCommand.getText())
                        info.name = String.valueOf(binding.dialogName.getText())
                        info.keepAlive = binding.dialogKeepItAlive.isChecked
                        info.useChid = binding.dialogChid.isChecked
                        info.ids =
                            if (binding.dialogChid.isChecked) binding.dialogIds.getText()
                                .toString() else null
                        adapter!!.add(info)
                    })
                .setOnDismissListener(DialogInterface.OnDismissListener { dialog: DialogInterface? ->
                    mContext.isDialogShow = false
                }).show()
        }


        val recyclerView: BorderRecyclerView = binding!!.list
        recyclerView.adapter = statusAdapter
        recyclerView.fixEdgeEffect(true, true)
        recyclerView.addItemSpacing(0f, 4f, 0f, 4f, TypedValue.COMPLEX_UNIT_DIP)
        recyclerView.addEdgeSpacing(16f, 4f, 16f, 4f, TypedValue.COMPLEX_UNIT_DIP)

        return root
    }

    //初始化列表
    fun initList() {
//        if (!Runner.pingServer()) {
//            Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
//            return;
//        }
//        try {
//            Runner.iService.closeCursor();
//            int count = iService.count();
//            if (adapter == null) {
//                adapter = new CmdAdapter(mContext, count);
//                binding.recyclerView.setAdapter(adapter);
//            }
//            else adapter.updateData(count);
//        } catch (Exception e) {
//            throwableToDialog(mContext, e);
//        }
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
        statusAdapter.updateData()
    } //菜单选择事件
    //    @SuppressLint("WrongConstant")
    //    @Override
    //    public boolean onContextItemSelected(MenuItem item) {
    //        switch (item.getItemId()) {
    //            case CmdAdapter.long_click_copy_name:
    //                if (!Runner.pingServer()) {
    //                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
    //                    return super.onContextItemSelected(item);
    //                }
    //                try {
    //                    String name = iService.query(item.getGroupId()).name;
    //                    ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", name));
    //                    Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + name, Toast.LENGTH_SHORT).show();
    //                } catch (RemoteException e) {
    //                    throwableToDialog(mContext, e);
    //                }
    //                return true;
    //            case CmdAdapter.long_click_copy_command:
    //                if (!Runner.pingServer()) {
    //                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
    //                    return super.onContextItemSelected(item);
    //                }
    //                try {
    //                    String command = iService.query(item.getGroupId()).command;
    //                    ((ClipboardManager) mContext.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("c", command));
    //                    Toast.makeText(mContext, getString(R.string.home_copy_command) + "\n" + command, Toast.LENGTH_SHORT).show();
    //                } catch (RemoteException e) {
    //                    throwableToDialog(mContext, e);
    //                }
    //                return true;
    //            case CmdAdapter.long_click_new:
    //
    //                return true;
    //            case CmdAdapter.long_click_pack:
    //                Intent intent = new Intent(getContext(), PackActivity.class);
    //                intent.putExtra("id", item.getGroupId());
    //                startActivity(intent);
    //                return true;
    //            case CmdAdapter.long_click_del:
    //                if (!App.pingServer()) {
    //                    Toast.makeText(mContext, R.string.home_service_is_not_running, Toast.LENGTH_SHORT).show();
    //                    return super.onContextItemSelected(item);
    //                }
    //                adapter.remove(item.getGroupId());
    //                return true;
    //            default:
    //                return super.onContextItemSelected(item);
    //        }
    //    }
}