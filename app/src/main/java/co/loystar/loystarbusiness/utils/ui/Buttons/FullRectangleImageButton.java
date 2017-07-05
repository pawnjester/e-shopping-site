package co.loystar.loystarbusiness.utils.ui.Buttons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatDrawableManager;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 4/5/17.
 */

public class FullRectangleImageButton extends AppCompatImageButton {
    private Context context;
    private AttributeSet attrs;
    private int styleAttr;

    public FullRectangleImageButton(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public FullRectangleImageButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        this.attrs = attrs;
        init();
    }

    public FullRectangleImageButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.attrs = attrs;
        this.styleAttr = defStyleAttr;
    }

    private void init() {
        final Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.full_rectangle_button);
        int defaultColor = ContextCompat.getColor(context, R.color.colorPrimary);
        TypedArray a = context.obtainStyledAttributes(attrs,R.styleable.FullRectangleImageButton,styleAttr,0);
        int color = a.getColor(R.styleable.FullRectangleImageButton_backGroundColor, defaultColor);

        assert drawable != null;
        drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC));

        setBackground(drawable);

        a.recycle();
    }
}
