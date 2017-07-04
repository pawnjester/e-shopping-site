package co.loystar.loystarbusiness.utils.ui.Buttons;

import android.content.Context;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.utils.LoystarApplication;

/**
 * Created by laudbruce-tagoe on 4/4/17.
 */

public class SpinnerButton extends AppCompatButton {
    public SpinnerButton(Context context) {
        super(context);
        init();
    }

    public SpinnerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SpinnerButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        if (isInEditMode()) {
            return;
        }
        setBackgroundResource(android.R.drawable.btn_dropdown);
        setTypeface(LoystarApplication.getInstance().getTypeface());
    }
}
