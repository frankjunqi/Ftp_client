package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;

import com.artifex.mupdfdemo.AsyncTask;

/**
 * Created by Frank on 16/4/23.
 */
public class TestActivity extends Activity {

    private static Handler mHandler;

    private TextView tv_countpage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_countpage = (TextView) findViewById(R.id.tv_countpage);
        mHandler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 1: {
                        tv_countpage.setText(msg.obj.toString());
                    }
                }

            }
        };

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 子线程做的操作任务－－ 访问网络
                Message message = new Message();
                message.what = 1;
                message.obj = "";
                mHandler.sendMessage(message);
            }
        });

        thread.start();


        TestAsyncTask testAsyncTask = new TestAsyncTask();
        testAsyncTask.execute();
    }

    class TestAsyncTask extends AsyncTask{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground(Object[] params) {
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
        }
    }
}
