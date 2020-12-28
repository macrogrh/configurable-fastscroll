/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.macrogrh.configurablefastscroll;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.MotionEvent;

import androidx.annotation.DimenRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.macrogrh.configurablefastscroll.R;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/*
 * modified recycler-view from The Android Open Source Project ( /recyclerview/recyclerview/src/main/java/androidx/recyclerview/widget/FastScroller.java )
 */
public class ConfigurableFastScroll extends RecyclerView.ItemDecoration implements RecyclerView.OnItemTouchListener {
    private static final String TAG = ConfigurableFastScroll.class.getSimpleName();
    private final float mThumbHeight;
    private final int mMinimumThumbTouchArea;

    @IntDef({STATE_HIDDEN, STATE_VISIBLE, STATE_DRAGGING})
    @Retention(RetentionPolicy.SOURCE)
    private @interface State {
    }

    // Scroll thumb not showing
    private static final int STATE_HIDDEN = 0;
    // Scroll thumb visible and moving along with the scrollbar
    private static final int STATE_VISIBLE = 1;
    // Scroll thumb being dragged by user
    private static final int STATE_DRAGGING = 2;

    @IntDef({DRAG_X, DRAG_Y, DRAG_NONE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface DragState {
    }

    private static final int DRAG_NONE = 0;
    private static final int DRAG_X = 1;
    private static final int DRAG_Y = 2;

    @IntDef({ANIMATION_STATE_OUT, ANIMATION_STATE_FADING_IN, ANIMATION_STATE_IN,
            ANIMATION_STATE_FADING_OUT})
    @Retention(RetentionPolicy.SOURCE)
    private @interface AnimationState {
    }

    private static final int ANIMATION_STATE_OUT = 0;
    private static final int ANIMATION_STATE_FADING_IN = 1;
    private static final int ANIMATION_STATE_IN = 2;
    private static final int ANIMATION_STATE_FADING_OUT = 3;

    private static final int SHOW_DURATION_MS = 500;
    private static final int HIDE_DELAY_AFTER_VISIBLE_MS = 1500;
    private static final int HIDE_DELAY_AFTER_DRAGGING_MS = 1200;
    private static final int HIDE_DURATION_MS = 500;
    private static final int SCROLLBAR_FULL_OPAQUE = 255;

    private static final int[] PRESSED_STATE_SET = new int[]{android.R.attr.state_pressed};
    private static final int[] EMPTY_STATE_SET = new int[]{};

    private final int mScrollbarMinimumRange;
    private final int mVerticalMargin;
    private final int mRightMargin;

    // Final values for the vertical scroll bar
    private final StateListDrawable mVerticalThumbDrawable;
    private final int mVerticalThumbWidth;

    // Dynamic values for the vertical scroll bar
    @VisibleForTesting
    int mVerticalThumbHeight;
    @VisibleForTesting
    int mVerticalThumbCenterY;
    @VisibleForTesting
    float mVerticalDragY;

    // Dynamic values for the horizontal scroll bar
    @VisibleForTesting
    int mHorizontalThumbCenterX;
    @VisibleForTesting
    float mHorizontalDragX;

    private int mRecyclerViewWidth = 0;
    private int mRecyclerViewHeight = 0;

    private RecyclerView mRecyclerView;
    /**
     * Whether the document is long/wide enough to require scrolling. If not, we don't show the
     * relevant scroller.
     */
    private boolean mNeedVerticalScrollbar = false;

    @State
    private int mState = STATE_HIDDEN;
    @DragState
    private int mDragState = DRAG_NONE;

    private final int[] mVerticalRange = new int[2];
    private final int[] mHorizontalRange = new int[2];
    private final ValueAnimator mShowHideAnimator = ValueAnimator.ofFloat(0, 1);
    @AnimationState
    private int mAnimationState = ANIMATION_STATE_OUT;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide(HIDE_DURATION_MS);
        }
    };
    private final RecyclerView.OnScrollListener
            mOnScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            updateScrollPosition(recyclerView.computeHorizontalScrollOffset(),
                    recyclerView.computeVerticalScrollOffset());
        }
    };


    ConfigurableFastScroll(RecyclerView recyclerView, StateListDrawable verticalThumbDrawable,
                           float thumbHeight, float thumbTopBottomMargin, float thumbRightMargin) {

        recyclerView.setVerticalScrollBarEnabled(false);
        mVerticalThumbDrawable = verticalThumbDrawable;

        if (thumbHeight > 0) {
            mThumbHeight = thumbHeight;
        } else {
            mThumbHeight = mVerticalThumbDrawable.getIntrinsicHeight();
        }

        mRightMargin = (int) thumbRightMargin;

        mMinimumThumbTouchArea = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, recyclerView.getResources().getDisplayMetrics());

        mVerticalThumbWidth = (int) (verticalThumbDrawable.getIntrinsicWidth() / (float) verticalThumbDrawable.getIntrinsicHeight() * mThumbHeight);

        mScrollbarMinimumRange = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, recyclerView.getResources().getDisplayMetrics());

        mVerticalMargin = (int) thumbTopBottomMargin;

        mVerticalThumbDrawable.setAlpha(SCROLLBAR_FULL_OPAQUE);

        mShowHideAnimator.addListener(new ConfigurableFastScroll.AnimatorListener());
        mShowHideAnimator.addUpdateListener(new ConfigurableFastScroll.AnimatorUpdater());

        attachToRecyclerView(recyclerView);
    }

    public void attachToRecyclerView(@Nullable RecyclerView recyclerView) {
        if (mRecyclerView == recyclerView) {
            return; // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks();
        }
        mRecyclerView = recyclerView;
        if (mRecyclerView != null) {
            setupCallbacks();
        }
    }

    private void setupCallbacks() {
        mRecyclerView.addItemDecoration(this);
        mRecyclerView.addOnItemTouchListener(this);
        mRecyclerView.addOnScrollListener(mOnScrollListener);
    }

    private void destroyCallbacks() {
        mRecyclerView.removeItemDecoration(this);
        mRecyclerView.removeOnItemTouchListener(this);
        mRecyclerView.removeOnScrollListener(mOnScrollListener);
        cancelHide();
    }

    private void requestRedraw() {
        mRecyclerView.invalidate();
    }

    private void setState(@State int state) {
        if (state == STATE_DRAGGING && mState != STATE_DRAGGING) {
            mVerticalThumbDrawable.setState(PRESSED_STATE_SET);
            cancelHide();
        }

        if (state == STATE_HIDDEN) {
            requestRedraw();
        } else {
            show();
        }

        if (mState == STATE_DRAGGING && state != STATE_DRAGGING) {
            mVerticalThumbDrawable.setState(EMPTY_STATE_SET);
            resetHideDelay(HIDE_DELAY_AFTER_DRAGGING_MS);
        } else if (state == STATE_VISIBLE) {
            resetHideDelay(HIDE_DELAY_AFTER_VISIBLE_MS);
        }
        mState = state;
    }

    private boolean isLayoutRTL() {
        return ViewCompat.getLayoutDirection(mRecyclerView) == ViewCompat.LAYOUT_DIRECTION_RTL;
    }

    public void show() {
        switch (mAnimationState) {
            case ANIMATION_STATE_FADING_OUT:
                mShowHideAnimator.cancel();
                // fall through
            case ANIMATION_STATE_OUT:
                mAnimationState = ANIMATION_STATE_FADING_IN;
                mShowHideAnimator.setFloatValues((float) mShowHideAnimator.getAnimatedValue(), 1);
                mShowHideAnimator.setDuration(SHOW_DURATION_MS);
                mShowHideAnimator.setStartDelay(0);
                mShowHideAnimator.start();
                break;
        }
    }

    public void hide() {
        hide(0);
    }

    @VisibleForTesting
    void hide(int duration) {
        switch (mAnimationState) {
            case ANIMATION_STATE_FADING_IN:
                mShowHideAnimator.cancel();
                // fall through
            case ANIMATION_STATE_IN:
                mAnimationState = ANIMATION_STATE_FADING_OUT;
                mShowHideAnimator.setFloatValues((float) mShowHideAnimator.getAnimatedValue(), 0);
                mShowHideAnimator.setDuration(duration);
                mShowHideAnimator.start();
                break;
        }
    }

    private void cancelHide() {
        mRecyclerView.removeCallbacks(mHideRunnable);
    }

    private void resetHideDelay(int delay) {
        cancelHide();
        mRecyclerView.postDelayed(mHideRunnable, delay);
    }

    @Override
    public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        if (mRecyclerViewWidth != mRecyclerView.getWidth()
                || mRecyclerViewHeight != mRecyclerView.getHeight()) {
            mRecyclerViewWidth = mRecyclerView.getWidth();
            mRecyclerViewHeight = mRecyclerView.getHeight();
            // This is due to the different events ordering when keyboard is opened or
            // retracted vs rotate. Hence to avoid corner cases we just disable the
            // scroller when size changed, and wait until the scroll position is recomputed
            // before showing it back.
            setState(STATE_HIDDEN);
            return;
        }

        if (mAnimationState != ANIMATION_STATE_OUT) {
            if (mNeedVerticalScrollbar) {
                drawVerticalScrollbar(canvas);
            }
        }
    }

    private void drawVerticalScrollbar(Canvas canvas) {
        int viewWidth = mRecyclerViewWidth;

        int left = viewWidth - mVerticalThumbWidth - mRightMargin;
        int top = mVerticalThumbCenterY - (mVerticalThumbHeight / 2);

        mVerticalThumbDrawable.setBounds(0, 0, mVerticalThumbWidth, mVerticalThumbHeight);

        canvas.translate(left, 0);
        canvas.translate(0, top);
        mVerticalThumbDrawable.draw(canvas);
        canvas.translate(-left, -top);
    }

    /**
     * Notify the scroller of external change of the scroll, e.g. through dragging or flinging on
     * the view itself.
     *
     * @param offsetX The new scroll X offset.
     * @param offsetY The new scroll Y offset.
     */
    void updateScrollPosition(int offsetX, int offsetY) {
        int verticalContentLength = mRecyclerView.computeVerticalScrollRange();
        int verticalVisibleLength = mRecyclerViewHeight;
        mNeedVerticalScrollbar = verticalContentLength - verticalVisibleLength > 0 && mRecyclerViewHeight >= mScrollbarMinimumRange;
        mNeedVerticalScrollbar = true;

        if (!mNeedVerticalScrollbar) {
            if (mState != STATE_HIDDEN) {
                setState(STATE_HIDDEN);
            }
            return;
        }

        if (mNeedVerticalScrollbar) {
            mVerticalThumbHeight = (int) Math.min(verticalVisibleLength, mThumbHeight);
            float ratioOffsetY = offsetY / (float) (verticalContentLength - verticalVisibleLength);
            mVerticalThumbCenterY = (int) ((int) (ratioOffsetY * (verticalVisibleLength - mThumbHeight - (2 * mVerticalMargin))) + mThumbHeight / 2 + mVerticalMargin);
        }

        if (mState == STATE_HIDDEN || mState == STATE_VISIBLE) {
            setState(STATE_VISIBLE);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView,
                                         @NonNull MotionEvent ev) {
        final boolean handled;
        if (mState == STATE_VISIBLE) {
            boolean insideVerticalThumb = isPointInsideVerticalThumb(ev.getX(), ev.getY());
            if (ev.getAction() == MotionEvent.ACTION_DOWN && (insideVerticalThumb)) {
                if (insideVerticalThumb) {
                    mDragState = DRAG_Y;
                    mVerticalDragY = (int) ev.getY();
                }

                setState(STATE_DRAGGING);
                handled = true;
            } else {
                handled = false;
            }
        } else if (mState == STATE_DRAGGING) {
            handled = true;
        } else {
            handled = false;
        }
        return handled;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent me) {
        if (mState == STATE_HIDDEN) {
            return;
        }

        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            boolean insideVerticalThumb = isPointInsideVerticalThumb(me.getX(), me.getY());
            if (insideVerticalThumb) {
                mDragState = DRAG_Y;
                mVerticalDragY = (int) me.getY();
                setState(STATE_DRAGGING);
            }
        } else if (me.getAction() == MotionEvent.ACTION_UP && mState == STATE_DRAGGING) {
            mVerticalDragY = 0;
            mHorizontalDragX = 0;
            setState(STATE_VISIBLE);
            mDragState = DRAG_NONE;
        } else if (me.getAction() == MotionEvent.ACTION_MOVE && mState == STATE_DRAGGING) {
            show();
            if (mDragState == DRAG_X) {
                horizontalScrollTo(me.getX());
            }
            if (mDragState == DRAG_Y) {
                verticalScrollTo(me.getY());
            }
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }

    private void verticalScrollTo(float y) {
        final int[] scrollbarRange = getVerticalRange();
        if (Math.abs(mVerticalThumbCenterY - y) < 2) {
            return;
        }
        int scrollingBy = scrollTo(mVerticalDragY, y, scrollbarRange,
                mRecyclerView.computeVerticalScrollRange(),
                mRecyclerView.computeVerticalScrollOffset(), mRecyclerViewHeight - mVerticalThumbHeight);

        if (scrollingBy != 0) {
            mRecyclerView.scrollBy(0, scrollingBy);
        }
        mVerticalDragY = y;
    }

    private void horizontalScrollTo(float x) {
        final int[] scrollbarRange = getHorizontalRange();
        x = Math.max(scrollbarRange[0], Math.min(scrollbarRange[1], x));
        if (Math.abs(mHorizontalThumbCenterX - x) < 2) {
            return;
        }

        int scrollingBy = scrollTo(mHorizontalDragX, x, scrollbarRange,
                mRecyclerView.computeHorizontalScrollRange(),
                mRecyclerView.computeHorizontalScrollOffset(), mRecyclerViewWidth);
        if (scrollingBy != 0) {
            mRecyclerView.scrollBy(scrollingBy, 0);
        }

        mHorizontalDragX = x;
    }

    private int scrollTo(float oldDragPos, float newDragPos, int[] scrollbarRange, int scrollRange,
                         int scrollOffset, int viewLength) {
        int scrollbarLength = scrollbarRange[1] - scrollbarRange[0];
        if (scrollbarLength == 0) {
            return 0;
        }
        float percentage = ((newDragPos - oldDragPos) / (float) scrollbarLength);
        int totalPossibleOffset = scrollRange - viewLength;
        int scrollingBy = (int) (percentage * totalPossibleOffset);
        int absoluteOffset = scrollOffset + scrollingBy;
        if (absoluteOffset < totalPossibleOffset) {
            return scrollingBy;
        } else {
            return 0;
        }
    }

    @VisibleForTesting
    boolean isPointInsideVerticalThumb(float x, float y) {
        boolean checkY;

        if (mVerticalThumbHeight >= mMinimumThumbTouchArea) {
            checkY = y >= mVerticalThumbCenterY - mVerticalThumbHeight / 2
                    && y <= mVerticalThumbCenterY + mVerticalThumbHeight / 2;
        } else {
            checkY = y >= mVerticalThumbCenterY - mMinimumThumbTouchArea / 2
                    && y <= mVerticalThumbCenterY + mMinimumThumbTouchArea / 2;
        }

        if (mVerticalThumbWidth >= mMinimumThumbTouchArea) {
            return x >= mRecyclerViewWidth - mVerticalThumbWidth - mRightMargin
                    && x <= mRecyclerViewWidth - mRightMargin
                    && checkY;
        } else {
            return x >= mRecyclerViewWidth - mMinimumThumbTouchArea - mRightMargin + (mMinimumThumbTouchArea - mVerticalThumbWidth) / 2
                    && x <= mRecyclerViewWidth - mRightMargin + (mMinimumThumbTouchArea - mVerticalThumbWidth) / 2
                    && checkY;
        }
    }

    /**
     * Gets the (min, max) vertical positions of the vertical scroll bar.
     */
    private int[] getVerticalRange() {
        mVerticalRange[0] = mVerticalMargin;
        mVerticalRange[1] = mRecyclerViewHeight - mVerticalMargin;
        return mVerticalRange;
    }

    /**
     * Gets the (min, max) horizontal positions of the horizontal scroll bar.
     */
    private int[] getHorizontalRange() {
        mHorizontalRange[0] = 0;
        mHorizontalRange[1] = mRecyclerViewWidth;
        return mHorizontalRange;
    }

    public static class Builder {
        private final RecyclerView recyclerview;
        private float thumbHeight;
        private float thumbTopBottomMargin = 0;
        private float thumbRightMargin = 0;
        private int thumbDrawable = R.drawable.thumb_drawable_default;
        private int thumbTint = 0;

        public Builder(RecyclerView recyclerview) {
            this.recyclerview = recyclerview;
        }

        public Builder setThumbDrawable(int thumb_drawable) {
            this.thumbDrawable = thumb_drawable;
            return this;
        }

        public RecyclerView.ItemDecoration build() {

            Drawable drawable = ContextCompat.getDrawable(recyclerview.getContext(), thumbDrawable);

            StateListDrawable stateListDrawable;
            try {
                stateListDrawable = (StateListDrawable) drawable;
            } catch (ClassCastException e) {
                e.printStackTrace();
                stateListDrawable = new StateListDrawable();
                stateListDrawable.addState(new int[]{}, drawable);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (stateListDrawable != null && thumbTint != 0) {
                    stateListDrawable.setTint(thumbTint);
                }
            }

            return new ConfigurableFastScroll(recyclerview,
                    stateListDrawable,
                    thumbHeight,
                    thumbTopBottomMargin,
                    thumbRightMargin
            );
        }

        public Builder setHeight(@DimenRes int thumb_height) {
            this.thumbHeight = recyclerview.getResources().getDimension(thumb_height);
            return this;
        }

        public Builder setTopBottomMargin(@DimenRes int thumb_top_bottom_margin) {
            this.thumbTopBottomMargin = recyclerview.getResources().getDimension(thumb_top_bottom_margin);
            return this;
        }

        public Builder setRightMargin(@DimenRes int thumb_right_margin) {
            this.thumbRightMargin = recyclerview.getResources().getDimension(thumb_right_margin);
            return this;
        }

        public Builder setThumbTint(int thumbTint) {
            this.thumbTint = recyclerview.getResources().getColor(thumbTint);

            return this;
        }
    }

    private class AnimatorListener extends AnimatorListenerAdapter {

        private boolean mCanceled = false;

        @Override
        public void onAnimationEnd(Animator animation) {
            // Cancel is always followed by a new directive, so don't update state.
            if (mCanceled) {
                mCanceled = false;
                return;
            }
            if ((float) mShowHideAnimator.getAnimatedValue() == 0) {
                mAnimationState = ANIMATION_STATE_OUT;
                setState(STATE_HIDDEN);
            } else {
                mAnimationState = ANIMATION_STATE_IN;
                requestRedraw();
            }
        }

        @Override
        public void onAnimationCancel(Animator animation) {
            mCanceled = true;
        }
    }

    private class AnimatorUpdater implements ValueAnimator.AnimatorUpdateListener {

        @Override
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            int alpha = (int) (SCROLLBAR_FULL_OPAQUE * ((float) valueAnimator.getAnimatedValue()));
            mVerticalThumbDrawable.setAlpha(alpha);
            requestRedraw();
        }
    }
}
