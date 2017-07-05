package co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

/**
 * Created by laudbruce-tagoe on 3/23/17.
 */

public class SingleChoiceSpinnerDialog extends AppCompatSpinner implements SingleChoiceSpinnerAlertBuilder.SelectEntityListener {
    private Context mContext;
    private SingleChoiceSpinnerDialogOnItemSelectedListener mListener;
    private SingleChoiceSpinnerAlertBuilder singleChoiceSpinnerAlertBuilder;
    private int preSelectedIndex = -1;
    private ArrayList<SingleChoiceSpinnerDialogEntity> entities;
    private boolean spinnerIsDirty = false;
    private String dialogTitle;
    private Intent createEntityIntent;
    private String noEntitiesString;
    private String createEntityString;

    public SingleChoiceSpinnerDialog(Context mContext, AttributeSet attrs) {
        super(mContext, attrs);
        this.mContext = mContext;
    }

    public SingleChoiceSpinnerDialog(Context mContext, AttributeSet attrs, int defStyleAttr) {
        super(mContext, attrs, defStyleAttr);
        this.mContext = mContext;
    }

    public void setItems(ArrayList<SingleChoiceSpinnerDialogEntity> items,
         String spinnerDefaultText,
         SingleChoiceSpinnerDialogOnItemSelectedListener listener,
         String dialogTitle,
         Long mSelected,
         Intent createEntityIntent,
         String noEntitiesString,
         String createEntityString) {

        this.entities = items;
        this.mListener = listener;
        this.dialogTitle = dialogTitle;
        this.noEntitiesString = noEntitiesString;
        this.createEntityIntent = createEntityIntent;
        this.createEntityString = createEntityString;

        SingleChoiceSpinnerDialogEntity preSelectedEntity = null;
        for (int i=0; i<items.size(); i++) {
            SingleChoiceSpinnerDialogEntity entity = items.get(i);
            if (entity.getId().equals(mSelected)) {
                preSelectedEntity = entity;
                preSelectedIndex = i;
            }
        }

        if (preSelectedEntity != null) {
            spinnerDefaultText = preSelectedEntity.getText();
        }


        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, new String[] { spinnerDefaultText });
        setAdapter(adapter);

        singleChoiceSpinnerAlertBuilder = new SingleChoiceSpinnerAlertBuilder().newInstance(
                items,
                dialogTitle,
                preSelectedIndex,
                createEntityIntent,
                noEntitiesString,
                createEntityString);
        singleChoiceSpinnerAlertBuilder.setSelectEntityListener(this);
    }

    @Override
    public boolean performClick() {
        if (spinnerIsDirty) {
            singleChoiceSpinnerAlertBuilder = new SingleChoiceSpinnerAlertBuilder().newInstance(
                    entities,
                    dialogTitle,
                    preSelectedIndex,
                    createEntityIntent,
                    noEntitiesString,
                    createEntityString);
            singleChoiceSpinnerAlertBuilder.setSelectEntityListener(this);
        }
        singleChoiceSpinnerAlertBuilder.show(scanForActivity(mContext).getSupportFragmentManager(), singleChoiceSpinnerAlertBuilder.getTag());
        return true;
    }

    public static AppCompatActivity scanForActivity(Context cont) {
        if (cont == null)
            return null;
        else if (cont instanceof AppCompatActivity)
            return (AppCompatActivity) cont;
        else if (cont instanceof ContextWrapper)
            return scanForActivity(((ContextWrapper) cont).getBaseContext());

        return null;
    }

    @Override
    public void selectedEntityId(Long Id) {
        for (int i=0; i<entities.size(); i++) {
            SingleChoiceSpinnerDialogEntity entity = entities.get(i);
            if (entity.getId().equals(Id)) {
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_item,
                        new String[] { entity.getText() });
                setAdapter(adapter);
                preSelectedIndex = i;
            }
        }
        spinnerIsDirty = true;
        mListener.itemSelectedId(Id);
    }
}
