package co.loystar.loystarbusiness.utils.ui.PosProducts;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.models.db.DBProduct;

/**
 * Created by laudbruce-tagoe on 5/24/17.
 */

public interface IPosProductsEntityFactory<Item> {
    ArrayList<Item> getProductItems(Context context);
    RecyclerView.Adapter<? extends RecyclerView.ViewHolder> createAdapter(Context context, ArrayList<Item> items, PosProductsCountListener posProductsCountListener);
}
