package co.loystar.loystarbusiness.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import co.loystar.loystarbusiness.R;

public class TransactionsConfirmation extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactions_confirmation);
        Toolbar toolbar = findViewById(R.id.transactions_confirmation_toolbar);
        setSupportActionBar(toolbar);
    }

}
