package co.loystar.loystarbusiness.activities;

import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;

import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 3/15/18.
 */

public class BaseActivity extends RxAppCompatActivity {
    @Nullable
    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        bindViews();
    }

    protected void bindViews() {
        ButterKnife.bind(this);
        setupToolbar();
    }

    protected void setupToolbar() {
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }
    }

    @Nullable
    public Toolbar getToolbar() {
        return toolbar;
    }
}
