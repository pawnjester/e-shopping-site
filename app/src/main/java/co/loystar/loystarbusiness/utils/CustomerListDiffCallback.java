package co.loystar.loystarbusiness.utils;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

import co.loystar.loystarbusiness.models.entities.CustomerEntity;

/**
 * Created by ordgen on 4/2/18.
 */

public class CustomerListDiffCallback extends DiffUtil.Callback {

    private List<CustomerEntity> oldCustomers;
    private List<CustomerEntity> newCustomers;

    public CustomerListDiffCallback(List<CustomerEntity> newCustomers, List<CustomerEntity> oldCustomers) {
        this.newCustomers = newCustomers;
        this.oldCustomers = oldCustomers;
    }

    @Override
    public int getOldListSize() {
        return oldCustomers.size();
    }

    @Override
    public int getNewListSize() {
        return newCustomers.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldCustomers.get(oldItemPosition).getId() == newCustomers.get(newItemPosition).getId();
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldCustomers.get(oldItemPosition).equals(newCustomers.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        //you can return particular field for changed item.
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
