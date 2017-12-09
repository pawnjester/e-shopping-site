package co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
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
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.DividerItemDecoration;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerTouchListener;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.SpacingItemDecoration;

/**
 * Created by ordgen on 11/13/17.
 */

public class CountryPhoneSpinnerDialog extends AppCompatDialogFragment implements
        SearchView.OnQueryTextListener {
    public static final String TAG = CountryPhoneSpinnerDialog.class.getSimpleName();
    private RecyclerView mRecyclerView;
    private CountryPhoneSpinnerDialogAdapter mAdapter;
    private CountriesFetcher.CountryList mCountryList;
    private OnItemSelectedListener mListener;

    public static CountryPhoneSpinnerDialog newInstance() {
        return new CountryPhoneSpinnerDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mCountryList = CountriesFetcher.getCountries(getActivity());
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        @SuppressLint("InflateParams") View rootView = inflater.inflate(R.layout.searchable_list_dialog, null);

        if (getActivity() == null) {
            return getDialog();
        }

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

        mAdapter = new CountryPhoneSpinnerDialogAdapter(getActivity(), mCountryList);
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
                    mListener.onCountryItemSelected(mAdapter.mCountries.get(position));
                }
                ((CountryPhoneSpinnerDialogAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
                getDialog().cancel();
            }

            @Override
            public void onLongClick(View view, int position) {
            }
        }));

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setView(rootView);
        alertDialog.setTitle("Select country");
        alertDialog.setPositiveButton("CLOSE", (dialogInterface, i) -> {
            dialogInterface.cancel();
            ((CountryPhoneSpinnerDialogAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
            getDialog().cancel();
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
            ((CountryPhoneSpinnerDialogAdapter) mRecyclerView.getAdapter()).getFilter().filter(null);
        } else {
            ((CountryPhoneSpinnerDialogAdapter) mRecyclerView.getAdapter()).getFilter().filter(newText);
        }
        return true;
    }

    public interface OnItemSelectedListener {
        void onCountryItemSelected(Country country);
    }

    public void setListener(OnItemSelectedListener listener) {
        mListener = listener;
    }

    private class CountryPhoneSpinnerDialogAdapter extends RecyclerView.Adapter<CountryPhoneSpinnerDialogAdapter.ViewHolder> implements Filterable {
        private ArrayList<Country> mCountries;
        private Filter filter;
        private Context context;

        CountryPhoneSpinnerDialogAdapter(Context context, ArrayList<Country> countries) {
            mCountries = countries;
            this.context = context;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            private ImageView mImageView;
            private TextView mNameView;
            private TextView mDialCode;

            ViewHolder(View itemView) {
                super(itemView);
                mImageView = itemView.findViewById(R.id.international_phone_input_country_flag);
                mNameView = itemView.findViewById(R.id.international_phone_input_country_name);
                mDialCode = itemView.findViewById(R.id.international_phone_input_country_dial_code);
            }
        }

        @Override
        public CountryPhoneSpinnerDialogAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.country_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CountryPhoneSpinnerDialogAdapter.ViewHolder holder, int position) {
            Country country = mCountries.get(position);
            holder.mImageView.setImageResource(getFlagResource(context, country));
            holder.mNameView.setText(country.getName());
            holder.mDialCode.setText(String.format("+%s", country.getDialCode()));
        }

        @Override
        public int getItemCount() {
            return mCountries.size();
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter =  new CountryFilter(mCountryList);
            }
            return filter;
        }

        /**
         * Fetch flag resource by Country
         *
         * @param context Context
         * @param country Country
         * @return int of resource | 0 value if not exists
         */
        private int getFlagResource(Context context, Country country) {
            return getResources().getIdentifier("country_" + country.getIso().toLowerCase(), "drawable", context.getPackageName());
        }

        private class CountryFilter extends Filter {

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
                if (TextUtils.isEmpty(filterSeq)) {
                    synchronized (this) {
                        result.count = mCountries.size();
                        result.values = mCountries;
                    }
                }
                else {
                    ArrayList<Country> filter = new ArrayList<>();
                    for (Country country: mCountries) {
                        if (country.getName().toLowerCase().contains(filterSeq)) {
                            filter.add(country);
                        }
                    }
                    result.count = filter.size();
                    result.values = filter;
                }
                return result;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                ArrayList<Country> filtered = (ArrayList<Country>) filterResults.values;
                if (filtered != null) {
                    animateTo(filtered);
                }
                else {
                    animateTo(mCountries);
                }
            }

            void animateTo(ArrayList<Country> countries) {
                applyAndAnimateRemovals(countries);
                applyAndAnimateAdditions(countries);
                applyAndAnimateMovedItems(countries);
            }

            private void applyAndAnimateRemovals(ArrayList<Country> newList) {
                for (int i = mCountryList.size() - 1; i >= 0; i--) {
                    final Country country = mCountryList.get(i);
                    if (!newList.contains(country)) {
                        removeItem(i);
                    }
                }
            }

            private void applyAndAnimateAdditions(ArrayList<Country> newList) {
                for (int i = 0, count = newList.size(); i < count; i++) {
                    final Country country = newList.get(i);
                    if (!mCountryList.contains(country)) {
                        addItem(i, country);
                    }
                }
            }

            private void applyAndAnimateMovedItems(ArrayList<Country> newList) {
                for (int toPosition = newList.size() - 1; toPosition >= 0; toPosition--) {
                    final Country country = newList.get(toPosition);
                    final int fromPosition = mCountryList.indexOf(country);
                    if (fromPosition >= 0 && fromPosition != toPosition) {
                        moveItem(fromPosition, toPosition);
                    }
                }
            }

            void removeItem(int position) {
                mCountryList.remove(position);
                notifyItemRemoved(position);
            }

            void addItem(int position, Country country) {
                mCountryList.add(position, country);
                notifyItemInserted(position);
            }

            void moveItem(int fromPosition, int toPosition) {
                final Country country = mCountryList.remove(fromPosition);
                mCountryList.add(toPosition, country);
                notifyItemMoved(fromPosition, toPosition);
            }
        }
    }
}
