package net.oschina.app.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.ScrollView;

/**
 * 可以拖动的ScrollView    只可以上下拖动，并且是拖动距离的1/4
 * <p>拖拽的过程实际上是一个“按下-移动-抬起”的过程，因此要重写onTouchEvent(MotionEvent ev)，
 *
 */
public class CustomerScrollView extends ScrollView {

	private static final int size = 4;
	private View inner;
	
	//
	private float y;
	//要拖动的view在正常情况下的位置
	private Rect normal = new Rect();

	public CustomerScrollView(Context context) {
		super(context);
	}

	public CustomerScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * 首先获取子view，获取要拖拽的对象
	 * <p>当View中所有的子控件 均被映射成xml后触发
	 */
	@Override
	protected void onFinishInflate() {
		if (getChildCount() > 0) {
			inner = getChildAt(0);
		}
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		if (inner == null) {
			return super.onTouchEvent(ev);
		} else {
			commOnTouchEvent(ev);
		}
		return super.onTouchEvent(ev);
	}

	/**
	 * 按下、滑动、抬起 监听
	 * @param ev
	 */
	public void commOnTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			y = ev.getY();
			break;
		case MotionEvent.ACTION_UP:
			if (isNeedAnimation()) {
				// Log.v("mlguitar", "will up and animation");
				animation();
			}
			break;
		case MotionEvent.ACTION_MOVE:
			final float preY = y;
			float nowY = ev.getY();
			/**
			 * size=4 表示 拖动的距离为屏幕的高度的1/4，注意正负
			 */
			int deltaY = (int) (preY - nowY) / size;
			// 滚动
			// scrollBy(0, deltaY);

			y = nowY;
			if (isNeedMove()) {
				if (normal.isEmpty()) {
					//初始化normal
					normal.set(inner.getLeft(), inner.getTop(),
							inner.getRight(), inner.getBottom());
					return;
				}
				//重点
				int yy = inner.getTop() - deltaY;

				// 移动布局 重点
				inner.layout(inner.getLeft(), yy, inner.getRight(),
						inner.getBottom() - deltaY);
			}
			break;
		default:
			break;
		}
	}

	/**
	 * 将拖动的view回复到原位
	 */
	public void animation() {
		TranslateAnimation ta = new TranslateAnimation(0, 0, inner.getTop(),
				normal.top);
		ta.setDuration(200);
		inner.startAnimation(ta);
		inner.layout(normal.left, normal.top, normal.right, normal.bottom);
		normal.setEmpty();
	}

	public boolean isNeedAnimation() {
		return !normal.isEmpty();
	}

	public boolean isNeedMove() {
		//相当于本身的身高减去实际能看到的身高就等于没有看到的身高部分。
		int offset = inner.getMeasuredHeight() - getHeight();
		int scrollY = getScrollY();
//scrollY == 0这个条件实际上是滚动到了最顶部的时候,，
		//而scrollY == offset是滚动到最底部的时候，两个条件满足其中一个都可以实现拖拽的效果。
		//不太懂？
		if (scrollY == 0 || scrollY == offset) {
			return true;
		}
		return false;
	}

}
