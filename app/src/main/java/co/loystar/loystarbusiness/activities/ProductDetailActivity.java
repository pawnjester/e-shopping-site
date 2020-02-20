package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.clans.fab.FloatingActionButton;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.auth.SessionManager;
import co.loystar.loystarbusiness.auth.api.ApiClient;
import co.loystar.loystarbusiness.auth.sync.SyncAdapter;
import co.loystar.loystarbusiness.models.DatabaseManager;
import co.loystar.loystarbusiness.models.databinders.Product;
import co.loystar.loystarbusiness.models.entities.LoyaltyProgramEntity;
import co.loystar.loystarbusiness.models.entities.ProductCategoryEntity;
import co.loystar.loystarbusiness.models.entities.ProductEntity;
import co.loystar.loystarbusiness.utils.FileUtils;
import co.loystar.loystarbusiness.utils.RequestBodyWithProgress;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.buttons.SpinnerButton;
import co.loystar.loystarbusiness.utils.ui.dialogs.MyAlertDialog;
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

import static android.content.DialogInterface.BUTTON_NEGATIVE;
import static android.content.DialogInterface.BUTTON_POSITIVE;

@RuntimePermissions
public class ProductDetailActivity extends BaseActivity {
    /*static fields*/
    public static final String ARG_ITEM_ID = "item_id";
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
    private EditText productDescriptionView;
    private CurrencyEditText productPriceView;
    private View mProgressView;
    private ProgressBar mProgressBar;
    private View editProductDetailFormView;
    private EditText mProductQuantity;


    /*shared variables*/
    private boolean isFabMenuOpen = false;
    private ContentResolver contentResolver;
    private DatabaseManager mDatabaseManager;
    private Uri imageUri;
    private boolean formIsDirty = false;
    private ProductCategoryEntity mSelectedProductCategory;
    private Context mContext;
    private ApiClient mApiClient;
    private String originalPrice;
    private TextView charCounterView;
    private ProductEntity mProductItem;
    private MyAlertDialog myAlertDialog;
    private SessionManager mSessionManager;
    private LoyaltyProgramEntity mSelectedProgram;
    private String mSelectedUnit;
    private boolean isTracked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        contentResolver = this.getContentResolver();
        mContext = this;
        mSessionManager = new SessionManager(this);
        mDatabaseManager = DatabaseManager.getInstance(this);
        mApiClient = new ApiClient(this);
        myAlertDialog = new MyAlertDialog();

        mLayout = findViewById(R.id.activity_product_detail_container);
        mProgressView = findViewById(R.id.productEditProgressView);
        mProgressBar = findViewById(R.id.productEditProgressBar);
        editProductDetailFormView = findViewById(R.id.productEditFormWrapper);
        baseFloatBtn = findViewById(R.id.baseFloatingActionButton);
        addFromGalleryBtn = findViewById(R.id.addFromGallery);
        takePictureBtn = findViewById(R.id.takePicture);
        removePictureBtn = findViewById(R.id.removePicture);
        addFromGalleryLayout = findViewById(R.id.addFromGalleryLayout);
        removePictureLayout = findViewById(R.id.removePictureLayout);
        thumbnailView = findViewById(R.id.thumbnail);
        mProductQuantity = findViewById(R.id.productQuantity);
        takePictureLayout = findViewById(R.id.takePictureLayout);
        productNameView = findViewById(R.id.productName);
        productDescriptionView = findViewById(R.id.productDescription);
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

        if (mProductItem != null) {
            Glide.with(this)
                    .load(mProductItem.getPicture())
                    .apply(RequestOptions.placeholderOf(AppCompatResources.getDrawable(mContext, R.drawable.ic_photo_black_24px)))
                    .into(thumbnailView);

            productNameView.setText(mProductItem.getName());
            productDescriptionView.setText(mProductItem.getDescription());

            originalPrice = String.format(Locale.UK, "%.2f", new BigDecimal(mProductItem.getPrice()));

            productPriceView.setText(originalPrice);
            mSelectedProductCategory = mProductItem.getCategory();
            mSelectedProgram = mProductItem.getLoyaltyProgram();
            mSelectedUnit = mProductItem.getUnit();


            Drawable cancelDrawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_cancel_white_24px);
            if (cancelDrawable != null && cancelDrawable.getConstantState() != null) {
                Drawable willBeWhite = cancelDrawable.getConstantState().newDrawable();
                willBeWhite.mutate().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                removePictureBtn.setImageDrawable(willBeWhite);
            }
            SpinnerButton productCategoriesSpinner = findViewById(R.id.productCategoriesSelectSpinner);
            List<ProductCategoryEntity> productCategories = mDatabaseManager.getMerchantProductCategories(mSessionManager.getMerchantId());
            final CharSequence[] spinnerItems = new CharSequence[productCategories.size()];
            for (int i = 0; i < productCategories.size(); i++) {
                spinnerItems[i] = productCategories.get(i).getName();
            }
            productCategoriesSpinner.setEntries(spinnerItems);

