package co.loystar.loystarbusiness.utils.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/12/17.
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
        if (getActivity() == null) {
            return super.onCreateDialog(savedInstanceState);
        }
        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(), android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }
        String strPositiveButton = mPositiveButtonText == null ? getString(android.R.string.yes) : mPositiveButtonText;
        builder.setPositiveButton(strPositiveButton, mOnClickListener);

        String strNegativeButton = mNegativeButtonText == null ? getString(android.R.string.no) : mNegativeButtonText;
        builder.setNegativeButton(strNegativeButton, mOnClickListener);

        if (mDialogIcon == null) {
            Drawable icon = ContextCompat.getDrawable(getActivity(), android.R.drawable.ic_dialog_alert);
            int color = ContextCompat.getColor(getActivity(), R.color.white);
            assert icon != null;
            icon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
            builder.setIcon(icon);
        } else {
            builder.setIcon(mDialogIcon);
        }

        builder.setTitle(mDialogTitle);
        builder.setMessage(mDialogMessage);

        return builder.create();
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
