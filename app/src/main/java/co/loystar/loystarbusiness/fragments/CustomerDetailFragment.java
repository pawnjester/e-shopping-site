package co.loystar.loystarbusiness.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.activities.AddStampsActivity;
import co.loystar.loystarbusiness.activities.CreateLoyaltyProgramListActivity;
import co.loystar.loystarbusiness.activities.MerchantBackOffice;
import co.loystar.loystarbusiness.activities.PaySubscription;
import co.loystar.loystarbusiness.activities.RecordDirectSalesActivity;
import co.loystar.loystarbusiness.activities.RecordPointsSalesWithPosActivity;
import co.loystar.loystarbusiness.activities.RecordStampsSalesWithPosActivity;
import co.loystar.loystarbusiness.activities.RewardCustomersActivity;
import co.loystar.loystarbusiness.activities.SelectLoyaltyProgramForSales;
import co.loystar.loystarbusiness.activities.SendSMS;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBCustomer;
import co.loystar.loystarbusiness.models.db.DBMerchant;
import co.loystar.loystarbusiness.models.db.DBMerchantLoyaltyProgram;
import co.loystar.loystarbusiness.models.db.DBTransaction;
import co.loystar.loystarbusiness.sync.AccountGeneral;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.TimeUtils;
import co.loystar.loystarbusiness.utils.ui.IntlCurrencyInput.CurrenciesFetcher;
import co.loystar.loystarbusiness.utils.ui.IntlPhoneInput.IntlPhoneInput;

import static android.app.Activity.RESULT_OK;

/**
 * Created by ordgen on 7/4/17.
 */

public class CustomerDetailFragment extends Fragment {

    /*constants*/
    public static final String ARG_ITEM_ID = "item_id";
    public static final int RECORD_SALES_WITH_POS_CHOOSE_PROGRAM = 130;
    public static final int RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM = 150;

    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private List<DBTransaction> transactions = new ArrayList<>();
    private OnCustomerDetailInteractionListener mListener;
    private String last_visit;
    private DBMerchant merchant;
    private SessionManager sessionManager;
    private boolean mTwoPane;
    public static DBCustomer mItem;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public CustomerDetailFragment() {}

    public static CustomerDetailFragment newInstance() {
        return new CustomerDetailFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(getActivity());
        merchant = databaseHelper.getMerchantById(sessionManager.getMerchantId());
        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItem = databaseHelper.getCustomerById(getArguments().getLong(ARG_ITEM_ID, 0L));
        }

