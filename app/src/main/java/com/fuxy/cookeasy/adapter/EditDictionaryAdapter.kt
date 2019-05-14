package com.fuxy.cookeasy.adapter

import android.content.Context
import android.view.*
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fuxy.cookeasy.R

class RecordList(private val records: MutableList<Pair<Int, String>>) {
    fun deleteById(id: Int) {
        records.removeAt(records.binarySearch { it.first.compareTo(id) })
    }

    fun removeAt(position: Int) {
        records.removeAt(position)
    }

    fun updateRecord(record: Pair<Int, String>) {
        records[records.binarySearch { it.first.compareTo(record.first) }] = record
    }

    fun getIndexById(id: Int): Int {
        return records.binarySearch { it.first.compareTo(id) }
    }

    fun getById(id: Int): Pair<Int, String> {
        return records[records.binarySearch { it.first.compareTo(id) }]
    }
    
    val size: Int
        get() = records.size

    operator fun get(index: Int): Pair<Int, String> {
        return records[index]
    }

    fun add(record: Pair<Int, String>) {
        records.add(record)
    }

    fun filterByConstraint(constraint: CharSequence): RecordList {
        return RecordList(records.filter { it.second.toLowerCase().contains(constraint) }.toMutableList())
    }
}

abstract class EditDictionaryAdapter(private var records: RecordList)
    : RecyclerView.Adapter<EditDictionaryAdapter.ViewHolder>(), Filterable {
    private val recordsBackup = records

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.record_item, parent, false)
        return ViewHolder(parent.context, view)
    }

    override fun getItemCount(): Int {
        return records.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.textView.text = records[position].second
    }


    inner class ViewHolder(private val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnCreateContextMenuListener {
        val textView: TextView = itemView.findViewById(R.id.tv_record)
        //private val popupMenu: PopupMenu = PopupMenu(context, itemView)

        init {
/*            popupMenu.inflate(R.menu.popupmenu_dictionary_record)
            popupMenu.setOnMenuItemClickListener {
                val position = adapterPosition
                when (it.itemId) {
                    R.id.record_edit -> {

                    }
                    R.id.record_delete -> {

                    }
                    else -> return@setOnMenuItemClickListener false
                }
            }*/

            itemView.setOnCreateContextMenuListener(this)
/*            itemView.setOnLongClickListener {
                popupMenu.show()
                return@setOnLongClickListener true
            }*/
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            MenuInflater(context).inflate(R.menu.popupmenu_dictionary_record, menu)

            menu?.getItem(0)?.setOnMenuItemClickListener {
                updateRecord(records[adapterPosition].first)
                return@setOnMenuItemClickListener true
            }
            menu?.getItem(1)?.setOnMenuItemClickListener {
                deleteRecord(records[adapterPosition].first)
                return@setOnMenuItemClickListener true
            }
        }
    }

    fun notifyItemInserted(record: Pair<Int, String>) {
        if (records != recordsBackup) {
            records = recordsBackup
            records.add(record)
            notifyDataSetChanged()
        } else {
            records.add(record)
            notifyItemInserted(records.size - 1)
        }
    }

    fun notifyItemChanged(record: Pair<Int, String>) {
        if (records != recordsBackup) {
            recordsBackup.updateRecord(record)
        }
        records.updateRecord(record)
        notifyItemChanged(records.getIndexById(record.first))
    }

    fun notifyRecordRemoved(id: Int) {
        if (records != recordsBackup) {
            recordsBackup.deleteById(id)
        }
        val position = records.getIndexById(id)
        records.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filteredResults: RecordList = if (constraint.isNullOrBlank()) {
                    recordsBackup
                } else {
                    recordsBackup.filterByConstraint(constraint)
                }

                val filterResults = FilterResults()
                filterResults.values = filteredResults
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null) {
                    records = results.values as RecordList
                    notifyDataSetChanged()
                }
            }

        }
    }

    abstract fun deleteRecord(id: Int)
    abstract fun updateRecord(id: Int)
}