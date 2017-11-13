package co.loystar.loystarbusiness.utils;

import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;

/**
 * Created by ordgen on 11/12/17.
 */

public class BindingHolder <B extends ViewDataBinding> extends RecyclerView.ViewHolder {
    public final B binding;

    public BindingHolder(B binding) {
        super(binding.getRoot());
        this.binding = binding;
    }
}
