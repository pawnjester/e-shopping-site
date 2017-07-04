package co.loystar.loystarbusiness.utils.ui.PosProducts;

import android.content.Context;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.utils.TextUtilsHelper;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerViewOnItemClickListener;
import co.loystar.loystarbusiness.utils.ui.SquaredImageView;

/**
 * Created by laudbruce-tagoe on 3/23/17.
 */

public class PosProductsGridViewAdapter extends RecyclerView.Adapter<PosProductsGridViewAdapter.ViewHolder> implements Filterable {
    private Context context;
    private ArrayList<PosProductEntity> productEntities;
    private Filter filter;
    private PosProductsCountListener productsCountListener;

    PosProductsGridViewAdapter(Context context, ArrayList<PosProductEntity> productEntities,
                               PosProductsCountListener productsCountListener) {
        this.context = context;
        this.productEntities = productEntities;
        this.productsCountListener = productsCountListener;
    }

    @Override
    public PosProductsGridViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.pos_product_grid_item_layout, parent, false);
        return new PosProductsGridViewAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(PosProductsGridViewAdapter.ViewHolder holder, int position) {
        holder.bindItem(productEntities.get(position));
    }

    @Override
    public long getItemId(int i) {
        return productEntities.get(i).getId();
    }

    @Override
    public int getItemCount() {
        return productEntities.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView productName;
        private TextView productPrice;
        private SquaredImageView productImage;
        private ImageView decrement;
        private TextView countView;
        private View countActions;

        public ViewHolder(View itemView) {
            super(itemView);
            productName = (TextView) itemView.findViewById(R.id.productName);
            productPrice = (TextView) itemView.findViewById(R.id.productPrice);
            productImage = (SquaredImageView) itemView.findViewById(R.id.productImg);
            decrement = (ImageView) itemView.findViewById(R.id.decrement);
            countView = (TextView) itemView.findViewById(R.id.count_view);
            countActions = itemView.findViewById(R.id.count_actions);
        }

        void bindItem(final PosProductEntity productEntity) {
            itemView.setTag(productEntity.getId());
            productName.setText(TextUtilsHelper.ellipsize(productEntity.getName(), 10));
            productPrice.setText(String.valueOf(productEntity.getPrice()));
            Glide.with(context)
                    .load(productEntity.getPicture())
                    //.centerCrop()
                    //.placeholder(AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_photo_black_24px))
                    //.crossFade()
                    .into(productImage);

            if (productEntity.getCount() > 0) {
                if (countActions.getVisibility() == View.GONE) {
                    countActions.setVisibility(View.VISIBLE);
                }
                countView.setText(String.valueOf(productEntity.getCount()));
            }
            else if (productEntity.getCount() == 0){
                if (countActions.getVisibility() == View.VISIBLE) {
                    countActions.setVisibility(View.GONE);
                }
            }

            decrement.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (productsCountListener != null) {
                        if (productEntity.getCount() > 0) {
                            productsCountListener.onCountChanged(productEntity.getId(), productEntity.getCount(), productEntity.getCount() - 1);
                        }
                    }
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (productsCountListener != null) {
                        productsCountListener.onCountChanged(productEntity.getId(), productEntity.getCount(), productEntity.getCount() + 1);
                    }
                }
            });
        }
    }

    public void animateTo(ArrayList<PosProductEntity> productEntities) {
        applyAndAnimateRemovals(productEntities);
        applyAndAnimateAdditions(productEntities);
        applyAndAnimateMovedItems(productEntities);
    }

    private void applyAndAnimateRemovals(ArrayList<PosProductEntity> newList) {
        for (int i = productEntities.size() - 1; i >= 0; i--) {
            final PosProductEntity productEntity = productEntities.get(i);
            if (!newList.contains(productEntity)) {
                removeItem(i);
            }
        }
    }

    private void applyAndAnimateAdditions(ArrayList<PosProductEntity> newList) {
        for (int i = 0, count = newList.size(); i < count; i++) {
            final PosProductEntity productEntity = newList.get(i);
            if (!productEntities.contains(productEntity)) {
                addItem(i, productEntity);
            }
        }
    }

    private void applyAndAnimateMovedItems(ArrayList<PosProductEntity> newList) {
        for (int toPosition = newList.size() - 1; toPosition >= 0; toPosition--) {
            final PosProductEntity productEntity = newList.get(toPosition);
            final int fromPosition = productEntities.indexOf(productEntity);
            if (fromPosition >= 0 && fromPosition != toPosition) {
                moveItem(fromPosition, toPosition);
            }
        }
    }

    private PosProductEntity removeItem(int position) {
        final PosProductEntity productEntity = productEntities.remove(position);
        notifyItemRemoved(position);
        return productEntity;
    }

    private void addItem(int position, PosProductEntity productEntity) {
        productEntities.add(position, productEntity);
        notifyItemInserted(position);
    }

    private void moveItem(int fromPosition, int toPosition) {
        final PosProductEntity model = productEntities.remove(fromPosition);
        productEntities.add(toPosition, model);
        notifyItemMoved(fromPosition, toPosition);
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new PosProductsGridViewAdapter.ProductsFilter<PosProductEntity>(productEntities);
        return filter;
    }

    private class ProductsFilter<T> extends Filter {
        private ArrayList<PosProductEntity> mProducts;

        ProductsFilter(ArrayList<PosProductEntity> products) {
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
                ArrayList<PosProductEntity> filter = new ArrayList<>();
                for (PosProductEntity product: mProducts) {
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
            ArrayList<PosProductEntity> filtered = (ArrayList<PosProductEntity>) filterResults.values;
            if (filtered != null) {
                animateTo(filtered);
            }
            else {
                animateTo(mProducts);
            }
        }
    }
}
