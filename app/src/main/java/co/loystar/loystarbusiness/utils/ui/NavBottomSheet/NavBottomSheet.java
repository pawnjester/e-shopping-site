package co.loystar.loystarbusiness.utils.ui.NavBottomSheet;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.CoordinatorLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.RewardCustomersActivity;
import co.loystar.loystarbusiness.activities.SendSMSBroadcast;

/**
 * Created by laudbruce-tagoe on 3/12/17.
 */

public class NavBottomSheet extends BottomSheetDialogFragment {
    public static final String TAG = NavBottomSheet.class.getName();
    @SuppressWarnings("unchecked")
    @Override
    public void setupDialog(final Dialog dialog, int style) {
        //noinspection RestrictedApi
        super.setupDialog(dialog, style);
        View contentView = View.inflate(getContext(), R.layout.nav_bottom_sheet, null);
        dialog.setContentView(contentView);
        CoordinatorLayout.LayoutParams layoutParams =
                (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = layoutParams.getBehavior();
        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
            ((BottomSheetBehavior) behavior).setState(BottomSheetBehavior.STATE_EXPANDED);
        }

        ArrayList items = new ArrayList();
        items.add( new NavBottomSheetItem(R.drawable.ic_marketing, getString(R.string.send_annoucement)) );
        items.add(new NavBottomSheetItem(R.drawable.ic_card_giftcard_white_24px, getString(R.string.rewards)) );

        NavBottomSheetItemAdapter adapter = new NavBottomSheetItemAdapter(getContext(), items);

        //ListView for the items
        ListView listView = (ListView) contentView.findViewById(R.id.list_items);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intent = null;
                switch (position) {
                    case 0:
                        intent = new Intent(getContext(), SendSMSBroadcast.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        break;
                    case 1:
                        intent = new Intent(getContext(), RewardCustomersActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        break;
                }
                startActivity(intent);
            }
        });
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
    }
}
