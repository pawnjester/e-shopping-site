package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.content.res.TypedArray;
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
        init();
    }

    private void init() {
        final Drawable drawable = AppCompatResources.getDrawable(context, R.drawable.full_rectangle_button);
        int defaultColor = ContextCompat.getColor(context, R.color.colorPrimary);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FullRectangleButton,styleAttr, 0);
        int color = a.getColor(R.styleable.FullRectangleButton_backGroundColor, defaultColor);

        Drawable drawableRight = AppCompatResources.getDrawable(context, R.drawable.ic_chevron_right_white_32px);

        setCompoundDrawablesWithIntrinsicBounds(null, null, drawableRight, null);

        assert drawable != null;
        drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC));

        setBackground(drawable);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        setTypeface(App.getInstance().getTypeface());

        a.recycle();
    }
}
