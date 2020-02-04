package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.clans.fab.FloatingActionButton;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.sql.Timestamp;
import java.util.List;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Product;
import co.loystar.loystarbusiness.models.databinders.ProductCategory;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.MerchantEntity;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.Constants;
import co.loystar.loystarbusiness.utils.FileUtils;
import co.loystar.loystarbusiness.utils.RequestBodyWithProgress;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;
import id.zelory.compressor.Compressor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@RuntimePermissions
public class AddProductActivity extends BaseActivity {
    /*static fields*/
    private static final int REQUEST_IMAGE_CAPTURE = 111;
    private static final int REQUEST_IMAGE_FROM_GALLERY = 133;

    /*views*/
    private ImageView thumbnailView;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private LinearLayout removePictureLayout;
    private View addFromGalleryLayout;
    private View takePictureLayout;
    private FloatingActionButton baseFloatBtn;
    private FloatingActionButton addFromGalleryBtn;
    private FloatingActionButton takePictureBtn;
    private FloatingActionButton removePictureBtn;
    private View mLayout;
    private EditText productNameView;
    private CurrencyEditText productPriceView;
    private View mProgressView;
    private ProgressBar mProgressBar;
    private View createProductFormView;
    private ProgressDialog progressDialog;
    SpinnerButton loyaltyProgramsSpinner;

    /*shared variables*/
    private boolean isFabMenuOpen = false;
    private ContentResolver contentResolver;
    private DatabaseManager mDatabaseManager;
    private Uri imageUri;
    private Context mContext;
    private ApiClient mApiClient;
    private TextView charCounterView;
    private MerchantEntity merchantEntity;
    private ProductCategoryEntity mSelectedProductCategory;
    private String getActivityInitiator = "";
    private LoyaltyProgramEntity mSelectedProgram;
    private SessionManager mSessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        getActivityInitiator = getIntent().getStringExtra(Constants.ACTIVITY_INITIATOR);

        contentResolver = this.getContentResolver();
        mContext = this;
        mSessionManager = new SessionManager(this);
        mDatabaseManager = DatabaseManager.getInstance(this);
        mApiClient = new ApiClient(this);
        merchantEntity = mDatabaseManager.getMerchant(mSessionManager.getMerchantId());

        mLayout = findViewById(R.id.activity_add_product_container);
        mProgressView = findViewById(R.id.productCreateProgressView);
        mProgressBar = findViewById(R.id.productCreateProgressBar);
        createProductFormView = findViewById(R.id.productCreateFormWrapper);
        baseFloatBtn = findViewById(R.id.baseFloatingActionButton);
        addFromGalleryBtn = findViewById(R.id.addFromGallery);
        takePictureBtn = findViewById(R.id.takePicture);
        removePictureBtn = findViewById(R.id.removePicture);
        addFromGalleryLayout = findViewById(R.id.addFromGalleryLayout);
        removePictureLayout = findViewById(R.id.removePictureLayout);
        thumbnailView = findViewById(R.id.thumbnail);
        takePictureLayout = findViewById(R.id.takePictureLayout);
        productNameView = findViewById(R.id.productName);
        productPriceView = findViewById(R.id.priceOfProduct);
        charCounterView = findViewById(R.id.program_name_char_counter);
        (findViewById(R.id.products_detail_fab_layout)).bringToFront();

        baseFloatBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_add_a_photo_white_24px));
        addFromGalleryBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_collections_white_24px));
        takePictureBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_camera_alt_white_24px));
        removePictureBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_cancel_white_24px));

        RxTextView.textChangeEvents(productNameView).subscribe(textViewTextChangeEvent -> {
            String char_temp = "%s %s / %s";
            String char_temp_unit = textViewTextChangeEvent.text().length() == 1 ? "Character" : "Characters";
            String char_counter_text = String.format(char_temp, textViewTextChangeEvent.text().length(), char_temp_unit, 20);
            charCounterView.setText(char_counter_text);
        });

        Drawable cancelDrawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_cancel_white_24px);
        if (cancelDrawable != null && cancelDrawable.getConstantState() != null) {
            Drawable willBeWhite = cancelDrawable.getConstantState().newDrawable();
            willBeWhite.mutate().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            removePictureBtn.setImageDrawable(willBeWhite);
        }

        SpinnerButton productCategoriesSpinner = findViewById(R.id.productCategoriesSelectSpinner);
        List<ProductCategoryEntity> productCategories = mDatabaseManager.getMerchantProductCategories(mSessionManager.getMerchantId());
        if (productCategories.isEmpty()) {
            SpinnerButton.CreateNewItemListener createNewItemListener = () -> {
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                LayoutInflater li = LayoutInflater.from(alertDialogBuilder.getContext());
                @SuppressLint("InflateParams") View createCategoryView = li.inflate(R.layout.add_product_category, null);
                EditText msgBox = createCategoryView.findViewById(R.id.category_text_box);
                TextView charCounterView = createCategoryView.findViewById(R.id.category_name_char_counter);

                RxTextView.textChangeEvents(msgBox).subscribe(textViewTextChangeEvent -> {
                    CharSequence s = textViewTextChangeEvent.text();
                    String char_temp = "%s %s / %s";
                    String char_temp_unit = s.length() == 1 ? "Character" : "Characters";
                    String char_counter_text = String.format(char_temp, s.length(), char_temp_unit, 30);
                    charCounterView.setText(char_counter_text);
                });

                alertDialogBuilder.setView(createCategoryView);
                alertDialogBuilder.setTitle("Create new category");
                alertDialogBuilder.setPositiveButton("Create", (dialogInterface, i) -> {
                    if (TextUtils.isEmpty(msgBox.getText().toString())) {
                        msgBox.setError(getString(R.string.error_name_required));
                        msgBox.requestFocus();
                        return;
                    }
                    showProgressDialog();

                    try {
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("name", msgBox.getText().toString());
                        JSONObject requestData = new JSONObject();
                        requestData.put("data", jsonObject);

                        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());
                        mApiClient.getLoystarApi(false).addProductCategory(requestBody).enqueue(new Callback<ProductCategory>() {
                            @Override
                            public void onResponse(@NonNull Call<ProductCategory> call, @NonNull Response<ProductCategory> response) {
                                dismissProgressDialog();
                                if (response.isSuccessful()) {
                                    ProductCategory productCategory = response.body();
                                    if (productCategory == null) {
                                        Toast.makeText(mContext, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                                    } else {
                                        ProductCategoryEntity productCategoryEntity = new ProductCategoryEntity();
                                        productCategoryEntity.setId(productCategory.getId());
                                        productCategoryEntity.setDeleted(false);
                                        productCategoryEntity.setName(productCategory.getName());
                                        productCategoryEntity.setCreatedAt(new Timestamp(productCategory.getCreated_at().getMillis()));
                                        productCategoryEntity.setUpdatedAt(new Timestamp(productCategory.getUpdated_at().getMillis()));
                                        productCategoryEntity.setOwner(merchantEntity);

                                        mDatabaseManager.insertNewProductCategory(productCategoryEntity);

                                        mSelectedProductCategory = merchantEntity.getProductCategories().get(0);
                                        CharSequence[] spinnerItems = new CharSequence[1];
                                        spinnerItems[0] = productCategory.getName();
                                        productCategoriesSpinner.setEntries(spinnerItems);
                                        productCategoriesSpinner.setSelection(0);

                                        Toast.makeText(mContext, getString(R.string.product_category_create_success), Toast.LENGTH_LONG).show();
                                    }
                                } else {
                                    Toast.makeText(mContext, getString(R.string.unknown_error), Toast.LENGTH_LONG).show();
                                }

                            }

                            @Override
                            public void onFailure(@NonNull Call<ProductCategory> call, @NonNull Throwable t) {
                                dismissProgressDialog();
                                Toast.makeText(mContext, getString(R.string.error_internet_connection_timed_out), Toast.LENGTH_LONG).show();
                            }
                        });
                    } catch (JSONException e) {
                        dismissProgressDialog();
                        e.printStackTrace();
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", (dialogInterface, i) -> dialogInterface.cancel());
                alertDialogBuilder.setCancelable(false);

                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
            };
            productCategoriesSpinner.setCreateNewItemListener(createNewItemListener);
            productCategoriesSpinner.setCreateNewItemDialogTitle("No Categories Found!");
        } else {
            CharSequence[] spinnerItems = new CharSequence[productCategories.size()];
            for (int i = 0; i < productCategories.size(); i++) {
                spinnerItems[i] = productCategories.get(i).getName();
            }
            productCategoriesSpinner.setEntries(spinnerItems);

            SpinnerButton.OnItemSelectedListener onItemSelectedListener = position -> mSelectedProductCategory = productCategories.get(position);
            productCategoriesSpinner.setListener(onItemSelectedListener);
        }

        loyaltyProgramsSpinner = findViewById(R.id.loyaltyProgramsSelectSpinner);
        List<LoyaltyProgramEntity> programEntities = mDatabaseManager.getMerchantLoyaltyPrograms(mSessionManager.getMerchantId());
        if (programEntities.isEmpty()) {
            SpinnerButton.CreateNewItemListener createNewItemListener = () -> {
                Intent intent = new Intent(AddProductActivity.this, NewLoyaltyProgramListActivity.class);
                startActivityForResult(intent, LoyaltyProgramListActivity.REQ_CREATE_PROGRAM);
            };
            loyaltyProgramsSpinner.setCreateNewItemListener(createNewItemListener);
            loyaltyProgramsSpinner.setCreateNewItemDialogTitle("No Programs Found!");
        } else {
            CharSequence[] spinnerItems = new CharSequence[programEntities.size()];
            for (int i = 0; i < programEntities.size(); i++) {
                spinnerItems[i] = programEntities.get(i).getName();
            }
            loyaltyProgramsSpinner.setEntries(spinnerItems);

            SpinnerButton.OnItemSelectedListener onItemSelectedListener = position -> mSelectedProgram = programEntities.get(position);
            loyaltyProgramsSpinner.setListener(onItemSelectedListener);
        }

        baseFloatBtn.setOnClickListener(view -> {
            if (isFabMenuOpen) {
                collapseFabMenu();
            } else {

                if (hasImage(thumbnailView)) {
                    if (removePictureLayout.getVisibility() == View.GONE) {
                        for (int i = 0; i < removePictureLayout.getChildCount(); i++) {
                            View v = removePictureLayout.getChildAt(i);
                            v.setVisibility(View.VISIBLE);
                        }
                        removePictureLayout.setVisibility(View.VISIBLE);
                    }
                } else {
                    if (removePictureLayout.getVisibility() == View.VISIBLE) {
                        for (int i = 0; i < removePictureLayout.getChildCount(); i++) {
                            View v = removePictureLayout.getChildAt(i);
                            v.setVisibility(View.GONE);
                        }
                        removePictureLayout.setVisibility(View.GONE);
                    }
                }
                expandFabMenu();
            }
        });

        addFromGalleryBtn.setOnClickListener(view -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_PICK);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE_FROM_GALLERY);
        });

        removePictureBtn.setOnClickListener(view -> {
            collapseFabMenu();
            thumbnailView.setImageBitmap(null);
            thumbnailView.destroyDrawingCache();
        });

        takePictureBtn.setOnClickListener(view -> AddProductActivityPermissionsDispatcher.takePictureWithCheck(this));

        getAnimations();
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(AppCompatResources.getDrawable(this, R.drawable.ic_close_white_24px));
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public void getAnimations() {
        fabOpenAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_open);
        fabCloseAnimation = AnimationUtils.loadAnimation(this, R.anim.fab_close);
    }

    private void collapseFabMenu() {
        ViewCompat.animate(baseFloatBtn).rotation(0.0F).withLayer().setDuration(300).setInterpolator(new OvershootInterpolator(10.0F)).start();
        addFromGalleryLayout.startAnimation(fabCloseAnimation);
        takePictureLayout.startAnimation(fabCloseAnimation);
        removePictureLayout.startAnimation(fabCloseAnimation);
        addFromGalleryBtn.setClickable(false);
        takePictureBtn.setClickable(false);
        removePictureBtn.setClickable(false);
        isFabMenuOpen = false;
    }

    private void expandFabMenu() {
        ViewCompat.animate(baseFloatBtn).rotation(45.0F).withLayer().setDuration(300).setInterpolator(new OvershootInterpolator(10.0F)).start();
        addFromGalleryLayout.startAnimation(fabOpenAnimation);
        takePictureLayout.startAnimation(fabOpenAnimation);
        removePictureLayout.startAnimation(fabOpenAnimation);
        addFromGalleryBtn.setClickable(true);
        takePictureBtn.setClickable(true);
        removePictureBtn.setClickable(true);
        isFabMenuOpen = true;
    }

    @Override
    public void onBackPressed() {
        if (isFabMenuOpen)
            collapseFabMenu();
        else
            super.onBackPressed();
    }

    private void closeKeyBoard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_with_icon_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isFormDirty()) {
                    if (isFabMenuOpen) {
                        collapseFabMenu();
                    }
                    closeKeyBoard();
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddProductActivity.this);
                    builder.setTitle(R.string.discard_changes);
                    builder.setMessage(R.string.discard_changes_explain)
                            .setPositiveButton(R.string.discard, (dialog, id) -> {
                                dialog.dismiss();
                                onBackPressed();
                            })
                            .setNegativeButton(getString(R.string.cancel), (dialog, id) -> dialog.dismiss());
                    builder.show();
                }
                else {
                    if (isFabMenuOpen) {
                        collapseFabMenu();
                    }
                    navigateUpTo(new Intent(this, ProductListActivity.class));
                }
                return true;
            case R.id.action_save:
                if (isFormDirty()) {
                    closeKeyBoard();
                    submitForm();
                    return true;
                }
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setMessage(getString(R.string.a_moment));
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (this.isFinishing()) {
            return;
        }
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }



    private void submitForm() {
//        if (imageUri == null) {
//            expandFabMenu();
////            String twoLetters = getFirstTwoLetter(productNameView.getText().toString());
//            // generate dummy bitmap
//
//
//            Snackbar.make(mLayout, getString(R.string.error_picture_required), Snackbar.LENGTH_LONG).show();
//            return;
//        }
        if (productNameView.getText().toString().trim().isEmpty()) {
            productNameView.setError(getString(R.string.error_name_required));
            productNameView.requestFocus();
            return;
        }
        if (productPriceView.getRawValue() == 0) {
            productPriceView.setError(getString(R.string.error_price_cant_be_zero));
            productPriceView.requestFocus();
            return;
        }

        if (mSelectedProductCategory == null) {
            Snackbar.make(mLayout, getString(R.string.error_product_category_required), Snackbar.LENGTH_LONG).show();
            return;
        }

        if (mSelectedProgram == null) {
            Snackbar.make(mLayout, getString(R.string.error_loyalty_program_required), Snackbar.LENGTH_LONG).show();
            return;
        }

        try {
            if (imageUri != null) {
                showProgress(true);
                File file = FileUtils.from(mContext, imageUri);
                File compressedFile = new Compressor(mContext).compressToFile(file);
                String mimeType = contentResolver.getType(imageUri);
                if (mimeType == null) {
                    mimeType = "image/jpeg";
                }
                RequestBodyWithProgress.ProgressListener progressListener = num -> mProgressBar.setProgress((int) num);
                RequestBodyWithProgress requestFile = new RequestBodyWithProgress(compressedFile, mimeType, progressListener);

                MultipartBody.Part body =
                        MultipartBody.Part.createFormData("data[picture]", compressedFile.getName(), requestFile);
                RequestBody name =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, productNameView.getText().toString());
                RequestBody price =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, productPriceView.getFormattedValue(productPriceView.getRawValue()));
                RequestBody merchant_product_category_id =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, String.valueOf(mSelectedProductCategory.getId()));
                RequestBody merchant_loyalty_program_id =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, String.valueOf(mSelectedProgram.getId()));


                mApiClient.getLoystarApi(false)
                        .addProduct(
                                name,
                                price,
                                merchant_product_category_id,
                                merchant_loyalty_program_id,
                                body
                        ).enqueue(new Callback<Product>() {
                    @Override
                    public void onResponse(@NonNull Call<Product> call, @NonNull Response<Product> response) {
                        showProgress(false);
                        if (response.isSuccessful()) {
                            Product product = response.body();
                            if (product == null) {
                                showSnackbar(R.string.unknown_error);
                            } else {
                                ProductEntity productEntity = new ProductEntity();
                                productEntity.setId(product.getId());
                                productEntity.setName(product.getName());
                                productEntity.setPicture(product.getPicture());
                                productEntity.setPrice(product.getPrice());
                                productEntity.setCreatedAt(new Timestamp(product.getCreated_at().getMillis()));
                                productEntity.setUpdatedAt(new Timestamp(product.getUpdated_at().getMillis()));
                                productEntity.setDeleted(false);

                                ProductCategoryEntity productCategoryEntity = mDatabaseManager.getProductCategoryById(product.getMerchant_product_category_id());
                                if (productCategoryEntity != null) {
                                    productEntity.setCategory(productCategoryEntity);
                                }
                                LoyaltyProgramEntity programEntity = mDatabaseManager.getLoyaltyProgramById(product.getMerchant_loyalty_program_id());
                                if (programEntity != null) {
                                    productEntity.setLoyaltyProgram(programEntity);
                                }
                                productEntity.setOwner(merchantEntity);

                                mDatabaseManager.insertNewProduct(productEntity);

                                if (getActivityInitiator.equals(ProductListActivity.TAG)) {
                                    Intent intent = new Intent(AddProductActivity.this, ProductListActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtra(getString(R.string.product_create_success), true);
                                    startActivity(intent);
                                } else if (getActivityInitiator.equals(SaleWithPosActivity.TAG)){
                                    Intent intent = new Intent(AddProductActivity.this, SaleWithPosActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtra(getString(R.string.product_create_success), true);
                                    startActivity(intent);
                                }
                            }
                        } else {
                            showSnackbar(R.string.unknown_error);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Product> call, @NonNull Throwable t) {
                        showProgress(false);
                        showSnackbar(R.string.error_internet_connection_timed_out);
                    }
                });
            } else {
                showProgress(true);
                RequestBody name =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, productNameView.getText().toString());
                RequestBody price =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, productPriceView.getFormattedValue(productPriceView.getRawValue()));
                RequestBody merchant_product_category_id =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, String.valueOf(mSelectedProductCategory.getId()));
                RequestBody merchant_loyalty_program_id =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, String.valueOf(mSelectedProgram.getId()));


                mApiClient.getLoystarApi(false)
                        .addProduct(
                                name,
                                price,
                                merchant_product_category_id,
                                merchant_loyalty_program_id,
                                null
                        ).enqueue(new Callback<Product>() {
                    @Override
                    public void onResponse(@NonNull Call<Product> call, @NonNull Response<Product> response) {
                        showProgress(false);
                        if (response.isSuccessful()) {
                            Product product = response.body();
                            if (product == null) {
                                showSnackbar(R.string.unknown_error);
                            } else {
                                ProductEntity productEntity = new ProductEntity();
                                productEntity.setId(product.getId());
                                productEntity.setName(product.getName());
                                productEntity.setPicture(product.getPicture());
                                productEntity.setPrice(product.getPrice());
                                productEntity.setCreatedAt(new Timestamp(product.getCreated_at().getMillis()));
                                productEntity.setUpdatedAt(new Timestamp(product.getUpdated_at().getMillis()));
                                productEntity.setDeleted(false);

                                ProductCategoryEntity productCategoryEntity = mDatabaseManager.getProductCategoryById(product.getMerchant_product_category_id());
                                if (productCategoryEntity != null) {
                                    productEntity.setCategory(productCategoryEntity);
                                }
                                LoyaltyProgramEntity programEntity = mDatabaseManager.getLoyaltyProgramById(product.getMerchant_loyalty_program_id());
                                if (programEntity != null) {
                                    productEntity.setLoyaltyProgram(programEntity);
                                }
                                productEntity.setOwner(merchantEntity);

                                mDatabaseManager.insertNewProduct(productEntity);

                                if (getActivityInitiator.equals(ProductListActivity.TAG)) {
                                    Intent intent = new Intent(AddProductActivity.this, ProductListActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtra(getString(R.string.product_create_success), true);
                                    startActivity(intent);
                                } else if (getActivityInitiator.equals(SaleWithPosActivity.TAG)){
                                    Intent intent = new Intent(AddProductActivity.this, SaleWithPosActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    intent.putExtra(getString(R.string.product_create_success), true);
                                    startActivity(intent);
                                }
                            }
                        } else {
                            showSnackbar(R.string.unknown_error);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Product> call, @NonNull Throwable t) {
                        showProgress(false);
                        showSnackbar(R.string.error_internet_connection_timed_out);
                    }
                });
            }


        } catch (Exception e) {
            e.printStackTrace();
            Log.e("sayyy", "never");
            showSnackbar(R.string.unknown_error);
            showProgress(false);
        }
    }

    private boolean isFormDirty() {
        return hasImage(thumbnailView) || !(mSelectedProductCategory == null) || !(mSelectedProgram == null) ||
                !(TextUtils.isEmpty(productNameView.getText().toString())) || productPriceView.getRawValue() != 0;
    }

    /**
     * Get URI to image received from capture by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = FileProvider.getUriForFile(
                    mContext,
                    mContext.getApplicationContext().getPackageName() + ".co.loystar.loystarbusiness.provider",
                    new File(getImage.getPath(), "pickImageResult.jpeg"));
        }
        return outputFileUri;
    }

    public Uri getPickImageResultUri(Intent data) {
        boolean isCamera = true;
        if (data != null && data.getData() != null) {
            String action = data.getAction();
            isCamera = action != null && action.equals(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        return isCamera ? getCaptureImageOutputUri() : data.getData();
    }

    private boolean hasImage(@NonNull ImageView view) {
        Drawable drawable = view.getDrawable();
        boolean hasImage = (drawable != null);

        if (hasImage && (drawable instanceof BitmapDrawable)) {
            hasImage = ((BitmapDrawable)drawable).getBitmap() != null;
        }

        return hasImage;
    }

    @MainThread
    private void showSnackbar(@StringRes int errorMessageRes) {
        Snackbar.make(mLayout, errorMessageRes, Snackbar.LENGTH_LONG).show();
    }


    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        createProductFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        createProductFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                createProductFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    /*
    * take picture with permissions
    * */
    @NeedsPermission(Manifest.permission.CAMERA)
    public void takePicture() {
        Uri outputFileUri = getCaptureImageOutputUri();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null && outputFileUri != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @OnShowRationale(Manifest.permission.CAMERA)
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.runtime_permission_is_needed, "Camera"))
                .setMessage(R.string.permission_camera_rationale)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> request.proceed())
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> request.cancel())
                .setCancelable(false)
                .show();
    }

    @OnPermissionDenied(Manifest.permission.CAMERA)
    void showDeniedForCamera() {
        showSnackbar(R.string.permission_camera_denied);
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    void showNeverAskForCamera() {
        showSnackbar(R.string.permission_camera_rationale_never_ask_again);
    }

    /**
     * crop image with permissions
     *
     */
    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public void cropImage(Uri uri) {
        CropImage.activity(uri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(AddProductActivity.this);
    }

    @OnShowRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showRationaleForReadExternalStorage(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.runtime_permission_is_needed, "Storage"))
                .setMessage(R.string.permission_storage_rationale)
                .setPositiveButton(R.string.ok, (dialogInterface, i) -> request.proceed())
                .setNegativeButton(R.string.cancel, (dialogInterface, i) -> request.cancel())
                .show();
    }

    @OnPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showDeniedForReadExternalStorage() {
        showSnackbar(R.string.permission_storage_denied);
    }

    @OnNeverAskAgain(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showNeverAskForReadExternalStorage() {
        showSnackbar(R.string.permission_storage_rationale_never_ask_again);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        AddProductActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri mCropImageUri = getPickImageResultUri(data);
            if (requestCode == REQUEST_IMAGE_FROM_GALLERY || requestCode == REQUEST_IMAGE_CAPTURE) {
                 /*Start crop activity*/
                AddProductActivityPermissionsDispatcher.cropImageWithCheck(AddProductActivity.this, mCropImageUri);
            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                /*Crop activity result success*/
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                imageUri = result.getUri();
                Glide.with(AddProductActivity.this)
                        .load(imageUri.getPath())
                        .into(thumbnailView);
                collapseFabMenu();
            } else if (requestCode == LoyaltyProgramListActivity.REQ_CREATE_PROGRAM) {
                mSelectedProgram = mDatabaseManager.getMerchantLoyaltyPrograms(mSessionManager.getMerchantId()).get(0);
                if (mSelectedProgram != null) {
                    CharSequence[] spinnerItems = new CharSequence[1];
                    spinnerItems[0] = mSelectedProgram.getName();
                    loyaltyProgramsSpinner.setEntries(spinnerItems);
                    loyaltyProgramsSpinner.setSelection(0);
                }
            }
        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Exception error = result.getError();
            Snackbar.make(mLayout, error.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }
}
