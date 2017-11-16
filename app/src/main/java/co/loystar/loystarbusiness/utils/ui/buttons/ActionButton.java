package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.Gravity;

import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/15/17.
 */

public class ActionButton extends AppCompatButton {
    private Context mContext;
    public ActionButton(Context context) {
        super(context);
        mContext = context;
        init(null);
    }

    public ActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init(attrs);
    }

    public ActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        Drawable defaultDrawable = AppCompatResources.getDrawable(mContext, R.drawable.ic_mode_edit_black_24px);
        Drawable drawableToUse = null;
        if (attrs != null) {
            TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.ActionButton);
            drawableToUse = typedArray.getDrawable(R.styleable.ActionButton_backgroundDrawable);
            typedArray.recycle();
        }
        if (drawableToUse == null) {
            drawableToUse = defaultDrawable;
        }
        if (drawableToUse != null) {
            drawableToUse.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            setCompoundDrawablesWithIntrinsicBounds(null, drawableToUse, null, null);
        }
        setBackgroundResource(R.drawable.brand_button_transparent);
    }
}
