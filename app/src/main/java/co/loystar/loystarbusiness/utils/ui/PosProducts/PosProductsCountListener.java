package co.loystar.loystarbusiness.utils.ui.PosProducts;

/**
 * Created by laudbruce-tagoe on 5/24/17.
 */

public interface PosProductsCountListener {
    void onCountChanged(Long entityId, int oldCount, int newCount);
}
