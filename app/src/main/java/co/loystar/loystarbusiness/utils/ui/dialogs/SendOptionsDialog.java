package co.loystar.loystarbusiness.utils.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;

public class SendOptionsDialog extends AppCompatDialogFragment {

    public static final String TAG = SendOptionsDialog.class.getSimpleName();

    private ImageButton sendWithEmail;

    private ImageButton sendWithWhatsapp;

    private ImageButton downloadPdf;

    private SendOptionsDialogClickListener mListener;

    public static SendOptionsDialog newInstance() {
        return new SendOptionsDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() == null) {
            return super.onCreateDialog(savedInstanceState);
        }

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(getActivity(),
                    android.R.style.Theme_Material_Light_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(getActivity());
        }

        builder.setTitle(getString(R.string.send_with));
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View rootView = inflater.inflate(
                R.layout.send_options_dialog, null);

        sendWithEmail = rootView.findViewById(R.id.sendWithEmail);
//        sendWithWhatsapp = rootView.findViewById(R.id.sendWithWhatsapp);
        downloadPdf = rootView.findViewById(R.id.download_pdf);

        downloadPdf.setImageDrawable(AppCompatResources.getDrawable(getActivity(),
                R.drawable.ic_file_download_48));
        sendWithEmail.setImageDrawable(AppCompatResources.getDrawable(getActivity(),
                R.drawable.ic_send_black));
//        sendWithWhatsapp.setImageDrawable(AppCompatResources.getDrawable(getActivity(),
//                R.drawable.ic_file_upload_black_48));

        downloadPdf.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onDownloadPdf();
                dismiss();
            }
        });

//        sendWithWhatsapp.setOnClickListener(view -> {
//            if (mListener != null) {
//                mListener.onSendWithWhatsapp();
//                dismiss();
//            }
//        });

        sendWithEmail.setOnClickListener(view -> {
            if (mListener != null) {
                mListener.onSendWithEmail();
                dismiss();
            }
        });

        builder.setView(rootView);
        builder.setPositiveButton(android.R.string.no, (dialogInterface, i) -> {
            dialogInterface.dismiss();
        });
        return builder.create();
    }

    public interface SendOptionsDialogClickListener {
        void onSendWithEmail();
//        void onSendWithWhatsapp();
        void onDownloadPdf();
    }

    public void setListener(SendOptionsDialogClickListener mListener) {
        this.mListener = mListener;
    }
}
