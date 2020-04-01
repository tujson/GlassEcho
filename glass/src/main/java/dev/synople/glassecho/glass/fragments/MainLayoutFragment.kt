package dev.synople.glassecho.glass.fragments

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import dev.synople.glassecho.glass.R

/*
* Copyright 2019 Google LLC
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

/**
 * Fragment with the main card layout.
 */
class MainLayoutFragment : BaseFragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View = inflater.inflate(R.layout.main_layout, container, false)
        if (arguments != null) {
            val textView = TextView(context)
            textView.text = arguments!!.getString(
                TEXT_KEY,
                getString(R.string.empty_string)
            )
            textView.textSize = BODY_TEXT_SIZE.toFloat()
            textView.typeface = Typeface.create(getString(R.string.thin_font), Typeface.NORMAL)
            val bodyLayout = view.findViewById<FrameLayout>(R.id.body_layout)
            bodyLayout.addView(textView)
            val footer = view.findViewById<TextView>(R.id.footer)
            footer.text = arguments!!.getString(
                FOOTER_KEY,
                getString(R.string.empty_string)
            )
            val timestamp = view.findViewById<TextView>(R.id.timestamp)
            timestamp.text = arguments!!.getString(
                TIMESTAMP_KEY,
                getString(R.string.empty_string)
            )
        }
        return view
    }

    companion object {
        private const val TEXT_KEY = "text_key"
        private const val FOOTER_KEY = "footer_key"
        private const val TIMESTAMP_KEY = "timestamp_key"
        private const val BODY_TEXT_SIZE = 40

        /**
         * Returns new instance of [MainLayoutFragment].
         *
         * @param text is a String with the card main text.
         * @param footer is a String with the card footer text.
         * @param timestamp is a String with the card timestamp text.
         */
        fun newInstance(
            text: String?, footer: String?, timestamp: String?,
            menu: Int?
        ): MainLayoutFragment {
            val myFragment = MainLayoutFragment()
            val args = Bundle()
            args.putString(TEXT_KEY, text)
            args.putString(FOOTER_KEY, footer)
            args.putString(TIMESTAMP_KEY, timestamp)
            if (menu != null) {
                args.putInt(MENU_KEY, menu)
            }
            myFragment.arguments = args
            return myFragment
        }
    }
}