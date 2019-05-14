package com.fuxy.cookeasy.activity

import android.content.DialogInterface
import android.database.sqlite.SQLiteConstraintException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.dialogfragment.EditRecordDialogFragment
import com.fuxy.cookeasy.dialogfragment.EditRecordDialogFragmentConstants
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.adapter.EditDictionaryAdapter
import com.fuxy.cookeasy.adapter.RecordList
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.entity.Unit
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

const val UPDATE_UNIT_DIALOG_TAG = "update_unit_dialog"
const val ADD_UNIT_DIALOG_TAG = "add_unit_dialog"

class EditUnitActivity : AppCompatActivity() {
    private var addUnitDialog: AddDialogFragment? = null
    private var updateUnitDialog: UpdateUnitDialog? = null
    private var unitsRecyclerView: RecyclerView? = null
    private var currentUnitId: Int = -1
    private var adapter: EditDictionaryAdapter? = null

    class AddDialogFragment(private val adapter: EditDictionaryAdapter, var searchView: SearchView? = null)
        : EditRecordDialogFragment() {
        override fun recordAction(recordId: Int, value: String) {
            var id: Int = -1
            GlobalScope.launch(IO) {
                val unitDao = AppDatabase.getInstance(context!!)?.unitDao()
                if (unitDao != null) {
                    id = unitDao.insert(Unit(unit = value))[0].toInt()
                }
            }.invokeOnCompletion {
                if (id > 0) {
                    GlobalScope.launch(Main) {
                        adapter.notifyItemInserted(Pair(id, value))
                        searchView?.setQuery("", false)
                    }
                }
            }
        }

    }

    class UpdateUnitDialog(private val adapter: EditDictionaryAdapter)
        : EditRecordDialogFragment() {
        override fun recordAction(recordId: Int, value: String) {
            val unit = Unit(
                id = recordId,
                unit = value
            )
            var count = 0
            GlobalScope.launch(IO) {
                val unitDao = AppDatabase.getInstance(context!!)?.unitDao()
                count = unitDao?.update(unit) ?: 0
            }.invokeOnCompletion {
                if (count > 0) {
                    GlobalScope.launch(Main) { adapter.notifyItemChanged(Pair(recordId, value)) }
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_unit)
        title = resources.getString(R.string.edit_unit_activity_title)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val deleteErrorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(R.string.delete_unit_error_message)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            .create()

        val deleteDialog = AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.unit_delete_confirmation)
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                if (currentUnitId >= 0) {
                    GlobalScope.launch(IO) {
                        try {
                            val unitDao = AppDatabase.getInstance(this@EditUnitActivity)?.unitDao()
                            unitDao?.deleteById(currentUnitId)
                            GlobalScope.launch(Main) { adapter?.notifyRecordRemoved(currentUnitId) }
                        } catch (e: SQLiteConstraintException) {
                            GlobalScope.launch(Main) {
                                cancel()
                                deleteErrorDialog.show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.no) { _: DialogInterface, _: Int -> }
            .create()

        unitsRecyclerView = findViewById(R.id.rv_units)
        unitsRecyclerView?.layoutManager = LinearLayoutManager(this)
        unitsRecyclerView?.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))


        GlobalScope.launch(IO) {
            val unitDao = AppDatabase.getInstance(this@EditUnitActivity)?.unitDao()
            if (unitDao != null) {
                val units = RecordList(unitDao.getAll().map { Pair(it.id ?: 0, it.unit) }.toMutableList())

                GlobalScope.launch(Main) {
                    adapter = object : EditDictionaryAdapter(units) {
                        override fun deleteRecord(id: Int) {
                            currentUnitId = id
                            deleteDialog.show()
                        }

                        override fun updateRecord(id: Int) {
                            updateUnitDialog?.recordId = id
                            updateUnitDialog?.recordValue = units.getById(id).second
                            showDialogFragment(updateUnitDialog!!, UPDATE_UNIT_DIALOG_TAG)
                        }
                    }
                    unitsRecyclerView?.adapter = adapter

                    addUnitDialog = AddDialogFragment(adapter!!)
                    val addUnitDialogArgs = Bundle()
                    addUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_TITLE,
                        resources.getString(R.string.add))
                    addUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_HINT,
                        resources.getString(R.string.enter_unit))
                    addUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_ERROR,
                        resources.getString(R.string.enter_unit_error))
                    addUnitDialog?.arguments = addUnitDialogArgs

                    updateUnitDialog = UpdateUnitDialog(adapter!!)
                    val updateUnitDialogArgs = Bundle()
                    updateUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_TITLE,
                        resources.getString(R.string.edit))
                    updateUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_HINT,
                        resources.getString(R.string.enter_unit))
                    updateUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_ERROR,
                        resources.getString(R.string.enter_unit_error))
                    updateUnitDialog?.arguments = updateUnitDialogArgs
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_unit, menu)
        val searchView = menu?.findItem(R.id.edit_unit_search)?.actionView as SearchView
        addUnitDialog?.searchView = searchView
        searchView.queryHint = resources.getString(R.string.edit_unit_search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter?.filter?.filter(newText)
                return true
            }

        })
        return true
    }

    private fun showDialogFragment(dialog: DialogFragment, dialogTag: String) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag(dialogTag)

        if (prev != null) {
            fragmentTransaction.remove(prev)
        }

        fragmentTransaction.addToBackStack(dialogTag)
        dialog.show(fragmentTransaction, dialogTag)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.edit_unit_add -> {
                showDialogFragment(addUnitDialog!!, ADD_UNIT_DIALOG_TAG)
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
