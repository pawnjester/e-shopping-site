package co.loystar.loystarbusiness.utils.ui.PosProducts;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;

/**
 * Created by laudbruce-tagoe on 5/24/17.
 */

public class PosProductsEntityFactory implements IPosProductsEntityFactory<PosProductEntity> {
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();

    @Override
    public ArrayList<PosProductEntity> getProductItems(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        ArrayList<DBProduct> products = databaseHelper.listMerchantProducts(sessionManager.getMerchantId());
        ArrayList<PosProductEntity> productEntities = new ArrayList<>();
        for (DBProduct product: products) {
            productEntities.add(new PosProductEntity(
                    product.getId(),
                    product.getName(),
                    product.getPicture(),
                    0,
                    product.getPrice())
            );
        }
        return productEntities;
    }

    @Override
    public RecyclerView.Adapter<? extends RecyclerView.ViewHolder> createAdapter(Context context, ArrayList<PosProductEntity> posProductEntities, PosProductsCountListener posProductsCountListener) {
        return new PosProductsGridViewAdapter(context, posProductEntities, posProductsCountListener);
    }
}
