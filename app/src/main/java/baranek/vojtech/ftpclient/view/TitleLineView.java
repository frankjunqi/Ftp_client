package baranek.vojtech.ftpclient.view;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.Calendar;

import baranek.vojtech.ftpclient.R;
import baranek.vojtech.ftpclient.SecretActivity;
import baranek.vojtech.ftpclient.api.Host;

/**
 * Created by Frank on 16/4/4.
 */
public class TitleLineView extends LinearLayout {

    private static final int TIMEFLAG = 0x11;
    private static Handler timeHandler = null;

    private TextClock textClock;
    private TextView digitalClock;
    private TextView tv_title;
    private TextView tv_weather;
    private TextView tv_temperature;
    private MarqueeTextView tv_info;
    private SimpleDraweeView iv_weather_icon;

    private ImageView iv_logo;


    private class TimeHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TIMEFLAG:
                    refreshTime();
                    timeHandler.sendEmptyMessageDelayed(TIMEFLAG, 30000);
                    break;
            }
        }
    }

    public TitleLineView(Context context) {
        super(context);
        initView(context);
    }

    private void initView(final Context mContext) {
        inflate(mContext, R.layout.rawmaterial_title, this);
        textClock = (TextClock) findViewById(R.id.textClock);
        iv_weather_icon = (SimpleDraweeView) findViewById(R.id.iv_weather_icon);
        digitalClock = (TextView) findViewById(R.id.digitalClock);
        tv_weather = (TextView) findViewById(R.id.tv_weather);
        tv_title = (TextView) findViewById(R.id.tv_title);
        tv_temperature = (TextView) findViewById(R.id.tv_temperature);
        tv_info = (MarqueeTextView) findViewById(R.id.tv_info);

        iv_logo = (ImageView) findViewById(R.id.iv_logo);

        // 计算高度
        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);

        int heightpix = wm.getDefaultDisplay().getHeight();
        int height = (heightpix - 18) / 9;
        this.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height));

        timeHandler = new TimeHandler();
        timeHandler.sendEmptyMessage(TIMEFLAG);
    }

    private void refreshTime() {
        Calendar c = Calendar.getInstance();
        int day = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);
        if (digitalClock != null) {
            digitalClock.setText(String.format("%s:%s", day < 10 ? "0" + String.valueOf(day) : String.valueOf(day)
                    , minute < 10 ? "0" + String.valueOf(minute) : String.valueOf(minute)));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (timeHandler != null) {
            timeHandler.removeMessages(TIMEFLAG);
        }
    }

    public void setNoticeContent(String noticeContent) {
        if (tv_info != null) {
            tv_info.setText(noticeContent);
        }
    }

    public void setTitle(String titleName) {
        if (tv_title != null) {
            tv_title.setText(titleName);
        }
    }

    public String getNoticContent() {
        String noticeContent = "";
        if (tv_info != null) {
            noticeContent = tv_info.getText().toString();
        }
        return noticeContent;
    }

    /**
     * 设置温度
     *
     * @param temperature 温度
     */
    public void setTv_temperature(String temperature) {
        tv_temperature.setText(temperature);
    }

    /**
     * 设置图片
     *
     * @param imageFlag
     */
    public void setIv_weather_icon(String imageFlag) {
        Uri uri = Uri.parse(Host.HOST + "/res/weather/" + imageFlag + ".png");
        iv_weather_icon.setImageURI(uri);
    }

    /**
     * 设置配置项目页面
     *
     * @param onClickListener
     */
    public void setImageLogoClickListen(OnClickListener onClickListener) {
        iv_logo.setOnClickListener(onClickListener);
    }

    /**
     * 设置天气多云
     * @param weather
     */
    public void setTv_weather(String weather) {
        tv_weather.setText(weather);
    }
}
