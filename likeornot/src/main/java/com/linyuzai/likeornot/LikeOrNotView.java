package com.linyuzai.likeornot;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * Created by Administrator on 2016/10/27 0027.
 */

public class LikeOrNotView extends FrameLayout {

    public static final String TAG = LikeOrNotView.class.getSimpleName();

    private State mState = null;
    private RotationDirection mRotationDirection = RotationDirection.CLOCKWISE;
    /**
     * 记录like和nope的操作
     */
    private Stack<Boolean> mOperations = new Stack<>();
    /**
     * 可复用的view
     */
    private List<View> mRecyclerViews = new ArrayList<>();
    /**
     * 是否有层次感
     */
    private boolean isStratified;
    /**
     * 是否可旋转
     */
    private boolean mRotatable;
    /**
     * 旋转的范围（会乘一定比例）
     */
    private float mRotationRange = 5f;
    /**
     * 非手动滑动时like和nope的动画时间
     */
    private long mAnimatorDuration = 300L;
    /**
     * 层次时，缩小的比例
     */
    private float mScale = 0.05f;
    /**
     * 层次时，y轴偏移量
     */
    private float mOffsetY;

    private OnItemClickListener mOnItemClickListener;
    private OnLikeOrNotListener mOnLikeOrNotListener;
    private ViewStateCallback mViewStateCallback;
    /**
     * 最后移动的距离相比手滑动距离的倍数
     */
    private int mMoveMultiplier = 3;
    /**
     * 触发like和nope所需的相对于屏幕宽度的比例
     */
    private float mDragScale = 0.35f;
    /**
     * 手指按下的x
     */
    private float mDownX;
    /**
     * 手指按下的y
     */
    private float mDownY;
    /**
     * 手指移动的x
     */
    private float mMoveX;
    /**
     * 手指移动的y
     */
    private float mMoveY;
    /**
     * 触发like和nope所需的距离
     */
    private float mLimitDragDistance;
    /**
     * 当前的position
     */
    private int mCurrentPosition;
    /**
     * 当前加载的position
     */
    private int mLoadPosition;
    /**
     * 可拖动范围的最大Y
     */
    private int mLimitBottomY;
    /**
     * 是否第一次添加child
     */
    private boolean isChildFirstLayout = true;
    /**
     * 被释放的view
     */
    private View mReleasedView;
    /**
     * 初始位置
     */
    private Point mInitialPosition = new Point();
    /**
     * release后的最终位置
     */
    private Point mFinalPosition = new Point();
    private IAdapter mAdapter;
    private ViewDragHelper mDragHelper = ViewDragHelper.create(this, 1f, new ViewDragHelper.Callback() {
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return mState == State.IDLE && !(child == getChildAt(0));
        }

        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            return left;
        }

        @Override
        public int clampViewPositionVertical(View child, int top, int dy) {
            return top;
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return getMeasuredWidth() - child.getMeasuredWidth();
        }