            SpinnerButton.OnItemSelectedListener onItemSelectedListener = position -> mSelectedProductCategory = productCategories.get(position);
            productCategoriesSpinner.setListener(onItemSelectedListener);
            productCategoriesSpinner.setSelection(productCategories.indexOf(mSelectedProductCategory));

            SpinnerButton loyaltyProgramsSpinner = findViewById(R.id.loyaltyProgramsSelectSpinner);
            List<LoyaltyProgramEntity> programEntities = mDatabaseManager.getMerchantLoyaltyPrograms(mSessionManager.getMerchantId());
            CharSequence[] charSequences = new CharSequence[programEntities.size()];
            for (int i = 0; i < programEntities.size(); i++) {
                charSequences[i] = programEntities.get(i).getName();
            }
            loyaltyProgramsSpinner.setEntries(charSequences);
            if (mProductItem.getQuantity() != null) {
                mProductQuantity.setText(mProductItem.getQuantity());
            }
            SpinnerButton.OnItemSelectedListener selectedListener =
                    position -> mSelectedProgram = programEntities.get(position);
            loyaltyProgramsSpinner.setListener(selectedListener);
            if (mSelectedProgram != null) {
                loyaltyProgramsSpinner.setSelection(programEntities.indexOf(mSelectedProgram));
            }

            SpinnerButton stockUnitSpinner = findViewById(R.id.stockUnitSpinner);
            ArrayList<String> time = new ArrayList<>();
            time.add("BAG");
            time.add("BUNCH");
            time.add("CARTON");
            time.add("DOZEN");
            time.add("GIGABYTE");
            time.add("GRAM");
            time.add("KILOGRAM");
            time.add("LITRE");
            time.add("PACK");
            time.add("PAIR");
            time.add("PCS");
            time.add("UNIT");
            time.add("YARD");
            CharSequence[] spinnerItem = new CharSequence[time.size()];
            for (int i = 0; i < time.size(); i++) {
                spinnerItem[i] = time.get(i);
            }
            stockUnitSpinner.setEntries(spinnerItem);
            SpinnerButton.OnItemSelectedListener onItemSelected =
                    position -> mSelectedUnit = time.get(position);
            stockUnitSpinner.setListener(onItemSelected);
            if (mSelectedUnit != null) {
                stockUnitSpinner.setSelection(time.indexOf(mSelectedUnit));
            }

