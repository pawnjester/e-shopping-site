package co.loystar.loystarbusiness.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.RecyclerViewOnItemClickListener;

/**
 * Created by laudbruce-tagoe on 5/19/16.
 */
public class DBUserArrayAdapter extends ArrayAdapter<DBCustomer>{
    private final Context context;
    private final ArrayList<DBCustomer> itemsArrayList;
    private RecyclerViewOnItemClickListener itemClickListener;

    public DBUserArrayAdapter(Context context, ArrayList<DBCustomer> itemsArrayList, RecyclerViewOnItemClickListener itemClickListener) {
        super(context, R.layout.search_activity_listview_row, itemsArrayList);
        this.context = context;
        this.itemsArrayList = itemsArrayList;
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        ViewHolder mViewHolder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.search_activity_listview_row, parent, false);

            mViewHolder = new ViewHolder();
            mViewHolder.labelView = (TextView) convertView.findViewById(R.id.rowTextView);
            mViewHolder.valueView = (TextView) convertView.findViewById(R.id.rowTextView2);

            convertView.setTag(mViewHolder);
        }
        else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (itemClickListener != null) {
                    itemClickListener.onItemClicked(position);
                }
            }
        });

        DBCustomer customer = itemsArrayList.get(position);
        String fullName = customer.getFirst_name() + " " + customer.getLast_name();
        mViewHolder.labelView.setText(fullName);
        mViewHolder.labelView.setTag(customer.getId());
        mViewHolder.valueView.setText(customer.getPhone_number());

        return convertView;
    }

    static class ViewHolder {
        private TextView labelView;
        private TextView valueView;
    }
}
