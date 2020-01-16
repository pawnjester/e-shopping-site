//package co.loystar.loystarbusiness.utils;
//
//import android.content.Context;
//import android.graphics.Canvas;
//import android.graphics.ColorFilter;
//import android.graphics.Paint;
//import android.graphics.Path;
//import android.graphics.RectF;
//import android.graphics.drawable.Drawable;
//import android.support.v7.widget.AppCompatImageView;
//import android.text.TextPaint;
//import android.util.AttributeSet;
//
//import co.loystar.loystarbusiness.models.entities.CustomerEntity;
//
//public class AvatarView extends AppCompatImageView {
//
//    Path clipPath;
//
//    Drawable drawable;
//
//    String text;
//
//    TextPaint textPaint;
//
//    Paint paint;
//
//    private Paint borderPaint;
//
//    int shape;
//
//    CustomerEntity cutomerEntity;
//
//    private int imageSize;
//
//    int cornerRadius;
//
//    RectF rectF;
//
//    /*
//     * Constants to define shape
//     * */
//    protected static final int CIRCLE = 0;
//    protected static final int RECTANGLE = 1;
//
//
//    public AvatarView(Context context) {
//        super(context);
//    }
//
//    public AvatarView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//
//        init();
//    }
//
//    public AvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//
//        init();
//    }
//
//    protected void init() {
//
//        rectF = new RectF();
//        clipPath = new Path();
//
//        text = cutomerEntity.getFirstName().substring(1);
//    }
//
//    public void setUser(CustomerEntity customer) {
//        this.cutomerEntity = customer;
//        setValues();
//    }
//
//    @Override
//    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
//        int screenWidth = MeasureSpec.getSize(widthMeasureSpec);
//        int screenHeight = MeasureSpec.getSize(heightMeasureSpec);
//        rectF.set(0, 0, screenWidth, screenHeight);
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//
//    }
//
//    @Override
//    public void setAlpha(float alpha) {
//        super.setAlpha(alpha);
//
//    }
//
//    @Override
//    public void setColorFilter(ColorFilter cf) {
//        super.setColorFilter(cf);
//
//    }
//
//}
