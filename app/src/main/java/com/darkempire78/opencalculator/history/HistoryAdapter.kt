package com.darkempire78.opencalculator.history

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.os.Build
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.darkempire78.opencalculator.MyPreferences
import com.darkempire78.opencalculator.R
import com.darkempire78.opencalculator.util.CalculatorMode

class HistoryAdapter(
    initialHistory: MutableList<History>,
    private val onElementClick: (value: String) -> Unit,
    private val context: Context,
    private val onDetailRequested: (History) -> Unit = {}
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        /** Every stored entry; [history] is the subset shown for the active mode filter. */
        private var allHistory: MutableList<History> = initialHistory
        private var filter: CalculatorMode? = null
        private var history: MutableList<History> = visibleOf(allHistory)

        private fun visibleOf(list: List<History>): MutableList<History> =
            list.filter { filter == null || it.origin == filter }.toMutableList()

        /** Shows only entries of [mode], or everything when null. */
        @android.annotation.SuppressLint("NotifyDataSetChanged")
        fun setFilter(mode: CalculatorMode?) {
            filter = mode
            history = visibleOf(allHistory)
            notifyDataSetChanged()
        }

        fun currentFilter(): CalculatorMode? = filter

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater
                .from(parent.context)
                .inflate(R.layout.history_item, parent, false)
            return HistoryViewHolder(view)
        }

        override fun getItemCount(): Int = history.size

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            holder.bind(history[position], position)
        }

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        fun appendHistory(historyList: List<History>) {
            allHistory.addAll(historyList)
            this.history = visibleOf(allHistory)
            notifyDataSetChanged()
        }

        fun appendOneHistoryElement(history: History) {
            allHistory.add(history)
            if (filter != null && history.origin != filter) return
            this.history.add(history)
            // Update the last 2 elements to avoid to have the same date and bar separator
            if (this.history.size > 1) {
                notifyItemInserted(this.history.size - 1)
                notifyItemRangeChanged(this.history.size - 2, 2)
            } else {
                notifyItemInserted(this.history.size - 1)
            }
        }

        /** Removes the entry shown at [position] and returns its id (null if the position is invalid). */
        fun removeHistoryElement(position: Int): String? {
            if (position !in history.indices) return null
            // No idea why, but time.isNotEmpty() is not working, only time.isNullOrEmpty() works
            val historyElement = history[position]
            if (!historyElement.time.isNullOrEmpty()){
                val nextHistoryElement = history.getOrNull(position + 1)
                nextHistoryElement?.let {
                    if (it.time.isNullOrEmpty()){
                        this.history[position + 1] = History(
                            calculation = nextHistoryElement.calculation,
                            result = nextHistoryElement.result,
                            time = historyElement.time,
                            id = nextHistoryElement.id,
                            mode = nextHistoryElement.mode,
                            type = nextHistoryElement.type,
                            details = nextHistoryElement.details
                        )
                    }
                }
            }
            this.history.removeAt(position)
            allHistory.removeAll { it.id == historyElement.id }
            notifyItemRemoved(position)
            return historyElement.id
        }

        /** Removes an entry by id wherever it is (used by the detail dialog). */
        fun removeHistoryElementById(id: String) {
            val position = history.indexOfFirst { it.id == id }
            if (position >= 0) removeHistoryElement(position) else allHistory.removeAll { it.id == id }
        }

        fun updateHistoryList() {
            allHistory = MyPreferences(context).getHistory()
            history = visibleOf(allHistory)
        }
        fun updateHistoryElement(historyElement: History) {
            updateHistoryList()
            val position = this.history.indexOfFirst { it.id == historyElement.id }
            if (position != -1) {
                this.history[position] = historyElement
                notifyItemChanged(position)
            }
        }

        fun removeFirstHistoryElement() {
            if (allHistory.isEmpty()) return
            val oldest = allHistory.removeAt(0)
            val position = history.indexOfFirst { it.id == oldest.id }
            if (position >= 0) {
                history.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        @android.annotation.SuppressLint("NotifyDataSetChanged")
        fun clearHistory() {
            allHistory.clear()
            this.history.clear()
            notifyDataSetChanged()
        }

        inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val calculation: TextView = itemView.findViewById(R.id.history_item_calculation)
            private val result: TextView = itemView.findViewById(R.id.history_item_result)
            private val time: TextView = itemView.findViewById(R.id.history_time)
            private val typeLabel: TextView = itemView.findViewById(R.id.history_item_type)
            // text size chosen by the screen-size specific layout; financial entries use half of it
            private val layoutCalculationSize: Float = calculation.textSize
            private val separator: View = itemView.findViewById(R.id.history_separator)
            private val sameDateSeparator: View = itemView.findViewById(R.id.history_same_date_separator)

            private fun wrapInParenthesis(string: String): String {
                // Verify it is not already in parenthesis
                return if (string.first() != '(' || string.last() != ')') {
                    "($string)"
                } else {
                    string
                }
            }

            fun bind(historyElement: History, position : Int) {
                // Set calculation, result and time
                calculation.text = historyElement.calculation
                result.text = historyElement.result
                // Financial entries are longer sentences: smaller text than the 35sp used for expressions
                calculation.setTextSize(
                    android.util.TypedValue.COMPLEX_UNIT_PX,
                    if (historyElement.origin == CalculatorMode.FINANCIAL_RPN) layoutCalculationSize * 0.5f else layoutCalculationSize
                )
                typeLabel.text = typeDescription(context, historyElement)
                typeLabel.contentDescription = context.getString(R.string.hist_open_detail, typeDescription(context, historyElement))
                typeLabel.setOnClickListener { onDetailRequested(historyElement) }
                // To avoid crashes with former histories that do not have stored dates
                if (historyElement.time.isNullOrEmpty()) {
                    time.visibility = View.GONE
                } else {
                    time.text = DateUtils.getRelativeTimeSpanString(
                        historyElement.time.toLong(),
                        System.currentTimeMillis(),
                        DateUtils.DAY_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE,
                    )
                    // Check if the former result has the same date -> hide the date
                    if (position > 0) {
                        if (
                            !history[position-1].time.isNullOrEmpty()
                            && DateUtils.getRelativeTimeSpanString(
                                history[position-1].time.toLong(),
                                System.currentTimeMillis(),
                                DateUtils.DAY_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE,
                            ) == time.text)
                        {
                            time.visibility = View.GONE
                        } else {
                            time.visibility = View.VISIBLE
                        }
                    } else {
                        time.visibility = View.VISIBLE
                    }
                    // Check if the next result has the same date -> hide the separator
                    if (position+1 < history.size) {
                        if (
                            DateUtils.getRelativeTimeSpanString(
                                history[position+1].time.toLong(),
                                System.currentTimeMillis(),
                                DateUtils.DAY_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_RELATIVE,
                            ) == time.text)
                        {
                            separator.visibility = View.GONE
                            // Add more space when it's the same date than the next history element
                            sameDateSeparator.visibility = View.VISIBLE
                        } else {
                            separator.visibility = View.VISIBLE
                            sameDateSeparator.visibility = View.GONE
                        }
                    } else {
                        separator.visibility = View.VISIBLE
                        sameDateSeparator.visibility = View.GONE
                    }
                }

                // On click
                calculation.setOnClickListener {
                    val formattedCalculation = wrapInParenthesis(historyElement.calculation)
                    onElementClick.invoke(formattedCalculation)
                }
                result.setOnClickListener {
                    val formattedResult = wrapInParenthesis(historyElement.result)
                    onElementClick.invoke(formattedResult)
                }

                calculation.setOnLongClickListener {
                    if (MyPreferences(itemView.context).longClickToCopyValue) {
                        val clipboardManager = itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(R.string.copied_history_calculation.toString(), historyElement.calculation))
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                            Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT).show()
                        true // Or false if not consumed}
                    } else {
                        false
                    }

                }
                result.setOnLongClickListener {
                    if (MyPreferences(itemView.context).longClickToCopyValue) {
                        val clipboardManager = itemView.context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        clipboardManager.setPrimaryClip(ClipData.newPlainText(R.string.copied_history_result.toString(), historyElement.result))
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                            Toast.makeText(itemView.context, R.string.value_copied, Toast.LENGTH_SHORT).show()
                        true // Or false if not consumed
                    } else {
                        false
                    }
                }
            }
        }
    }

fun historyTypeLabel(context: Context, type: HistoryType): String = context.getString(
    when (type) {
        HistoryType.BASIC -> R.string.mode_basic_short
        HistoryType.SCIENTIFIC -> R.string.mode_scientific_short
        HistoryType.RPN_OPERATION -> R.string.hist_type_rpn
        HistoryType.TVM -> R.string.hist_type_tvm
        HistoryType.NPV -> R.string.hist_type_npv
        HistoryType.IRR -> R.string.hist_type_irr
        HistoryType.AMORTIZATION -> R.string.hist_type_amortization
        HistoryType.RATE_CONVERSION -> R.string.hist_type_rate_conversion
    }
)

/** Type plus its relevant settings (e.g. "TVM - end of period"), shown under each entry. */
fun typeDescription(context: Context, entry: History): String {
    val label = historyTypeLabel(context, entry.entryType)
    return if (entry.details.isNullOrBlank()) label else "$label \u00B7 ${entry.details}"
}
