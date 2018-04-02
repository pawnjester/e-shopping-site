package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ordgen on 4/2/18.
 */

public abstract class BaseRecyclerViewAdapter<T>
    extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    LayoutInflater mInflater;
    List<T> mDataList;
    ItemClickListener mItemClickListener;

    BaseRecyclerViewAdapter(@NonNull Context context,
                            ItemClickListener itemClickListener) {
        mInflater = LayoutInflater.from(context);
        mItemClickListener = itemClickListener;
        mDataList = new ArrayList<>();
    }

    public void add(List<T> itemList) {
        mDataList.addAll(itemList);
        notifyDataSetChanged();
    }

    public void set(List<T> dataList) {
        List<T> clone = new ArrayList<>(dataList);
        mDataList.clear();
        mDataList.addAll(clone);
        notifyDataSetChanged();
    }

    public List<T> getDataList() {
        return mDataList;
    }

    public void clear() {
        mDataList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onLongItemClick(View view, int position);
    }
}
