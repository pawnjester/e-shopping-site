package co.loystar.loystarbusiness.utils.ui.Chips;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

/**
 * Created by laudbruce-tagoe on 3/18/17.
 */

public interface IItemsFactory<Item> {
    ArrayList<Item> getCategoryItems(Context context);
    RecyclerView.Adapter<? extends RecyclerView.ViewHolder> createAdapter(ArrayList<Item> items, ChipsOnRemoveListener chipsOnRemoveListener);
}
