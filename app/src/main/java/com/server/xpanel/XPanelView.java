package com.server.xpanel;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class XPanelView extends FrameLayout {
    /**
     * The view which will be drag.
     */
    protected LinearLayout mDragViewGroup;
    /**
     * Callback when the user touch or drag the view.
     */
    protected com.server.xpanel.XPanelDragMotionDetection mDetection;
    /**
     * Expose whole panel in parent layout percent.
     * values range in {0 - 1} can not be zero.
     */
    protected float mExposedPercent;

    private View mSlideLayout;

    public XPanelView(Context context) {
        super(context);
        init(context, 0);
    }

    public XPanelView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideLayout);
        int slideLayout = a.getResourceId(R.styleable.SlideLayout_slide_layout, 0);

        a.recycle();

        init(context, slideLayout);
    }

    public XPanelView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideLayout);
        int slideLayout = a.getResourceId(R.styleable.SlideLayout_slide_layout, 0);

        a.recycle();

        init(context, slideLayout);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public XPanelView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlideLayout);
        int slideLayout = a.getResourceId(R.styleable.SlideLayout_slide_layout, 0);

        a.recycle();

        init(context, slideLayout);
    }

    private void init(Context context, int layout) {
        LayoutParams groupParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        mDragViewGroup = new LinearLayout(context);
        mDragViewGroup.setOrientation(LinearLayout.VERTICAL);

        addView(mDragViewGroup, groupParams);
        initListView(context, layout);
        initDragHelper();
        mExposedPercent = 0.3f;//the default value is ten percent.
    }

    private void initListView(Context context,int layout) {
        LayoutInflater inflater = LayoutInflater.from(context);
        mSlideLayout = inflater.inflate(layout, null);

        //measure方法的参数值都设为0即可
        mSlideLayout.measure(0,0);
        int height = mSlideLayout.getMeasuredHeight();

        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        mDragViewGroup.addView(mSlideLayout, params);
    }

    private void initDragHelper() {
        mDetection = new com.server.xpanel.XPanelDragMotionDetection(mDragViewGroup, this);
    }

    @Override
    public void computeScroll() {
        if (mDetection.getDragHelper().continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDetection.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getY() >= mDetection.getTop()) {
            return mDetection.processTouchEvent(event);
        } else {
            return false;
        }
    }

    /**
     * Control the whole panel exposed height in parent layout.
     * Avoid modify it in animate.because it very unfriendly with performance.
     * Default value is 30 percent.
     *
     * @param exposedPercent the range in 0 ~ 1. 1 is mean the panel is exposed 100 percent
     */
    public void setExposedPercent(float exposedPercent) {
        exposedPercent = exposedPercent < 0 ? 0.01f : exposedPercent > 1 ? 1 : exposedPercent;
        mExposedPercent = exposedPercent;
        requestLayout();
    }

    /**
     * When the view is in chutty mode,this value is valuable.
     * Drag view and release ,the view will be kick back if release position is not bigger than this percent of parent viewgroup.
     * Default value is 50 percent.you should be bigger than {@link #mExposedPercent}
     *
     * @param percent the range in 0 ~ 1.
     */
    public void setKickBackPercent(float percent) {
        mDetection.setKickBackPercent(percent);
    }

    /**
     * Make the drag view has some stick feeling. something like chutty.
     *
     * @param chuttyMode true is has chutty mode.
     */
    public void setChuttyMode(boolean chuttyMode) {
        mDetection.setChuttyMode(chuttyMode);
    }

    /**
     * Set the XPanel can fling.When your set the chutty mode is true,than this flag is invalid.
     *
     * @param isCanFling true is can fling.
     */
    public void setCanFling(boolean isCanFling) {
        mDetection.setCanFling(isCanFling);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        ViewGroup.LayoutParams params = mDragViewGroup.getLayoutParams();
        params.height = mSlideLayout.getMeasuredHeight();
        mDragViewGroup.setLayoutParams(params);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        View view = getChildAt(0);
        if (view instanceof LinearLayout) {
            if (!mDetection.isOriginState()) {//if is not origin state,the window height definitely changed, so need to restore the height
                int childLeft = getPaddingLeft();
                int topShouldBe = getMeasuredHeight() - mDetection.getOffsetPixel();
                view.layout(childLeft,
                        topShouldBe,
                        childLeft + view.getMeasuredWidth(),
                        topShouldBe + view.getMeasuredHeight());
            } else {
                int childLeft = getPaddingLeft();
                int childTop = getPaddingTop();
                int topOffset;
                if (getMeasuredHeight() * mExposedPercent > view.getMeasuredHeight()) {
                    topOffset = getMeasuredHeight() - view.getMeasuredHeight();
                } else {
                    topOffset = (int) (getMeasuredHeight() - getMeasuredHeight() * (mExposedPercent));
                }
                view.layout(childLeft,
                        childTop + topOffset,
                        childLeft + view.getMeasuredWidth(),
                        childTop + topOffset + view.getMeasuredHeight());
                mDetection.setOriginTop(childTop + topOffset);
            }
        }
    }

    public void setOnXPanelMotionListener(XPanelDragMotionDetection.OnXPanelMotionListener listener) {
        if (mDetection != null) {
            mDetection.setOnXPanelMotionListener(listener);
        }
    }
}
