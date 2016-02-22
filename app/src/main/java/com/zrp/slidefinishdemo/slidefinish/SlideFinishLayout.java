package com.zrp.slidefinishdemo.slidefinish;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * 向右滑动finish当前activity的顶层控件
 * <p/>
 * <li>如果子view为viewPager，当滑动到position为0的时候再次右滑退出页面</li>
 * <li>支持通过{@link #addIgnoredView(View)}的方式手动添加忽略右滑退出的view</li>
 *
 * @author ZRP
 */
public class SlideFinishLayout extends LinearLayout {

    private int mTouchSlop;//系统默认的滑动事件触发距离
    private int startX = 0, startY = 0;//触摸事件开始点
    private int moveDistanceX = 0, moveDistanceY = 0;//滑动的距离
    private VelocityTracker mVelocityTracker;//速度计算器
    private static final int XSPEED_MIN = 1000;//手指在X方向滑动时的最小速度（px/s）

    private List<View> mIgnoredViews = new ArrayList<View>();//滑动忽略控件列表
    private List<ViewPager> mViewPagers = new LinkedList<ViewPager>();//该控件子控件中包含ViewPager的集合
    private BaseActivity activity;//所有activity的基类

    public SlideFinishLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SlideFinishLayout(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    /**
     * 绑定到activity
     *
     * @param activity
     */
    public void attachToActivity(BaseActivity activity) {
        this.activity = activity;

        ViewGroup decor = (ViewGroup) activity.getWindow().getDecorView();
        ViewGroup decorChild = (ViewGroup) decor.getChildAt(0);
        decor.removeView(decorChild);
        addView(decorChild);
        decor.addView(this);
    }

    /**
     * @param v 添加右滑退出忽略控件
     */
    public void addIgnoredView(View v) {
        if (!mIgnoredViews.contains(v)) {
            v.setClickable(true);//防止因添加的是textView等本身没有处理touch事件的view，而引起viewGroup的onInterceptTouchEvent返回false，但是其onTouchEvent还是会执行的BUG
            mIgnoredViews.add(v);
        }
    }

    /**
     * @param v 移除右滑退出忽略控件
     */
    public void removeIgnoredView(View v) {
        mIgnoredViews.remove(v);
    }

    /**
     * 清除所有忽略的右滑退出控件
     */
    public void clearIgnoredViews() {
        mIgnoredViews.clear();
    }

    /**
     * 是否是可忽略的滑动事件
     *
     * @param ev 手势
     * @return 如果是可忽略的view，则返回true
     */
    private boolean isInIgnoredView(MotionEvent ev) {
        Rect rect = new Rect();
        int[] location = new int[2];
        for (View v : mIgnoredViews) {
            v.getLocationInWindow(location);
            rect.set(location[0], location[1], location[0] + v.getMeasuredWidth(), location[1] + v.getMeasuredHeight());
            if (rect.contains((int) ev.getX(), (int) ev.getY())) return true;
        }
        return false;
    }

    /**
     * 事件拦截操作
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        //如果滑动的是viewpager或者是已添加的事件忽略view，就分发给子控件进行事件处理
        ViewPager mViewPager = getTouchViewPager(ev);
        if ((mViewPager != null && mViewPager.getCurrentItem() != 0) || isInIgnoredView(ev)) {
            return false;
        }

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startX = (int) ev.getRawX();
                startY = (int) ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int moveX = (int) ev.getRawX();
                // 若满足此条件，屏蔽子类的touch事件
                if (moveX - startX > mTouchSlop && Math.abs((int) ev.getRawY() - startY) < mTouchSlop) {
                    return true;
                }
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        mVelocityTracker.addMovement(ev);

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:// 手指按下
                startX = (int) ev.getRawX();
                startY = (int) ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:// 手指移动
                moveDistanceX = (int) (ev.getRawX() - startX);
                moveDistanceY = (int) (ev.getRawY() - startY);
                break;
            case MotionEvent.ACTION_UP:// 手指离开
                startX = 0;
                startY = 0;
                if ((moveDistanceX > getScreenWidth() / 20)
                        && (moveDistanceX > 2 * Math.abs(moveDistanceY))
                        && getScrollVelocity() > XSPEED_MIN) {
                    activity.finish();
                    return true;
                }
                moveDistanceX = 0;
                moveDistanceY = 0;
                recycleVelocityTracker();
                break;
            default:
                break;
        }
        return true;
    }

    /**
     * @return 获取手机屏幕宽度
     */
    private int getScreenWidth() {
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    /**
     * 释放速度计算
     */
    private void recycleVelocityTracker() {
        mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

    /**
     * 计算在x方向的速度
     *
     * @return 当前在x方向速度的绝对值
     */
    private int getScrollVelocity() {
        mVelocityTracker.computeCurrentVelocity(1000);
        int velocity = (int) mVelocityTracker.getXVelocity();
        return Math.abs(velocity);
    }

    /**
     * 获取SwipeFinishLayout里面的ViewPager的集合
     *
     * @param parent 父控件
     */
    private void getAllViewPager(ViewGroup parent) {
        int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = parent.getChildAt(i);
            if (child instanceof ViewPager) {
                mViewPagers.add((ViewPager) child);
            } else if (child instanceof ViewGroup) {
                getAllViewPager((ViewGroup) child);
            }
        }
    }

    /**
     * 返回我们touch的ViewPager
     *
     * @param ev 触摸事件
     * @return 当前触摸的viewPager
     */
    private ViewPager getTouchViewPager(MotionEvent ev) {
        if (mViewPagers == null || mViewPagers.size() == 0) {
            return null;
        }
        Rect mRect = new Rect();
        int[] location = new int[2];
        for (ViewPager v : mViewPagers) {
            v.getLocationInWindow(location);
            mRect.set(location[0], location[1], location[0] + v.getMeasuredWidth(), location[1] + v.getMeasuredHeight());
            if (mRect.contains((int) ev.getX(), (int) ev.getY())) {
                return v;
            }
        }
        return null;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) getAllViewPager(this);//布局子控件的时候获取其中的所有viewPager
    }
}