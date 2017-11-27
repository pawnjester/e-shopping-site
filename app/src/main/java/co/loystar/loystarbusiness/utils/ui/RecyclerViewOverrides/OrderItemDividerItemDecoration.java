package co.loystar.loystarbusiness.utils.ui.RecyclerViewOverrides;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import co.loystar.loystarbusiness.R;

/**
 * Created by ordgen on 11/26/17.
 */

public class OrderItemDividerItemDecoration extends RecyclerView.ItemDecoration{
    private Drawable mDivider;

    public OrderItemDividerItemDecoration(Context context) {
        mDivider = ResourcesCompat.getDrawable(context.getResources(), R.drawable.thick_horizontal_line, null);
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft();
        int right = parent.getWidth() - parent.getPaddingRight();

        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);

            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

            int top = child.getBottom() + params.bottomMargin;
            int bottom = top + mDivider.getIntrinsicHeight();

            mDivider.setBounds(left, top, right, bottom);
            mDivider.draw(c);
        }
    }
}