            SwitchCompat switchCompat = findViewById(R.id.trackSwitch);
            isTracked = mProductItem.isTracked();
            switchCompat.setChecked(isTracked);
            switchCompat.setOnCheckedChangeListener((buttonView, isChecked) -> isTracked = isChecked);

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
                formIsDirty = true;
            });

            takePictureBtn.setOnClickListener(view -> ProductDetailActivityPermissionsDispatcher.takePictureWithCheck(ProductDetailActivity.this));

            getAnimations();
        }
    }

    @Override
    protected void setupToolbar() {
        super.setupToolbar();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            DatabaseManager databaseManager = DatabaseManager.getInstance(this);
            mProductItem = databaseManager.getProductById(getIntent().getIntExtra(ARG_ITEM_ID, 0));
            if (mProductItem != null) {
                actionBar.setTitle(mProductItem.getName());
            }
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeAsUpIndicator(AppCompatResources.getDrawable(this, R.drawable.ic_close_white_24px));
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
        inflater.inflate(R.menu.save_or_delete_menu, menu);
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(ProductDetailActivity.this);
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
            case R.id.action_delete:
                if (mProductItem != null) {
                    myAlertDialog.setTitle("Are you sure?");
                    myAlertDialog.setMessage("You won't be able to recover this product.");
                    myAlertDialog.setPositiveButton(getString(R.string.confirm_delete_positive), (dialogInterface, i) -> {
                        switch (i) {
                            case BUTTON_NEGATIVE:
                                dialogInterface.dismiss();
                                break;
                            case BUTTON_POSITIVE:
                                dialogInterface.dismiss();
                                mProductItem.setDeleted(true);
                                mDatabaseManager.updateProduct(mProductItem);
                                SyncAdapter.performSync(mContext, mSessionManager.getEmail());

                                Intent intent = new Intent(mContext, ProductListActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                startActivity(intent);
                        }
                    });
                    myAlertDialog.setNegativeButtonText(getString(android.R.string.no));
                    myAlertDialog.show(getSupportFragmentManager(), MyAlertDialog.TAG);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void submitForm() {
        if (!hasImage(thumbnailView)) {
            expandFabMenu();
            Snackbar.make(mLayout, getString(R.string.error_picture_required), Snackbar.LENGTH_LONG).show();
            return;
        }
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

        if (mSelectedProgram == null) {
            Snackbar.make(mLayout, getString(R.string.error_loyalty_program_required), Snackbar.LENGTH_LONG).show();
            return;
        }
        if (isTracked) {
            if (mSelectedUnit == null) {
                Snackbar.make(mLayout, getString(R.string.error_stock_unit_required), Snackbar.LENGTH_LONG).show();
                return;
            }
            if (mProductQuantity.getText().toString().trim().isEmpty()) {
                mProductQuantity.setError("Please enter a quantity");
                mProductQuantity.requestFocus();
                return;
            }
        } else {
            mProductQuantity.setText("");
            isTracked= false;
            mSelectedUnit = "";
        }

        if (imageUri == null) {
            try {
                mProgressBar.setIndeterminate(true);
                showProgress(true);
                JSONObject req = new JSONObject();
                req.put("merchant_product_category_id", mSelectedProductCategory.getId());
                req.put("merchant_loyalty_program_id", mSelectedProgram.getId());
                req.put("name", productNameView.getText().toString());
                req.put("description", productDescriptionView.getText().toString());
                req.put("quantity", mProductQuantity.getText().toString());
                req.put("unit", mSelectedUnit);
                req.put("track_inventory", isTracked);


                JSONObject requestData = new JSONObject();
                requestData.put("data", req);

                RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), requestData.toString());

                mApiClient.getLoystarApi(false).updateProduct(requestBody, mProductItem.getId()).enqueue(new Callback<Product>() {
                    @Override
                    public void onResponse(@NonNull Call<Product> call, @NonNull Response<Product> response) {
                        showProgress(false);

                        if (response.isSuccessful()) {
                            Product product = response.body();
                            if (product == null) {
                                showSnackbar(R.string.unknown_error);
                            } else {
                                mProductItem.setName(product.getName());
                                mProductItem.setPrice(product.getPrice());
                                mProductItem.setPicture(product.getPicture());
                                mProductItem.setDescription(product.getDescription());
                                mProductItem.setQuantity(product.getQuantity());
                                mProductItem.setUnit(product.getUnit());
                                mProductItem.setTracked(product.getTrackInventory());
                                ProductCategoryEntity productCategoryEntity = mDatabaseManager.getProductCategoryById(product.getMerchant_product_category_id());
                                if (productCategoryEntity != null) {
                                    mProductItem.setCategory(productCategoryEntity);
                                }
                                LoyaltyProgramEntity programEntity = mDatabaseManager.getLoyaltyProgramById(product.getMerchant_loyalty_program_id());
                                if (programEntity != null) {
                                    mProductItem.setLoyaltyProgram(programEntity);
                                }
                                mDatabaseManager.updateProduct(mProductItem);

                                Intent intent = new Intent(ProductDetailActivity.this, ProductListActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                intent.putExtra(getString(R.string.product_edit_success), true);
                                startActivity(intent);
                            }
                        } else {
                            Snackbar.make(mLayout, getString(R.string.error_product_update), Snackbar.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Product> call, @NonNull Throwable t) {
                        showProgress(false);
                        showSnackbar(R.string.error_internet_connection_timed_out);
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
                showProgress(false);
                showSnackbar(R.string.unknown_error);
            }
        } else {
            try {
                showProgress(true);
                File file = FileUtils.from(mContext, imageUri);
                File compressedFile = new Compressor(mContext).compressToFile(file);

                RequestBody name =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, productNameView.getText().toString());
                RequestBody description =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, productDescriptionView.getText().toString());
                RequestBody price =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, productPriceView.getFormattedValue(productPriceView.getRawValue()));
                RequestBody merchant_product_category_id =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, String.valueOf(mSelectedProductCategory.getId()));

                RequestBody merchant_loyalty_program_id =
                    RequestBody.create(
                        okhttp3.MultipartBody.FORM, String.valueOf(mSelectedProgram.getId()));

                RequestBody stock_unit =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, mSelectedUnit);
                RequestBody quantity =
                        RequestBody.create(
                                okhttp3.MultipartBody.FORM, mProductQuantity.getText().toString());

                String mimeType = contentResolver.getType(imageUri);
                if (mimeType == null) {
                    mimeType = "image/jpeg";
                }
                RequestBodyWithProgress.ProgressListener progressListener = num -> mProgressBar.setProgress((int) num);
                RequestBodyWithProgress requestFile = new RequestBodyWithProgress(compressedFile, mimeType, progressListener);

                MultipartBody.Part body =
                        MultipartBody.Part.createFormData("data[picture]", compressedFile.getName(), requestFile);

                mApiClient.getLoystarApi(false)
                        .updateProduct(
                            mProductItem.getId(),
                            name,
                            price,
                            description,
                            merchant_product_category_id,
                            merchant_loyalty_program_id,
                            stock_unit,
                            quantity,
                            isTracked,
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
                               mProductItem.setName(product.getName());
                               mProductItem.setPrice(product.getPrice());
                               mProductItem.setDescription(product.getDescription());
                               mProductItem.setPicture(product.getPicture());
                               mProductItem.setQuantity(product.getQuantity());
                               mProductItem.setUnit(product.getUnit());
                               mProductItem.setTracked(product.getTrackInventory());
                               ProductCategoryEntity productCategoryEntity = mDatabaseManager.getProductCategoryById(product.getMerchant_product_category_id());
                               if (productCategoryEntity != null) {
                                   mProductItem.setCategory(productCategoryEntity);
                               }
                               LoyaltyProgramEntity programEntity = mDatabaseManager.getLoyaltyProgramById(product.getMerchant_loyalty_program_id());
                               if (programEntity != null) {
                                   mProductItem.setLoyaltyProgram(programEntity);
                               }
                               mDatabaseManager.updateProduct(mProductItem);

                               Intent intent = new Intent(ProductDetailActivity.this, ProductListActivity.class);
                               intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                               intent.putExtra(getString(R.string.product_edit_success), true);
                               startActivity(intent);
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
            } catch (Exception e) {
                e.printStackTrace();
                showSnackbar(R.string.unknown_error);
                showProgress(false);
            }
        }
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            Uri mCropImageUri = getPickImageResultUri(data);
            if (requestCode == REQUEST_IMAGE_FROM_GALLERY || requestCode == REQUEST_IMAGE_CAPTURE) {
                 /*Start crop activity*/
                 ProductDetailActivityPermissionsDispatcher.cropImageWithCheck(ProductDetailActivity.this, mCropImageUri);
            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                /*Crop activity result success*/
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                imageUri = result.getUri();
                Glide.with(ProductDetailActivity.this)
                        .load(imageUri.getPath())
                        .into(thumbnailView);
                collapseFabMenu();
                formIsDirty = true;
            }
        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Exception error = result.getError();
            Snackbar.make(mLayout, error.getMessage(), Snackbar.LENGTH_LONG).show();
        }
    }

    private boolean isFormDirty() {
        boolean productCategoryHasChanged = false;
        if (mSelectedProductCategory != null) {
            if (mProductItem.getCategory() == null) {
                productCategoryHasChanged = true;
            } else {
                if (mSelectedProductCategory.getId() != mProductItem.getCategory().getId()) {
                    productCategoryHasChanged = true;
                }
            }
        }
        boolean loyaltyProgramHasChanged = false;
        if (mSelectedProgram != null) {
            if (mProductItem.getLoyaltyProgram() == null) {
                loyaltyProgramHasChanged = true;
            } else {
                if (mSelectedProgram.getId() != mProductItem.getLoyaltyProgram().getId()) {
                    loyaltyProgramHasChanged = true;
                }
            }
        }

        boolean stockUnitHasChanged = false;
        if (mSelectedUnit != null) {
            if (mProductItem.getUnit() == null){
                stockUnitHasChanged = true;
            } else {
                if (!mSelectedUnit.equals(mProductItem.getUnit())) {
                    stockUnitHasChanged = true;
                }
            }
        }
        return formIsDirty ||
                !productNameView.getText().toString().equals(mProductItem.getName()) ||
                !productDescriptionView.getText().toString().equals(mProductItem.getDescription()) ||
                !mProductQuantity.getText().toString().equals(mProductItem.getQuantity()) ||
                !isTracked == mProductItem.isTracked() ||
                productCategoryHasChanged ||
                loyaltyProgramHasChanged ||
                stockUnitHasChanged ||
                !(originalPrice.equals(productPriceView.getFormattedValue(productPriceView.getRawValue())));

    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        editProductDetailFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        editProductDetailFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                editProductDetailFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
                .start(ProductDetailActivity.this);
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
        ProductDetailActivityPermissionsDispatcher.onRequestPermissionsResult(ProductDetailActivity.this, requestCode, grantResults);
    }
}
