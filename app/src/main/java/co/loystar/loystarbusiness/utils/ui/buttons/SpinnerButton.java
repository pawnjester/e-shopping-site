package co.loystar.loystarbusiness.utils.ui.buttons;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.AppCompatButton;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.DatePicker;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import co.loystar.loystarbusiness.App;
import co.loystar.loystarbusiness.R;
import co.loystar.loystarbusiness.utils.ui.TextUtilsHelper;

/**
 * Created by ordgen on 11/13/17.
 */

public class SpinnerButton extends AppCompatButton implements View.OnClickListener {
    private static final String TAG = SpinnerButton.class.getSimpleName();
    private String mPrompt;
    private CharSequence[] mEntries;
    private int mSelection;
    private OnItemSelectedListener mListener;
    private boolean mAllowMultipleSelection = false;
    private boolean isCalendarView = false;
    private Context mContext;
    private boolean[] mPreSelectedEntries;
    private List<String> mSelectedEntries = new ArrayList<>();
    private Date dateSelection;
    private OnDatePickedListener datePickedListener;
    private CreateNewItemListener createNewItemListener;
    private String createNewItemDialogTitle;

    public SpinnerButton(Context context) {
        super(context);
        this.mContext = context;
        init(null);
    }

    public SpinnerButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        init(attrs);
    }

    public SpinnerButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mContext = context;
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        if (isInEditMode()) {
            return;
        }
        if (attrs != null) {
            TypedArray typedArray = mContext.obtainStyledAttributes(attrs, R.styleable.SpinnerButton);
            mPrompt = typedArray.getString(R.styleable.SpinnerButton_android_prompt);
            mEntries = typedArray.getTextArray(R.styleable.SpinnerButton_android_entries);
            mPreSelectedEntries = new boolean[mEntries.length];
            typedArray.recycle();
        }
        mSelection = -1;
        mPrompt = (mPrompt == null)? "" : mPrompt;
        setText(mPrompt);
        setOnClickListener(this);

        Drawable drawableRight = AppCompatResources.getDrawable(mContext, R.drawable.ic_arrow_drop_down_white_24px);
        int color = ContextCompat.getColor(mContext, R.color.colorPrimary);
        assert drawableRight != null;
        drawableRight.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP));
        setCompoundDrawablesWithIntrinsicBounds(null, null, drawableRight, null);

        setPadding(30, 0, 30, 0);
        setBackgroundResource(R.drawable.brand_button_transparent);
        setTypeface(App.getInstance().getTypeface());
    }

    public String getSelectedItem() {
        if (mSelection < 0 || mSelection >= mEntries.length) {
            return null;
        } else {
            return mEntries[mSelection].toString();
        }
    }

    /**
     * set calendar view
     * */
    public void setCalendarView() {
        isCalendarView = true;
    }

    /**
     * set entries to use for spinner
     * @param mEntries entries
     * */
    public void setEntries(CharSequence[] mEntries) {
        this.mEntries = mEntries;
    }

    public List<String> getSelectedItems() {
        return mSelectedEntries;
    }

    public int getSelectedItemPosition() {
        return mSelection;
    }

    /**
     * Set preselected entry for single-mode
     * @param selection position of entry
     * */
    public void setSelection(int selection) {
        mSelection = selection;

        if (selection < 0) {
            setText(mPrompt);
        } else if (selection < mEntries.length) {
            setText(mEntries[mSelection]);
        }
    }

    /**
     * set prompt text
     * @param mPrompt text to use as propmt
     * */
    public void setPrompt(String mPrompt) {
        this.mPrompt = mPrompt;
    }

    /**
     * set multi-mode for spinner
     * @param mAllowMultipleSelection
     * */
    public void setAllowMultipleSelection(boolean mAllowMultipleSelection) {
        this.mAllowMultipleSelection = mAllowMultipleSelection;
    }

    /**
     * Set preselected entries for multi-mode
     *
     * @param  mPSelectedEntries preselected entries
     */
    public void setSelectedEntries(boolean[] mPSelectedEntries) {
        this.mPreSelectedEntries = mPSelectedEntries;
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mEntries.length; i ++) {
            if (mPSelectedEntries[i]) {
                mSelectedEntries.add(mEntries[i].toString());
                stringBuilder.append(mEntries[i]).append(", ");
            }
        }
        setText(stringBuilder.toString());
    }

    public void setDateSelection(Date dateSelection) {
        this.dateSelection = dateSelection;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(dateSelection);
        setText(TextUtilsHelper.getFormattedDateString(calendar));
    }

    public Date getDateSelection() {
        return dateSelection;
    }

    public void setListener(OnItemSelectedListener listener) {
        mListener = listener;
    }

    @Override
    public void onClick(View view) {
        final List<CharSequence> entries = Arrays.asList(mEntries);
        if (isCalendarView) {
            final Calendar c = Calendar.getInstance();
            if (dateSelection != null) {
                c.setTime(dateSelection);
            }
            int mYear = c.get(Calendar.YEAR);
            int mMonth = c.get(Calendar.MONTH);
            int mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(mContext,
                    (view1, year, monthOfYear, dayOfMonth) -> {
                        String m = (monthOfYear + 1) < 10 ? ("0" + (monthOfYear + 1)) : String.valueOf(monthOfYear + 1);
                        String d = dayOfMonth < 10 ? ("0" + dayOfMonth) : String.valueOf(dayOfMonth);
                        String date = year + "-" + m + "-" + d;
                        StdDateFormat mDateFormat = new StdDateFormat();
                        try {
                            dateSelection = mDateFormat.parse(date);
                            if (datePickedListener != null) {
                                datePickedListener.onDatePicked(dateSelection);
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        setText(date);

                    }, mYear, mMonth, mDay);
            assert datePickerDialog.getWindow() != null;
            datePickerDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
            datePickerDialog.show();
        } else {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
            if (createNewItemListener != null && createNewItemDialogTitle != null) {
                dialogBuilder.setTitle(createNewItemDialogTitle);
                dialogBuilder.setPositiveButton("Create new", (dialogInterface, i) -> {
                    createNewItemListener.onCreateNewItemClicked();
                    dialogInterface.cancel();
                });
                dialogBuilder.setNegativeButton("Cancel", (dialogInterface, i) -> {
                    dialogInterface.cancel();
                });
            } else {
                dialogBuilder.setTitle(mPrompt);
                if (mAllowMultipleSelection) {
                    dialogBuilder.setMultiChoiceItems(mEntries, mPreSelectedEntries, (dialogInterface, which, isChecked) -> {
                        if (isChecked) {
                            mSelectedEntries.add(entries.get(which).toString());
                        } else {
                            mSelectedEntries.remove(entries.get(which).toString());
                        }
                        if (mListener != null) {
                            mListener.onItemSelected(which);
                        }
                        for (int i = 0; i < entries.size(); i ++) {
                            mPreSelectedEntries[i] = mSelectedEntries.contains(entries.get(i).toString());
                        }
                    })
                            .setPositiveButton("DONE", (dialogInterface, i) -> {
                                if (mSelectedEntries.isEmpty()) {
                                    setText(mPrompt);
                                } else {
                                    StringBuilder stringBuilder = new StringBuilder();
                                    for (String s: mSelectedEntries) {
                                        stringBuilder.append(s).append(", ");
                                    }
                                    setText(stringBuilder.toString());
                                }
                            });
                } else {
                    SpinnerButtonAdapter mAdapter = new SpinnerButtonAdapter(mContext, R.layout.checked_view, entries);
                    dialogBuilder.setSingleChoiceItems(mAdapter, mSelection, (dialogInterface, which) -> {
                        mSelection = which;
                        setText(mEntries[mSelection]);
                        if (mListener != null) {
                            mListener.onItemSelected(which);
                        }
                        dialogInterface.dismiss();
                    });
                }
            }
            dialogBuilder.create().show();
        }
    }

    public interface OnItemSelectedListener {
        void onItemSelected(int position);
    }

    public interface OnDatePickedListener {
        void onDatePicked(Date date);
    }

    public interface CreateNewItemListener {
        void onCreateNewItemClicked();
    }

    public void setCreateNewItemListener(CreateNewItemListener createNewItemListener) {
        this.createNewItemListener = createNewItemListener;
    }

    public void setCreateNewItemDialogTitle(String createNewItemDialogTitle) {
        this.createNewItemDialogTitle = createNewItemDialogTitle;
    }

    public void setDatePickedListener(OnDatePickedListener datePickedListener) {
        this.datePickedListener = datePickedListener;
    }

    private class SpinnerButtonAdapter extends ArrayAdapter<CharSequence> {
        private List<CharSequence> mEntries;
        private Context mContext;

        SpinnerButtonAdapter(@NonNull Context context, @LayoutRes int resource, List<CharSequence> entries) {
            super(context, resource);
            mEntries = entries;
            mContext = context;
        }

        ViewHolder holder;

        class ViewHolder {
            CheckedTextView checkedTextView;
        }

        @Override
        public int getCount() {
            return mEntries.size();
        }

        @Nullable
        @Override
        public String getItem(int position) {
            return mEntries.get(position).toString();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            final LayoutInflater inflater = LayoutInflater.from(mContext);
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.checked_view, parent, false);;

                holder = new ViewHolder();
                holder.checkedTextView = convertView.findViewById(R.id.checked_list_item);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.checkedTextView.setText(mEntries.get(position));
            return convertView;
        }
    }
}
