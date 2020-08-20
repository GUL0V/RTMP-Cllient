package com.esona.webcamcloud.ui

import android.app.Dialog
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.esona.webcamcloud.R


class MessageDialog : DialogFragment() {
    private var okListener: View.OnClickListener? = null
    private var cancelListener: View.OnClickListener? = null

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val bundle = arguments
        val caption = bundle!!.getString("title")
        val description = bundle.getString("text")
        val okTitle = bundle.getString("okTitle")
        val cancelTitle = bundle.getString("cancelTitle")
        val view: View
        if (cancelTitle != null) {
            view = inflater.inflate(R.layout.appdialog_two, null, false)
        } else {
            view = inflater.inflate(R.layout.appdialog_one, null, false)
        }
        val defaultListener = View.OnClickListener { dismiss() }
        var buttonCancel: Button? = null
        cancelTitle?.let{
                buttonCancel = view.findViewById<View>(R.id.buttonCancel) as Button
                buttonCancel!!.text = it
                buttonCancel!!.setOnClickListener(defaultListener)
        }
        val buttonOK = view.findViewById<View>(R.id.buttonOK) as Button
        buttonOK.text = okTitle ?: getString(R.string.ok)

        buttonOK.setOnClickListener(defaultListener)

        val textViewCaption = view.findViewById<View>(R.id.textViewCaption) as TextView
        textViewCaption.text = caption
        val textViewDesc = view.findViewById<View>(R.id.textViewDescription) as TextView
        textViewDesc.text = description

        if (okListener != null) buttonOK.setOnClickListener { v ->
            okListener!!.onClick(v)
            dismiss()
        }
        if (cancelListener != null && cancelTitle != null)
            buttonCancel!!.setOnClickListener { v ->
                cancelListener!!.onClick(v)
                dismiss()
            }
        return view
    }

    fun setOkListener(okListener: View.OnClickListener?) {
        this.okListener = okListener
    }

    fun setCancelListener(cancelListener: View.OnClickListener?) {
        this.cancelListener = cancelListener
    }

    override fun onResume() {
        super.onResume()
        val dialog = dialog
        val display = requireActivity().windowManager.defaultDisplay
        val p = Point()
        display.getSize(p)
        var size = Math.min(p.x, p.y)
        size = 3 * size / 4
        dialog!!.window!!.setLayout(size, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }
}