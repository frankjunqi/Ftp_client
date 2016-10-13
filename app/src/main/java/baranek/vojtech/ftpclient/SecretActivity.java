package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import baranek.vojtech.ftpclient.api.EwiService;
import baranek.vojtech.ftpclient.api.Host;
import baranek.vojtech.ftpclient.entity.UpdaeResBody;
import baranek.vojtech.ftpclient.entity.Update;
import baranek.vojtech.ftpclient.gsonfactory.GsonConverterFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Created by Frank on 16/4/23.
 */
public class SecretActivity extends Activity implements View.OnClickListener {

    private static final String TAG = "TAG";

    private EditText et_secret;

    private Button btn_sure, btn_update;

    private ContentObserver contentObserver;

    private Context mContext;

    private TextView tv_currentversion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret);
        mContext = this;
        tv_currentversion = (TextView) findViewById(R.id.tv_currentversion);
        et_secret = (EditText) findViewById(R.id.et_secret);
        btn_sure = (Button) findViewById(R.id.btn_sure);
        btn_sure.setOnClickListener(this);
        btn_update = (Button) findViewById(R.id.btn_update);
        btn_update.setOnClickListener(this);

        et_secret.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    return true;
                }
                return false;
            }
        });

        tv_currentversion.setText("版本号:  " + getVersionCode(mContext));
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sure:
                String secret = et_secret.getText().toString().replace(" ", "");
                if ("mesmubea".equals(secret)) {
                    Toast.makeText(this, "登陆成功！", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, SettingActivity.class));
                    this.finish();
                } else {
                    Toast.makeText(this, "密码错误，请联系管理员！", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_update:
                requestUpdate();
                break;
        }
    }

    public void requestUpdate() {
        btn_update.setEnabled(false);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Host.UpdateHost)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        EwiService service = retrofit.create(EwiService.class);
        Call<UpdaeResBody> updaeResBodyCall = service.updateVersion("Srv", "Base.svc", "CheckUpdate", "MubeaEWI", String.valueOf(getVersionCode(mContext)));
        updaeResBodyCall.enqueue(new Callback<UpdaeResBody>() {
            @Override
            public void onResponse(Call<UpdaeResBody> call, Response<UpdaeResBody> response) {
                if (response != null && response.body() != null && response.body().d != null && response.body().d.Data != null) {
                    Update update = response.body().d.Data;
                    if (update != null && !TextUtils.isEmpty(update.FileUrl) && !TextUtils.isEmpty(update.Ver) && Integer.parseInt(update.Ver) > getVersionCode(mContext)) {
                        // 下载
                        Toast.makeText(SecretActivity.this, "Downloding EWI.apk ... ", Toast.LENGTH_SHORT).show();
                        downloadApk(Host.UpdateHost + response.body().d.Data.FileUrl);
                    } else {
                        Toast.makeText(SecretActivity.this, "已经是最新版本！", Toast.LENGTH_SHORT).show();
                        btn_update.setEnabled(true);
                    }
                } else {
                    Toast.makeText(SecretActivity.this, "无版本更新！", Toast.LENGTH_SHORT).show();
                    btn_update.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<UpdaeResBody> call, Throwable throwable) {
                Toast.makeText(SecretActivity.this, "更新接口异常，请联系维护人员 ！", Toast.LENGTH_SHORT).show();
                btn_update.setEnabled(true);
            }
        });
    }

    private void downloadApk(String apkUrl) {
        btn_update.setEnabled(false);
        btn_sure.setEnabled(false);
        // 下载
        try {
            final DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            clearDownLoadApk(downloadManager);
            Uri uri = Uri.parse(apkUrl);
            DownloadManager.Request request = new DownloadManager.Request(uri);
            request.setTitle("EWI");
            // 指定文件保存在应用的私有目录
            request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, TCTInsatllActionBroadcastReceiver.APK_FILE_NAME);
            long reference = 0;
            try {
                reference = downloadManager.enqueue(request);// 下载id
            } catch (IllegalArgumentException e) {
                return;
            }
            final long id = reference;
            // 注册数据库监听
            getContentResolver().registerContentObserver(
                    Uri.parse("content://downloads/my_downloads"),
                    true,
                    contentObserver = new ContentObserver(null) {
                        @Override
                        public void onChange(boolean selfChange) {
                            DownloadManager.Query query = new DownloadManager.Query();
                            query.setFilterById(id);
                            Cursor my = downloadManager.query(query);
                            if (my != null) {
                                if (my.moveToFirst()) {
                                    // String
                                    // fileUri=my.getString(my.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                                    int fileSize = my.getInt(my
                                            .getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                                    int bytesDL = my.getInt(my
                                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                    int percent = bytesDL * 100 / fileSize;
                                    updateProgress(percent);

                                    if (percent >= 99) {
                                        btn_update.setEnabled(true);
                                        btn_sure.setEnabled(true);
                                    }

                                }
                                my.close();
                            } else {
                                btn_update.setEnabled(true);
                                btn_sure.setEnabled(true);
                                btn_update.setText("升级");
                            }
                        }
                    });
        } catch (Exception e) {
            btn_update.setEnabled(true);
            btn_sure.setEnabled(true);
            btn_update.setText("升级");
        }
    }

    /**
     * 删除之前下载的apk文件
     */
    private void clearDownLoadApk(final DownloadManager manager) {
        // 删除失败和成功状态的数据
        new Thread(new Runnable() {
            @Override
            public void run() {
                int delete[] = {DownloadManager.STATUS_FAILED, DownloadManager.STATUS_SUCCESSFUL};
                DownloadManager.Query query = new DownloadManager.Query();
                for (int element : delete) {
                    query.setFilterByStatus(element);
                    Cursor cursor = manager.query(query);
                    if (cursor != null) {
                        if (cursor.moveToFirst()) {
                            do {
                                manager.remove(cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_ID)));
                            } while (cursor.moveToNext());
                        }
                        cursor.close();
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contentObserver != null) {
            getContentResolver().unregisterContentObserver(contentObserver);
        }
    }

    /**
     * 版本升级更新进度
     *
     * @param percent
     */
    protected void updateProgress(int percent) {
        Message message = handler.obtainMessage();
        message.what = 2;
        message.arg1 = percent;
        handler.sendMessage(message);
    }

    public Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 2:
                    if (msg.arg1 == 100) {
                        btn_update.setText("升级");
                    } else {
                        btn_update.setText("下载进度： " + String.valueOf(msg.arg1) + "%");
                    }
                    break;
            }
        }
    };

    public int getVersionCode(Context context)//获取版本号(内部识别号)
    {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
