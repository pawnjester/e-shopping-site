package co.loystar.loystarbusiness.activities;

import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.fragments.ProductCategoryDetailFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;

public class ProductCategoryDetailActivity extends AppCompatActivity {

    private ProductCategoryEntity mItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_productcategory_detail);
        Toolbar toolbar = findViewById(R.id.activity_product_category_detail_toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            DatabaseManager databaseManager = DatabaseManager.getInstance(this);
            mItem = databaseManager.getProductCategoryById(getIntent().getIntExtra(ProductCategoryDetailFragment.ARG_ITEM_ID, 0));
            if (mItem != null) {
                actionBar.setTitle(mItem.getName());
            }
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(AppCompatResources.getDrawable(this, R.drawable.ic_close_white_24px));
        }

        if (savedInstanceState == null) {
            Bundle arguments = new Bundle();
            arguments.putInt(ProductCategoryDetailFragment.ARG_ITEM_ID,
                    getIntent().getIntExtra(ProductCategoryDetailFragment.ARG_ITEM_ID, 0));
            ProductCategoryDetailFragment fragment = new ProductCategoryDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.product_category_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_with_icon, menu);
        return true;
    }

    /*@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, ProductCategoryListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }*/
}
