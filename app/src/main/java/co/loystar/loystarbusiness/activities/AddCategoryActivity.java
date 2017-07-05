package co.loystar.loystarbusiness.activities;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBProductCategory;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by ordgen on 7/4/17.
 */

public class AddCategoryActivity extends AppCompatActivity {
    private static final String TAG = AddCategoryActivity.class.getSimpleName();
    private View mLayout;
    private ProgressDialog progressDialog;
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private EditText addCategoryTextBox;
    private MenuItem done;
    private ApiClient mApiClient;
    private TextView charCounterView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);
        mLayout = findViewById(R.id.add_category_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(AppCompatResources.getDrawable(this, R.drawable.ic_close_white_24px));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mApiClient = new ApiClient(this);
        charCounterView = (TextView) findViewById(R.id.category_name_char_counter);
        addCategoryTextBox = (EditText) findViewById(R.id.add_category_text_box);
        addCategoryTextBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int i, int i1, int i2) {
                if (done != null && !done.isEnabled()) {
                    done.setEnabled(true);
                }

                String char_temp = "%s %s / %s";
                String char_temp_unit = s.length() == 1 ? "Character" : "Characters";
                String char_counter_text = String.format(char_temp, s.length(), char_temp_unit, 30);
                charCounterView.setText(char_counter_text);
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0 && !done.isEnabled()) {
                    if (done != null) {
                        done.setEnabled(true);
                    }
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_with_icon, menu);
        done = menu.findItem(R.id.action_done);
        done.setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (formIsDirty()) {
                    closeKeyBoard();
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddCategoryActivity.this);
                    builder.setTitle(R.string.discard_changes);
                    builder.setMessage(R.string.discard_changes_explain)
                            .setPositiveButton(R.string.discard, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                    onBackPressed();
                                }
                            })
                            .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.dismiss();
                                }
                            });
                    builder.show();
                }
                else {
                    onBackPressed();
                }
                return true;
            case R.id.action_done:
                if (formIsDirty()) {
                    closeKeyBoard();
                    submitForm();
                    return true;
                }
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void submitForm() {
        if (addCategoryTextBox.getText().toString().trim().isEmpty()) {
            Snackbar.make(mLayout, getString(R.string.error_category_name_required), Snackbar.LENGTH_LONG).show();
            return;
        }

        progressDialog = new ProgressDialog(AddCategoryActivity.this);
        progressDialog.setMessage(getString(R.string.a_moment));
        progressDialog.setIndeterminate(true);
        progressDialog.show();

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", addCategoryTextBox.getText().toString());
            JSONObject requestData = new JSONObject();
            requestData.put("data", jsonObject);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
            mApiClient.getLoystarApi().addProductCategory(requestBody).enqueue(new Callback<DBProductCategory>() {
                @Override
                public void onResponse(Call<DBProductCategory> call, Response<DBProductCategory> response) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    if (response.isSuccessful()) {
                        Long id = databaseHelper.insertProductCategory(response.body());
                        if (id != null) {
                            Intent intent = new Intent(AddCategoryActivity.this, ProductCategoriesActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.putExtra(getString(R.string.new_product_category_added), true);
                            startActivity(intent);
                        }
                        else {
                            Snackbar.make(mLayout, getString(R.string.error_add_category), Snackbar.LENGTH_LONG).show();
                        }
                    }
                    else {
                        Snackbar.make(mLayout, getString(R.string.error_add_category), Snackbar.LENGTH_LONG).show();
                    }

                }

                @Override
                public void onFailure(Call<DBProductCategory> call, Throwable t) {
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }

                    //Crashlytics.log(2, TAG, t.getMessage());
                    Snackbar.make(mLayout, getString(R.string.error_add_category), Snackbar.LENGTH_LONG).show();
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void closeKeyBoard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private boolean formIsDirty() {
        return !addCategoryTextBox.getText().toString().isEmpty();
    }
}
