package co.loystar.loystarbusiness.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
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
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.iceteck.silicompressorr.SiliCompressor;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import co.loystar.loystarbusiness.BuildConfig;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.api.JsonUtils;
import co.loystar.loystarbusiness.api.WrapperResponseConverter;
import co.loystar.loystarbusiness.models.DatabaseHelper;
import co.loystar.loystarbusiness.models.db.DBProduct;
import co.loystar.loystarbusiness.models.db.DBProductCategory;
import co.loystar.loystarbusiness.utils.LoystarApplication;
import co.loystar.loystarbusiness.utils.SessionManager;
import co.loystar.loystarbusiness.utils.ui.CurrencyEditText.CurrencyEditText;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialog;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogEntity;
import co.loystar.loystarbusiness.utils.ui.SingleChoiceSpinnerDialog.SingleChoiceSpinnerDialogOnItemSelectedListener;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

/**
 * Created by ordgen on 7/4/17.
 */

@RuntimePermissions
public class AddProductActivity extends AppCompatActivity implements SingleChoiceSpinnerDialogOnItemSelectedListener {

    /*static fields*/
    public static final String PRODUCT_ID = "productId";
    private static final String TAG = AddProductActivity.class.getCanonicalName();
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    public static final String hostname = BuildConfig.HOST;
    private static final int REQUEST_IMAGE_FROM_GALLERY = 2;
    public static final String url_prefix = BuildConfig.URL_PREFIX;
    public static final String ACTIVITY_INITIATOR = "activityInitiator";

    /*shared variables*/
    private boolean isFabMenuOpen = false;
    private SessionManager sessionManager;
    private String mSelectedCategory = "";
    private DatabaseHelper databaseHelper = LoystarApplication.getInstance().getDatabaseHelper();
    private Uri imageUri;
    private Context mContext;