        Activity activity = this.getActivity();
        if (activity != null) {
            View view = activity.findViewById(R.id.customer_detail_container);
            if (view != null && view.getTag() != null && view.getTag().toString().equals("multiPaneCustomerDetail")) {
                // The detail container view will be present only in the
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
            int customer_stamps = databaseHelper.getTotalUserStampsForMerchant(mItem.getUser_id(), sessionManager.getMerchantId());
            int customer_points = databaseHelper.getTotalUserPointsForMerchant(mItem.getUser_id(), sessionManager.getMerchantId());
            int total_amount_spent = databaseHelper.getTotalAmountSpentByUserForMerchant(mItem.getUser_id(), sessionManager.getMerchantId());

            ArrayList<DBTransaction> getTransactions = databaseHelper.getAllUserTransactions(mItem.getUser_id(), sessionManager.getMerchantId());
            for (DBTransaction tr: getTransactions) {
                if (tr.getLocal_db_created_at() == null && tr.getCreated_at() != null) {
                    tr.setLocal_db_created_at(tr.getCreated_at());
                    databaseHelper.updateTransaction(tr);
                    transactions.add(tr);
                }
                else if (tr.getLocal_db_created_at() != null){
                    transactions.add(tr);
                }
            }

            if (transactions != null && transactions.size() > 0) {
                Collections.sort(transactions, new TransactionDateComparator());

                Date lastTransactionDate = transactions.get(transactions.size() - 1).getLocal_db_created_at();
                last_visit = TimeUtils.getTimeAgo(lastTransactionDate.getTime(), getActivity());
            }
            String tmt = "%s %s ";

            String currencySymbol = CurrenciesFetcher.getCurrencies(getContext()).getCurrency(sessionManager.getMerchantCurrency()).getSymbol();

            String total_amount_text = String.format(tmt, currencySymbol, Math.round(total_amount_spent));
            if (mTwoPane) {
                ((TextView) rootView.findViewById(R.id.first_name)).setText(mItem.getFirst_name());
                ((TextView) rootView.findViewById(R.id.last_name)).setText(mItem.getLast_name());
                rootView.findViewById(R.id.edit_action).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mListener.onCustomerDetailInteraction(mItem.getId());
                    }
                });
            }

            IntlPhoneInput phoneInput = (IntlPhoneInput) rootView.findViewById(R.id.phone_number);
            phoneInput.setNumber(mItem.getPhone_number());
            phoneInput.setEnabled(false);

            if (last_visit != null && !last_visit.trim().isEmpty()) {
                rootView.findViewById(R.id.last_visit_bloc).setVisibility(View.VISIBLE);
                ((TextView) rootView.findViewById(R.id.last_visit)).setText(last_visit);
            }

            if (customer_stamps == 1) {
                ((TextView) rootView.findViewById(R.id.total_stamps_text)).setText(getString(R.string.stamp_label_text));
            }

            ((TextView) rootView.findViewById(R.id.total_stamps_value)).setText(String.valueOf(customer_stamps));
            ((TextView) rootView.findViewById(R.id.total_points_value)).setText(String.valueOf(customer_points));
            ((TextView) rootView.findViewById(R.id.total_amount_spent_value)).setText(total_amount_text);

            rootView.findViewById(R.id.record_sale).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean isPosTurnedOn = merchant.getTurn_on_point_of_sale() != null
                            && merchant.getTurn_on_point_of_sale();
                    ArrayList<DBMerchantLoyaltyProgram> loyaltyPrograms = databaseHelper.listMerchantPrograms(sessionManager.getMerchantId());

                    if (loyaltyPrograms.isEmpty()) {
                        Intent intent = new Intent(getActivity(), CreateLoyaltyProgramListActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                    }
                    else {
                        if (loyaltyPrograms.size() == 1) {
                            DBMerchantLoyaltyProgram loyaltyProgram = loyaltyPrograms.get(0);
                            if (isPosTurnedOn) {
                                if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                                    Intent intent = new Intent(getActivity(), RecordPointsSalesWithPosActivity.class);
                                    intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                    intent.putExtra(RecordDirectSalesActivity.CUSTOMER_ID, mItem.getId());
                                    startActivity(intent);
                                }
                                else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                                    Intent intent = new Intent(getActivity(), RecordStampsSalesWithPosActivity.class);
                                    intent.putExtra(RecordDirectSalesActivity.CUSTOMER_ID, mItem.getId());
                                    intent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                    startActivity(intent);
                                }
                            }
                            else {
                                if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                                    Bundle extras = new Bundle();
                                    extras.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                    extras.putString(RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE, getString(R.string.simple_points));
                                    extras.putLong(RecordDirectSalesActivity.CUSTOMER_ID, mItem.getId());
                                    extras.putBoolean(RecordDirectSalesActivity.START_ADD_POINTS_FRAGMENT, true);

                                    Intent intent = new Intent(getActivity(), RecordDirectSalesActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtras(extras);
                                    startActivity(intent);
                                }
                                else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                                    Intent addStampsIntent = new Intent(getActivity(), AddStampsActivity.class);
                                    addStampsIntent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, loyaltyProgram.getId());
                                    addStampsIntent.putExtra(RecordDirectSalesActivity.CUSTOMER_ID, mItem.getId());
                                    startActivity(addStampsIntent);
                                }
                            }
                        }
                        else {
                            if (merchant.getTurn_on_point_of_sale() != null && merchant.getTurn_on_point_of_sale()) {
                                Intent chooseProgram = new Intent(getActivity(), SelectLoyaltyProgramForSales.class);
                                getActivity().startActivityForResult(chooseProgram, RECORD_SALES_WITH_POS_CHOOSE_PROGRAM);
                            }
                            else {
                                Intent chooseProgram = new Intent(getActivity(), SelectLoyaltyProgramForSales.class);
                                getActivity().startActivityForResult(chooseProgram, RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM);
                            }
                        }
                    }
                }
            });

            rootView.findViewById(R.id.message_me).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!AccountGeneral.merchantAccountIsActive()) {
                        new AlertDialog.Builder(getActivity())
                                .setTitle("Your Account Is Inactive")
                                .setMessage("SMS communications are disabled until you resubscribe.")
                                .setPositiveButton(getString(R.string.pay_subscription), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        Intent intent = new Intent(LoystarApplication.getInstance(), PaySubscription.class);
                                        startActivity(intent);
                                    }
                                })
                                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })

                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .show();
                        return;
                    }
                    Intent msgIntent = new Intent(getActivity(), SendSMS.class);
                    msgIntent.putExtra(MerchantBackOffice.CUSTOMER_NAME, mItem.getFirst_name());
                    msgIntent.putExtra("merchantEmail", sessionManager.getMerchantEmail());
                    msgIntent.putExtra(MerchantBackOffice.CUSTOMER_PHONE_NUMBER, mItem.getPhone_number());
                    msgIntent.putExtra(MerchantBackOffice.CUSTOMER_ID, mItem.getId().toString());
                    startActivity(msgIntent);
                }
            });

            rootView.findViewById(R.id.redeemBtn).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent redeemIntent = new Intent(getActivity(), RewardCustomersActivity.class);
                    redeemIntent.putExtra(RewardCustomersActivity.CUSTOMER_ID, mItem.getId());
                    startActivity(redeemIntent);
                }
            });
        }

        return rootView;
    }


    public class TransactionDateComparator implements Comparator<DBTransaction> {
        @Override
        public int compare(DBTransaction o1, DBTransaction o2) {
            return o1.getLocal_db_created_at().compareTo(o2.getLocal_db_created_at());
        }
    }

    public interface OnCustomerDetailInteractionListener {
        void onCustomerDetailInteraction(Long customerId);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCustomerDetailInteractionListener) {
            mListener = (OnCustomerDetailInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCustomerDetailInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RECORD_SALES_WITH_POS_CHOOSE_PROGRAM) {
            if (resultCode == RESULT_OK) {
                Bundle extras = new Bundle();
                Long programId = data.getExtras().getLong(MerchantBackOffice.LOYALTY_PROGRAM_ID, 0L);
                DBMerchantLoyaltyProgram loyaltyProgram = databaseHelper.getProgramById(programId, sessionManager.getMerchantId());

                if (loyaltyProgram != null) {
                    extras.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, programId);
                    extras.putLong(RecordDirectSalesActivity.CUSTOMER_ID, mItem.getId());

                    if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                        Intent recordSalesIntent = new Intent(getActivity(), RecordPointsSalesWithPosActivity.class);
                        recordSalesIntent.putExtras(extras);
                        startActivity(recordSalesIntent);
                    }

                    else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                        Intent recordSalesIntent = new Intent(getActivity(), RecordStampsSalesWithPosActivity.class);
                        recordSalesIntent.putExtras(extras);
                        startActivity(recordSalesIntent);
                    }
                }
            }
        }
        else if (requestCode == RECORD_SALES_WITHOUT_POS_CHOOSE_PROGRAM) {
            if (resultCode == RESULT_OK) {
                Long programId = data.getExtras().getLong(MerchantBackOffice.LOYALTY_PROGRAM_ID, 0L);
                DBMerchantLoyaltyProgram loyaltyProgram = databaseHelper.getProgramById(programId, sessionManager.getMerchantId());

                if (loyaltyProgram != null) {

                    if (loyaltyProgram.getProgram_type().equals(getString(R.string.simple_points))) {
                        Bundle extras = new Bundle();
                        extras.putLong(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, programId);
                        extras.putString(RecordDirectSalesActivity.LOYALTY_PROGRAM_TYPE, getString(R.string.simple_points));
                        extras.putLong(RecordDirectSalesActivity.CUSTOMER_ID, mItem.getId());
                        extras.putBoolean(RecordDirectSalesActivity.START_ADD_POINTS_FRAGMENT, true);

                        Intent intent = new Intent(getActivity(), RecordDirectSalesActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        intent.putExtras(extras);
                        startActivity(intent);
                    }

                    else if (loyaltyProgram.getProgram_type().equals(getString(R.string.stamps_program))) {
                        Intent addStampsIntent = new Intent(getActivity(), AddStampsActivity.class);
                        addStampsIntent.putExtra(RecordDirectSalesActivity.LOYALTY_PROGRAM_ID, programId);
                        addStampsIntent.putExtra(RecordDirectSalesActivity.CUSTOMER_ID, mItem.getId());
                        startActivity(addStampsIntent);
                    }
                }
            }
        }
    }
}
