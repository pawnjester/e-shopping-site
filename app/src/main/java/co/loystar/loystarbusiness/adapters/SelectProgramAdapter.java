package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.SelectProgramAdapterItemClickEvent;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;

/**
 * Created by laudbruce-tagoe on 4/15/16.
 */

public class SelectProgramAdapter extends RecyclerView.Adapter<SelectProgramAdapter.ProgramsViewHolder>{
    private ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms;
    private Context mContext;

    public SelectProgramAdapter(ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms, Context mContext){
        this.loyaltyPrograms = loyaltyPrograms;
        this.mContext = mContext;

        Collections.sort(this.loyaltyPrograms, Collections.reverseOrder(new Comparator<DBMerchantLoyaltyProgram>() {
            @Override
            public int compare(DBMerchantLoyaltyProgram lhs, DBMerchantLoyaltyProgram rhs) {
                return lhs.getCreated_at().compareTo(rhs.getCreated_at());
            }
        }));
    }

    @Override
    public ProgramsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_program_layout, parent, false);
        return new ProgramsViewHolder(itemView);
    }

    @Override
    public int getItemCount() {
        return loyaltyPrograms.size();
    }

    class ProgramsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        private TextView title;
        private TextView subTitle;

        ProgramsViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
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
        }

        @Override
        public void onClick(View v) {
            BusProvider.getInstance().post(new SelectProgramAdapterItemClickEvent.OnItemClicked(getAdapterPosition()));
        }
    }


    @Override
    public void onBindViewHolder(ProgramsViewHolder holder, int position) {
        holder.bindItem(loyaltyPrograms.get(position));
    }
}
