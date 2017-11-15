package co.loystar.loystarbusiness.utils;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;

/**
 * Created by ordgen on 11/15/17.
 */

public class CustomerAutoCompleteDialogAdapter extends BaseAdapter implements Filterable {
    private ArrayList<CustomerEntity> mCustomers;
    private Filter filter;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;
    private LayoutInflater mLayoutInflater;

    public CustomerAutoCompleteDialogAdapter(Context context, ArrayList<CustomerEntity> customerEntities) {
        mCustomers = customerEntities;
        mSessionManager = new SessionManager(context);
        mDatabaseManager = DatabaseManager.getInstance(context);
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mCustomers.size();
    }

    @Override
    public CustomerEntity getItem(int i) {
        return mCustomers.get(i);
    }

    @Override
    public long getItemId(int i) {
        return mCustomers.get(i).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder mViewHolder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.simple_dropdown_item_2line, parent, false);
            mViewHolder = new ViewHolder();
            mViewHolder.nameField = convertView.findViewById(R.id.name);
            mViewHolder.numberField = convertView.findViewById(R.id.number);

            convertView.setTag(mViewHolder);
        }
        else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        CustomerEntity customerEntity = getItem(position);
        if (customerEntity != null) {
            mViewHolder.nameField.setText(getItem(position).getFirstName());
            mViewHolder.numberField.setText(getItem(position).getPhoneNumber());
        }
        return convertView;
    }


    private static class ViewHolder {
        private TextView nameField;
        private TextView numberField;
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new CustomerFilter<CustomerEntity>(mCustomers);
        return filter;
    }

    private class CustomerFilter<T> extends Filter {
        private ArrayList<CustomerEntity> mUsers;

        CustomerFilter(ArrayList<CustomerEntity> customerEntities) {
            mUsers = new ArrayList<>();
            synchronized (this) {
                mUsers.addAll(customerEntities);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults result = new FilterResults();
            if (!TextUtils.isEmpty(charSequence.toString())) {
                List<CustomerEntity> iFilterList = mDatabaseManager.searchCustomersByNameOrNumber(
                        charSequence.toString(), mSessionManager.getMerchantId());
                result.count = iFilterList.size();
                result.values = iFilterList;
            }
            else {
                synchronized (this) {
                    result.count = mCustomers.size();
                    result.values = mCustomers;
                }
            }
            return result;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            List<CustomerEntity> filtered = (List<CustomerEntity>) filterResults.values;
            mCustomers.clear();
            notifyDataSetChanged();
            if (filtered != null) {
                mCustomers.addAll(filtered);

            }
            else {
                mCustomers.addAll(mUsers);
            }
            notifyDataSetInvalidated();
        }
    }
}
