package view;

// github.com/devunwired/custom-view-examples/

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;

public class AspectImageView extends androidx.appcompat.widget.AppCompatImageView {

    public AspectImageView(Context context){
        super(context);
    }

    public AspectImageView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    public AspectImageView(Context context, AttributeSet attrs, int defStyle){
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec){
        // Figure out the aspect ratio of the imageView
        int desiredSize;
        float aspect;
        Drawable d = getDrawable();
        if(d == null){
            desiredSize = 0;
            aspect = 1f;
        }else{
            desiredSize = d.getIntrinsicWidth();
            aspect =  (float) d.getIntrinsicWidth() / (float) d.getIntrinsicHeight();
        }
        // get the width based on the measure specs
        int widthSize = View.resolveSize(desiredSize, widthMeasureSpec);

        // Calculate height based on aspect
        int heightSize = (int)(widthSize / aspect);

        // Make sure the height we want isn't too large
        int specMode = MeasureSpec.getMode(heightMeasureSpec);
        int specSize = MeasureSpec.getSize(heightMeasureSpec);
        if(specMode == MeasureSpec.AT_MOST || specMode == MeasureSpec.EXACTLY){
            // if our measurement exceeds the maxheight, shrinkback
            if(heightSize > specSize){
                heightSize = specSize;
                widthSize = (int)(heightSize * aspect);
            }
        }

        // MUST do this to store the measurements
        setMeasuredDimension(widthSize, heightSize);
    }
}
