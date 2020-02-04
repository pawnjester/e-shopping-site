package co.loystar.loystarbusiness.fragments;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.Product;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.BindingHolder;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.OrderItemDividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;
import io.requery.Persistable;
import io.requery.android.QueryRecyclerAdapter;
import io.requery.query.Result;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProductListFragment extends Fragment {

//    RecyclerView productRec
    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    private ReactiveEntityStore<Persistable> mDataStore;
    private Context context;
    private EmptyRecyclerView mProductsRecyclerView;
    private ProductsAdapter mProductAdapter;
    private String merchantCurrencySymbol;

    public ProductListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_product_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        context = getActivity();
        mDataStore = DatabaseManager.getDataStore(context);
        mSessionManager = new SessionManager(context);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class,
                mSessionManager.getMerchantId()).blockingGet();
        merchantCurrencySymbol = CurrenciesFetcher
                .getCurrencies(context)
                .getCurrency(mSessionManager.getCurrency())
                .getSymbol();
        mProductAdapter = new ProductsAdapter();

        EmptyRecyclerView productsRecyclerview = getActivity().findViewById(R.id.product_list_land);
        assert productsRecyclerview != null;
        setupProductsRecyclerView(productsRecyclerview);

    }

    private void setupProductsRecyclerView(@NonNull EmptyRecyclerView recyclerView) {
        mProductsRecyclerView = recyclerView;
        mProductsRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager mLayoutManager = new GridLayoutManager(context, 4);
        mProductsRecyclerView.setLayoutManager(mLayoutManager);
        mProductsRecyclerView.addItemDecoration(new SpacingItemDecoration(
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                getResources().getDimensionPixelOffset(R.dimen.item_space_medium)
        ));
        mProductsRecyclerView.addItemDecoration(new OrderItemDividerItemDecoration(context));
        mProductsRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mProductsRecyclerView.setAdapter(mProductAdapter);
    }

    private ArrayList<ProductCategoryEntity> getCategory(){
        Selection<ReactiveResult<ProductCategoryEntity>> productsSelection =
                mDataStore.select(ProductCategoryEntity.class);
        productsSelection.where(ProductCategoryEntity.OWNER.eq(merchantEntity));
        productsSelection.where(ProductCategoryEntity.DELETED.notEqual(true));
        return new ArrayList<>(productsSelection.orderBy(ProductCategoryEntity.NAME.asc()).get().toList());
    }

    private class ProductsAdapter extends
            QueryRecyclerAdapter<ProductEntity,
                    BindingHolder<PosProductItemBinding>> implements Filterable {

        private Filter filter;

        ProductsAdapter() {
            super(ProductEntity.$TYPE);
        }

        @Override
        public Result<ProductEntity> performQuery() {
            if (merchantEntity == null) {
                return null;
            } else {
                Selection<ReactiveResult<ProductEntity>> productsSelection =
                        mDataStore.select(ProductEntity.class);
                productsSelection.where(ProductEntity.OWNER.eq(merchantEntity));
                productsSelection.where(ProductEntity.DELETED.notEqual(true));
                return productsSelection.orderBy(ProductEntity.NAME.asc()).get();
            }
        }

        @Override
        public void onBindViewHolder(ProductEntity item,
                                     BindingHolder<PosProductItemBinding> holder,
                                     int position) {
            holder.binding.setProduct(item);
            RequestOptions options = new RequestOptions();
            options.centerCrop().apply(RequestOptions.placeholderOf(
                    AppCompatResources.getDrawable(context, R.drawable.ic_photo_black_24px)
            ));
            Glide.with(context)
                    .load(item.getPicture())
                    .apply(options)
                    .into(holder.binding.productImage);
            Glide.with(context)
                    .load(item.getPicture())
                    .apply(options)
                    .into(holder.binding.productImageCopy);
            holder.binding.productName.setText(item.getName());
//            if (mSelectedProducts.get(item.getId()) == 0) {
//                holder.binding.productDecrementWrapper.setVisibility(View.GONE);
//            } else {
//                holder.binding.productDecrementWrapper.setVisibility(View.VISIBLE);
//                holder.binding.productCount.setText(
//                        getString(R.string.product_count,
//                                String.valueOf(mSelectedProducts.get(item.getId()))));
//            }
            holder.binding.productPrice.setText(
                    getString(R.string.product_price, merchantCurrencySymbol,
                            String.valueOf(item.getPrice())));
            holder.binding.addImage.bringToFront();
            holder.binding.decrementCount.setImageDrawable(
                    AppCompatResources.getDrawable(context,
                            R.drawable.ic_remove_circle_outline_white_24px));
        }

        @NonNull
        @Override
        public BindingHolder<PosProductItemBinding> onCreateViewHolder(
                @NonNull ViewGroup parent, int i) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            final PosProductItemBinding binding = PosProductItemBinding.inflate(inflater);
            binding.getRoot().setTag(binding);
            binding.decrementCount.setTag(binding);
            binding.productImageWrapper.setTag(binding);
            binding.productImageWrapper.setOnClickListener(view -> {
                        PosProductItemBinding posProductItemBinding = (PosProductItemBinding) view.getTag();
                        if (posProductItemBinding != null) {
                            Product product = posProductItemBinding.getProduct();
//                            if (mSelectedProducts.get(product.getId()) == 0) {
//                                mSelectedProducts.put(product.getId(), 1);
//                                mProductAdapter.queryAsync();
//                                orderSummaryAdapter.queryAsync();
//                                setCheckoutValue();
//                            } else {
//                                mSelectedProducts.put(product.getId(),
//                                        (mSelectedProducts.get(product.getId()) + 1));
//                                mProductAdapter.queryAsync();
//                                orderSummaryAdapter.queryAsync();
//                                setCheckoutValue();
//                            }
//
//                            binding.productDecrementWrapper.setVisibility(View.VISIBLE);
//                            makeFlyAnimation(posProductItemBinding.productImageCopy, cartCountImageView);
                        }
                    }
            );

            binding.decrementCount.setOnClickListener(view -> {
                PosProductItemBinding posProductItemBinding = (PosProductItemBinding) view.getTag();
                if (posProductItemBinding != null) {
                    Product product = posProductItemBinding.getProduct();
//                    if (mSelectedProducts.get(product.getId()) != 0) {
//                        int newValue = mSelectedProducts.get(product.getId()) - 1;
//                        setProductCountValue(newValue, product.getId());
//                        if (newValue == 0) {
//                            posProductItemBinding.productCount.setText("0");
//                            posProductItemBinding.productDecrementWrapper.setVisibility(View.GONE);
//                        } else {
//                            posProductItemBinding.productCount.setText(
//                                    getString(R.string.product_count, String.valueOf(newValue)));
//                        }
//                    }
                }
            });
            return new BindingHolder<>(binding);
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new ProductFilter(new ArrayList<>(mProductAdapter.performQuery().toList()));
            }
            return filter;
        }

        private class ProductFilter extends Filter {

            private ArrayList<ProductEntity> productEntities;

            ProductFilter(ArrayList<ProductEntity> productEntities) {
                this.productEntities = new ArrayList<>();
                synchronized (this) {
                    this.productEntities.addAll(productEntities);
                }
            }


            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                FilterResults result = new FilterResults();
                String searchString = charSequence.toString();
                if (TextUtils.isEmpty(searchString)) {
                    synchronized (this) {
                        result.count = productEntities.size();
                        result.values = productEntities;
                    }
                } else {
//                    searchFilterText = searchString;
//                    result.count = 0;
//                    result.values = new ArrayList<>();
                }
                return result;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                mProductAdapter.queryAsync();
            }
        }
    }

}
