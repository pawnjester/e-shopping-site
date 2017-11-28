package co.loystar.loystarbusiness.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.CustomerDetailActivity;
import co.loystar.loystarbusiness.activities.CustomerListActivity;
import co.loystar.loystarbusiness.activities.EditCustomerDetailsActivity;
import co.loystar.loystarbusiness.activities.PaySubscriptionActivity;
import co.loystar.loystarbusiness.activities.RewardCustomersActivity;
import co.loystar.loystarbusiness.activities.SendSmsActivity;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.sync.AccountGeneral;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.entities.CustomerEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.SalesTransactionEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.CustomerDetailActivityEventBus;
import co.loystar.loystarbusiness.utils.TimeUtils;
import co.loystar.loystarbusiness.utils.ui.Currency.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.InternationalPhoneInput.InternationalPhoneInput;

/**
 * A fragment representing a single Customer detail screen.
 * This fragment is either contained in a {@link CustomerListActivity}
 * in two-pane mode (on tablets) or a {@link CustomerDetailActivity}
 * on handsets.
 */
public class CustomerDetailFragment extends Fragment {
    private static final String TAG = CustomerDetailFragment.class.getSimpleName();
    public static final String ARG_ITEM_ID = "item_id";
    private CustomerEntity mItem;
    private boolean mTwoPane;
    private String last_visit;
    private DatabaseManager mDatabaseManager;
    private SessionManager mSessionManager;

    public CustomerDetailFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDatabaseManager = DatabaseManager.getInstance(getActivity());
        mSessionManager = new SessionManager(getActivity());

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            DatabaseManager databaseManager = DatabaseManager.getInstance(getActivity());
            mItem = databaseManager.getCustomerById(getArguments().getInt(ARG_ITEM_ID, 0));

            Activity activity = this.getActivity();
            CollapsingToolbarLayout appBarLayout = activity.findViewById(R.id.customer_toolbar_layout);
            if (appBarLayout != null && mItem != null) {
                String fullCustomerName = mItem.getFirstName() + " " + mItem.getLastName();
                appBarLayout.setTitle(fullCustomerName);
            }

            View multiPaneView = activity.findViewById(R.id.customer_detail_container);
            if (multiPaneView != null && multiPaneView.getTag() != null && multiPaneView.getTag().toString().equals("multiPaneCustomerDetail")) {
                // The multiPaneView container view will be present only in the
                // large-screen layouts (res/values-w900dp).
                mTwoPane = true;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.customer_detail, container, false);
        if (mItem != null) {
            int customer_stamps = mDatabaseManager.getTotalCustomerStamps(
                    mSessionManager.getMerchantId(),
                    mItem.getId()
            );
            int customer_points = mDatabaseManager.getTotalCustomerPoints(
                    mSessionManager.getMerchantId(),
                    mItem.getId()
            );

            int amount_spent = mDatabaseManager.getTotalCustomerSpent(
                    mSessionManager.getMerchantId(),
                    mItem.getId()
            );
            MerchantEntity merchantEntity = mDatabaseManager.getMerchant(mSessionManager.getMerchantId());
            assert merchantEntity != null;
            SalesTransactionEntity lastTransaction = mDatabaseManager.getCustomerLastTransaction(
                    merchantEntity,
                    mItem
            );

            if (lastTransaction != null) {
                last_visit = TimeUtils.getTimeAgo(lastTransaction.getCreatedAt().getTime(), getActivity());
            }

            String tmt = "%s %s ";

            String currencySymbol = CurrenciesFetcher.getCurrencies(getContext()).getCurrency(mSessionManager.getCurrency()).getSymbol();

            String total_amount_text = String.format(tmt, currencySymbol, Math.round(amount_spent));
            if (mTwoPane) {
                String name = mItem.getFirstName() + " " + mItem.getLastName();
                ((TextView) rootView.findViewById(R.id.nameView)).setText(name);
                rootView.findViewById(R.id.editCustomerDetailBtn).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(getActivity(), EditCustomerDetailsActivity.class);
                        intent.putExtra(Constants.CUSTOMER_ID, mItem.getId());
                        startActivity(intent);
                    }
                });
            }

            InternationalPhoneInput phoneInput = rootView.findViewById(R.id.customerDetailPhone);
            phoneInput.setNumber(mItem.getPhoneNumber());
            phoneInput.setEnabled(false);

            if (last_visit != null && !last_visit.trim().isEmpty()) {
                rootView.findViewById(R.id.lastVisitWrapper).setVisibility(View.VISIBLE);
                ((TextView) rootView.findViewById(R.id.lastVisitValue)).setText(last_visit);
            }

            if (customer_stamps == 1) {
                ((TextView) rootView.findViewById(R.id.total_stamps_text)).setText(getString(R.string.stamp_label_text));
            }

            ((TextView) rootView.findViewById(R.id.total_stamps_value)).setText(String.valueOf(customer_stamps));
            ((TextView) rootView.findViewById(R.id.total_points_value)).setText(String.valueOf(customer_points));
            ((TextView) rootView.findViewById(R.id.total_amount_spent_value)).setText(total_amount_text);

            rootView.findViewById(R.id.messageBtn).setOnClickListener(v -> {
                if (!AccountGeneral.isAccountActive(getActivity())) {
                    new AlertDialog.Builder(getActivity())
                            .setTitle("Your Account Is Inactive")
                            .setMessage("SMS communications are disabled until you resubscribe.")
                            .setPositiveButton(getString(R.string.pay_subscription), (dialog, which) -> {
                                dialog.dismiss();
                                Intent intent = new Intent(getActivity(), PaySubscriptionActivity.class);
                                startActivity(intent);
                            })
                            .setNegativeButton(android.R.string.no, (dialog, which) -> dialog.dismiss())

                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                    return;
                }
                Intent msgIntent = new Intent(getActivity(), SendSmsActivity.class);
                msgIntent.putExtra(Constants.CUSTOMER_NAME, mItem.getFirstName());
                msgIntent.putExtra(Constants.PHONE_NUMBER, mItem.getPhoneNumber());
                startActivity(msgIntent);
            });

            rootView.findViewById(R.id.redeemBtn).setOnClickListener(v -> {
                Intent redeemIntent = new Intent(getActivity(), RewardCustomersActivity.class);
                redeemIntent.putExtra(Constants.CUSTOMER_ID, mItem.getId());
                startActivity(redeemIntent);
            });

            rootView.findViewById(R.id.sellBtn).setOnClickListener(v -> {
                CustomerDetailActivityEventBus
                        .getInstance()
                        .postFragmentAction(CustomerDetailActivityEventBus.ACTION_START_SALE);
            });
        }
        return rootView;
    }
}
