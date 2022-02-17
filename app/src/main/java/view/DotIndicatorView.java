package view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;

import com.scit.stauc.R;

public class DotIndicatorView extends LinearLayout {
    private static final String TAG = "DotIndicatorView";
    private final Context context;
    private int indicatorCount;
    private int selectedResource;
    private int defaultResource;
    private int currentIndex;
    private int padding;

    public DotIndicatorView(Context context){
        super(context, null);
        this.context = context;
        setDefaults();
        setUpIndicators();
    }

    public DotIndicatorView(Context context, AttributeSet attrs){
        super(context, attrs);
        this.context = context;
        setDefaults();
        setUpIndicators();
    }

    private void setDefaults(){
        this.setOrientation(HORIZONTAL);
        this.setGravity(Gravity.CENTER);
        this.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        indicatorCount = 4;
        selectedResource = R.drawable.tab_indicator_selected;
        defaultResource = R.drawable.tab_indicator_default;
        currentIndex = 0;
        padding = 1;
    }

    public int getCount(){ return indicatorCount; }
    public int getSelectedResource(){ return selectedResource; }
    public int getDefaultResource(){ return R.drawable.tab_indicator_default; }
    public int getCurrentIndex(){ return currentIndex; }

    public void setCount(int indicatorCount){
        this.indicatorCount = indicatorCount;
        setUpIndicators();
    }

    public void setSelectedResource(int selectedResource){
        this.selectedResource = selectedResource;
        setUpIndicators();
    }

    public void setDefaultResource(int defaultResource){
        this.defaultResource = defaultResource;
        setUpIndicators();
    }

    public void setCurrentIndex(int index){
        currentIndex = index;
        setUpIndicators();
    }


    private void setUpIndicators(){
        removeAllViews();
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT);
        for(int i=0; i<getCount(); i++){
            AspectImageView imageView = new AspectImageView(context);
            imageView.setLayoutParams(params);
            imageView.setPadding(padding, 0, padding, 0);
            if(i == currentIndex){
                imageView.setImageResource(getSelectedResource());
            }else{
                imageView.setImageResource(getDefaultResource());
            }
            addView(imageView);
        }
    }

}
