package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.InvoiceAdapter;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.Invoice;
import co.loystar.loystarbusiness.models.entities.InvoiceEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.utils.Constants;
import io.requery.Persistable;
import io.requery.query.Selection;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveResult;

public class InvoiceListActivity extends AppCompatActivity {

    private ReactiveEntityStore<Persistable> mDataStore;
    private MerchantEntity merchantEntity;
    private SessionManager mSessionManager;
    private Toolbar toolbar;

    private RecyclerView mRecyclerView;
    private InvoiceAdapter mAdapter;
    private int limit = 5;
    private int currentTotalItemsCount = 0;
    int nextLimit;
    View emptyView;

    ArrayList<InvoiceEntity> invoices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invoice_list);
        toolbar = findViewById(R.id.toolbar);

        toolbar.setTitle(getString(R.string.invoice));
        setSupportActionBar(toolbar);
        emptyView = findViewById(R.id.no_invoice_empty_view);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        actionbar.setDisplayShowHomeEnabled(true);
        mDataStore = DatabaseManager.getDataStore(this);
        mSessionManager = new SessionManager(this);
        merchantEntity = mDataStore.findByKey(MerchantEntity.class, mSessionManager.getMerchantId()).blockingGet();
        invoices = new ArrayList<>();

        invoices.addAll(getInvoices());

        if (invoices.size() == 0){
            emptyView.setVisibility(View.VISIBLE);
            TextView introText =  emptyView.findViewById(R.id.stateIntroText);
            introText.setText(getString(R.string.hello_text, mSessionManager.getFirstName()));
        } else {
            emptyView.setVisibility(View.GONE);
            mRecyclerView = findViewById(R.id.invoice_recyclerview);
            LinearLayoutManager mLayoutManager = new LinearLayoutManager(this,
                    LinearLayoutManager.VERTICAL, false);
            mRecyclerView.setLayoutManager(mLayoutManager);
            mAdapter = new InvoiceAdapter(this, invoices, this::showInvoiceActivity,
                    mRecyclerView);
            mRecyclerView.setHasFixedSize(true);
            mRecyclerView.setAdapter(mAdapter);
        }


        mDataStore.count(InvoiceEntity.class)
                .where(InvoiceEntity.OWNER.eq(merchantEntity))
                .and(InvoiceEntity.NUMBER.notNull())
                .get()
                .consume(totalItems -> {
                    ActionBar actionBar = getSupportActionBar();
                    if (actionBar != null) {
                        if (totalItems == 1) {
                            actionBar.setTitle(getString(R.string.invoice_count, "1"));
                        } else if (totalItems < 1){
                            actionBar.setTitle("Invoice");
                        }
                        else {
                            actionBar.setTitle(getString(R.string.invoices_count, String.valueOf(totalItems)));
                        }
                        actionBar.setDisplayShowHomeEnabled(true);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        Intent newIntent = new Intent(this, MerchantBackOfficeActivity.class);
        newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_CLEAR_TOP );
        startActivity(newIntent);
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
        invoiceIntent.putExtra(Constants.INVOICE_NUMBER, invoice.getNumber());
        invoiceIntent.putExtra(Constants.INVOICE_MESSAGE, invoice.getPaymentMessage());
        invoiceIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(invoiceIntent);
    }

    private ArrayList<InvoiceEntity> getInvoices() {
        return getInitialInvoiceData();
    }

    private ArrayList<InvoiceEntity> getInitialInvoiceData() {
        Selection<ReactiveResult<InvoiceEntity>> invoiceSelection = mDataStore.select(InvoiceEntity.class);
        invoiceSelection.where(InvoiceEntity.OWNER.eq(merchantEntity));
        invoiceSelection.where(InvoiceEntity.NUMBER.notNull());
        invoiceSelection.orderBy(InvoiceEntity.CREATED_AT.upper().desc());
//        invoiceSelection.limit(limit);

        return new ArrayList<>(invoiceSelection.get().toList());
    }

    private void loadMoreInvoices() {
        ArrayList<InvoiceEntity> nextEntities;
        Selection<ReactiveResult<InvoiceEntity>> invoiceSelection = mDataStore.select(InvoiceEntity.class);
        invoiceSelection.where(InvoiceEntity.OWNER.eq(merchantEntity));
        invoiceSelection.where(InvoiceEntity.NUMBER.notNull());
        invoiceSelection.orderBy(InvoiceEntity.CREATED_AT.upper().desc());
        invoiceSelection.limit(nextLimit + limit);
        nextEntities = new ArrayList<>(invoiceSelection.get().toList());
        mAdapter.set(nextEntities);
    }
}
