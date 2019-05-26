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
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.adapter.EditDictionaryAdapter
import com.fuxy.cookeasy.adapter.RecordList
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.dialogfragment.EditRecordDialogFragment
import com.fuxy.cookeasy.dialogfragment.EditRecordDialogFragmentConstants
import com.fuxy.cookeasy.entity.DishType
import com.fuxy.cookeasy.entity.Ingredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

const val UPDATE_DISH_TYPE_DIALOG_TAG = "update_dish_type_dialog"
const val ADD_DISH_TYPE_DIALOG_TAG = "add_dish_type_dialog"

class EditDishTypeActivity : AppCompatActivity() {
    private var addDishTypeDialog: AddDialogFragment? = null
    private var updateDishTypeDialog: UpdateDialogFragment? = null
    private var dishTypesRecyclerView: RecyclerView? = null
    private var currentDishTypeId: Int = -1
    private var adapter: EditDictionaryAdapter? = null

    class AddDialogFragment(private val adapter: EditDictionaryAdapter, var searchView: SearchView? = null)
        : EditRecordDialogFragment() {
        override fun recordAction(recordId: Int, value: String) {
            var id: Int = -1
            GlobalScope.launch(Dispatchers.IO) {
                val dishTypeDao = AppDatabase.getInstance(context!!)?.dishTypeDao()
                if (dishTypeDao != null) {
                    id = dishTypeDao.insert(DishType(dishType = value))[0].toInt()
                }
            }.invokeOnCompletion {
                if (id > 0) {
                    GlobalScope.launch(Dispatchers.Main) {
                        adapter.notifyItemInserted(Pair(id, value))
                        searchView?.setQuery("", false)
                    }
                }
            }
        }

    }

    class UpdateDialogFragment(private val adapter: EditDictionaryAdapter)
        : EditRecordDialogFragment() {
        override fun recordAction(recordId: Int, value: String) {
            val dishType = DishType(
                id = recordId,
                dishType = value
            )
            var count = 0
            GlobalScope.launch(Dispatchers.IO) {
                val dishTypeDao = AppDatabase.getInstance(context!!)?.dishTypeDao()
                count = dishTypeDao?.update(dishType) ?: 0
            }.invokeOnCompletion {
                if (count > 0) {
                    GlobalScope.launch(Dispatchers.Main) { adapter.notifyItemChanged(Pair(recordId, value)) }
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_dish_type)
        title = resources.getString(R.string.dish_types)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val deleteErrorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(R.string.delete_dish_type_error_message)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            .create()

        val deleteDialog = AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.dish_type_delete_confirmation)
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                if (currentDishTypeId >= 0) {
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val dishTypeDao = AppDatabase
                                .getInstance(this@EditDishTypeActivity)?.dishTypeDao()
                            dishTypeDao?.deleteById(currentDishTypeId)
                            GlobalScope.launch(Dispatchers.Main) { adapter?.notifyRecordRemoved(currentDishTypeId) }
                        } catch (e: SQLiteConstraintException) {
                            GlobalScope.launch(Dispatchers.Main) {
                                cancel()
                                deleteErrorDialog.show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.no) { _: DialogInterface, _: Int -> }
            .create()

        dishTypesRecyclerView = findViewById(R.id.rv_dish_types)
        dishTypesRecyclerView?.layoutManager = LinearLayoutManager(this)
        dishTypesRecyclerView?.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))


        GlobalScope.launch(Dispatchers.IO) {
            val dishTypeDao = AppDatabase.getInstance(this@EditDishTypeActivity)
                ?.dishTypeDao()
            if (dishTypeDao != null) {
                val dishTypes =
                    RecordList(dishTypeDao.getAll().map { Pair(it.id ?: 0, it.dishType) }.toMutableList())

                GlobalScope.launch(Dispatchers.Main) {
                    adapter = object : EditDictionaryAdapter(dishTypes) {
                        override fun deleteRecord(id: Int) {
                            currentDishTypeId = id
                            deleteDialog.show()
                        }

                        override fun updateRecord(id: Int) {
                            updateDishTypeDialog?.recordId = id
                            updateDishTypeDialog?.recordValue = dishTypes.getById(id).second
                            showDialogFragment(updateDishTypeDialog!!, UPDATE_INGREDIENT_DIALOG_TAG)
                        }
                    }
                    dishTypesRecyclerView?.adapter = adapter

                    addDishTypeDialog = AddDialogFragment(adapter!!)
                    val addDishTypeDialogArgs = Bundle()
                    addDishTypeDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_TITLE,
                        resources.getString(R.string.add))
                    addDishTypeDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_HINT,
                        resources.getString(R.string.enter_dish_type))
                    addDishTypeDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_ERROR,
                        resources.getString(R.string.enter_dish_type_error))
                    addDishTypeDialog?.arguments = addDishTypeDialogArgs

                    updateDishTypeDialog = UpdateDialogFragment(adapter!!)
                    val updateDishTypeDialogArgs = Bundle()
                    updateDishTypeDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_TITLE,
                        resources.getString(R.string.edit))
                    updateDishTypeDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_HINT,
                        resources.getString(R.string.enter_dish_type))
                    updateDishTypeDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_ERROR,
                        resources.getString(R.string.enter_dish_type_error))
                    updateDishTypeDialog?.arguments = updateDishTypeDialogArgs
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_unit, menu)
        val searchView = menu?.findItem(R.id.edit_unit_search)?.actionView as SearchView
        addDishTypeDialog?.searchView = searchView
        searchView.queryHint = resources.getString(R.string.enter_dish_type)
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
                showDialogFragment(addDishTypeDialog!!, ADD_INGREDIENT_DIALOG_TAG)
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
