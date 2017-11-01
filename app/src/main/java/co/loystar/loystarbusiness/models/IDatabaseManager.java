package co.loystar.loystarbusiness.models;

import android.support.annotation.Nullable;

import co.loystar.loystarbusiness.models.entities.MerchantEntity;

/**
 * Created by ordgen on 11/1/17.
 */

public interface IDatabaseManager {
    @Nullable
    MerchantEntity getMerchant(int merchantId);
}
