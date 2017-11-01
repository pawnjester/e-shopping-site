package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/1/17.
 */

public class BrandButtonNormal extends AppCompatButton {
    private Context context;
    public BrandButtonNormal(Context context) {
        super(context);
        this.context = context;
    }

    public BrandButtonNormal(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public BrandButtonNormal(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
        init();
    }

    private void init() {
        if (isInEditMode()){
            return;
        }
        setBackgroundResource(R.drawable.brand_button_normal);
        setTextAppearance(context, android.R.style.TextAppearance_Medium);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        setTypeface(App.getInstance().getTypeface());
    }
}
