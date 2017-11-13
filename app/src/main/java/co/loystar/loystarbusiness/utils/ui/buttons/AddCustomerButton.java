package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/1/17.
 */

public class AddCustomerButton extends AppCompatButton{
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

        Drawable drawableToUse = AppCompatResources.getDrawable(context, R.drawable.ic_person_add_white_24px);

        if (drawableToUse != null) {
            drawableToUse.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            setCompoundDrawablesWithIntrinsicBounds(drawableToUse, null, null, null);
            setCompoundDrawablePadding(8);
        }

        setBackgroundResource(R.drawable.digits_button);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        setTypeface(App.getInstance().getTypeface());

    }
}
