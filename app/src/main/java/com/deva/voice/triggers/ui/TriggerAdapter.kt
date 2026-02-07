package com.deva.voice.triggers.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.deva.voice.R
import com.deva.voice.triggers.Trigger
import com.deva.voice.triggers.TriggerType
import java.util.Locale

class TriggerAdapter(
    private val triggers: MutableList<Trigger>,
    private val onCheckedChange: (Trigger, Boolean) -> Unit,
    private val onDeleteClick: (Trigger) -> Unit,
    private val onEditClick: (Trigger) -> Unit
) : RecyclerView.Adapter<TriggerAdapter.TriggerViewHolder>() {

    private var interactionsEnabled: Boolean = true

    fun setInteractionsEnabled(enabled: Boolean) {
        this.interactionsEnabled = enabled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TriggerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_trigger, parent, false)
        return TriggerViewHolder(view)
    }

    override fun onBindViewHolder(holder: TriggerViewHolder, position: Int) {
        val trigger = triggers[position]
        holder.bind(trigger)
    }

    override fun getItemCount(): Int = triggers.size

    fun updateTriggers(newTriggers: List<Trigger>) {
        triggers.clear()
        triggers.addAll(newTriggers)
        notifyDataSetChanged()
    }

    inner class TriggerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val instructionTextView: TextView = itemView.findViewById(R.id.triggerInstructionTextView)
        private val timeTextView: TextView = itemView.findViewById(R.id.triggerTimeTextView)
        private val enabledSwitch: SwitchCompat = itemView.findViewById(R.id.triggerEnabledSwitch)
        private val deleteButton: android.widget.ImageButton = itemView.findViewById(R.id.deleteTriggerButton)
        private val editButton: android.widget.ImageButton = itemView.findViewById(R.id.editTriggerButton)

        fun bind(trigger: Trigger) {
            instructionTextView.text = trigger.instruction

            deleteButton.setOnClickListener {
                onDeleteClick(trigger)
            }

            editButton.setOnClickListener {
                onEditClick(trigger)
            }

            when (trigger.type) {
                TriggerType.SCHEDULED_TIME -> {
                    timeTextView.text = String.format(
                        Locale.getDefault(),
                        "At %02d:%02d",
                        trigger.hour ?: 0,
                        trigger.minute ?: 0
                    )
                }
                TriggerType.NOTIFICATION -> {
                    timeTextView.text = "On notification from ${trigger.appName}"
                }

                TriggerType.CHARGING_STATE -> {
                    timeTextView.text = "battery state"
                }
            }

            enabledSwitch.setOnCheckedChangeListener(null) // Avoid triggering listener during bind
            enabledSwitch.isChecked = trigger.isEnabled
            enabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                onCheckedChange(trigger, isChecked)
            }

            enabledSwitch.isEnabled = interactionsEnabled
            deleteButton.isEnabled = interactionsEnabled
            editButton.isEnabled = interactionsEnabled
        }
    }
}




