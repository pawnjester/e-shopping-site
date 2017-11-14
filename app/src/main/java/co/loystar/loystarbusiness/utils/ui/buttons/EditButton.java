package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/14/17.
 */

public class EditButton extends AppCompatButton {
    private Context mContext;
    public EditButton(Context context) {
        super(context);
        mContext = context;
        init();

    }

    public EditButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public EditButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
        init();
    }

    private void init() {
        if (isInEditMode()){
            return;
        }

        Drawable drawableToUse = AppCompatResources.getDrawable(mContext, R.drawable.ic_mode_edit_black_24px);

        if (drawableToUse != null) {
            drawableToUse.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            setCompoundDrawablesWithIntrinsicBounds(drawableToUse, null, null, null);
            setCompoundDrawablePadding(8);
        }

        setText(mContext.getString(R.string.edit));
        setBackgroundResource(R.drawable.brand_button_transparent);
        setPadding(30, 0, 30, 0);
    }
}
