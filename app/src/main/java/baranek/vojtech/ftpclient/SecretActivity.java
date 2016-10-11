package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import baranek.vojtech.ftpclient.api.EwiService;
import baranek.vojtech.ftpclient.api.Host;
import baranek.vojtech.ftpclient.entity.UpdaeResBody;
import baranek.vojtech.ftpclient.entity.Update;
import baranek.vojtech.ftpclient.entity.UpdateObj;
import baranek.vojtech.ftpclient.gsonfactory.GsonConverterFactory;
import okhttp3.ResponseBody;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_secret);



        et_secret = (EditText) findViewById(R.id.et_secret);
        btn_sure = (Button) findViewById(R.id.btn_sure);
        btn_sure.setOnClickListener(this);
        btn_update = (Button) findViewById(R.id.btn_update);
        btn_update.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sure:
                String secret = et_secret.getText().toString().replace(" ","");
                if("mesmubea".equals(secret)){
                    Toast.makeText(this,"登陆成功！",Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this,SettingActivity.class));
                }else{
                    Toast.makeText(this,"密码错误，请联系管理员！",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.btn_update:
                requestUpdate();
                break;
        }
    }

    public void requestUpdate(){
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Host.UpdateHost)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        EwiService service = retrofit.create(EwiService.class);
        Call<UpdaeResBody> updaeResBodyCall = service.updateVersion("Srv", "Base.svc", "CheckUpdate", "MubeaEWI", "1.0");
        updaeResBodyCall.enqueue(new Callback<UpdaeResBody>() {
            @Override
            public void onResponse(Call<UpdaeResBody> call, Response<UpdaeResBody> response) {
                response.body().d = new UpdateObj();
                response.body().d.Data = new Update();
                response.body().d.Data.FileUrl = "http://gdown.baidu.com/data/wisegame/a6cb82227e027503/tongchenglvyou_114.apk";
                if (response != null && response.body() != null && response.body().d != null && response.body().d.Data != null) {
                    Update update = response.body().d.Data;
                    if (!TextUtils.isEmpty(update.FileUrl)) {
                        // 下载
                        Toast.makeText(SecretActivity.this, "Downloding...", Toast.LENGTH_SHORT).show();

                        downlaod(response.body().d.Data.FileUrl);
                    }
                } else {
                    Toast.makeText(SecretActivity.this, "已经是最新版本！", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<UpdaeResBody> call, Throwable throwable) {
                Toast.makeText(SecretActivity.this, "更新接口异常，请联系维护人员 ！", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void downlaod(final String fileUrl){

        new Thread(){
            @Override
            public void run() {
                super.run();
                Retrofit retrofit = new Retrofit.Builder()
                        .baseUrl(Host.HOST)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
                EwiService service = retrofit.create(EwiService.class);
                Call<ResponseBody> call = service.downloadFileWithDynamicUrlSync(fileUrl);
                call.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        boolean writtenToDisk = writeResponseBodyToDisk(response.body());
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable throwable) {

                    }
                });
            }
        }.start();

    }



    private boolean writeResponseBodyToDisk(ResponseBody body) {
        try {
            // todo change the file location/name according to your needs
            File futureStudioIconFile = new File(getExternalFilesDir(null) + File.separator + "Future Studio Icon.png");

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[4096];

                long fileSize = body.contentLength();
                long fileSizeDownloaded = 0;

                inputStream = body.byteStream();
                outputStream = new FileOutputStream(futureStudioIconFile);

                while (true) {
                    int read = inputStream.read(fileReader);

                    if (read == -1) {
                        break;
                    }

                    outputStream.write(fileReader, 0, read);

                    fileSizeDownloaded += read;

                    Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                }

                outputStream.flush();

                return true;
            } catch (IOException e) {
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            return false;
        }
    }
}
