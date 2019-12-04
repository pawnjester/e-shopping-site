package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.pojos.LoyaltyDeal;

public class DealAdapter extends RecyclerView.Adapter<DealAdapter.ViewHolder> {

    public ArrayList<LoyaltyDeal> mDeals;
    public OnItemClickListener mlistener;
    Context mcontext;

    CustomerEntity mCustomerEntity;

    public DealAdapter(Context context,
                       ArrayList<LoyaltyDeal> deals,
                       CustomerEntity customerEntity,
                       OnItemClickListener listener
    ) {
        mDeals = deals;
        mCustomerEntity = customerEntity;
        mlistener = listener;
        mcontext = context;
    }

    public interface OnItemClickListener {
        void onItemClick(LoyaltyDeal deal);
    }
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.program_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LoyaltyDeal deal = mDeals.get(position);
        holder.bind(deal, mlistener);
    }

    @Override
    public int getItemCount() {
        return mDeals.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private TextView mTitle;
        private TextView mDescription;
        private ImageView mShareImageView;
        ViewHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.title);
            mDescription = itemView.findViewById(R.id.description);
            mShareImageView = itemView.findViewById(R.id.share_to_whatsapp);
        }

        public void bind(final LoyaltyDeal item, final OnItemClickListener listener) {
            String txt = "";
            mTitle.setText(item.getReward());
            if (item.getProgram_type().equals(mcontext.getResources().getString(R.string.simple_points))) {
                if (item.getTotal_user_points() >= item.getThreshold()) {
                    txt = mCustomerEntity.getFirstName() + " is due for this reward.";
                } else {
                    String pointsTxt;
                    if (item.getTotal_user_points() == 1) {
                        pointsTxt = "point";
                    } else {
                        pointsTxt = "points";
                    }
                    txt = mCustomerEntity.getFirstName() + " has earned " + item.getTotal_user_points() + " " + pointsTxt;
                }
            } else if (item.getProgram_type().equals(mcontext.getResources().getString(R.string.stamps_program))) {
                if (item.getTotal_user_stamps() >= item.getThreshold()) {
                    txt = mCustomerEntity.getFirstName() + " is due for this reward.";
                } else {
                    String stampsTxt;
                    if (item.getTotal_user_stamps() == 1) {
                        stampsTxt = "stamp";
                    } else {
                        stampsTxt = "stamps";
                    }
                    txt = mCustomerEntity.getFirstName() + " has earned " + item.getTotal_user_stamps() + " " + stampsTxt;
                }
            }
            mDescription.setText(txt);
            mShareImageView.setOnClickListener(view -> {
                listener.onItemClick(item);
            });
        }
    }
}
