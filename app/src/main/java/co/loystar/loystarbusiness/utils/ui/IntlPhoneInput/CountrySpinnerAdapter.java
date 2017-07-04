package co.loystar.loystarbusiness.utils.ui.IntlPhoneInput;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 6/17/16.
 */
class CountrySpinnerAdapter extends ArrayAdapter<Country> implements SpinnerAdapter, Filterable {
    private LayoutInflater mLayoutInflater;
    private Filter filter;
    private ArrayList<Country> countries;

    /**
     * Constructor
     *
     * @param context Context
     */
    CountrySpinnerAdapter(Context context, int resourceId, ArrayList<Country> countries) {
        super(context, resourceId, countries);
        mLayoutInflater = LayoutInflater.from(context);
        this.countries = countries;
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
            convertView = mLayoutInflater.inflate(R.layout.item_country, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.mImageView = (ImageView) convertView.findViewById(R.id.intl_phone_edit__country__item_image);
            viewHolder.mNameView = (TextView) convertView.findViewById(R.id.intl_phone_edit__country__item_name);
            viewHolder.mDialCode = (TextView) convertView.findViewById(R.id.intl_phone_edit__country__item_dialcode);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Country country = getItem(position);
        viewHolder.mImageView.setImageResource(getFlagResource(country));
        assert country != null;
        viewHolder.mNameView.setText(country.getName());
        viewHolder.mDialCode.setText(String.format("+%s", country.getDialCode()));
        return convertView;
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
        if (parent.getTag() == null) {
            Country country = getItem(position);

            if (convertView == null) {
                convertView = new ImageView(getContext());
            }

            ((ImageView) convertView).setImageResource(getFlagResource(country));

            return convertView;
        }
        else {
            return getDropDownView(position, convertView, parent);
        }
    }


    /**
     * Fetch flag resource by Country
     *
     * @param country Country
     * @return int of resource | 0 value if not exists
     */
    private int getFlagResource(Country country) {
        return getContext().getResources().getIdentifier("country_" + country.getIso().toLowerCase(), "drawable", getContext().getPackageName());
    }

    @NonNull
    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new CountryFilter<Country>(countries);
        return filter;
    }


    /**
     * View holder for caching
     */
    private static class ViewHolder {
        ImageView mImageView;
        TextView mNameView;
        TextView mDialCode;
    }

    private class CountryFilter<T> extends Filter {

        private ArrayList<Country> mCountries;

        CountryFilter(ArrayList<Country> countries) {
            mCountries = new ArrayList<>();
            synchronized (this) {
                mCountries.addAll(countries);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            String filterSeq = charSequence.toString().toLowerCase();
            FilterResults result = new FilterResults();
            if (!TextUtils.isEmpty(filterSeq)) {
                ArrayList<Country> filter = new ArrayList<>();
                for (Country country: mCountries) {
                    if (country.getName().toLowerCase().contains(filterSeq)) {
                        filter.add(country);
                    }
                }
                result.count = filter.size();
                result.values = filter;
            }
            else {
                synchronized (this) {
                    result.count = mCountries.size();
                    result.values = mCountries;
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            ArrayList<Country> filtered = (ArrayList<Country>) filterResults.values;
            notifyDataSetChanged();
            clear();
            if (filtered != null) {
                for (Country country: filtered) {
                    add(country);
                }
            }
            else {
                for (Country country: mCountries) {
                    add(country);
                }
            }
            notifyDataSetInvalidated();
        }
    }
}
