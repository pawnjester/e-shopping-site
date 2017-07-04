package co.loystar.loystarbusiness.models.db;

import org.greenrobot.greendao.annotation.*;


/**
 * Entity mapped to table "DBCONFIG".
 */
@Entity
public class DBConfig {
    private Long merchant_id;
    private Boolean dbs_migrated_to_single_db;

    @Generated(hash = 1773307525)
    public DBConfig() {
    }

    @Generated(hash = 507973708)
    public DBConfig(Long merchant_id, Boolean dbs_migrated_to_single_db) {
        this.merchant_id = merchant_id;
        this.dbs_migrated_to_single_db = dbs_migrated_to_single_db;
    }

    public Long getMerchant_id() {
        return merchant_id;
    }

    public void setMerchant_id(Long merchant_id) {
        this.merchant_id = merchant_id;
    }

    public Boolean getDbs_migrated_to_single_db() {
        return dbs_migrated_to_single_db;
    }

    public void setDbs_migrated_to_single_db(Boolean dbs_migrated_to_single_db) {
        this.dbs_migrated_to_single_db = dbs_migrated_to_single_db;
    }

}
