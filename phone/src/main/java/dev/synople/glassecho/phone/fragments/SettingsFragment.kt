package dev.synople.glassecho.phone.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.View
import android.widget.Switch
import dev.synople.glassecho.phone.Constants
import dev.synople.glassecho.phone.R
import dev.synople.glassecho.phone.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment(R.layout.fragment_settings) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        FragmentSettingsBinding.bind(view)
        view.findViewById<Switch>(R.id.shouldWakeDevice).isChecked = Constants.IS_WAKE_SCREEN_DEFAULT
    }

    fun toggleWakeGlass(view: View) {
        Constants.IS_WAKE_SCREEN_DEFAULT = !Constants.IS_WAKE_SCREEN_DEFAULT
    }
}