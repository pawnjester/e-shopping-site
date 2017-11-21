package co.loystar.loystarbusiness.utils.ui.Currency;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.DividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;

/**
 * Created by ordgen on 11/20/17.
 */

public class CurrencyPickerDialog extends AppCompatDialogFragment implements
        SearchView.OnQueryTextListener {

    public static final String TAG = CurrencyPickerDialog.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private CurrencyPickerDialogAdapter mAdapter;
    private CurrenciesFetcher.CurrencyList mCurrenciesList;
    private OnItemSelectedListener mListener;

    public static CurrencyPickerDialog newInstance() {
        return new CurrencyPickerDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCurrenciesList = CurrenciesFetcher.getCurrencies(getActivity());
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        View rootView = inflater.inflate(R.layout.searchable_list_dialog, null);

        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context
                .SEARCH_SERVICE);

        SearchView mSearchView = rootView.findViewById(R.id.items_search_view);
        if (searchManager != null) {
            mSearchView.setSearchableInfo(
                    searchManager.getSearchableInfo(getActivity().getComponentName())
            );
            mSearchView.setIconifiedByDefault(false);
            mSearchView.setOnQueryTextListener(this);
            mSearchView.clearFocus();
        }
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (mgr != null) {
            mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);
        }

        mAdapter = new CurrencyPickerDialogAdapter(mCurrenciesList);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView = rootView.findViewById(R.id.items_rv);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.addItemDecoration(
                new SpacingItemDecoration(
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium),
                        getResources().getDimensionPixelOffset(R.dimen.item_space_medium))
        );
        mRecyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), LinearLayoutManager.HORIZONTAL));
        mRecyclerView.setAdapter(mAdapter);

        mRecyclerView.addOnItemTouchListener(new RecyclerTouchListener(getActivity(), mRecyclerView, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, int position) {
                if (mListener != null) {
                    mListener.onItemSelected(mAdapter.currencyList.get(position));
                }
                getDialog().cancel();
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setView(rootView);
        alertDialog.setTitle("Select country");
        alertDialog.setPositiveButton("CLOSE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        final AlertDialog dialog = alertDialog.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_STATE_HIDDEN);
        }
        return dialog;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            ((CurrencyPickerDialogAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
        } else {
            ((CurrencyPickerDialogAdapter) mRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        return true;
    }

    public interface OnItemSelectedListener {
        void onItemSelected(Currency currency);
    }

    public void setListener(CurrencyPickerDialog.OnItemSelectedListener listener) {
        mListener = listener;
    }

    private class CurrencyPickerDialogAdapter extends RecyclerView.Adapter<CurrencyPickerDialogAdapter.ViewHolder>
            implements Filterable {
        private ArrayList<Currency> currencyList;
        private Filter filter;

        public CurrencyPickerDialogAdapter(ArrayList<Currency> currencies) {
            currencyList = currencies;
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new CurrencyFilter<>(mCurrenciesList);
            }
            return filter;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private TextView mCurrencySymbol;
            private TextView mCurrencyName;
            private TextView mCurrencyIsoCode;

            public ViewHolder(View itemView) {
                super(itemView);
                mCurrencySymbol = itemView.findViewById(R.id.currency_symbol);
                mCurrencyName = itemView.findViewById(R.id.currency_name);
                mCurrencyIsoCode = itemView.findViewById(R.id.currency_iso_code);
            }
        }

        @Override
        public CurrencyPickerDialogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.currency_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CurrencyPickerDialogAdapter.ViewHolder holder, int position) {
            Currency currency = currencyList.get(position);
            holder.mCurrencySymbol.setText(currency.getSymbol());
            holder.mCurrencyName.setText(currency.getName());
            holder.mCurrencyIsoCode.setText(currency.getCode());
        }

        @Override
        public int getItemCount() {
            return currencyList.size();
        }

        private class CurrencyFilter<T> extends Filter {

            private ArrayList<Currency>currencyArrayList;
            CurrencyFilter(ArrayList<Currency> currencies) {
                currencyArrayList = new ArrayList<>();
                synchronized (this) {
                    currencyArrayList.addAll(currencies);
                }
            }

            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                String filterSeq = charSequence.toString().toLowerCase();
                FilterResults result = new FilterResults();
                if (TextUtils.isEmpty(filterSeq)) {
                    synchronized (this) {
                        result.count = currencyArrayList.size();
                        result.values = currencyArrayList;
                    }
                } else {
                    ArrayList<Currency> filter = new ArrayList<>();
                    for (Currency currency: currencyArrayList) {
                        if (currency.getName().toLowerCase().contains(filterSeq)) {
                            filter.add(currency);
                        }
                    }
                    result.count = filter.size();
                    result.values = filter;
                }
                return null;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                ArrayList<Currency> filtered = (ArrayList<Currency>) filterResults.values;
                if (filtered != null) {
                    animateTo(filtered);
                }
                else {
                    animateTo(currencyArrayList);
                }
            }

            void animateTo(ArrayList<Currency> currencies) {
                applyAndAnimateRemovals(currencies);
                applyAndAnimateAdditions(currencies);
                applyAndAnimateMovedItems(currencies);
            }

            private void applyAndAnimateRemovals(ArrayList<Currency> newList) {
                for (int i = mCurrenciesList.size() - 1; i >= 0; i--) {
                    final Currency currency = mCurrenciesList.get(i);
                    if (!newList.contains(currency)) {
                        removeItem(i);
                    }
                }
            }

            private void applyAndAnimateAdditions(ArrayList<Currency> newList) {
                for (int i = 0, count = newList.size(); i < count; i++) {
                    final Currency currency = newList.get(i);
                    if (!mCurrenciesList.contains(currency)) {
                        addItem(i, currency);
                    }
                }
            }

            private void applyAndAnimateMovedItems(ArrayList<Currency> newList) {
                for (int toPosition = newList.size() - 1; toPosition >= 0; toPosition--) {
                    final Currency currency = newList.get(toPosition);
                    final int fromPosition = mCurrenciesList.indexOf(currency);
                    if (fromPosition >= 0 && fromPosition != toPosition) {
                        moveItem(fromPosition, toPosition);
                    }
                }
            }

            void removeItem(int position) {
                mCurrenciesList.remove(position);
                notifyItemRemoved(position);
            }

            void addItem(int position, Currency currency) {
                mCurrenciesList.add(position, currency);
                notifyItemInserted(position);
            }

            void moveItem(int fromPosition, int toPosition) {
                final Currency currency = mCurrenciesList.remove(fromPosition);
                mCurrenciesList.add(toPosition, currency);
                notifyItemMoved(fromPosition, toPosition);
            }
        }
    }
}
