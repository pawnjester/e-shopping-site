package co.loystar.loystarbusiness.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.SalesOrderDetailFragment;

public class SalesOrderDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sales_order_detail);
        Toolbar toolbar = findViewById(R.id.activity_sales_order_detail_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putInt(SalesOrderDetailFragment.ARG_ITEM_ID,
                getIntent().getIntExtra(SalesOrderDetailFragment.ARG_ITEM_ID, 0));
            SalesOrderDetailFragment fragment = new SalesOrderDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                .add(R.id.sales_order_detail_container, fragment)
                .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            navigateUpTo(new Intent(this, SalesOrderListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
