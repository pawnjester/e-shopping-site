package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/1/17.
 */

public class BrandButtonTransparent extends AppCompatButton {
    private Context context;
    public BrandButtonTransparent(Context context) {
        super(context);
        this.context = context;
        init(null);
    }
    public BrandButtonTransparent(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init(attrs);
    }

    public BrandButtonTransparent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (isInEditMode()){
            return;
        }
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BrandButtonTransparent);
            Drawable drawable = typedArray.getDrawable(R.styleable.BrandButtonTransparent_backgroundDrawable);
            if (drawable != null) {
                drawable.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
                setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
            }
            typedArray.recycle();
        }

        setBackgroundResource(R.drawable.brand_button_transparent);
        setTextAppearance(context, android.R.style.TextAppearance_Medium);
        setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        setTypeface(App.getInstance().getTypeface());
    }
}