    /*views*/
    private com.github.clans.fab.FloatingActionButton baseFloatBtn;
    private com.github.clans.fab.FloatingActionButton addFromGalleryBtn;
    private  com.github.clans.fab.FloatingActionButton takePictureBtn;
    private com.github.clans.fab.FloatingActionButton removePictureBtn;
    private ImageView thumbnailView;
    private Animation fabOpenAnimation;
    private Animation fabCloseAnimation;
    private LinearLayout removePictureLayout;
    private View addFromGalleryLayout;
    private View takePictureLayout;
    private View mLayout;
    private EditText productNameView;
    private CurrencyEditText productPriceView;
    private TextView charCounterView;
    private SingleChoiceSpinnerDialog productCategoriesSpinner;
    private ArrayList<DBProductCategory> productCategories;
    private String getActivityInitiator = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_products);
        mLayout = findViewById(R.id.add_product_container);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(AppCompatResources.getDrawable(this, R.drawable.ic_close_white_24px));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        sessionManager = new SessionManager(this);
        mContext = this;
        getActivityInitiator = getIntent().getStringExtra(ACTIVITY_INITIATOR);

        baseFloatBtn = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.baseFloatingActionButton);
        addFromGalleryBtn = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.addFromGallery);
        takePictureBtn = ( com.github.clans.fab.FloatingActionButton) findViewById(R.id.takePicture);
        removePictureBtn = (com.github.clans.fab.FloatingActionButton) findViewById(R.id.removePicture);
        addFromGalleryLayout = findViewById(R.id.addFromGalleryLayout);
        removePictureLayout = (LinearLayout) findViewById(R.id.removePictureLayout);
        thumbnailView = (ImageView) findViewById(R.id.thumbnail);
        takePictureLayout = findViewById(R.id.takePictureLayout);
        productNameView = (EditText) findViewById(R.id.productName);
        productCategoriesSpinner = (SingleChoiceSpinnerDialog) findViewById(R.id.productCategoriesSelectSpinner);
        productPriceView = (CurrencyEditText) findViewById(R.id.priceOfProduct);
        charCounterView = (TextView) findViewById(R.id.program_name_char_counter);
        (findViewById(R.id.add_products_fab_layout)).bringToFront();

        baseFloatBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_add_a_photo_white_24px));
        addFromGalleryBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_collections_white_24px));
        takePictureBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_camera_alt_white_24px));
        removePictureBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_cancel_white_24px));

        Drawable cancelDrawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_cancel_white_24px);
        if (cancelDrawable != null && cancelDrawable.getConstantState() != null) {
            Drawable willBeWhite = cancelDrawable.getConstantState().newDrawable();
            willBeWhite.mutate().setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            removePictureBtn.setImageDrawable(willBeWhite);
        }

        productCategories = databaseHelper.listMerchantProductCategories(sessionManager.getMerchantId());
        ArrayList<SingleChoiceSpinnerDialogEntity> spinnerItems = new ArrayList<>();
        for (DBProductCategory p: productCategories) {
            SingleChoiceSpinnerDialogEntity entity = new SingleChoiceSpinnerDialogEntity(p.getName(), p.getId());
            spinnerItems.add(entity);
        }

        Long preselectedCategory = null;
        if (productCategories.size() == 1) {
            DBProductCategory productCategory = productCategories.get(0);
            preselectedCategory = productCategory.getId();
            mSelectedCategory = productCategory.getName();
        }

        Intent addCategoryIntent = new Intent(mContext, AddCategoryActivity.class);
        productCategoriesSpinner.setItems(
                spinnerItems,
                "Select One",
                this,
                "Select Category",
                preselectedCategory,
                addCategoryIntent,
                "No Categories Found",
                "Create Category");

        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int count, int after) {
                String char_temp = "%s %s / %s";
                String char_temp_unit = s.length() == 1 ? "Character" : "Characters";
                String char_counter_text = String.format(char_temp, s.length(), char_temp_unit, 20);
                charCounterView.setText(char_counter_text);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        };

        productNameView.addTextChangedListener(textWatcher);

        baseFloatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isFabMenuOpen) {
                    collapseFabMenu();
                }
                else {

                    if (hasImage(thumbnailView)) {
                        if (removePictureLayout.getVisibility() == View.GONE) {
                            for ( int i = 0; i < removePictureLayout.getChildCount();  i++ ){
                                View v = removePictureLayout.getChildAt(i);
                                v.setVisibility(View.VISIBLE);
                            }
                            removePictureLayout.setVisibility(View.VISIBLE);
                        }
                    }
                    else {
                        if (removePictureLayout.getVisibility() == View.VISIBLE) {
                            for ( int i = 0; i < removePictureLayout.getChildCount();  i++ ){
                                View v = removePictureLayout.getChildAt(i);
                                v.setVisibility(View.GONE);
                            }
                            removePictureLayout.setVisibility(View.GONE);
                        }
                    }
                    expandFabMenu();
                }
            }
        });

        addFromGalleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_PICK);
                startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_IMAGE_FROM_GALLERY);
            }
        });

        takePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri outputFileUri = getCaptureImageOutputUri();
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null && outputFileUri != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });

        removePictureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                collapseFabMenu();
                thumbnailView.setImageBitmap(null);
                thumbnailView.destroyDrawingCache();
            }
        });

        getAnimations();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_with_icon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (formIsDirty()) {
                    if (isFabMenuOpen) {
                        collapseFabMenu();
                    }
                    closeKeyBoard();
                    AlertDialog.Builder builder = new AlertDialog.Builder(AddProductActivity.this);
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
                    if (isFabMenuOpen) {
                        collapseFabMenu();
                    }
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

    private void closeKeyBoard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private boolean formIsDirty() {
        return hasImage(thumbnailView) || !TextUtils.isEmpty(mSelectedCategory) ||
                !productNameView.getText().toString().trim().isEmpty() || productPriceView.getRawValue() != 0;
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
        if (TextUtils.isEmpty(mSelectedCategory)) {
            Snackbar.make(mLayout, getString(R.string.error_product_category_required), Snackbar.LENGTH_LONG).show();
            return;
        }

        try {
            final String filePath = SiliCompressor.with(AddProductActivity.this).compress(
                    imageUri.toString(),
                    new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+getPackageName()+"/media/images")
            );
            DBProductCategory productCategory = databaseHelper.getProductCategoryByName(
                    mSelectedCategory, sessionManager.getMerchantId());

            final ProgressDialog progressDialog = new ProgressDialog(AddProductActivity.this);
            progressDialog.setIndeterminate(false);
            progressDialog.setMessage(getString(R.string.uploading));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setProgress(0);
            progressDialog.show();

            String url = hostname + url_prefix + getString(R.string.add_product_url);
            new MultipartUploadRequest(AddProductActivity.this, url)
                    .addFileToUpload(filePath, "data[picture]")
                    .addHeader("access-token", sessionManager.getAccessToken())
                    .addHeader("client", sessionManager.getClientKey())
                    .addHeader("uid", sessionManager.getMerchantEmail())
                    .addParameter("data[merchant_product_category_id]", String.valueOf(productCategory.getId()))
                    .addParameter("data[name]", productNameView.getText().toString())
                    .addParameter("data[price]", productPriceView.getFormattedValue(productPriceView.getRawValue()))
                    .setNotificationConfig(new UploadNotificationConfig())
                    .setMaxRetries(2)
                    .setDelegate(new UploadStatusDelegate() {
                        @Override
                        public void onProgress(Context context, UploadInfo uploadInfo) {
                            progressDialog.setProgress(uploadInfo.getProgressPercent());
                        }

                        @Override
                        public void onError(Context context, UploadInfo uploadInfo, Exception exception) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Snackbar.make(mLayout, getString(R.string.something_went_wrong), Snackbar.LENGTH_LONG).show();
                        }

                        @Override
                        public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }

                            if (serverResponse.getHttpCode() == 200) {
                                try {

                                    String responseString = serverResponse.getBodyAsString();
                                    ObjectMapper mapper = JsonUtils.objectMapper;
                                    ResponseBody value  = ResponseBody.create(MediaType.parse("application/json; charset=utf-8"), responseString);
                                    JavaType javaType = mapper.getTypeFactory().constructType(DBProduct.class);
                                    ObjectReader reader = mapper.readerFor(javaType);
                                    WrapperResponseConverter responseConverter = new WrapperResponseConverter(reader, mapper);
                                    DBProduct product = (DBProduct) responseConverter.convert(value);

                                    databaseHelper.insertProduct(product);


                                    Intent intent = new Intent(AddProductActivity.this, ProductsActivity.class);
                                    if (getActivityInitiator.equals(ProductsActivity.class.getSimpleName())) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    }
                                    else {
                                        intent.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                                    }
                                    intent.putExtra(getString(R.string.product_add_success), true);
                                    startActivity(intent);
                                    finish();

                                } catch (IOException e) {
                                    //Crashlytics.logException(e);
                                    e.printStackTrace();
                                }
                            }
                            else {
                                Snackbar.make(mLayout, getString(R.string.error_upload_failed), Snackbar.LENGTH_LONG).show();
                            }
                        }

                        @Override
                        public void onCancelled(Context context, UploadInfo uploadInfo) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                        }
                    })
                    .startUpload();
        } catch (Exception e) {
            //Crashlytics.logException(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Uri mCropImageUri = getPickImageResultUri(data);
            if (requestCode == REQUEST_IMAGE_FROM_GALLERY || requestCode == REQUEST_IMAGE_CAPTURE) {

                 /*Start crop activity*/
                AddProductActivityPermissionsDispatcher.cropImageWithCheck(AddProductActivity.this, mCropImageUri);
            }
            else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
                /*Crop activity result success*/
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                imageUri = result.getUri();
                Glide.with(AddProductActivity.this)
                        .load(imageUri.getPath())
                        //.crossFade()
                        .into(thumbnailView);
                collapseFabMenu();
            }
        }
        else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            /*Crop activity result error*/
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Exception error = result.getError();
            //Crashlytics.logException(error);
            Snackbar.make(mLayout, getString(R.string.error_image_crop), Snackbar.LENGTH_LONG).show();
        }

    }

    @SuppressWarnings("MissingPermission")
    @NeedsPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
    public void cropImage(Uri uri) {
        CropImage.activity(uri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(AddProductActivity.this);
    }

    @OnShowRationale(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showRationaleForReadExternalStorage(final PermissionRequest request) {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.permission_read_external_storage_rationale)
                .setPositiveButton(R.string.buttonc_allow, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.button_deny, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.cancel();
                    }
                })
                .setCancelable(false)
                .show();
    }

    @OnPermissionDenied(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showDeniedForReadExternalStorage() {
        Toast.makeText(mContext, R.string.permission_read_external_storage_denied, Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.READ_EXTERNAL_STORAGE)
    void showNeverAskForReadExternalStorage() {
        Toast.makeText(mContext, R.string.permission_external_storage_neverask, Toast.LENGTH_SHORT).show();
    }

    /**
     * Get URI to image received from capture by camera.
     */
    private Uri getCaptureImageOutputUri() {
        Uri outputFileUri = null;
        File getImage = getExternalCacheDir();
        if (getImage != null) {
            outputFileUri = Uri.fromFile(new File(getImage.getPath(), "pickImageResult.jpeg"));
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        AddProductActivityPermissionsDispatcher.onRequestPermissionsResult(
                AddProductActivity.this, requestCode, grantResults);
    }

    private boolean hasImage(@NonNull ImageView view) {
        Drawable drawable = view.getDrawable();
        boolean hasImage = (drawable != null);

        if (hasImage && (drawable instanceof BitmapDrawable)) {
            hasImage = ((BitmapDrawable)drawable).getBitmap() != null;
        }

        return hasImage;
    }


    @Override
    public void itemSelectedId(Long Id) {
        DBProductCategory category = databaseHelper.getProductCategoryById(Id);
        if (category != null) {
            mSelectedCategory = category.getName();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (productCategoriesSpinner != null && productCategories.isEmpty()) {
            productCategories = databaseHelper.listMerchantProductCategories(sessionManager.getMerchantId());
            ArrayList<SingleChoiceSpinnerDialogEntity> spinnerItems = new ArrayList<>();
            for (DBProductCategory p: productCategories) {
                SingleChoiceSpinnerDialogEntity entity = new SingleChoiceSpinnerDialogEntity(p.getName(), p.getId());
                spinnerItems.add(entity);
            }

            Long preselectedCategory = null;
            if (!productCategories.isEmpty()) {
                DBProductCategory productCategory = productCategories.get(0);
                preselectedCategory = productCategory.getId();
                mSelectedCategory = productCategory.getName();
            }

            Intent addCategoryIntent = new Intent(mContext, AddCategoryActivity.class);

            productCategoriesSpinner.setItems(
                    spinnerItems,
                    "Select One",
                    this,
                    "Select Category",
                    preselectedCategory,
                    addCategoryIntent,
                    "No Categories Found",
                    "Create Category");
        }
    }
}
