package co.loystar.loystarbusiness.models.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.util.SparseIntArray;

public class SharedViewModel extends ViewModel {

    private MutableLiveData<Integer> customer = new MutableLiveData<>();
    private MutableLiveData<SparseIntArray> damn = new MutableLiveData<>();
    private MutableLiveData<Integer> deleted = new MutableLiveData<>();

    private MutableLiveData<SparseIntArray> selectedProducts = new MutableLiveData<>();

    public void setCustomer(Integer id) {
        customer.setValue(id);
    }

    public LiveData<Integer> getDeleted() {
        return deleted;
    }
    public LiveData<Integer> getCustomer() {
        return customer;
    }

    public LiveData<SparseIntArray> getDamn() {
        return damn;
    }

    public LiveData<SparseIntArray> getSelectedProducts() {
        return selectedProducts;
    }




    public void setDeleted(int set) {
        deleted.setValue(set);
    }

    public void setSelectedProducts(SparseIntArray selectedProduct) {
        selectedProducts.setValue(selectedProduct);
    }

    public void updateSelected(SparseIntArray selectedProduct) {
        damn.setValue(selectedProduct);
    }

    public void clearProducts() {
        selectedProducts.getValue().clear();
    }
}
