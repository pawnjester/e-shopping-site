package co.loystar.loystarbusiness.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import co.loystar.loystarbusiness.R;

/**
 * Created by laudbruce-tagoe on 4/13/16.
 */
public class StampsImageViewAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> totalStamps;

    public StampsImageViewAdapter(Context context, List<String> totalStamps) {
        this.mContext = context;
        this.totalStamps = totalStamps;
    }

    @Override
    public int getCount() {
        return totalStamps.size();
    }

    @Override
    public Object getItem(int position) {
        return totalStamps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    public List<String>  getList(){
        return totalStamps;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        String txt = "" + (position + 1);

        ViewHolder mViewHolder;

        if (convertView == null) {

            mViewHolder = new ViewHolder();
            convertView = inflater.inflate(R.layout.stamps_grid_layout, parent, false);

            mViewHolder.itemLabel = (TextView) convertView.findViewById(R.id.grid_item_label);

            mViewHolder.itemImage = (ImageView) convertView.findViewById(R.id.grid_item_image);

            convertView.setTag(mViewHolder);

        } else {
            mViewHolder = (ViewHolder) convertView.getTag();
        }

        String stamp = totalStamps.get(position);
        if (stamp.equals("CHECKED")) {
            mViewHolder.itemImage.setImageResource(R.drawable.ic_tick);
            mViewHolder.itemLabel.setText(txt);
        } else {
            mViewHolder.itemLabel.setText(txt);
        }

        return convertView;
    }

    static class ViewHolder {
        private TextView itemLabel;
        private ImageView itemImage;
    }
}
