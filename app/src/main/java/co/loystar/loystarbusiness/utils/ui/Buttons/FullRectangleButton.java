package co.loystar.loystarbusiness.utils.ui.Buttons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.AttributeSet;
import android.util.Log;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.LoystarApplication;

/**
 * Created by laudbruce-tagoe on 4/5/17.
 */

public class FullRectangleButton extends AppCompatButton {
    private Context context;
    private AttributeSet attrs;
    private int styleAttr;

    public FullRectangleButton(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public FullRectangleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
        init();
    }

    public FullRectangleButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.attrs = attrs;
        this.styleAttr = defStyleAttr;
    }

    private void init() {
        final Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.full_rectangle_button);
        int defaultColor = ContextCompat.getColor(context, R.color.colorPrimary);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FullRectangleButton,styleAttr, 0);
        int color = a.getColor(R.styleable.FullRectangleButton_backGroundColor, defaultColor);

        Drawable drawableRight = AppCompatResources.getDrawable(context, R.drawable.ic_chevron_right_white_48px);

        setCompoundDrawablesWithIntrinsicBounds(null, null, drawableRight, null);

        assert drawable != null;
        drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC));

        setBackground(drawable);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        setTypeface(LoystarApplication.getInstance().getTypeface());

        a.recycle();
    }
}
