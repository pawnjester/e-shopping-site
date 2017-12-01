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
 * Created by ordgen on 12/1/17.
 */

public class PrintButton extends AppCompatButton {
    public PrintButton(Context context) {
        super(context);
        init();
    }

    public PrintButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PrintButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        Drawable printDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_print_white_24px);

        if (printDrawable != null) {
            printDrawable.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            setCompoundDrawablesWithIntrinsicBounds(printDrawable, null, null, null);
            setCompoundDrawablePadding(8);
        }

        setBackgroundResource(R.drawable.brand_button_transparent);
        setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        setTypeface(App.getInstance().getTypeface());
    }
}
