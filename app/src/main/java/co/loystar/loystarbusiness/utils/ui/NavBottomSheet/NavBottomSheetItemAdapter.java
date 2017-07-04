package co.loystar.loystarbusiness.utils.ui.NavBottomSheet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 3/12/17.
 */

class NavBottomSheetItemAdapter extends BaseAdapter {
    protected LayoutInflater inflater;
    private List bsItems;
    private Context context;

    static class ViewHolder{
        ImageView image;
        TextView text;
    }

    NavBottomSheetItemAdapter(Context context, List bsItems){
        this.bsItems = bsItems;
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return bsItems.size();
    }

    @Override
    public Object getItem(int i) {
        return bsItems.get(i);
    }

    @Override
    public long getItemId(int i) {
        NavBottomSheetItem item = (NavBottomSheetItem) bsItems.get(i);
        return item.getImage();
    }

    @Override
    public View getView(int i, View convertView, ViewGroup parent) {
        ViewHolder holder;
        NavBottomSheetItem item = (NavBottomSheetItem) bsItems.get(i);

        if( convertView == null ){
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.nav_bottom_sheet_items, parent, false);
            convertView.setTag( holder );

            holder.image = (ImageView) convertView.findViewById(R.id.bs_image);
            holder.text = (TextView) convertView.findViewById(R.id.bs_text);
        }
        else{
            holder = (ViewHolder) convertView.getTag();
        }

        Drawable drawable = ContextCompat.getDrawable(context, item.getImage());
        if (drawable != null && drawable.getConstantState() != null) {
            Drawable willBeLightDark = drawable.getConstantState().newDrawable();
            PorterDuffColorFilter greyFilter = new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            willBeLightDark.mutate().setColorFilter(greyFilter);
            holder.image.setImageDrawable(willBeLightDark);
        }
        holder.text.setText(item.getText());

        return convertView;
    }
}
