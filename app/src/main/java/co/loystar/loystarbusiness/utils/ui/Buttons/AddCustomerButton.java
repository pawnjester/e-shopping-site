package co.loystar.loystarbusiness.utils.ui.Buttons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.LoystarApplication;

/**
 * Created by laudbruce-tagoe on 3/25/17.
 */

public class AddCustomerButton extends android.support.v7.widget.AppCompatButton {
    private Context context;

    public AddCustomerButton(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public AddCustomerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }

        Drawable drawableToUse = AppCompatDrawableManager.get().getDrawable(context, R.drawable.ic_person_add_white_24px);

        drawableToUse.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
        setCompoundDrawablesWithIntrinsicBounds(drawableToUse, null, null, null);
        setCompoundDrawablePadding(8);

        setTextAppearance(context, android.R.style.TextAppearance_Medium);
        setBackgroundResource(R.drawable.digits_button);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        setTypeface(LoystarApplication.getInstance().getTypeface());

    }
}
