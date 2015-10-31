package net.oschina.app.fragment;

import java.io.ByteArrayInputStream;

import net.oschina.app.AppContext;
import net.oschina.app.R;
import net.oschina.app.api.remote.OSChinaApi;
import net.oschina.app.base.BaseFragment;
import net.oschina.app.bean.ShakeObject;
import net.oschina.app.util.KJAnimations;
import net.oschina.app.util.StringUtils;
import net.oschina.app.util.UIHelper;
import net.oschina.app.util.XmlUtils;

import org.apache.http.Header;
import org.kymjs.kjframe.KJBitmap;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;

import com.loopj.android.http.AsyncHttpResponseHandler;
/**
 * 摇一摇界面
 * <p>在使用传感器的过程中，要注意：当不在使用传感器，特别是在界面处于Pause状态时，应该让传感器不可用
 * @author WangLuJie
 *
 */
public class ShakeFragment extends BaseFragment implements SensorEventListener {

    @InjectView(R.id.shake_img)
    ImageView mImgShake;

    @InjectView(R.id.progress)
    ProgressBar mProgress;
    @InjectView(R.id.shake_bottom)
    LinearLayout mLayoutBottom;
    @InjectView(R.id.exploreproject_listitem_userface)
    ImageView mImgHead;
    @InjectView(R.id.exploreproject_listitem_title)
    TextView mTvTitle;
    @InjectView(R.id.exploreproject_listitem_description)
    TextView mTvDetail;
    @InjectView(R.id.exploreproject_listitem_language)
    TextView mTvAuthor;
    @InjectView(R.id.exploreproject_listitem_star)
    TextView mTvCommentCount;
    @InjectView(R.id.exploreproject_listitem_fork)
    TextView mTvDate;

    private SensorManager sensorManager = null;
    private Sensor sensor;
    private Vibrator vibrator = null;
    private Activity aty;

    //表示正在请求数据
    private boolean isRequest = false;

    private float lastX;
    private float lastY;
    private float lastZ;
    private long lastUpdateTime;
    private static final int SPEED_SHRESHOLD = 45;// 这个值越大需要越大的力气来摇晃手机，手机移动的速度
    
    //时间间隔  单位毫秒
    private static final int UPTATE_INTERVAL_TIME = 50;
/**
 * 初始化sensormanager与vibrator
 */
    @Override
    public void initData() {
        aty = getActivity();
        sensorManager = (SensorManager) aty
                .getSystemService(Context.SENSOR_SERVICE);
        vibrator = (Vibrator) aty.getSystemService(Service.VIBRATOR_SERVICE);
    }

    @Override
    public void initView(View view) {}

    /**
     * 摇动手机成功后调用:先动画，完后再请求数据
     * 
     */
    private void onShake() {
        isRequest = true;
        mProgress.setVisibility(View.VISIBLE);
        Animation anim = KJAnimations.shakeAnimation(mImgShake.getLeft());
        anim.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationRepeat(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                OSChinaApi.shake(new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
                        isRequest = false;
                        final ShakeObject obj = XmlUtils.toBean(
                                ShakeObject.class, new ByteArrayInputStream(
                                        arg2));
                        if (obj != null) {
                            if (StringUtils.isEmpty(obj.getAuthor())
                                    && StringUtils.isEmpty(obj
                                            .getCommentCount())
                                    && StringUtils.isEmpty(obj.getPubDate())) {
                                jokeToast();
                            } else {
                                mLayoutBottom.setVisibility(View.VISIBLE);
                                mLayoutBottom
                                        .setOnClickListener(new OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                UIHelper.showUrlShake(aty, obj);
                                            }
                                        });
                                new KJBitmap()
                                        .displayWithLoadBitmap(mImgHead,
                                                obj.getImage(),
                                                R.drawable.widget_dface);
                                mTvTitle.setText(obj.getTitle());
                                mTvDetail.setText(obj.getDetail());
                                mTvAuthor.setText("作者:" + obj.getAuthor());
                                mTvCommentCount.setText("评论:"
                                        + obj.getCommentCount());
                                mTvDate.setText("时间:" + obj.getPubDate());
                            }
                        } else {
                            jokeToast();
                        }
                        mProgress.setVisibility(View.GONE);
                    }

                    @Override
                    public void onFailure(int arg0, Header[] arg1, byte[] arg2,
                            Throwable arg3) {
                        isRequest = false;
                        mProgress.setVisibility(View.GONE);
                        jokeToast();
                    }
                });
            }
        });
        mImgShake.startAnimation(anim);
    }

    private void jokeToast() {
        AppContext.showToast("红薯跟你开了个玩笑");
    }

    /**
     * 解绑传感器
     */
    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    /**
     * 注册传感器
     */
    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (sensor != null) {
            sensorManager.registerListener(this, sensor,
                    SensorManager.SENSOR_DELAY_GAME);
        }
    }

    /**
     * 传感器监听
     * 
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
    	
    	//两个条件一个是传感器值变化前后的时间差是否满足时间阈值；另外一个，摇动力气的阈值
    	//毫秒数
        long currentUpdateTime = System.currentTimeMillis();
        long timeInterval = currentUpdateTime - lastUpdateTime;
        if (timeInterval < UPTATE_INTERVAL_TIME) {
            return;
        }
        lastUpdateTime = currentUpdateTime;

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float deltaX = x - lastX;
        float deltaY = y - lastY;
        float deltaZ = z - lastZ;

        lastX = x;
        lastY = y;
        lastZ = z;

        // 速度 = 距离/时间  表示单位时间内加速度的变化情况，
        double speed = (Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ
                * deltaZ) / timeInterval) * 100;
        if (speed >= SPEED_SHRESHOLD && !isRequest) {
            mLayoutBottom.setVisibility(View.GONE);
            //震动
            vibrator.vibrate(300);
            onShake();
        }
    }
/**
 * 开始的地方
 */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View rootView = inflater.inflate(R.layout.fragment_shake, container,
                false);
        ButterKnife.inject(this, rootView);
        initData();
        initView(rootView);
        return rootView;
    }

    // @Override
    // public void onCreate(Bundle savedInstanceState) {
    // super.onCreate(savedInstanceState);
    // setHasOptionsMenu(true);
    // }
    //
    // @Override
    // public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    // inflater.inflate(R.menu.pub_tweet_menu, menu);
    // }
    //
    // @Override
    // public boolean onOptionsItemSelected(MenuItem item) {
    // switch (item.getItemId()) {
    // case R.id.public_menu_send:
    // break;
    // }
    // return true;
    // }

    @Override
    public void onClick(View v) {}

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
