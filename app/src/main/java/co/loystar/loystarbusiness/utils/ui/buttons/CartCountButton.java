package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/27/17.
 */

public class CartCountButton extends RelativeLayout {
    private Context context;
    private AttributeSet attrs;
    private int styleAttr;
    private TextView cartCountTextView;
    private ImageView cartImageView;
    private TextView checkoutTextView;

    public CartCountButton(Context context) {
        super(context);
        this.context = context;
        initView();
    }

    public CartCountButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
        initView();
    }

    public CartCountButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.attrs = attrs;
        this.styleAttr = defStyleAttr;
        initView();
    }

    private void initView() {
        inflate(context, R.layout.cart_count_button,this);
        int defaultColor = ContextCompat.getColor(context, R.color.green);
        final int defaultTextColor = ContextCompat.getColor(context, R.color.white);
        final Drawable defaultDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.full_rectangle_button);
        final Drawable cartImageDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_add_shopping_cart_white_24px);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CartCountButton, styleAttr, 0);
        int backgroundColor = a.getColor(R.styleable.CartCountButton_backGroundColor, defaultColor);
        Drawable drawableRight = AppCompatResources.getDrawable(getContext(), R.drawable.ic_chevron_right_white_32px);

        cartImageView = findViewById(R.id.cartImageView);
        cartCountTextView = findViewById(R.id.cartCountText);
        ImageView chevImage = findViewById(R.id.chevronRight);
        checkoutTextView = findViewById(R.id.checkoutText);

        cartImageView.setImageDrawable(cartImageDrawable);
        cartCountTextView.setTextColor(defaultTextColor);
        checkoutTextView.setTextColor(defaultTextColor);
        cartCountTextView.setTypeface(App.getInstance().getTypeface());
        chevImage.setImageDrawable(drawableRight);

        assert  defaultDrawable != null;
        defaultDrawable.setColorFilter(new PorterDuffColorFilter(backgroundColor, PorterDuff.Mode.SRC));
        setBackground(defaultDrawable);

        a.recycle();
    }

    public void setCartCount(String count) {
        cartCountTextView.setText(count);
    }

    public ImageView getCartImageView() {
        return cartImageView;
    }

    public void setCheckoutText(String currency, String price) {
        checkoutTextView.setText(context.getString(R.string.checkout, currency, price));;
    }
}
