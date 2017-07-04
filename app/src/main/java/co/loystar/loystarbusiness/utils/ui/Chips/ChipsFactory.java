package co.loystar.loystarbusiness.utils.ui.Chips;

import android.content.Context;
import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;

import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBProductCategory;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;

/**
 * Created by laudbruce-tagoe on 3/18/17.
 */

public class ChipsFactory implements IItemsFactory<ChipsEntity> {
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();

    @Override
    public ArrayList<ChipsEntity> getCategoryItems(Context context) {
        SessionManager sessionManager = new SessionManager(context);
        ArrayList<DBProductCategory> categories = databaseHelper.listMerchantProductCategories(sessionManager.getMerchantId());
        ArrayList<ChipsEntity> chipsList = new ArrayList<>();
        for (DBProductCategory category: categories) {
            chipsList.add(ChipsEntity.newBuilder()
                    .name(category.getName())
                    .entityId(category.getId())
                    .build());
        }
        return chipsList;
    }

    @Override
    public RecyclerView.Adapter<? extends RecyclerView.ViewHolder> createAdapter(ArrayList<ChipsEntity> chipsEntities, ChipsOnRemoveListener chipsOnRemoveListener) {
        return new ChipsAdapter(chipsEntities, chipsOnRemoveListener);
    }
}
