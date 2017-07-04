package co.loystar.loystarbusiness.utils.ui.MyAlertDialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

/**
 * Created by laudbruce-tagoe on 5/14/17.
 */

public class MyAlertDialog extends DialogFragment {
    public static final String TAG = MyAlertDialog.class.getSimpleName();
    private String mDialogTitle;
    private String mPositiveButtonText;
    private String mNegativeButtonText;
    private String mDialogMessage;
    private DialogInterface.OnClickListener mOnClickListener;
    private Drawable mDialogIcon;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        String strPositiveButton = mPositiveButtonText == null ? getString(android.R.string.yes) : mPositiveButtonText;
        alertDialog.setPositiveButton(strPositiveButton, mOnClickListener);

        String strNegativeButton = mNegativeButtonText == null ? getString(android.R.string.no) : mNegativeButtonText;
        alertDialog.setNegativeButton(strNegativeButton, mOnClickListener);

        if (mDialogIcon != null) {
            alertDialog.setIcon(mDialogIcon);
        }

        alertDialog.setTitle(mDialogTitle);
        alertDialog.setMessage(mDialogMessage);

        return alertDialog.create();
    }

    public void setTitle(String strTitle) {
        mDialogTitle = strTitle;
    }

    public void setMessage(String strMessage) {
        mDialogMessage = strMessage;
    }

    public void setDialogIcon(Drawable dialogIcon) {
        mDialogIcon = dialogIcon;
    }

    public void setPositiveButton(String strPositiveButtonText) {
        mPositiveButtonText = strPositiveButtonText;
    }

    public void setNegativeButtonText(String strNegativeButtonText) {
        mNegativeButtonText = strNegativeButtonText;
    }

    public void setPositiveButton(String strPositiveButtonText, DialogInterface.OnClickListener onClickListener) {
        mPositiveButtonText = strPositiveButtonText;
        mOnClickListener = onClickListener;
    }
}
