package co.loystar.loystarbusiness.adapters;

import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.squareup.otto.Bus;

import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.LoyaltyProgramsAdapterItemClickEvent;
import co.loystar.loystarbusiness.models.LoyaltyProgram;

/**
 * Created by laudbruce-tagoe on 3/3/16.
 */
public class DefaultLoyaltyProgramsAdapter extends RecyclerView.Adapter<DefaultLoyaltyProgramsAdapter.ProgramsViewHolder> {
    private List<LoyaltyProgram> loyaltyPrograms;
    private Bus mBus = BusProvider.getInstance();
    public DefaultLoyaltyProgramsAdapter(List<LoyaltyProgram> loyaltyPrograms){
        this.loyaltyPrograms = loyaltyPrograms;
    }

    @Override
    public DefaultLoyaltyProgramsAdapter.ProgramsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.default_loyalty_programs_list, parent, false);
        return new ProgramsViewHolder(v);
    }


    @Override
    public int getItemCount() {
        return loyaltyPrograms.size();
    }


    class ProgramsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        CardView cv;
        TextView title;
        TextView description;
        TextView actionText;

        ProgramsViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            cv = (CardView)itemView.findViewById(R.id.cv);
            title = (TextView)itemView.findViewById(R.id.program_title);
            description = (TextView)itemView.findViewById(R.id.program_description);
            actionText = (TextView)itemView.findViewById(R.id.action_text);

            cv.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            mBus.post(new LoyaltyProgramsAdapterItemClickEvent.OnItemClicked(getAdapterPosition()));
        }
    }

    @Override
    public void onBindViewHolder(ProgramsViewHolder holder, int position) {
        holder.title.setText(loyaltyPrograms.get(position).title);
        holder.title.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));
        holder.description.setText(loyaltyPrograms.get(position).description);
        holder.actionText.setText(R.string.set_program_details_text);
        holder.actionText.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.colorPrimary));

    }
}
