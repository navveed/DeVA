package com.deva.voice.triggers.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.Filter
import android.widget.Filterable
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.deva.voice.R
import java.util.Locale

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable? = null
)

class AppAdapter(
    private var apps: List<AppInfo>,
    private val onSelectionChanged: (List<AppInfo>) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>(), Filterable {

    private var filteredApps: List<AppInfo> = apps
    private val selectedApps = mutableSetOf<AppInfo>()

    fun updateApps(newApps: List<AppInfo>) {
        this.apps = newApps
        this.filteredApps = newApps
        notifyDataSetChanged()
    }

    fun setSelectedApps(apps: List<AppInfo>) {
        selectedApps.clear()
        selectedApps.addAll(apps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.bind(app, selectedApps.contains(app))
    }

    override fun getItemCount(): Int = filteredApps.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val charString = constraint?.toString()?.lowercase(Locale.getDefault()) ?: ""
                val filteredList = if (charString.isEmpty()) {
                    apps
                } else {
                    apps.filter {
                        it.appName.lowercase(Locale.getDefault()).contains(charString)
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = filteredList
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredApps = results?.values as? List<AppInfo> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val appIconImageView: ImageView = itemView.findViewById(R.id.appIconImageView)
        private val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
        private val appCheckBox: CheckBox = itemView.findViewById(R.id.appCheckBox)

        fun bind(app: AppInfo, isSelected: Boolean) {
            appIconImageView.setImageDrawable(app.icon)
            appNameTextView.text = app.appName
            appCheckBox.isChecked = isSelected

            itemView.setOnClickListener {
                if (selectedApps.contains(app)) {
                    selectedApps.remove(app)
                } else {
                    selectedApps.add(app)
                }
                notifyItemChanged(bindingAdapterPosition)
                onSelectionChanged(selectedApps.toList())
            }
        }
    }
}




