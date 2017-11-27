package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/25/17.
 */

public class ClearCartButton extends AppCompatButton {
    public ClearCartButton(Context context) {
        super(context);
        init();
    }

    public ClearCartButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ClearCartButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }
        final Drawable drawable = AppCompatResources.getDrawable(getContext(), R.drawable.full_rectangle_button);
        int defaultColor = ContextCompat.getColor(getContext(), android.R.color.holo_red_dark);
        final Drawable clearCart = AppCompatResources.getDrawable(getContext(), R.drawable.ic_remove_shopping_cart_white_48px);
        if (clearCart != null) {
            setCompoundDrawablesWithIntrinsicBounds(clearCart, null, null, null);
        }

        assert drawable != null;
        drawable.setColorFilter(new PorterDuffColorFilter(defaultColor, PorterDuff.Mode.SRC));

        setPadding(30, 0, 30, 0);
        setBackground(drawable);
        setTextSize(16);
        setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        setTypeface(App.getInstance().getTypeface());
    }
}
