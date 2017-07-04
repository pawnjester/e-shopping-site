package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.otto.Bus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.LoyaltyProgramsOverflowItemClickEvent;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;

/**
 * Created by laudbruce-tagoe on 6/11/16.
 */

public class MerchantLoyaltyProgramsAdapter extends RecyclerView.Adapter<MerchantLoyaltyProgramsAdapter.ProgramsViewHolder> {
    private ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms;
    private Bus mBus = BusProvider.getInstance();
    private Context mContext;

    public MerchantLoyaltyProgramsAdapter(ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms, Context context) {
        this.loyaltyPrograms = loyaltyPrograms;
        this.mContext = context;

        Collections.sort(this.loyaltyPrograms, Collections.reverseOrder(new Comparator<DBMerchantLoyaltyProgram>() {
            @Override
            public int compare(DBMerchantLoyaltyProgram lhs, DBMerchantLoyaltyProgram rhs) {
                return lhs.getCreated_at().compareTo(rhs.getCreated_at());
            }
        }));
    }

    @Override
    public MerchantLoyaltyProgramsAdapter.ProgramsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.merchant_loyalty_programs_list_layout, parent, false);
        return new ProgramsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MerchantLoyaltyProgramsAdapter.ProgramsViewHolder holder, int position) {
        holder.bindItem(loyaltyPrograms.get(position));
    }

    @Override
    public int getItemCount() {
        return loyaltyPrograms.size();
    }

    class ProgramsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private ImageView overflow;
        private TextView title;
        private TextView subTitle;

        ProgramsViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            overflow = (ImageView) itemView.findViewById(R.id.overflow);
            title = (TextView) itemView.findViewById(R.id.title);
            subTitle = (TextView) itemView.findViewById(R.id.sub_title);

        }

        void bindItem(DBMerchantLoyaltyProgram program) {
            title.setText(TextUtilsHelper.capitalizeString(program.getName()));
            title.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.colorPrimary));
            String programType = program.getProgram_type();
            if (programType.equals(mContext.getString(R.string.simple_points))) {
                subTitle.setText(R.string.simple_points_program);
            }
            else if (programType.equals(mContext.getString(R.string.stamps_program))) {
                subTitle.setText(R.string.stamp_loyalty_program);
            }

            overflow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showPopupMenu(overflow, getAdapterPosition());
                }
            });
        }

        @Override
        public void onClick(View view) {
            mBus.post(new LoyaltyProgramsOverflowItemClickEvent.OnItemClicked(getAdapterPosition(), "edit"));
        }
    }

    /**
     * Showing popup menu when tapping on 3 dots
     */
    private void showPopupMenu(View view, int adapterPosition) {
        // inflate menu
        PopupMenu popup = new PopupMenu(mContext, view);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_program_card_overflow, popup.getMenu());
        popup.setOnMenuItemClickListener(new MyMenuItemClickListener(adapterPosition));
        popup.show();
    }

    /**
     * Click listener for popup menu items
     */
    private class MyMenuItemClickListener implements PopupMenu.OnMenuItemClickListener {
        private int adapterPosition;
        MyMenuItemClickListener(int adapterPosition) {this.adapterPosition = adapterPosition;}

        @Override
        public boolean onMenuItemClick(MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.edit_program:
                    mBus.post(new LoyaltyProgramsOverflowItemClickEvent.OnItemClicked(adapterPosition, "edit"));
                    return true;
                case R.id.delete_program:
                    new AlertDialog.Builder(mContext)
                        .setTitle("Are you sure?")
                        .setMessage("You won't be able to recover this program.")
                        .setPositiveButton(mContext.getString(R.string.confirm_delete_positive), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                mBus.post(new LoyaltyProgramsOverflowItemClickEvent.OnItemClicked(adapterPosition, "delete"));
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
                    return true;
                default:
            }
            return false;
        }
    }

    public void animateTo(ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms) {
        applyAndAnimateRemovals(loyaltyPrograms);
        applyAndAnimateAdditions(loyaltyPrograms);
        applyAndAnimateMovedItems(loyaltyPrograms);
    }

    private void applyAndAnimateRemovals(ArrayList<DBMerchantLoyaltyProgram> newList) {
        for (int i = loyaltyPrograms.size() - 1; i >= 0; i--) {
            final DBMerchantLoyaltyProgram loyaltyProgram = loyaltyPrograms.get(i);
            if (!newList.contains(loyaltyProgram)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(ArrayList<DBMerchantLoyaltyProgram> newList) {
        for (int i = 0, count = newList.size(); i < count; i++) {
            final DBMerchantLoyaltyProgram loyaltyProgram = newList.get(i);
            if (!loyaltyPrograms.contains(loyaltyProgram)) {
                addItem(i, loyaltyProgram);
            }
        }
    }

    private void applyAndAnimateMovedItems(ArrayList<DBMerchantLoyaltyProgram> newList) {
        for (int toPosition = newList.size() - 1; toPosition >= 0; toPosition--) {
            final DBMerchantLoyaltyProgram loyaltyProgram = newList.get(toPosition);
            final int fromPosition = loyaltyPrograms.indexOf(loyaltyProgram);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private DBMerchantLoyaltyProgram removeItem(int position) {
        final DBMerchantLoyaltyProgram loyaltyProgram = loyaltyPrograms.remove(position);
        notifyItemRemoved(position);
        return loyaltyProgram;
    }

    private void addItem(int position, DBMerchantLoyaltyProgram loyaltyProgram) {
        loyaltyPrograms.add(position, loyaltyProgram);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final DBMerchantLoyaltyProgram loyaltyProgram = loyaltyPrograms.remove(fromPosition);
        loyaltyPrograms.add(toPosition, loyaltyProgram);
        notifyItemMoved(fromPosition, toPosition);
    }

}