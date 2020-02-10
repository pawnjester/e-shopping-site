package co.loystar.loystarbusiness.activities;

import android.arch.lifecycle.ViewModelProvider;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.concurrent.ExecutorService;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.databinding.OrderSummaryItemBinding;
import co.loystar.loystarbusiness.databinding.PosProductItemBinding;
import co.loystar.loystarbusiness.fragments.OnBackPressed;
import co.loystar.loystarbusiness.fragments.ProductListFragment;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.viewmodel.SharedViewModel;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides.EmptyRecyclerView;
import co.loystar.loystarbusiness.utils.ui.buttons.BrandButtonNormal;
import co.loystar.loystarbusiness.utils.ui.buttons.CartCountButton;
import co.loystar.loystarbusiness.utils.ui.buttons.FullRectangleButton;
import co.loystar.loystarbusiness.utils.ui.dialogs.CustomerAutoCompleteDialog;
import co.loystar.loystarbusiness.utils.ui.dialogs.MyAlertDialog;
import co.loystar.loystarbusiness.utils.ui.dialogs.PayOptionsDialog;
import io.requery.Persistable;
import io.requery.reactivex.ReactiveEntityStore;

public class SaleWithPosActivity extends BaseActivity{

    public static final String TAG = SaleWithPosActivity.class.getSimpleName();


    private int customerId;
    private View mLayout;
    private boolean isDualPane = false;
    private SharedViewModel viewModel;
    FragmentTransaction transaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sale_with_pos);

        boolean yes = getResources().getBoolean(R.bool.is_tablet);
        if (!yes) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        customerId = getIntent().getIntExtra(Constants.CUSTOMER_ID, 0);
        viewModel = ViewModelProviders.of(this).get(SharedViewModel.class);
        viewModel.setCustomer(customerId);


        mLayout = findViewById(R.id.activity_sale_with_pos_container);

        int orientation = getResources().getConfiguration().orientation;
        Log.e("jjj", orientation +"");
        if (orientation == Configuration.ORIENTATION_LANDSCAPE && yes) {
            transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_checkout_container, CheckoutFragment.newInstance());
            transaction.commit();
        }
        View fragmentCheckout = findViewById(R.id.fragment_checkout_container);
        isDualPane = fragmentCheckout != null && fragmentCheckout.getVisibility() == View.VISIBLE;
        transaction = getSupportFragmentManager().beginTransaction();
        Bundle bundle = new Bundle();
        bundle.putBoolean("isDual", isDualPane);
        ProductListFragment listFragment = new ProductListFragment();
        listFragment.setArguments(bundle);
        transaction.replace(R.id.fragment_product_container, listFragment , "PRODUCTLIST");
        transaction.commit();

        boolean productCreatedIntent = getIntent().getBooleanExtra(getString(R.string.product_create_success), false);
        if (productCreatedIntent) {
            Snackbar.make(mLayout, getString(R.string.product_create_success), Snackbar.LENGTH_LONG).show();
        }


    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }


    @Override
    public void onBackPressed() {
        if (!isDualPane) {
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for(Fragment f : fragments){
                if(f instanceof ProductListFragment)
                    ((ProductListFragment)f).onBackPressed();
            }
        } else {
            super.onBackPressed();
        }
    }
}
