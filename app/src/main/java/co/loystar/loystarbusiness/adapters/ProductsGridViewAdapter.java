package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerViewOnItemClickListener;
import co.loystar.loystarbusiness.utils.ui.SquaredImageView;

/**
 * Created by laudbruce-tagoe on 3/16/17.
 */

public class ProductsGridViewAdapter extends RecyclerView.Adapter<ProductsGridViewAdapter.ViewHolder> implements Filterable{
    private Context mContext;
    private ArrayList<DBProduct> products;
    private Filter filter;
    private RecyclerViewOnItemClickListener productsOnClickListener;

    public ProductsGridViewAdapter(Context mContext, ArrayList<DBProduct> products,
                                   RecyclerViewOnItemClickListener productsOnClickListener) {
        this.mContext = mContext;
        this.products = products;
        this.productsOnClickListener = productsOnClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.product_grid_item_layout, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bindItem(products.get(position));
    }

    @Override
    public long getItemId(int i) {
        return products.get(i).getId();
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder  implements View.OnClickListener {
        private TextView productName;
        private TextView productPrice;
        private SquaredImageView productImage;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            productName = (TextView) itemView.findViewById(R.id.productName);
            productPrice = (TextView) itemView.findViewById(R.id.productPrice);
            productImage = (SquaredImageView) itemView.findViewById(R.id.productImg);
        }

        void bindItem(DBProduct product) {
            itemView.setTag(product.getId());
            productName.setText(TextUtilsHelper.ellipsize(product.getName(), 10));
            productPrice.setText(String.valueOf(product.getPrice()));
            Glide.with(mContext)
                    .load(product.getPicture())
                    //.centerCrop()
                    //.placeholder(AppCompatDrawableManager.get().getDrawable(mContext, R.drawable.ic_photo_black_24px))
                    //.crossFade()
                    .into(productImage);
        }

        @Override
        public void onClick(View view) {
            if (productsOnClickListener != null && getAdapterPosition() != -1) {
                productsOnClickListener.onItemClicked(getAdapterPosition());
            }
        }
    }

    public void animateTo(ArrayList<DBProduct> products) {
        applyAndAnimateRemovals(products);
        applyAndAnimateAdditions(products);
        applyAndAnimateMovedItems(products);
    }

    private void applyAndAnimateRemovals(ArrayList<DBProduct> newList) {
        for (int i = products.size() - 1; i >= 0; i--) {
            final DBProduct product = products.get(i);
            if (!newList.contains(product)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(ArrayList<DBProduct> newList) {
        for (int i = 0, count = newList.size(); i < count; i++) {
            final DBProduct product = newList.get(i);
            if (!products.contains(product)) {
                addItem(i, product);
            }
        }
    }

    private void applyAndAnimateMovedItems(ArrayList<DBProduct> newList) {
        for (int toPosition = newList.size() - 1; toPosition >= 0; toPosition--) {
            final DBProduct product = newList.get(toPosition);
            final int fromPosition = products.indexOf(product);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private DBProduct removeItem(int position) {
        final DBProduct model = products.remove(position);
        notifyItemRemoved(position);
        return model;
    }

    private void addItem(int position, DBProduct user) {
        products.add(position, user);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final DBProduct model = products.remove(fromPosition);
        products.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new ProductsFilter<DBProduct>(products);
        return filter;
    }

    private class ProductsFilter<T> extends Filter {
        private ArrayList<DBProduct> mProducts;

        ProductsFilter(ArrayList<DBProduct> products) {
            mProducts = new ArrayList<>();
            synchronized (this) {
                mProducts.addAll(products);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            String filterQuery = charSequence.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (!TextUtils.isEmpty(filterQuery)) {
                ArrayList<DBProduct> filter = new ArrayList<>();
                for (DBProduct product: mProducts) {
                    if (product.getName().toLowerCase().contains(filterQuery)) {
                        filter.add(product);
                    }
                }
                result.count = filter.size();
                result.values = filter;
            }
            else {
                synchronized (this) {
                    result.count = mProducts.size();
                    result.values = mProducts;
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            ArrayList<DBProduct> filtered = (ArrayList<DBProduct>) filterResults.values;
            if (filtered != null) {
                animateTo(filtered);
            }
            else {
                animateTo(mProducts);
            }
        }
    }
}