        @Override
        public int getViewVerticalDragRange(View child) {
            return getMeasuredHeight() - child.getMeasuredHeight();
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            mState = State.AUTO_SLIDING;
            if (Math.abs(mDownX - mMoveX) < mLimitDragDistance) {
                //滑动距离不够，最终位置为初始位置
                mReleasedView = null;
                mFinalPosition.x = mInitialPosition.x;
                mFinalPosition.y = mInitialPosition.y;
            } else {
                //最终的位置为手滑动的距离*mMoveMultiplier
                mReleasedView = releasedChild;
                mFinalPosition.x = mInitialPosition.x + (int) (mMoveX - mDownX) * mMoveMultiplier;
                mFinalPosition.y = mInitialPosition.y + (int) (mMoveY - mDownY) * mMoveMultiplier;
                //如果有层次，将下面两张按比例还原
                if (isStratified)
                    overlapAfterReleased();
            }
            mDragHelper.settleCapturedViewAt(mFinalPosition.x, mFinalPosition.y);
            invalidate();
            if (mViewStateCallback != null)
                mViewStateCallback.onViewReleased(releasedChild);
            if (mOnLikeOrNotListener != null) {
                if (mFinalPosition.x > mInitialPosition.x)
                    mOnLikeOrNotListener.onLike(releasedChild, mCurrentPosition);
                else if (mFinalPosition.x < mInitialPosition.x)
                    mOnLikeOrNotListener.onNope(releasedChild, mCurrentPosition);
            }
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            if (mState == State.IDLE) {
                mState = State.DRAGGING;
            }
            //相对于在x轴移动的一个比例
            float scale = (left - mInitialPosition.x) / mLimitDragDistance;
            //如果可旋转，旋转一定角度
            if (mRotatable) {
                float mRotation = 0f;
                switch (mRotationDirection) {
                    case ANTICLOCKWISE:
                        mRotation = -mRotationRange;
                        break;
                    case CLOCKWISE:
                        mRotation = mRotationRange;
                        break;
                }
                //旋转
                changedView.setRotation(mRotation * scale);
            }
            //返回原处或被甩出去
            if (left == mFinalPosition.x && top == mFinalPosition.y) {
                //设置为没有旋转角度
                changedView.setRotation(0f);
                //被甩出去
                if (mReleasedView != null && left != mInitialPosition.x && top != mInitialPosition.y) {
                    mReleasedView = null;
                    //当前的position+1
                    mCurrentPosition++;
                    removeView(changedView);
                    //在最后载入一个view，如果需要有层次，进行层次的动画
                    setStratifiedAfterLoadView(loadView(changedView));
                    if (left > mInitialPosition.x) {
                        mOperations.push(true);
                    } else {
                        mOperations.push(false);
                    }
                }
                mState = State.IDLE;
            }
            if (mViewStateCallback != null)
                mViewStateCallback.onViewPositionChanged(changedView, left, top, scale);
        }
    });

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnLikeOrNotListener {
        void onLike(View view, int position);

        void onNope(View view, int position);

        /**
         * like或nope或back动画结束
         */
        void onAnimationEnd();
    }

    public interface ViewStateCallback {
        void onViewReleased(View view);

        /**
         * @param view
         * @param left
         * @param top
         * @param scale x轴上的拖动比例
         */
        void onViewPositionChanged(View view, int left, int top, float scale);
    }

    public LikeOrNotView(Context context) {
        super(context);
    }

    public LikeOrNotView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initAttrs(context, attrs);
    }

    public LikeOrNotView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public LikeOrNotView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs);
    }

    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.LikeOrNotView);
        mRotatable = a.getBoolean(R.styleable.LikeOrNotView_style_rotatable, false);
        isStratified = a.getBoolean(R.styleable.LikeOrNotView_style_stratified, false);
        mAnimatorDuration = a.getInt(R.styleable.LikeOrNotView_animator_duration, (int) mAnimatorDuration);
        int ordinal = a.getInt(R.styleable.LikeOrNotView_rotation_direction, mRotationDirection.ordinal());
        mRotationDirection = RotationDirection.values()[ordinal];
        mRotationRange = a.getFloat(R.styleable.LikeOrNotView_rotation_range, mRotationRange);
        mDragScale = a.getFloat(R.styleable.LikeOrNotView_drag_scale, mDragScale);
        mMoveMultiplier = (int) (1 / mDragScale + 1);
        mMoveMultiplier = a.getInt(R.styleable.LikeOrNotView_move_multiplier, mMoveMultiplier);
        a.recycle();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int mOffsetY = 0;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDownX = ev.getX();
                mDownY = ev.getY();
                mOffsetY = (int) ev.getY();
                break;
        }
        return mOffsetY > mLimitBottomY ? super.onInterceptTouchEvent(ev) : mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mState == State.AUTO_SLIDING)
            return false;
        mDragHelper.processTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                mMoveX = event.getX();
                mMoveY = event.getY();
                break;
        }
        return true;
    }

    @Override
    public void computeScroll() {
        if (mDragHelper.continueSettling(true)) {
            invalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        View child;
        if (isChildFirstLayout && (child = getChildAt(1)) != null) {
            mLimitDragDistance = getWidth() * mDragScale;
            mInitialPosition.x = child.getLeft();
            mInitialPosition.y = child.getTop();
            mLimitBottomY = child.getBottom();
            ((LayoutParams) getChildAt(0).getLayoutParams()).topMargin += mLimitBottomY;
            isChildFirstLayout = false;
        }
    }

    public RotationDirection getRotationDirection() {
        return mRotationDirection;
    }

    public void setRotationDirection(RotationDirection direction) {
        this.mRotationDirection = direction;
    }

    public boolean isStratified() {
        return isStratified;
    }

    public void setStratified(boolean stratified) {
        isStratified = stratified;
        if (isStratified) {
            setStratifiedParams();
            initOverlap();
        }
    }

    public boolean isRotatable() {
        return mRotatable;
    }

    public void setRotatable(boolean rotatable) {
        this.mRotatable = rotatable;
    }

    public float getRotationRange() {
        return mRotationRange;
    }

    public void setRotationRange(float range) {
        this.mRotationRange = range;
    }

    public long getAnimatorDuration() {
        return mAnimatorDuration;
    }

    public void setAnimatorDuration(long duration) {
        this.mAnimatorDuration = duration;
    }

    public float getDragScale() {
        return mDragScale;
    }

    public void setDragScale(float scale) {
        this.mDragScale = scale;
    }

    public int getMoveMultiplier() {
        return mMoveMultiplier;
    }

    public void setMoveMultiplier(int multiplier) {
        this.mMoveMultiplier = multiplier;
    }

    public int getCurrentPosition() {
        return mCurrentPosition;
    }

    public IAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(IAdapter adapter) {
        mOperations.clear();
        removeViews(1, getChildCount() - 1);
        this.mAdapter = adapter;
        int count = 0;
        //加载5个view
        while (count < 5) {
            loadView(null);
            count++;
        }
        //有层次，初始化层次效果
        if (isStratified) {
            setStratifiedParams();
            initOverlap();
        }
        mState = State.IDLE;
    }

    private void setStratifiedParams() {
        View child = getChildAt(1);
        if (child == null)
            return;
        int w = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int h = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        child.measure(w, h);
        mOffsetY = child.getMeasuredWidth() * mScale * 7f;
    }

    public OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClickListener = listener;
    }

    public OnLikeOrNotListener getOnLikeOrNotListener() {
        return mOnLikeOrNotListener;
    }

    public void setOnLikeOrNotListener(OnLikeOrNotListener listener) {
        this.mOnLikeOrNotListener = listener;
    }

    public ViewStateCallback getViewStateCallback() {
        return mViewStateCallback;
    }

    public void setViewStateCallback(ViewStateCallback callback) {
        this.mViewStateCallback = callback;
    }

    private void setStratifiedAfterLoadView(View view) {
        if (isStratified && view != null) {
            view.setScaleX(1f - 2 * mScale);
            view.setScaleY(1f - 2 * mScale);
            view.setTranslationY(2 * mOffsetY);
        }
    }

    private View loadView(View convertView) {
        //如果已经加载完最后一个view
        if (mLoadPosition == mAdapter.getCount()) {
            //如果convertView不为null，则作为可复用的view
            if (convertView != null)
                mRecyclerViews.add(convertView);
            return null;
        }
        return loadView(convertView, mLoadPosition++, 1);
    }

    private View loadView(View convertView, final int position, int index) {
        //Log.e("loadView", mLoadPosition + "");
        View view = mAdapter.getView(convertView, this, position);
        FrameLayout.LayoutParams params = (LayoutParams) view.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(params);
        }
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        addView(view, index);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnItemClickListener != null)
                    mOnItemClickListener.onItemClick(v, position);
                invalidateState();
            }
        });
        return view;
    }

    private void initOverlap() {
        View child0 = getChildAt(0);
        View child1 = getChildAt(getChildCount() - 2);
        View child2 = getChildAt(getChildCount() - 3);
        if (child1 != null && child1 != child0) {
            child1.setScaleX(1f - mScale);
            child1.setScaleY(1f - mScale);
            child1.setTranslationY(mOffsetY);
        }
        if (child2 != null && child2 != child0) {
            child2.setScaleX(1f - 2f * mScale);
            child2.setScaleY(1f - 2f * mScale);
            child2.setTranslationY(2f * mOffsetY);
        }
        if (getChildCount() > 4) {
            for (int index = 1; index < getChildCount() - 3; index++) {
                View child = getChildAt(index);
                if (child != null) {
                    child.setScaleX(1f - 2f * mScale);
                    child.setScaleY(1f - 2f * mScale);
                    child.setTranslationY(2f * mOffsetY);
                }
            }
        }
    }

    private void overlapAfterReleased() {
        View child0 = getChildAt(0);
        View child1 = getChildAt(getChildCount() - 2);
        View child2 = getChildAt(getChildCount() - 3);
        if (child0 == child1)
            child1 = null;
        if (child0 == child2)
            child2 = null;
        ObjectAnimator mTranslationAnimator1;
        ObjectAnimator mScaleXAnimator1;
        ObjectAnimator mScaleYAnimator1;
        ObjectAnimator mTranslationAnimator2;
        ObjectAnimator mScaleXAnimator2;
        ObjectAnimator mScaleYAnimator2;
        AnimatorSet mAnimatorSet = new AnimatorSet();
        mAnimatorSet.setDuration(100);
        if (child1 != null && child2 != null) {
            mTranslationAnimator1 = ObjectAnimator.ofFloat(child1, "translationY", mOffsetY, 0f);
            mScaleXAnimator1 = ObjectAnimator.ofFloat(child1, "scaleX", 1f - mScale, 1f);
            mScaleYAnimator1 = ObjectAnimator.ofFloat(child1, "scaleY", 1f - mScale, 1f);

            mTranslationAnimator2 = ObjectAnimator.ofFloat(child2, "translationY", 2f * mOffsetY, mOffsetY);
            mScaleXAnimator2 = ObjectAnimator.ofFloat(child2, "scaleX", 1f - 2 * mScale, 1f - mScale);
            mScaleYAnimator2 = ObjectAnimator.ofFloat(child2, "scaleY", 1f - 2 * mScale, 1f - mScale);

            mAnimatorSet.playTogether(mTranslationAnimator1, mScaleXAnimator1, mScaleYAnimator1,
                    mTranslationAnimator2, mScaleXAnimator2, mScaleYAnimator2);
        } else if (child1 != null && child2 == null) {
            mTranslationAnimator1 = ObjectAnimator.ofFloat(child1, "translationY", mOffsetY, 0f);
            mScaleXAnimator1 = ObjectAnimator.ofFloat(child1, "scaleX", 1f - mScale, 1f);
            mScaleYAnimator1 = ObjectAnimator.ofFloat(child1, "scaleY", 1f - mScale, 1f);

            mAnimatorSet.playTogether(mTranslationAnimator1, mScaleXAnimator1, mScaleYAnimator1);
        } else if (child1 == null && child2 != null) {
            mTranslationAnimator2 = ObjectAnimator.ofFloat(child2, "translationY", 2f * mOffsetY, mOffsetY);
            mScaleXAnimator2 = ObjectAnimator.ofFloat(child2, "scaleX", 1f - 2 * mScale, 1f - mScale);
            mScaleYAnimator2 = ObjectAnimator.ofFloat(child2, "scaleY", 1f - 2 * mScale, 1f - mScale);

            mAnimatorSet.playTogether(mTranslationAnimator2, mScaleXAnimator2, mScaleYAnimator2);
        }
        mAnimatorSet.start();
    }

    private void overlapBeforeBack() {
        View child0 = getChildAt(0);
        View child1 = getChildAt(getChildCount() - 1);
        View child2 = getChildAt(getChildCount() - 2);
        if (child0 == child1)
            child1 = null;
        if (child0 == child2)
            child2 = null;
        ObjectAnimator mTranslationAnimator1;
        ObjectAnimator mScaleXAnimator1;
        ObjectAnimator mScaleYAnimator1;
        ObjectAnimator mTranslationAnimator2;
        ObjectAnimator mScaleXAnimator2;
        ObjectAnimator mScaleYAnimator2;
        AnimatorSet mAnimatorSet = new AnimatorSet();
        mAnimatorSet.setDuration(100);
        if (child1 != null && child2 != null) {
            mTranslationAnimator1 = ObjectAnimator.ofFloat(child1, "translationY", 0f, mOffsetY);
            mScaleXAnimator1 = ObjectAnimator.ofFloat(child1, "scaleX", 1f, 1f - mScale);
            mScaleYAnimator1 = ObjectAnimator.ofFloat(child1, "scaleY", 1f, 1f - mScale);

            mTranslationAnimator2 = ObjectAnimator.ofFloat(child2, "translationY", mOffsetY, 2f * mOffsetY);
            mScaleXAnimator2 = ObjectAnimator.ofFloat(child2, "scaleX", 1f - mScale, 1f - 2 * mScale);
            mScaleYAnimator2 = ObjectAnimator.ofFloat(child2, "scaleY", 1f - mScale, 1f - 2 * mScale);

            mAnimatorSet.playTogether(mTranslationAnimator1, mScaleXAnimator1, mScaleYAnimator1,
                    mTranslationAnimator2, mScaleXAnimator2, mScaleYAnimator2);
        } else if (child1 != null && child2 == null) {
            mTranslationAnimator1 = ObjectAnimator.ofFloat(child1, "translationY", 0f, mOffsetY);
            mScaleXAnimator1 = ObjectAnimator.ofFloat(child1, "scaleX", 1f, 1f - mScale);
            mScaleYAnimator1 = ObjectAnimator.ofFloat(child1, "scaleY", 1f, 1f - mScale);

            mAnimatorSet.playTogether(mTranslationAnimator1, mScaleXAnimator1, mScaleYAnimator1);
        } else if (child1 == null && child2 != null) {
            mTranslationAnimator2 = ObjectAnimator.ofFloat(child2, "translationY", mOffsetY, 2f * mOffsetY);
            mScaleXAnimator2 = ObjectAnimator.ofFloat(child2, "scaleX", 1f - mScale, 1f - 2 * mScale);
            mScaleYAnimator2 = ObjectAnimator.ofFloat(child2, "scaleY", 1f - mScale, 1f - 2 * mScale);

            mAnimatorSet.playTogether(mTranslationAnimator2, mScaleXAnimator2, mScaleYAnimator2);
        }
        mAnimatorSet.start();
    }

    public void like() {
        likeOrNope(true);
    }

    public void nope() {
        likeOrNope(false);
    }

    private void likeOrNope(boolean like) {
        invalidateNoChild();
        if (mState != State.IDLE)
            return;
        mState = State.AUTO_SLIDING;
        //当前最上面的view
        final View child = getChildAt(getChildCount() - 1);
        if (child == null || getChildCount() == 1)
            return;
        mOperations.push(like);
        mReleasedView = child;
        if (mOnLikeOrNotListener != null) {
            if (like)
                mOnLikeOrNotListener.onLike(child, mCurrentPosition);
            else
                mOnLikeOrNotListener.onNope(child, mCurrentPosition);
        }
        ObjectAnimator mTranslationAnimator = ObjectAnimator.ofFloat(child, "translationX", 0f, like ? getWidth() : -getWidth());
        AnimatorSet mAnimatorSet = new AnimatorSet();
        mAnimatorSet.setDuration(mAnimatorDuration);
        //如果可旋转
        if (mRotatable) {
            ObjectAnimator mRotationAnimator = ObjectAnimator.ofFloat(child, "rotation", 0f, like ? mRotationRange * 3f : -mRotationRange * 3f);
            mAnimatorSet.playTogether(mTranslationAnimator, mRotationAnimator);
        } else
            mAnimatorSet.playTogether(mTranslationAnimator);
        mAnimatorSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (mReleasedView != null) {
                    mReleasedView = null;
                    mCurrentPosition++;
                    //清除动画效果，设置为原状
                    child.setTranslationX(0f);
                    child.setRotation(0f);
                    removeView(child);
                    setStratifiedAfterLoadView(loadView(child));
                    if (mOnLikeOrNotListener != null) {
                        mOnLikeOrNotListener.onAnimationEnd();
                    }
                }
                mState = State.IDLE;
            }
        });
        mAnimatorSet.start();
        overlapAfterReleased();
    }

    public void back() {
        invalidateNoChild();
        if (mState != State.IDLE || mOperations.empty())
            return;
        mState = State.AUTO_SLIDING;
        Boolean operation = mOperations.pop();
        if (isStratified)
            overlapBeforeBack();
        View child;
        if (mRecyclerViews.isEmpty()) {
            child = getChildAt(1);
            mLoadPosition--;
        } else {
            child = mRecyclerViews.get(0);
            mRecyclerViews.remove(0);
        }
        if (child != null) {
            mReleasedView = child;
            removeView(child);
            ObjectAnimator mTranslationAnimator = ObjectAnimator.ofFloat(child, "translationX", operation ? getWidth() : -getWidth(), 0f);
            AnimatorSet mAnimatorSet = new AnimatorSet();
            mAnimatorSet.setDuration(mAnimatorDuration);
            if (mRotatable) {
                ObjectAnimator mRotationAnimator = ObjectAnimator.ofFloat(child, "rotation", operation ? mRotationRange * 3f : -mRotationRange * 3f, 0f);
                mAnimatorSet.playTogether(mTranslationAnimator, mRotationAnimator);
            } else
                mAnimatorSet.playTogether(mTranslationAnimator);
            mAnimatorSet.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (mReleasedView != null) {
                        mReleasedView = null;
                        mState = State.IDLE;
                        if (mOnLikeOrNotListener != null) {
                            mOnLikeOrNotListener.onAnimationEnd();
                        }
                    }
                }
            });
            child.setScaleX(1f);
            child.setScaleY(1f);
            child.setTranslationY(0f);
            loadView(child, --mCurrentPosition, getChildCount());
            mAnimatorSet.start();
        }
    }

    private void invalidateNoChild() {
        if (getChildCount() <= 1)
            invalidateState();
    }

    public void invalidateState() {
        mState = State.IDLE;
    }

    enum State {
        /**
         * 空闲的
         */
        IDLE,
        /**
         * 自动滑动
         */
        AUTO_SLIDING,
        /**
         * 用手拖动
         */
        DRAGGING
    }

    /**
     * 旋转方向，以like时为顺时针
     */
    public enum RotationDirection {
        /**
         * 顺时针
         */
        CLOCKWISE,
        /**
         * 逆时针
         */
        ANTICLOCKWISE
    }
}