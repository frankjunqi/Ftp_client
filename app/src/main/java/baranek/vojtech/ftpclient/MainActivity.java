package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.UUID;

import baranek.vojtech.ftpclient.api.EwiLoginService;
import baranek.vojtech.ftpclient.api.Host;
import baranek.vojtech.ftpclient.entity.EwiResBody;
import baranek.vojtech.ftpclient.gsonfactory.GsonConverterFactory;
import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class MainActivity extends Activity implements View.OnClickListener {

    private final String TAG = getClass().getSimpleName();
    private String FTP_HOST = "27.54.248.35", FTP_USER = "MES", FTP_PASS = "";
    private final String EWIPATH = "/EWI";// 本地建立的文件夹的根目录

    @Bind(R.id.progress)
    MaterialProgressBar progress;

    @Bind(R.id.btn_right_1)
    Button btn_right_1;

    @Bind(R.id.btn_right_2)
    Button btn_right_2;

    @Bind(R.id.btn_right_3)
    Button btn_right_3;

    @Bind(R.id.btn_right_4)
    Button btn_right_4;

    @Bind(R.id.ll_pdf)
    LinearLayout ll_pdf;

    @Bind(R.id.tv_custname)
    TextView tv_custname;

    @Bind(R.id.tv_machinename)
    TextView tv_machinename;

    @Bind(R.id.tv_deviceid)
    TextView tv_deviceid;

    @Bind(R.id.iv_show_fresco)
    SimpleDraweeView iv_show_fresco;

    private MyRecyclerAdapter adapter;
    private FTPFile[] files;

    // 接口请求的数据
    private String MachineCode = "";//设备代码
    private String PN = "";//零件号
    private String CustName = "";
    private String MachineName = "";
    private String deviceId = "";


    private static final String FILE_PATH = "filepath";
    private MuPDFCore core;
    private MuPDFReaderView mDocView;
    private String mFilePath = "";

    private AssyncFtpTaskActions assyncFtpTaskActions;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        deviceId = getDeviceId();
        asyncRequest();
        btn_right_1.setOnClickListener(this);
        btn_right_2.setOnClickListener(this);
        btn_right_3.setOnClickListener(this);
        btn_right_4.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {
        ll_pdf.removeAllViews();
        switch (v.getId()) {
            case R.id.btn_right_1:
                DownloadFile(EWIPATH, MachineCode, PN, "1.pdf");
                break;
            case R.id.btn_right_2:
                DownloadFile(EWIPATH, MachineCode, PN, "2.pdf");
                break;
            case R.id.btn_right_3:
                DownloadFile(EWIPATH, MachineCode, PN, "3.pdf");
                break;
            case R.id.btn_right_4:
                DownloadFile(EWIPATH, MachineCode, PN, "4.pdf");
                break;

        }
    }

    private void initPDF() {
        core = openFile(Uri.decode(mFilePath));
        if (core != null && core.countPages() == 0) {
            core = null;
        }
        if (core == null || core.countPages() == 0 || core.countPages() == -1) {
            Log.e(TAG, "Document Not Opening");
        }
        if (core != null) {
            mDocView = new MuPDFReaderView(MainActivity.this) {
                @Override
                protected void onMoveToChild(int i) {
                    if (core == null)
                        return;
                    super.onMoveToChild(i);
                }
            };
            mDocView.setAdapter(new MuPDFPageAdapter(MainActivity.this, core));
            ll_pdf.addView(mDocView);
        }
    }

    private MuPDFCore openBuffer(byte buffer[]) {
        System.out.println("Trying to open byte buffer");
        try {
            core = new MuPDFCore(MainActivity.this, buffer);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return core;
    }

    private MuPDFCore openFile(String path) {
        int lastSlashPos = path.lastIndexOf('/');
        mFilePath = new String(lastSlashPos == -1
                ? path
                : path.substring(lastSlashPos + 1));
        try {
            core = new MuPDFCore(MainActivity.this, path);
            // New file: drop the old outline data
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            return null;
        }
        return core;
    }

    /**
     * Retrofit 异步请求
     */
    private void asyncRequest() {
        // 异步请求处理
        Log.e(TAG, "MainActivity_async_Request");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Host.HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        EwiLoginService service = retrofit.create(EwiLoginService.class);
        Call<EwiResBody> allMachineResBodyCall = service.allMachineList("Srv", "EWI.svc", "EWILogin", "test");
        allMachineResBodyCall.enqueue(new Callback<EwiResBody>() {
            @Override
            public void onResponse(Call<EwiResBody> call, Response<EwiResBody> response) {
                if (response != null && response.body() != null && response.body().d != null && response.body().d.Data != null) {
                    MachineCode = response.body().d.Data.MachineCode;
                    PN = response.body().d.Data.PN;
                    CustName = response.body().d.Data.CustName;
                    MachineName = response.body().d.Data.MachineName;
                    String CustLogo = response.body().d.Data.CustLogo;

                    tv_custname.setText(CustName);
                    tv_machinename.setText(MachineName);
                    tv_deviceid.setText(deviceId);

                    Uri uri = Uri.parse(Host.HOST + "res/customer/" + CustLogo);
                    iv_show_fresco.setImageURI(uri);

                } else {
                    Toast.makeText(MainActivity.this, "接口错误", Toast.LENGTH_LONG).show();
                    return;
                }
            }

            @Override
            public void onFailure(Call<EwiResBody> call, Throwable throwable) {
                Toast.makeText(MainActivity.this, "网络出现异常，请检查网络链接", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Download file method
     */
    private void DownloadFile(String ewiPath, String MachineCode, String PN, String pdfName) {
        if (assyncFtpTaskActions != null) {
            assyncFtpTaskActions.cancel(true);
        }
        assyncFtpTaskActions = new AssyncFtpTaskActions();
        assyncFtpTaskActions.execute("DOWNLOAD", ewiPath, MachineCode, PN, pdfName);

    }


    /**
     * Assync task  for operations with files
     */
    public class AssyncFtpTaskActions extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            FTPClient mFTPClient = new FTPClient();
            Integer result = 0;
            String path = "";
            try {
                /**
                 Establish connection
                 */
                mFTPClient.setControlEncoding("UTF-8");
                mFTPClient.connect(FTP_HOST, 6888);
                mFTPClient.login(FTP_USER, FTP_PASS);
                mFTPClient.enterLocalPassiveMode();
                mFTPClient.changeWorkingDirectory(EWIPATH);

                /**
                 Action with files
                 */

                switch (params[0]) {
                    case "DELETE": {
                        mFTPClient.deleteFile(params[1]);

                        break;
                    }
                    case "RENAME": {
                        mFTPClient.rename(params[1], params[2]);
                        break;
                    }
                    case "DOWNLOAD": {
                        String ewi = params[1];
                        String machineCode = params[2];
                        String pn = params[3];
                        String pdfPath = params[4];
                        // 创建目录
                        File ewiFile = new File(Environment.getExternalStorageDirectory() + File.separator + ewi);
                        ewiFile.mkdir();
                        File machineFile = new File(Environment.getExternalStorageDirectory() + File.separator + ewi + File.separator + machineCode);
                        machineFile.mkdir();
                        File pnFile = new File(Environment.getExternalStorageDirectory() + File.separator + ewi + File.separator + machineCode + File.separator + pn);
                        pnFile.mkdir();
                        // 创建pdf
                        File pdfFile = new File(Environment.getExternalStorageDirectory() + File.separator + ewi + File.separator + machineCode + File.separator + pn + File.separator + pdfPath);
                        path = ewi + File.separator + machineCode + File.separator + pn + File.separator + pdfPath;
                        if (pdfFile.exists()) {
                            pdfFile.delete();
                        }
                        pdfFile.createNewFile();
                        FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + ewi + File.separator + machineCode + File.separator + pn + File.separator + pdfPath);
                        mFTPClient.retrieveFile(ewi + File.separator + machineCode + File.separator + pn + File.separator + pdfPath, fos);
                        fos.flush();
                        fos.close();
                        break;
                    }
                    case "DELETEDIR": {
                        boolean rem = mFTPClient.removeDirectory(params[1]);

                        break;
                    }
                    case "CREATEDIR": {
                        String strPath = "";
                        if (mFTPClient.changeWorkingDirectory(params[1])) {
                            strPath = strPath + "/" + params[1];
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), R.string.folderalreadyexists, Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            mFTPClient.makeDirectory(params[1]);
                        }
                        break;
                    }
                    case "UPLOADFILE": {
                        File f = new File(params[1]);
                        InputStream input = new FileInputStream(f);
                        if (mFTPClient.getReplyCode() != 550) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), R.string.filealreadyExiists, Toast.LENGTH_SHORT).show();
                                }
                            });

                        } else {
                            mFTPClient.storeFile(EWIPATH + "/" + f.getName().toString(), input);
                        }
                        break;
                    }
                }
                files = mFTPClient.listFiles();
                result = 1;
                mFTPClient.disconnect();
            } catch (SocketException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return String.valueOf(path);
        }

        /**
         * show progress bar
         */

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
            setProgressBarIndeterminateVisibility(true);
        }

        /**
         * Show changed files or fail message
         */

        @Override
        protected void onPostExecute(String path) {
            progress.setVisibility(View.GONE);
            if (!TextUtils.isEmpty(path)) {
                mFilePath = Environment.getExternalStorageDirectory().getAbsolutePath() + path;
                initPDF();
//                Intent intent = new Intent(MainActivity.this, SampleActivity.class);
//                intent.putExtra("path", path);
//                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), R.string.action_failed, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static String getDeviceId() {
        String deviceId = "";
        try {
            // 先获取androidid
            deviceId = Settings.Secure.getString(MyApplication.getInstance().getContentResolver(), Settings.Secure.ANDROID_ID);
            // 在主流厂商生产的设备上，有一个很经常的bug，
            // 就是每个设备都会产生相同的ANDROID_ID：9774d56d682e549c
            if (TextUtils.isEmpty(deviceId) || "9774d56d682e549c".equals(deviceId)) {
                TelephonyManager telephonyManager = (TelephonyManager) MyApplication.getInstance()
                        .getSystemService(Context.TELEPHONY_SERVICE);
                deviceId = telephonyManager.getDeviceId();
            }
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = UUID.randomUUID().toString();
                deviceId = deviceId.replaceAll("-", "");
            }
        } catch (Exception e) {
            deviceId = UUID.randomUUID().toString();
            deviceId = deviceId.replaceAll("-", "");
        } finally {
            return deviceId;
        }
    }

}
