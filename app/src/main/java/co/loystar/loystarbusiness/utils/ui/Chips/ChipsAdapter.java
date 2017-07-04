package co.loystar.loystarbusiness.utils.ui.Chips;

import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.robertlevonyan.views.chip.Chip;
import com.robertlevonyan.views.chip.OnChipClickListener;
import com.robertlevonyan.views.chip.OnCloseClickListener;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;

/**
 * Created by laudbruce-tagoe on 3/18/17.
 */

public class ChipsAdapter extends  RecyclerView.Adapter<ChipsAdapter.ViewHolder> implements Filterable{
    private ArrayList<ChipsEntity> chipsEntities;
    private ChipsOnRemoveListener chipsOnRemoveListener;
    private Filter filter;

    ChipsAdapter(ArrayList<ChipsEntity> chipsEntities, ChipsOnRemoveListener chipsOnRemoveListener) {
        this.chipsEntities = chipsEntities;
        this.chipsOnRemoveListener = chipsOnRemoveListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chip, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindItem(chipsEntities.get(position));
    }

    @Override
    public int getItemCount() {
        return chipsEntities.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private Chip chip;

        ViewHolder(View itemView) {
            super(itemView);
            chip = (Chip) itemView.findViewById(R.id.chip);
        }

        void bindItem(ChipsEntity entity) {

            itemView.setTag(entity.getName());
            chip.setChipText(TextUtilsHelper.ellipsize(entity.getName(), 20));
            chip.setOnChipClickListener(new OnChipClickListener() {
                @Override
                public void onChipClick(View v) {
                if (chipsOnRemoveListener != null && getAdapterPosition() != -1) {
                    chipsOnRemoveListener.onItemRemoved(getAdapterPosition());
                }
                }
            });
            chip.setOnCloseClickListener(new OnCloseClickListener() {
                @Override
                public void onCloseClick(View v) {
                    if (chipsOnRemoveListener != null && getAdapterPosition() != -1) {
                        chipsOnRemoveListener.onItemRemoved(getAdapterPosition());
                    }
                }
            });
        }
    }

    private void animateTo(ArrayList<ChipsEntity> chips) {
        applyAndAnimateRemovals(chips);
        applyAndAnimateAdditions(chips);
        applyAndAnimateMovedItems(chips);
    }

    private void applyAndAnimateRemovals(ArrayList<ChipsEntity> newList) {
        for (int i = chipsEntities.size() - 1; i >= 0; i--) {
            final ChipsEntity chip = chipsEntities.get(i);
            if (!newList.contains(chip)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(ArrayList<ChipsEntity> newList) {
        for (int i = 0, count = newList.size(); i < count; i++) {
            final ChipsEntity chip = newList.get(i);
            if (!chipsEntities.contains(chip)) {
                addItem(i, chip);
            }
        }
    }

    private void applyAndAnimateMovedItems(ArrayList<ChipsEntity> newList) {
        for (int toPosition = newList.size() - 1; toPosition >= 0; toPosition--) {
            final ChipsEntity chip = newList.get(toPosition);
            final int fromPosition = chipsEntities.indexOf(chip);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private ChipsEntity removeItem(int position) {
        final ChipsEntity model = chipsEntities.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    private void addItem(int position, ChipsEntity chip) {
        chipsEntities.add(position, chip);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final ChipsEntity chip = chipsEntities.remove(fromPosition);
        chipsEntities.add(toPosition, chip);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new ChipsFilter<ChipsEntity>(chipsEntities);
        return filter;
    }

    private class ChipsFilter<T> extends Filter {
        private ArrayList<ChipsEntity> mChips;

        ChipsFilter(ArrayList<ChipsEntity> chips) {
            mChips = new ArrayList<>();
            synchronized (this) {
                mChips.addAll(chips);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            String filterQuery = charSequence.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (!TextUtils.isEmpty(filterQuery)) {
                ArrayList<ChipsEntity> filter = new ArrayList<>();
                for (ChipsEntity chip: mChips) {
                    if (chip.getName().toLowerCase().contains(filterQuery)) {
                        filter.add(chip);
                    }
                }
                result.count = filter.size();
                result.values = filter;
            }
            else {
                synchronized (this) {
                    result.count = mChips.size();
                    result.values = mChips;
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            ArrayList<ChipsEntity> filtered = (ArrayList<ChipsEntity>) filterResults.values;
            if (filtered != null) {
                animateTo(filtered);
            }
            else {
                animateTo(mChips);
            }
        }
    }
}
