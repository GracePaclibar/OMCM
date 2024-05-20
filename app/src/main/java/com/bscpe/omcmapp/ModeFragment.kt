package com.bscpe.omcmapp

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment

class ModeFragment : Fragment(R.layout.fragment_mode) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_mode, container, false)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val modePreviewSwitch = view.findViewById<SwitchCompat>(R.id.modePreview)
        modePreviewSwitch.visibility = View.INVISIBLE

        fetchData(view)
    }

    private fun fetchData(view: View) {
        val modeTextView = view.findViewById<TextView>(R.id.mode_TextView)

        val sharedPrefs = requireActivity().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        val modePreviewSwitch = view.findViewById<SwitchCompat>(R.id.modePreview)

        val autoPreviewState = sharedPrefs.getBoolean("autoSwitchState", true)
        val waterPreviewState = sharedPrefs.getBoolean("waterSwitchState", true)

        if (autoPreviewState == true) {
            modePreviewSwitch.visibility = View.INVISIBLE
            modeTextView.text = "Automatic"
        } else {
            modePreviewSwitch.visibility = View.VISIBLE
            modeTextView.text = "Manual"
            modePreviewSwitch.isChecked = waterPreviewState
        }

        modePreviewSwitch.setOnTouchListener { v, event -> true }
        modePreviewSwitch.setFocusable(false)
        modePreviewSwitch.setClickable(false)
    }
}