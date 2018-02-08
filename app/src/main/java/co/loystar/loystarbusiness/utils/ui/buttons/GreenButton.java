package co.loystar.loystarbusiness.utils.ui.buttons;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 2/7/18.
 */

public class GreenButton extends AppCompatButton {
    private Context context;

    public GreenButton(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public GreenButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public GreenButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    private void init() {
        if (isInEditMode()){
            return;
        }

        setBackgroundResource(R.drawable.green_button);
        setTextAppearance(context, android.R.style.TextAppearance_Medium);
        setPadding(30, 0, 30, 0);
        setTextColor(ContextCompat.getColor(getContext(), R.color.white));
        setTypeface(App.getInstance().getTypeface());
    }
}
