package view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import androidx.appcompat.graphics.drawable.DrawerArrowDrawable;

import java.util.Objects;

public class BadgeDrawableArrowDrawable extends DrawerArrowDrawable {
    //Fraction of Drawbale's intrinsic sizw we want the badge to be
    private static final float SIZE_FACTOR = .3f;
    private static final float HALF_SIZE_FACTOR = SIZE_FACTOR / 2;

    private Paint backgroundPaint;
    private Paint textPaint;
    private String text;
    private boolean enabled = true;

    public BadgeDrawableArrowDrawable(Context context){
        super(context);

        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.RED);
        backgroundPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setAntiAlias(true);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(SIZE_FACTOR * getIntrinsicHeight());
    }

    @Override
    public void draw(Canvas canvas){
        super.draw(canvas);

        if(!enabled){
            return;
        }

        final Rect bounds = getBounds();
        final float x = (1 - HALF_SIZE_FACTOR) * bounds.width();
        final float y = HALF_SIZE_FACTOR * bounds.height();
        canvas.drawCircle(x, y, SIZE_FACTOR * bounds.width(), backgroundPaint);

        if(text == null || text.length() == 0){
            return;
        }

        final Rect textBounds = new Rect();
        textPaint.getTextBounds(text, 0, text.length(), textBounds);
        canvas.drawText(text, x, y + textBounds.height() / 2f, textPaint);
    }

    public void setEnabled(boolean enabled){
        if(this.enabled != enabled){
           this.enabled = enabled;
           invalidateSelf();
        }
    }

    public boolean isEnabled(){
        return enabled;
    }

    public void setText(String text){
        if(!this.text.equals(text)){
            this.text = text;
            invalidateSelf();
        }
    }

    public String getText(){
        return text;
    }

    public void setBackgroundColor(int color){
        if(backgroundPaint.getColor() != color){
            backgroundPaint.setColor(color);
            invalidateSelf();
        }
    }

    public void setTextColor(int color){
        if(textPaint.getColor() != color){
            textPaint.setColor(color);
            invalidateSelf();
        }
    }

    public int getTextColor(){
        return textPaint.getColor();
    }
}
