package co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by laudbruce-tagoe on 3/23/17.
 */

public class SingleChoiceSpinnerAlertBuilder extends DialogFragment {
    private static final String SINGLE_CHOICE_SPINNER_ITEMS = "singleChoiceSpinnerItems";
    private static final String ALERT_TITLE = "alertTitle";
    private static final String NO_ENTITIES_STRING = "noEntitiesString";
    private static final String CREATE_ENTITY_INTENT = "createEntityIntent";
    private static final String CREATE_ENTITY_STRING = "createEntityString";
    private static final String PRESELECTED_INDEX = "preSelectedIndex";
    private SelectEntityListener mListener;
    private Long selectedEntityId;

    public SingleChoiceSpinnerAlertBuilder newInstance(ArrayList<SingleChoiceSpinnerDialogEntity> items,
           String alertTitle,
           int preSelectedIndex,
           Intent createEntityIntent,
           String noEntitiesString,
           String createEntityString) {

        SingleChoiceSpinnerAlertBuilder singleChoiceSpinnerAlertBuilder = new SingleChoiceSpinnerAlertBuilder();
        Bundle args = new Bundle();
        args.putParcelable(CREATE_ENTITY_INTENT, createEntityIntent);
        args.putString(NO_ENTITIES_STRING, noEntitiesString);
        args.putString(CREATE_ENTITY_STRING, createEntityString);
        args.putString(ALERT_TITLE, alertTitle);
        args.putInt(PRESELECTED_INDEX, preSelectedIndex);
        args.putParcelableArrayList(SINGLE_CHOICE_SPINNER_ITEMS, items);

        singleChoiceSpinnerAlertBuilder.setArguments(args);
        return singleChoiceSpinnerAlertBuilder;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final ArrayList<SingleChoiceSpinnerDialogEntity> items = getArguments().getParcelableArrayList(SINGLE_CHOICE_SPINNER_ITEMS);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (items != null && items.size() > 0) {
            List<String> itemsArray = new ArrayList<>();
            for (int i=0; i<items.size(); i++) {
                SingleChoiceSpinnerDialogEntity entity = items.get(i);
                itemsArray.add(entity.getText());
            }

            final CharSequence[] charSequenceItems = itemsArray.toArray(new CharSequence[itemsArray.size()]);
            builder.setTitle(getArguments().getString(ALERT_TITLE));

            builder.setSingleChoiceItems(charSequenceItems, getArguments().getInt(PRESELECTED_INDEX), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    selectedEntityId = items.get(i).getId();
                    mListener.selectedEntityId(selectedEntityId);
                    dialogInterface.dismiss();
                }
            });

            builder.setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                   dialogInterface.dismiss();
                }
            });
        }
        else {
            builder.setTitle(getArguments().getString(NO_ENTITIES_STRING));
            builder.setPositiveButton(getArguments().getString(CREATE_ENTITY_STRING), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    Intent intent = getArguments().getParcelable(CREATE_ENTITY_INTENT);
                    if (intent != null) {
                        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                    }
                }
            });
            builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setIcon(android.R.drawable.ic_dialog_alert);
        }

        return builder.create();
    }

    public void setSelectEntityListener(SelectEntityListener selectEntityListener) {
        this.mListener = selectEntityListener;
    }

    public interface SelectEntityListener {
        void selectedEntityId(Long Id);
    }
}
