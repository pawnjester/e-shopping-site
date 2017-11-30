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
 * Created by ordgen on 11/30/17.
 */

public class ProductCategoryChip extends AppCompatButton {
    public ProductCategoryChip(Context context) {
        super(context);
        init();
    }

    public ProductCategoryChip(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProductCategoryChip(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        final Drawable deleteDrawable = AppCompatResources.getDrawable(getContext(), R.drawable.ic_close_white_24px);
        if (deleteDrawable != null) {
            deleteDrawable.setColorFilter(ContextCompat.getColor(getContext(), android.R.color.holo_red_dark), PorterDuff.Mode.SRC_ATOP);
            setCompoundDrawablesWithIntrinsicBounds(null, null, deleteDrawable, null);
        }

        setBackgroundResource(R.drawable.brand_button_transparent);
        setTextAppearance(getContext(), android.R.style.TextAppearance_Medium);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
        setTypeface(App.getInstance().getTypeface());
    }
}
