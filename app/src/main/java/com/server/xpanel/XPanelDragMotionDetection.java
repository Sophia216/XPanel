package com.server.xpanel;

import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

public class XPanelDragMotionDetection extends ViewDragHelper.Callback {

    private ViewGroup mDragView;

    private ViewGroup mDragContainer;

    private ViewDragHelper mDragHelper;

    private boolean isChuttyMode;

    private boolean isCeiling;

    private boolean isOriginState;

    private int mOriginTop;

    private int mTop;

    private float mKickBackPercent;

    private int mOffsetPixel;

    private boolean isCanFling;

    private boolean isDragUp;

    private boolean isInFling;

    private boolean isInBaseLine;

    private OnXPanelMotionListener mOnXPanelMotionListener;

    public XPanelDragMotionDetection(ViewGroup dragView, ViewGroup dragContainer) {
        mDragView = dragView;
        mDragContainer = dragContainer;
        mDragHelper = ViewDragHelper.create(mDragContainer, 1.0f, this);
        isOriginState = true;
        mKickBackPercent = 0.5f;
    }

    @Override
    public boolean tryCaptureView(View child, int pointerId) {
        return child == mDragView;
    }

    @Override
    public int clampViewPositionVertical(View child, int top, int dy) {
        int containerHeight = mDragContainer.getMeasuredHeight();

        //resolve base line
        if (dy > 0) {
            //move down
            int currentHeight = containerHeight - top;
            int exposedHeight = containerHeight - mOriginTop;
            if (currentHeight <= exposedHeight) {
                isInBaseLine = true;
                return mOriginTop;
            } else {
                isInBaseLine = false;
            }
        } else {
            isInBaseLine = false;
        }

        int offset = -mDragView.getMeasuredHeight() + containerHeight;
        if (top <= offset) {
            return offset;
        } else {
            return top;
        }
    }

    @Override
    public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
        mOffsetPixel = mDragContainer.getMeasuredHeight() - top;
        mTop = top;
        isDragUp = dy < 0;

        if (top == mOriginTop) {
            isOriginState = true;
        } else {
            isOriginState = false;
        }

        if (mOnXPanelMotionListener != null) {
            int offset = getOffsetPixel();

            if (isInFling) {
                mOnXPanelMotionListener.OnDrag(DragMotion.DRAG_FLING, offset);
            } else {
                if (isDragUp) {
                    mOnXPanelMotionListener.OnDrag(DragMotion.DRAG_UP, offset);
                } else {
                    if (!isInBaseLine) {
                        mOnXPanelMotionListener.OnDrag(DragMotion.DRAG_DOWN, offset);
                    }
                }
            }

            if (top <= 0 != isCeiling) {//call once
                mOnXPanelMotionListener.OnCeiling(top <= 0);
            }
        }

        isCeiling = top <= 0;
    }

    @Override
    public void onViewCaptured(View capturedChild, int activePointerId) {

    }

    @Override
    public void onViewReleased(View releasedChild, float xvel, float yvel) {
        if (isChuttyMode) {
            float threshold = mDragContainer.getMeasuredHeight() * (mKickBackPercent);

            if (mOffsetPixel >= threshold) {//before touch the captured view ,view state is origin state.
                int top = Math.max(mDragContainer.getMeasuredHeight() - mDragView.getMeasuredHeight(), 0);
                mDragHelper.settleCapturedViewAt(0, top);
            } else {
                mDragHelper.settleCapturedViewAt(0, mOriginTop);
            }
        }
        fling();
        ViewCompat.postInvalidateOnAnimation(mDragContainer);
    }

    private void fling() {
        if (!isCanFling || isChuttyMode) {
            return;
        }
        isInFling = true;
        int minTop = mDragContainer.getMeasuredHeight() - mDragView.getMeasuredHeight();
        int maxTop = mOriginTop;
        mDragHelper.flingCapturedView(0, minTop, 0, maxTop);
    }

    @Override
    public void onViewDragStateChanged(int state) {
        if (state == ViewDragHelper.STATE_IDLE) {
            mDragHelper.abort();
            isInFling = false;
            if (mOnXPanelMotionListener != null) {
                int offset = getOffsetPixel();
                mOnXPanelMotionListener.OnDrag(DragMotion.DRAG_STOP, offset);
            }
        }
    }

    @Override
    public int getViewVerticalDragRange(View child) {
        return child.getMeasuredHeight();
    }

    public int getOffsetPixel() {
        return mOffsetPixel;
    }

    public int getTop() {
        return mTop;
    }

    public boolean isOriginState() {
        return isOriginState;
    }

    /**
     * set the kick back percent when the chutty mode is true.
     *
     * @param kickBackPercent range in 0 ~ 1.
     */
    public void setKickBackPercent(float kickBackPercent) {
        if (kickBackPercent < 0) {
            kickBackPercent = 0.01f;
        }
        if (kickBackPercent > 1) {
            kickBackPercent = 1;
        }
        mKickBackPercent = kickBackPercent;
    }

    public void setOriginTop(int originTop) {
        mOriginTop = originTop;
        mOffsetPixel = mOriginTop;
        mTop = originTop;
    }

    public int getOriginTop() {
        return mOriginTop;
    }

    public void setChuttyMode(boolean chuttyMode) {
        isChuttyMode = chuttyMode;
    }

    public ViewDragHelper getDragHelper() {
        return mDragHelper;
    }

    public void setCanFling(boolean canFling) {
        isCanFling = canFling;
    }

    public boolean isCeiling() {
        return isCeiling;
    }

    public boolean isInBaseLine() {
        return isInBaseLine;
    }

    public void setOnXPanelMotionListener(OnXPanelMotionListener onXPanelMotionListener) {
        mOnXPanelMotionListener = onXPanelMotionListener;
    }

    public boolean shouldInterceptTouchEvent(MotionEvent ev) {
        return getDragHelper().shouldInterceptTouchEvent(ev);
    }

    public boolean processTouchEvent(MotionEvent event) {
        getDragHelper().processTouchEvent(event);
        return true;
    }

    public interface DragMotion {
        /**
         * Drag down
         */
        int DRAG_DOWN = 1;
        /**
         * Drag up
         */
        int DRAG_UP = 2;
        /**
         * Drag stop
         */
        int DRAG_STOP = 3;
        /**
         * When not touch but in scrolling.
         */
        int DRAG_FLING = 4;
    }

    public interface OnXPanelMotionListener {
        /**
         * When user drag the view or fling.
         *
         * @param dragMotion drag motion.
         * @param offset     offset from original point.
         */
        void OnDrag(int dragMotion, int offset);

        /**
         * When the drag view touch ceil,top may be small than or equal 0.
         *
         * @param isCeiling true is in ceiling.
         */
        void OnCeiling(boolean isCeiling);
    }
}
