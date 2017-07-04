package co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput;

import android.app.Dialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import java.io.Serializable;
import java.util.ArrayList;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 1/5/17.
 */

public class CurrencySearchableListDialog extends DialogFragment implements
        SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private static final String CURRENCIES = "currencies";
    private ListView mListViewItems;
    private CurrencySpinnerAdapter mListAdapter;
    private String mDialogTitle;

    private String mPositiveButtonText;

    private DialogInterface.OnClickListener mOnClickListener;

    private SearchableItem mSearchableItem;

    private OnSearchTextChanged mOnSearchTextChanged;

    private SearchView mSearchView;

    public static CurrencySearchableListDialog newInstance(ArrayList<Currency> currencies) {
        CurrencySearchableListDialog multiSelectExpandableFragment = new
                CurrencySearchableListDialog();

        Bundle args = new Bundle();
        args.putSerializable(CURRENCIES, currencies);

        multiSelectExpandableFragment.setArguments(args);

        return multiSelectExpandableFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_STATE_HIDDEN);
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater inflater = LayoutInflater.from(getActivity());

        if (null != savedInstanceState) {
            mSearchableItem = (SearchableItem) savedInstanceState.getSerializable("currency");
        }

        View rootView = inflater.inflate(R.layout.searchable_list_dialog, null);
        setData(rootView);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
        alertDialog.setView(rootView);

        String strPositiveButton = mPositiveButtonText == null ? "CLOSE" : mPositiveButtonText;
        alertDialog.setPositiveButton(strPositiveButton, mOnClickListener);

        String strTitle = mDialogTitle == null ? "Select Currency" : mDialogTitle;
        alertDialog.setTitle(strTitle);

        final AlertDialog dialog = alertDialog.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams
                    .SOFT_INPUT_STATE_HIDDEN);
        }
        return dialog;
    }

    private void setData(View rootView) {
        SearchManager searchManager = (SearchManager) getActivity().getSystemService(Context
                .SEARCH_SERVICE);

        mSearchView = (SearchView) rootView.findViewById(R.id.search);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName
                ()));
        mSearchView.setIconifiedByDefault(false);
        mSearchView.setOnQueryTextListener(this);
        mSearchView.setOnCloseListener(this);
        mSearchView.clearFocus();
        InputMethodManager mgr = (InputMethodManager) getActivity().getSystemService(Context
                .INPUT_METHOD_SERVICE);
        mgr.hideSoftInputFromWindow(mSearchView.getWindowToken(), 0);


        mListViewItems = (ListView) rootView.findViewById(R.id.listItems);

        mListAdapter = new CurrencySpinnerAdapter(getActivity(), 0, CurrenciesFetcher.getCurrencies(getActivity()));
        mListViewItems.setAdapter(mListAdapter);
        mListViewItems.setTag(20);

        mListViewItems.setTextFilterEnabled(true);

        mListViewItems.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @SuppressWarnings("unchecked")
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSearchableItem.onSearchableItemClicked(mListAdapter.getItem(position), position);
                getDialog().dismiss();
            }
        });
    }


    public void setTitle(String strTitle) {
        mDialogTitle = strTitle;
    }

    public void setPositiveButton(String strPositiveButtonText) {
        mPositiveButtonText = strPositiveButtonText;
    }

    public void setPositiveButton(String strPositiveButtonText, DialogInterface.OnClickListener onClickListener) {
        mPositiveButtonText = strPositiveButtonText;
        mOnClickListener = onClickListener;
    }

    public void setOnSearchableItemClickListener(SearchableItem searchableItem) {
        this.mSearchableItem = searchableItem;
    }

    public void setOnSearchTextChangedListener(OnSearchTextChanged onSearchTextChanged) {
        this.mOnSearchTextChanged = onSearchTextChanged;
    }

    @Override
    public boolean onClose() {
        return false;
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        mSearchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (TextUtils.isEmpty(newText)) {
            ((CurrencySpinnerAdapter) mListViewItems.getAdapter()).getFilter().filter(null);
        } else {
            ((CurrencySpinnerAdapter) mListViewItems.getAdapter()).getFilter().filter(newText);
        }
        if (null != mOnSearchTextChanged) {
            mOnSearchTextChanged.onSearchTextChanged(newText);
        }
        return true;
    }

    public CurrencySearchableListDialog() {};

    public interface SearchableItem<T> extends Serializable {
        void onSearchableItemClicked(T country, int position);
    }

    public interface OnSearchTextChanged {
        void onSearchTextChanged(String strText);
    }
}
