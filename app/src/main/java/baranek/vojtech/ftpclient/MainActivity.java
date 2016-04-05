package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import baranek.vojtech.ftpclient.api.EwiLoginService;
import baranek.vojtech.ftpclient.api.Host;
import baranek.vojtech.ftpclient.entity.EwiResBody;
import baranek.vojtech.ftpclient.gsonfactory.GsonConverterFactory;
import baranek.vojtech.ftpclient.view.TitleLineView;
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


    // 错误标识码
    private final String NOFILEERROR = "1";// 远程无文件
    private final String NOREGISTEDEVICE = "2";// 设备没有注册
    private final String NONETWORK = "3";// 设备没有注册
    private final String FILECHECKOK = "4";// FileCheckOK
    private final String FILEDOWNOK = "5";// FILEDOWNOK
    private final String FILEDOWNERROR = "6";// FILEDOWNOKEOOR
    private final String PDFNERROR = "7";// PDF error


    // 发送请求的标志码
    private static final int SENDFLAG = 0x10;

    private String currentErrorFlag = "";// 当前的错误类型

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
    @Bind(R.id.tv_countpage)
    TextView tv_countpage;
    @Bind(R.id.ll_title)
    LinearLayout ll_title;
    @Bind(R.id.ll_bottom)
    LinearLayout ll_bottom;
    @Bind(R.id.ll_content)
    LinearLayout ll_content;
    @Bind(R.id.tv_progress_desc)
    TextView tv_progress_desc;
    @Bind(R.id.tv_product)
    TextView tv_product;
    @Bind(R.id.iv_logo)
    ImageView iv_logo;
    @Bind(R.id.tv_order)
    TextView tv_order;

    private MyRecyclerAdapter adapter;
    private FTPFile[] files;

    // 接口请求的数据
    private String MachineCode = "";//设备代码
    private String PN = "";//零件号
    private String PO = "";//订单号
    private String CustName = "";
    private String MachineName = "";
    private String deviceId = "";

    private MuPDFCore core;
    private int totalPage = 0;
    private int currentPage = 1;

    private TitleLineView titleLineView;


    private ArrayList<Button> buttonList = new ArrayList<>();

    // 定时器操作
    private static RequestHandler requestHandler = null;

    private class RequestHandler extends Handler {

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case SENDFLAG:
                    asyncRequest();
                    break;
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        deleteDirectory(Environment.getExternalStorageDirectory() + EWIPATH + File.separator);
        ButterKnife.bind(this);
        deviceId = getDeviceId();

        // 计算高度
        WindowManager wm = (WindowManager) getApplication()
                .getSystemService(Context.WINDOW_SERVICE);

        int heightpix = wm.getDefaultDisplay().getHeight();
        int height = heightpix / 9 - 18;


        // init height
        titleLineView = new TitleLineView(MainActivity.this);
        titleLineView.setTitle("E-WI");
        ll_title.addView(titleLineView);
        ll_content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightpix * 8 / 9));
        ll_bottom.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 18));


        btn_right_1.setOnClickListener(this);
        btn_right_2.setOnClickListener(this);
        btn_right_3.setOnClickListener(this);
        btn_right_4.setOnClickListener(this);
        buttonList.add(btn_right_1);
        buttonList.add(btn_right_2);
        buttonList.add(btn_right_3);
        buttonList.add(btn_right_4);
        requestHandler = new RequestHandler();
        requestHandler.sendEmptyMessage(SENDFLAG);
    }


    @Override
    public void onClick(final View v) {
        if (core != null) {
            core.onDestroy();
            core = null;
        }
        ll_pdf.removeAllViews();
        tv_countpage.setText("");
        requestHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                switch (currentErrorFlag) {
                    case NOREGISTEDEVICE: {
                        tv_progress_desc.setText("联系管理员关联本机设备，并显示本机设备唯一码...");
                        return;
                    }
                    case NONETWORK: {
                        tv_progress_desc.setText("网络出现异常，请检查网络链接...");
                        return;
                    }
                }
                switch (v.getId()) {
                    case R.id.btn_right_1:
                        openPdf(0);
                        break;
                    case R.id.btn_right_2:
                        openPdf(1);
                        break;
                    case R.id.btn_right_3:
                        openPdf(2);
                        break;
                    case R.id.btn_right_4:
                        openPdf(3);
                        break;

                }
            }
        }, 300);

    }


    private void initPDF(String mFilePath) {
        core = openFile(Uri.decode(mFilePath));
        if (core != null && core.countPages() == 0) {

        }
        if (core == null || core.countPages() == 0 || core.countPages() == -1) {
            Log.e(TAG, "Document Not Opening");
        }
        if (core != null) {
            MuPDFReaderView mDocView = new MuPDFReaderView(MainActivity.this) {
                @Override
                protected void onMoveToChild(int index) {
                    if (core == null) {
                        return;
                    }
                    currentPage = index + 1;
                    tv_countpage.setText(currentPage + "/" + totalPage + "页");
                    super.onMoveToChild(index);
                }
            };
            mDocView.setAdapter(new MuPDFPageAdapter(MainActivity.this, core));
            totalPage = core.countPages();
            tv_countpage.setText(currentPage + "/" + totalPage + "页");
            Log.e(TAG, "totalpagecount = " + core.countPages() + " - ");
            ll_pdf.addView(mDocView);
        }
    }


    private MuPDFCore openFile(String path) {
        MuPDFCore core = null;
        try {
            core = new MuPDFCore(MainActivity.this, path);
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
                tv_deviceid.setText(deviceId);
                if (response != null && response.body() != null && response.body().d != null && response.body().d.Data != null) {
                    MachineCode = response.body().d.Data.MachineCode;
                    String tempPN = response.body().d.Data.PN;
                    CustName = response.body().d.Data.CustName;
                    MachineName = response.body().d.Data.MachineName;
                    PO = response.body().d.Data.PO;
                    String CustLogo = response.body().d.Data.CustLogo;

                    tv_custname.setText(CustName);
                    tv_machinename.setText(MachineName);
                    tv_product.setText(tempPN);
                    tv_order.setText(PO);

                    Uri uri = Uri.parse(Host.HOST + "res/customer/" + CustLogo);
                    iv_show_fresco.setImageURI(uri);

                    handleMarqueeText(response.body().d.MsgList);

                    // 判断pn是否发生变化，有变化需要更新远程files
                    if (!PN.equals(tempPN) || files == null || files.length == 0) {
                        PN = tempPN;
                        // delete file
                        deleteDirectory(Environment.getExternalStorageDirectory() + EWIPATH + File.separator);
                        // 创建目录
                        File pnFile = new File(Environment.getExternalStorageDirectory() + EWIPATH);
                        pnFile.mkdirs();
                        // 检查远程指定文件夹的file list
                        checkFileList(MachineCode, PN);
                    } else {
                        requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                    }
                } else {
                    currentErrorFlag = NOREGISTEDEVICE;
                    tv_progress_desc.setText("联系管理员关联本机设备，并显示本机设备唯一码...");
                    // Toast.makeText(MainActivity.this, "联系管理员关联本机设备，并显示本机设备唯一码。", Toast.LENGTH_LONG).show();
                    requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                }
            }

            @Override
            public void onFailure(Call<EwiResBody> call, Throwable throwable) {
                currentErrorFlag = NONETWORK;
                tv_progress_desc.setText("网络出现异常，请检查网络链接...");
                // Toast.makeText(MainActivity.this, "网络出现异常，请检查网络链接", Toast.LENGTH_LONG).show();
                requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
            }
        });
    }

    /**
     * 处理滚动的字幕
     */
    private void handleMarqueeText(List<String> marqueeText) {
        if (marqueeText != null && marqueeText.size() > 0) {
            String showDate = "";
            if (marqueeText != null) {
                for (int i = 0; i < marqueeText.size(); i++) {
                    showDate = showDate + marqueeText.get(i);
                }
                // 判断现实的文案是否一样
                if (showDate.equals(titleLineView.getNoticContent())) {
                    return;
                }
                titleLineView.setNoticeContent(showDate);
            }
        }

    }

    // Download file method
    private void DownloadFile(String MachineCode, String PN, int number) {
        AssyncFtpTaskActions assyncFtpTaskActions = new AssyncFtpTaskActions();
        assyncFtpTaskActions.execute("DOWNLOAD", MachineCode, PN, files[number].getName());
    }

    private void checkFileList(String MachineCode, String PN) {
        AssyncFtpTaskActions assyncFtpTaskActions = new AssyncFtpTaskActions();
        assyncFtpTaskActions.execute("CHECKFILELIST", MachineCode, PN);
    }

    private void openPdf(int number) {
        // 远程文件目录
        if (files == null || files.length == 0) {
            tv_progress_desc.setText("远程文件为空，维护后，请刷新当前页面...");
            // Toast.makeText(MainActivity.this, "远程文件为空，维护后，请刷新当前页面。", Toast.LENGTH_LONG).show();
            return;
        }

        // 是否存在索引文件
        if (number >= files.length) {
            tv_progress_desc.setText("文件暂不存在...");
            // Toast.makeText(MainActivity.this, "文件暂不存在", Toast.LENGTH_LONG).show();
            return;
        }

        // 设置按钮的颜色
        for (int i = 0; i < buttonList.size(); i++) {
            if (number == i) {
                buttonList.get(i).setBackgroundResource(R.drawable.bg_tab_sel);
                buttonList.get(i).setTextColor(getResources().getColor(R.color.main_white));
            } else {
                buttonList.get(i).setBackgroundResource(R.drawable.bg_tab);
                buttonList.get(i).setTextColor(getResources().getColor(R.color.title_dark));
            }
        }

        // 判断本地是否有文件
        String localPdfFile = Environment.getExternalStorageDirectory() + EWIPATH + File.separator + files[number].getName();
        Log.e(TAG, localPdfFile);
        File file = new File(localPdfFile);
        if (file.exists()) {
            initPDF(localPdfFile);
        } else {
            tv_progress_desc.setText("文件暂不存在...");
            // Toast.makeText(MainActivity.this, "文件暂不存在", Toast.LENGTH_LONG).show();
        }
    }

    private String fileName = Environment.getExternalStorageDirectory() + EWIPATH + File.separator + "error.txt";

    //保存字符串到文件中
    private void saveAsFileWriter(String content) {
        try {
            File fileF = new File(fileName);
            fileF.createNewFile();
        } catch (Exception exp) {

        }
        FileWriter fwriter = null;
        try {
            fwriter = new FileWriter(fileName);
            fwriter.write(content);
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                fwriter.flush();
                fwriter.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * ftp task 的post的值
     */
    public class TaskDate {
        public String resultFlag;
        public String pdfPath;
    }


    /**
     * Assync task  for operations with files
     */
    public class AssyncFtpTaskActions extends AsyncTask<String, Void, TaskDate> {

        @Override
        protected TaskDate doInBackground(String... params) {
            FTPClient mFTPClient = new FTPClient();
            TaskDate taskDate = new TaskDate();
            try {
                // Establish connection
                mFTPClient.setControlEncoding("UTF-8");
                mFTPClient.connect(FTP_HOST, 6888);
                mFTPClient.login(FTP_USER, FTP_PASS);
                mFTPClient.enterLocalPassiveMode();
                mFTPClient.changeWorkingDirectory(EWIPATH);
                //mFTPClient.setReceiveBufferSize(1024);

                // Action with files
                switch (params[0]) {

                    case "CHECKFILELIST": {
                        String machineCode = params[1];
                        String pn = params[2];
                        if (mFTPClient.changeWorkingDirectory(EWIPATH + File.separator + machineCode + File.separator + pn + File.separator)) {
                            files = mFTPClient.listFiles();
                        }
                        if (files == null || files.length == 0) {
                            currentErrorFlag = NOFILEERROR;
                            taskDate.resultFlag = NOFILEERROR;
                        } else {
                            taskDate.resultFlag = FILECHECKOK;
                        }
                        break;
                    }

                    case "DOWNLOAD": {
                        String machineCode = params[1];
                        String pn = params[2];
                        String pdfPath = params[3];

                        // 创建pdf
                        File pdfFile = new File(Environment.getExternalStorageDirectory() + EWIPATH + File.separator + pdfPath);
                        taskDate.pdfPath = pdfPath;
                        // task调用下载，即需要重新进行下载
                        if (pdfFile.exists()) {
                            pdfFile.delete();
                        }
                        pdfFile.createNewFile();


                        FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + EWIPATH + File.separator + pdfPath, true);
                        // boolean isOk = mFTPClient.retrieveFile(EWIPATH + File.separator + machineCode + File.separator + pn + File.separator + pdfPath, fos);
                        InputStream inputStream = mFTPClient.retrieveFileStream(EWIPATH + File.separator + machineCode + File.separator + pn + File.separator + pdfPath);
                        long step = inputStream.available() / 100;
                        long process = 0;
                        long currentSize = 0;
                        byte[] b = new byte[1024];
                        int length = 0;
                        while ((length = inputStream.read(b)) != -1) {
                            fos.write(b, 0, length);
                            currentSize = currentSize + length;
                            /*if (currentSize / step != process) {
                                process = currentSize / step;
                                // 每隔%5的进度返回一次
                                //进度
                                Log.e(TAG, process + "进度");
                            }*/
                        }

                        fos.flush();
                        fos.close();
                        inputStream.close();
                        if (mFTPClient.completePendingCommand()) {
                            Log.e(TAG, "success" + pdfPath);
                            taskDate.resultFlag = FILEDOWNOK;
                        } else {
                            Log.e(TAG, "failed" + pdfPath);
                            taskDate.resultFlag = FILEDOWNERROR;
                        }
                        break;
                    }
                }
                mFTPClient.disconnect();
            } catch (SocketException e) {
                taskDate.resultFlag = NONETWORK;
            } catch (IOException e) {
                taskDate.resultFlag = PDFNERROR;
            }
            return taskDate;
        }

        /**
         * show progress bar
         */

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
            tv_progress_desc.setVisibility(View.VISIBLE);
            setProgressBarIndeterminateVisibility(true);
        }

        /**
         * Show changed files or fail message
         */

        @Override
        protected void onPostExecute(TaskDate taskDate) {
            switch (taskDate.resultFlag) {
                case NOFILEERROR: {
                    // 远程文件不存在的情况下，10s后进行重新请求pn号
                    tv_progress_desc.setText("远程文件资料不存在，请及时维护...");
                    // Toast.makeText(getApplicationContext(), "远程文件资料不存在，请及时维护。", Toast.LENGTH_SHORT).show();
                    requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                    break;
                }
                case FILECHECKOK: {
                    // 远程文件有数据do nothing
                    tv_progress_desc.setText("获取远程列表成功，开始进行下载文件...");
                    // Toast.makeText(getApplicationContext(), "获取远程列表成功，开始进行下载文件", Toast.LENGTH_SHORT).show();
                    for (int i = 0; files != null && i < files.length && i < buttonList.size(); i++) {
                        Log.e(TAG, files[i].getName());
                        DownloadFile(MachineCode, PN, i);
                    }
                    requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                    break;
                }
                case FILEDOWNOK: {
                    // 文件下载完成并且打开
                    tv_progress_desc.setText("文件：" + taskDate.pdfPath + "，下载完成.");
                    // Toast.makeText(getApplicationContext(), taskDate.pdfPath + "下载完成", Toast.LENGTH_SHORT).show();
                    for (int i = 0; i < files.length && i < buttonList.size(); i++) {
                        if (files[i].getName().equals(taskDate.pdfPath)) {
                            /*buttonList.get(i).setText(taskDate.pdfPath);
                            buttonList.get(i).setBackgroundColor(getResources().getColor(R.color.accentBL));*/
                        }
                    }
                    if (new File(Environment.getExternalStorageDirectory() + EWIPATH + File.separator).list().length == 4) {
                        progress.setVisibility(View.INVISIBLE);
                        tv_progress_desc.setVisibility(View.INVISIBLE);
                    }
                    break;
                }
                case FILEDOWNERROR: {
                    // ftp下载失败，提示重新下载
                    // 下载失败，删除本地文件
                    if (!TextUtils.isEmpty(taskDate.pdfPath)) {
                        File file = new File(Environment.getExternalStorageDirectory() + taskDate.pdfPath);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    tv_progress_desc.setText("远程文件" + taskDate.pdfPath + "下载失败，请重新尝试...");
                    // Toast.makeText(getApplicationContext(), "远程文件" + taskDate.pdfPath + "下载失败，请重新尝试。", Toast.LENGTH_SHORT).show();
                    break;
                }
                case NONETWORK: {
                    tv_progress_desc.setText("网络出现异常，请检查网络链接...");
                    // Toast.makeText(getApplicationContext(), "网络出现异常，请检查网络链接", Toast.LENGTH_SHORT).show();
                    break;
                }
                case PDFNERROR: {
                    // 下载失败，删除本地文件
                    if (!TextUtils.isEmpty(taskDate.pdfPath)) {
                        File file = new File(Environment.getExternalStorageDirectory() + taskDate.pdfPath);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    tv_progress_desc.setText("文件下载出现异常，请重新下载...");
                    // Toast.makeText(getApplicationContext(), "pdf下载出现异常，请重新下载", Toast.LENGTH_SHORT).show();
                    break;
                }
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

    /**
     * 删除目录（文件夹）以及目录下的文件
     *
     * @param sPath 被删除目录的文件路径
     * @return 目录删除成功返回true，否则返回false
     */
    public boolean deleteDirectory(String sPath) {
        File dirFile = new File(sPath);
        //删除文件夹下的所有文件(包括子目录)
        File[] files = dirFile.listFiles();
        for (int i = 0; files != null && i < files.length; i++) {
            //删除子文件
            files[i].delete();
        }
        return true;
    }

    @Override
    protected void onDestroy() {
        if (requestHandler != null) {
            requestHandler.removeMessages(SENDFLAG);
        }
        super.onDestroy();
    }

    // 系统退出的纪录时间
    private long mExitTime = 0;

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - mExitTime > 2000) {
            Toast.makeText(this, "在按一次退出系统", Toast.LENGTH_LONG).show();
            mExitTime = System.currentTimeMillis();
        } else {
            this.finish();
        }


    }
}
