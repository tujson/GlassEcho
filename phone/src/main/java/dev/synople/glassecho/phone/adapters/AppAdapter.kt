package dev.synople.glassecho.phone.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.databinding.RowAppBinding
import dev.synople.glassecho.phone.models.EchoApp

class AppAdapter(
    private val echoApps: List<EchoApp>,
    private val onAppChange: (EchoApp, Int) -> Unit,
) : RecyclerView.Adapter<AppAdapter.ViewHolder>() {
    class ViewHolder(
        private val rowAppBinding: RowAppBinding
    ) : RecyclerView.ViewHolder(rowAppBinding.root) {

        fun bindApp(
            echoApp: EchoApp,
            position: Int,
            onAppChange: (EchoApp, Int) -> Unit,
        ) {
            val packageManager = rowAppBinding.root.context.packageManager
            val appIcon = packageManager.getApplicationIcon(echoApp.packageName)
            val appName = packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(
                    echoApp.packageName,
                    0
                )
            )

            rowAppBinding.apply {
                ivAppIcon.setImageDrawable(appIcon)
                tvAppName.text = appName

                switchApp.setOnCheckedChangeListener(null)
                switchApp.isChecked = echoApp.isNotify
                switchApp.setOnCheckedChangeListener { _, isChecked ->
                    onAppChange(echoApp.copy(isNotify = isChecked), position)
                    switchWakeScreen.isEnabled = isChecked
                }

                switchWakeScreen.setOnCheckedChangeListener(null)
                switchWakeScreen.isEnabled = echoApp.isNotify
                switchWakeScreen.isChecked = echoApp.isWakeScreen
                switchWakeScreen.setOnCheckedChangeListener { _, isChecked ->
                    onAppChange(echoApp.copy(isWakeScreen = isChecked), position)
                }

                ivExpandOptions.setOnClickListener {
                    switchWakeScreen.visibility = if (switchWakeScreen.visibility == View.VISIBLE) {
                        ivExpandOptions.setImageDrawable(
                            ContextCompat.getDrawable(
                                ivExpandOptions.context,
                                R.drawable.ic_arrow_down
                            )
                        )
                        View.GONE
                    } else {
                        ivExpandOptions.setImageDrawable(
                            ContextCompat.getDrawable(
                                ivExpandOptions.context,
                                R.drawable.ic_arrow_up
                            )
                        )
                        View.VISIBLE
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(RowAppBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindApp(
            echoApps[position],
            position,
            onAppChange,
        )
    }

    override fun getItemCount() = echoApps.size
}