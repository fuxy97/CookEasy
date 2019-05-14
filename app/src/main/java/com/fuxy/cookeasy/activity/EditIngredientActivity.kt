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
import com.fuxy.cookeasy.EditRecordDialogFragment
import com.fuxy.cookeasy.EditRecordDialogFragmentConstants
import com.fuxy.cookeasy.R
import com.fuxy.cookeasy.adapter.EditDictionaryAdapter
import com.fuxy.cookeasy.adapter.RecordList
import com.fuxy.cookeasy.db.AppDatabase
import com.fuxy.cookeasy.entity.Ingredient
import com.fuxy.cookeasy.entity.Unit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

const val UPDATE_INGREDIENT_DIALOG_TAG = "update_ingredient_dialog"
const val ADD_INGREDIENT_DIALOG_TAG = "add_ingredient_dialog"

class EditIngredientActivity : AppCompatActivity() {
    private var addIngredientDialog: AddDialogFragment? = null
    private var updateIngredientDialog: UpdateIngredientDialog? = null
    private var ingredientsRecyclerView: RecyclerView? = null
    private var currentIngredientId: Int = -1
    private var adapter: EditDictionaryAdapter? = null

    class AddDialogFragment(private val adapter: EditDictionaryAdapter, var searchView: SearchView? = null)
        : EditRecordDialogFragment() {
        override fun recordAction(recordId: Int, value: String) {
            var id: Int = -1
            GlobalScope.launch(Dispatchers.IO) {
                val ingredientDao = AppDatabase.getInstance(context!!)?.ingredientDao()
                if (ingredientDao != null) {
                    id = ingredientDao.insert(Ingredient(ingredient = value))[0].toInt()
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

    class UpdateIngredientDialog(private val adapter: EditDictionaryAdapter)
        : EditRecordDialogFragment() {
        override fun recordAction(recordId: Int, value: String) {
            val ingredient = Ingredient(
                id = recordId,
                ingredient = value
            )
            var count = 0
            GlobalScope.launch(Dispatchers.IO) {
                val ingredientDao = AppDatabase.getInstance(context!!)?.ingredientDao()
                count = ingredientDao?.update(ingredient) ?: 0
            }.invokeOnCompletion {
                if (count > 0) {
                    GlobalScope.launch(Dispatchers.Main) { adapter.notifyItemChanged(Pair(recordId, value)) }
                }
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_ingredient)
        title = resources.getString(R.string.ingredients)

        val deleteErrorDialog = AlertDialog.Builder(this)
            .setTitle(R.string.error)
            .setMessage(R.string.delete_ingredient_error_message)
            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int -> }
            .create()

        val deleteDialog = AlertDialog.Builder(this)
            .setTitle(R.string.delete)
            .setMessage(R.string.ingredient_delete_confirmation)
            .setPositiveButton(R.string.yes) { _: DialogInterface, _: Int ->
                if (currentIngredientId >= 0) {
                    GlobalScope.launch(Dispatchers.IO) {
                        try {
                            val ingredientDao = AppDatabase
                                .getInstance(this@EditIngredientActivity)?.ingredientDao()
                            ingredientDao?.deleteById(currentIngredientId)
                            GlobalScope.launch(Dispatchers.Main) { adapter?.notifyRecordRemoved(currentIngredientId) }
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

        ingredientsRecyclerView = findViewById(R.id.rv_ingredients)
        ingredientsRecyclerView?.layoutManager = LinearLayoutManager(this)
        ingredientsRecyclerView?.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))


        GlobalScope.launch(Dispatchers.IO) {
            val ingredientDao = AppDatabase.getInstance(this@EditIngredientActivity)?.ingredientDao()
            if (ingredientDao != null) {
                val ingredients =
                    RecordList(ingredientDao.getAll().map { Pair(it.id ?: 0, it.ingredient) }.toMutableList())

                GlobalScope.launch(Dispatchers.Main) {
                    adapter = object : EditDictionaryAdapter(ingredients) {
                        override fun deleteRecord(id: Int) {
                            currentIngredientId = id
                            deleteDialog.show()
                        }

                        override fun updateRecord(id: Int) {
                            updateIngredientDialog?.recordId = id
                            updateIngredientDialog?.recordValue = ingredients.getById(id).second
                            showDialogFragment(updateIngredientDialog!!, UPDATE_INGREDIENT_DIALOG_TAG)
                        }
                    }
                    ingredientsRecyclerView?.adapter = adapter

                    addIngredientDialog = AddDialogFragment(adapter!!)
                    val addUnitDialogArgs = Bundle()
                    addUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_TITLE,
                        resources.getString(R.string.add))
                    addUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_HINT,
                        resources.getString(R.string.enter_ingredient))
                    addUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_ERROR,
                        resources.getString(R.string.enter_ingredient_error))
                    addIngredientDialog?.arguments = addUnitDialogArgs

                    updateIngredientDialog = UpdateIngredientDialog(adapter!!)
                    val updateUnitDialogArgs = Bundle()
                    updateUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_TITLE,
                        resources.getString(R.string.edit))
                    updateUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_HINT,
                        resources.getString(R.string.enter_ingredient))
                    updateUnitDialogArgs.putString(
                        EditRecordDialogFragmentConstants.ARGUMENT_ERROR,
                        resources.getString(R.string.enter_ingredient_error))
                    updateIngredientDialog?.arguments = updateUnitDialogArgs
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_edit_unit, menu)
        val searchView = menu?.findItem(R.id.edit_unit_search)?.actionView as SearchView
        addIngredientDialog?.searchView = searchView
        searchView.queryHint = resources.getString(R.string.enter_ingredient)
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
                showDialogFragment(addIngredientDialog!!, ADD_INGREDIENT_DIALOG_TAG)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}