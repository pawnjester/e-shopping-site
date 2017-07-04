package co.loystar.loystarbusiness.utils.ui.Buttons;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.LoystarApplication;

/**
 * Created by laudbruce-tagoe on 1/13/17.
 */

public class BrandButtonTransparent extends android.support.v7.widget.AppCompatButton {
    private Context context;
    public BrandButtonTransparent(Context context) {
        super(context);
        this.context = context;
    }
    public BrandButtonTransparent(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public BrandButtonTransparent(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    private void init() {
        if (isInEditMode()){
            return;
        }
        setBackgroundResource(R.drawable.brand_button_transparent);
        setTextAppearance(context, android.R.style.TextAppearance_Medium);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        setTypeface(LoystarApplication.getInstance().getTypeface());
    }
}
