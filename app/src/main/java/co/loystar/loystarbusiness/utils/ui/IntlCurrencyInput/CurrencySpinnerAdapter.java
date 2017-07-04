package co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 1/5/17.
 */

class CurrencySpinnerAdapter extends ArrayAdapter<Currency> implements SpinnerAdapter, Filterable {

    private LayoutInflater mLayoutInflater;
    private Filter filter;
    private ArrayList<Currency> currencies;
    boolean firstTime = true;

    CurrencySpinnerAdapter(Context context, int resource, ArrayList<Currency> currencyArrayList) {
        super(context, resource, currencyArrayList);
        mLayoutInflater = LayoutInflater.from(context);
        this.currencies = currencyArrayList;
    }


    /**
     * Drop down item view
     *
     * @param position    position of item
     * @param convertView View of item
     * @param parent      parent view of item's view
     * @return covertView
     */
    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.item_currency, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.mSymbolView = (TextView) convertView.findViewById(R.id.intl_currency_symbol);
            viewHolder.mNameView = (TextView) convertView.findViewById(R.id.intl_currency_name);
            viewHolder.mIsoCode = (TextView) convertView.findViewById(R.id.intl_currency_iso_code);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Currency currency = getItem(position);
        if (currency != null) {
            viewHolder.mSymbolView.setText(currency.getSymbol());
            viewHolder.mNameView.setText(currency.getName());
            viewHolder.mIsoCode.setText(currency.getCode());
        }
        return convertView;
    }

    /**
     * View holder for caching
     */
    private static class ViewHolder {
        private TextView mSymbolView;
        private TextView mNameView;
        private TextView mIsoCode;
    }

    /**
     * Drop down selected view
     *
     * @param position    position of selected item
     * @param convertView View of selected item
     * @param parent      parent of selected view
     * @return convertView
     */
    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        //getView is initially called on load and later after selection

        if (parent.getTag() == null) {
            if (firstTime) {
                firstTime = false;
                if (position == 0) {
                    convertView = getNothingSelectedView(parent);
                }
            }
            else {
                convertView = getDropDownView(position, convertView, parent);
            }
            return convertView;
        }
        else {
            return getDropDownView(position, convertView, parent);
        }
    }

    /**
     * View to show in Spinner with Nothing Selected
     * @param parent ViewGroup
     * @return View
     */
    private View getNothingSelectedView(ViewGroup parent) {
        return mLayoutInflater.inflate(R.layout.spinner_row_nothing_selected, parent, false);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new CurrencyFilter<Currency>(currencies);
        return filter;
    }

    private class CurrencyFilter<T> extends Filter {
        private ArrayList<Currency> mCurrencies;
        CurrencyFilter(ArrayList<Currency> currencies) {
            mCurrencies = new ArrayList<>();
            synchronized (this) {
                mCurrencies.addAll(currencies);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            String filterSeq = charSequence.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (!TextUtils.isEmpty(filterSeq)) {
                ArrayList<Currency> filter = new ArrayList<>();
                for (Currency currency: mCurrencies) {
                    if (currency.getName().toLowerCase().contains(filterSeq)) {
                        filter.add(currency);
                    }
                }
                result.count = filter.size();
                result.values = filter;
            }
            else {
                synchronized (this) {
                    result.count = mCurrencies.size();
                    result.values = mCurrencies;
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            ArrayList<Currency> filtered = (ArrayList<Currency>) filterResults.values;
            notifyDataSetChanged();
            clear();
            if (filtered != null) {
                for (Currency currency: filtered) {
                    add(currency);
                }
            }
            else {
                for (Currency currency: mCurrencies) {
                    add(currency);
                }
            }
            notifyDataSetInvalidated();
        }
    }
}
