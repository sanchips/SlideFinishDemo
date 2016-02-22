package com.zrp.slidefinishdemo.slidefinish;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;

import com.zrp.slidefinishdemo.R;

/**
 * 所有activity的基类，可以在此统一做一些相关的处理
 *
 * @author ZRP
 */
public class BaseActivity extends FragmentActivity {

    private int startInAnimationResources = 0;
    private int startOutAnimationResources = 0;
    private int finishInAnimationResources = 0;
    private int finishOutAnimationResources = 0;

    private SlideFinishLayout slideFinishLayout;
    private boolean isCanBack = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setInOutAnimation(R.anim.left_in, R.anim.left_out, R.anim.right_in, R.anim.right_out);

        if (isCanBack) initSlideFinish();
    }

    /**
     * 初始化右滑退出控件
     */
    private void initSlideFinish() {
        slideFinishLayout = (SlideFinishLayout) LayoutInflater.from(this).inflate(
                R.layout.custom_slidefinish_container, null);
        slideFinishLayout.attachToActivity(this);
    }

    /**
     * 右滑返回：添加忽略view，内部维护一个list，可在一个页面添加多个忽略view
     */
    public void addIgnoredView(View v) {
        if (slideFinishLayout != null) slideFinishLayout.addIgnoredView(v);
    }

    /**
     * 设置当前页面是否支持滑动退出，需要写在继承该类的子类onCreate中super.onCreate();的前面
     *
     * @param isCanBack 是否能右滑finish
     */
    public void setSlideFinish(boolean isCanBack) {
        this.isCanBack = isCanBack;
    }

    /**
     * 设置打开界面和关闭界面的动画效果
     *
     * @param startInAnimationResources
     * @param startOutAnimationResources
     * @param finishInAnimationResources
     * @param finishOutAnimationResources
     */
    public void setInOutAnimation(int startInAnimationResources, int startOutAnimationResources,
                                  int finishInAnimationResources, int finishOutAnimationResources) {
        this.startInAnimationResources = startInAnimationResources;
        this.startOutAnimationResources = startOutAnimationResources;
        this.finishInAnimationResources = finishInAnimationResources;
        this.finishOutAnimationResources = finishOutAnimationResources;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (startOutAnimationResources != 0) {
            overridePendingTransition(startInAnimationResources, startOutAnimationResources);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if (finishInAnimationResources != 0) {
            overridePendingTransition(finishInAnimationResources, finishOutAnimationResources);
        }
    }
}
