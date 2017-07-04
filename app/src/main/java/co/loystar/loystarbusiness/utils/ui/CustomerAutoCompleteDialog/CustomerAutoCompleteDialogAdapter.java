package co.loystar.loystarbusiness.utils.ui.CustomerAutoCompleteDialog;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.greenrobot.greendao.database.Database;
import org.w3c.dom.Text;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;

import static co.loystar.loystarbusiness.utils.TextUtilsHelper.isInteger;

/**
 * Created by laudbruce-tagoe on 3/26/17.
 */

public class CustomerAutoCompleteDialogAdapter extends BaseAdapter implements Filterable {
    private Context mContext;
    private ArrayList<DBCustomer> mCustomers;
    private Filter filter;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private SessionManager sessionManager;

    public CustomerAutoCompleteDialogAdapter(Context context, ArrayList<DBCustomer> mCustomers) {
        this.mContext = context;
        this.mCustomers = mCustomers;
        sessionManager = new SessionManager(context);
    }

    @Override
    public int getCount() {
        return mCustomers.size();
    }

    @Override
    public DBCustomer getItem(int i) {
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
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.simple_dropdown_item_2line, parent, false);
            mViewHolder = new ViewHolder();
            mViewHolder.nameField = (TextView) convertView.findViewById(R.id.name);
            mViewHolder.numberField = (TextView) convertView.findViewById(R.id.number);

            convertView.setTag(mViewHolder);
        }
        else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        mViewHolder.nameField.setText(getItem(position).getFirst_name());
        mViewHolder.numberField.setText(getItem(position).getPhone_number());
        return convertView;
    }


    static class ViewHolder {
        private TextView nameField;
        private TextView numberField;
    }

    @Override
    public Filter getFilter() {
        if (filter == null)
            filter = new CustomerFilter<DBCustomer>(mCustomers);
        return filter;
    }

    private class CustomerFilter<T> extends Filter {
        private ArrayList<DBCustomer> mUsers;

        CustomerFilter(ArrayList<DBCustomer> chips) {
            mUsers = new ArrayList<>();
            synchronized (this) {
                mUsers.addAll(chips);
            }
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            FilterResults result = new FilterResults();
            if (!TextUtils.isEmpty(charSequence.toString())) {
                ArrayList<DBCustomer> iFilterList = databaseHelper.searchCustomersByNameOrNumber(
                        charSequence.toString(), sessionManager.getMerchantId());
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
            ArrayList<DBCustomer> filtered = (ArrayList<DBCustomer>) filterResults.values;
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
