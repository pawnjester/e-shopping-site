package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.InvoiceAdapter;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.Invoice;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EndlessRecyclerViewScrollListener;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;
import kotlin.collections.IndexedValue;

public class InvoiceListActivity extends AppCompatActivity {

    private ReactiveEntityStore<Persistable> mDataStore;
    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    private Toolbar toolbar;

    private RecyclerView mRecyclerView;
    private InvoiceAdapter mAdapter;
    private int limit = 40;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_list);
        toolbar = findViewById(R.id.toolbar);

        toolbar.setTitle(getString(R.string.invoice));
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setDisplayShowHomeEnabled(true);
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();

        mRecyclerView = findViewById(R.id.invoice_recyclerview);
        ArrayList<InvoiceEntity> list = getInvoices();
        mAdapter = new InvoiceAdapter(this, getInvoices(), this::showInvoiceActivity);
        setupRecyclerView(mRecyclerView);
        getInvoices();


    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void showInvoiceActivity(Invoice invoice) {
        Intent invoiceIntent = new Intent(this, InvoicePayActivity.class);
        invoiceIntent.putExtra(Constants.CUSTOMER_ID, invoice.getCustomer().getId());
        invoiceIntent.putExtra(Constants.CHARGE, Double.parseDouble(invoice.getAmount()));
        invoiceIntent.putExtra(Constants.INVOICE_ID, invoice.getId());
        invoiceIntent.putExtra(Constants.PAID_AMOUNT, invoice.getPaidAmount());
        invoiceIntent.putExtra(Constants.PAYMENT_METHOD, invoice.getPaymentMethod());
        invoiceIntent.putExtra(Constants.STATUS, invoice.getStatus());
        startActivity(invoiceIntent);
    }

    private ArrayList<InvoiceEntity> getInvoices() {
        return getInitialInvoiceData();
    }

    public void setupRecyclerView(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
        mRecyclerView.setHasFixedSize(true);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(mAdapter);
    }

    private ArrayList<InvoiceEntity> getInitialInvoiceData() {
        Selection<ReactiveResult<InvoiceEntity>> invoiceSelection = mDataStore.select(InvoiceEntity.class);
        invoiceSelection.where(InvoiceEntity.OWNER.eq(merchantEntity));
        invoiceSelection.orderBy(InvoiceEntity.CREATED_AT.upper().desc());
        invoiceSelection.limit(limit);

        return new ArrayList<>(invoiceSelection.get().toList());
    }
}
