package co.loystar.loystarbusiness.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.adapters.SelectProgramAdapter;
import co.loystar.loystarbusiness.events.BusProvider;
import co.loystar.loystarbusiness.events.SelectProgramAdapterItemClickEvent;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.Buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;

/**
 * Created by ordgen on 7/4/17.
 */

public class SelectLoyaltyProgramForSales extends AppCompatActivity {
    private ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_loyalty_program_for_sales);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Context context = this;

        EmptyRecyclerView mRecyclerView = (EmptyRecyclerView) findViewById(R.id.rvChooseProgram);
        SessionManager sessionManager = new SessionManager(this);
        loyaltyPrograms = databaseHelper.listMerchantPrograms(sessionManager.getMerchantId());

        View emptyView = findViewById(R.id.select_program_empty_container);
        BrandButtonNormal addBtn = (BrandButtonNormal) emptyView.findViewById(R.id.select_program_empty_add_program);
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SelectLoyaltyProgramForSales.this, CreateLoyaltyProgramListActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
            }
        });

        SelectProgramAdapter selectProgramAdapter = new SelectProgramAdapter(loyaltyPrograms, context);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(llm);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setAdapter(selectProgramAdapter);
        mRecyclerView.setEmptyView(emptyView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BusProvider.getInstance().register(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        BusProvider.getInstance().unregister(this);
    }

    @Subscribe
    public void onItemClicked(SelectProgramAdapterItemClickEvent.OnItemClicked onItemClicked) {
        Bundle extras = new Bundle();
        extras.putLong(MerchantBackOffice.LOYALTY_PROGRAM_ID, loyaltyPrograms.get(onItemClicked.getAdapterPosition()).getId());
        Intent intent = new Intent();
        intent.putExtras(extras);
        setResult(RESULT_OK, intent);
        finish();
    }
}
