package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.artifex.mupdfdemo.MuPDFCore;
import com.artifex.mupdfdemo.MuPDFPageAdapter;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.List;
import java.util.UUID;

import baranek.vojtech.ftpclient.api.EwiService;
import baranek.vojtech.ftpclient.api.Host;
import baranek.vojtech.ftpclient.entity.EwiResBody;
import baranek.vojtech.ftpclient.entity.TempResBody;
import baranek.vojtech.ftpclient.entity.UpdaeResBody;
import baranek.vojtech.ftpclient.entity.Update;
import baranek.vojtech.ftpclient.gsonfactory.GsonConverterFactory;
import baranek.vojtech.ftpclient.view.TitleLineView;
import butterknife.Bind;
import butterknife.ButterKnife;
import me.zhanghai.android.materialprogressbar.MaterialProgressBar;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


public class MainActivity extends Activity {

    private final String TAG = getClass().getSimpleName();
    private String FTP_USER = "MES", FTP_PASS = "";
    private final String EWIPATH = "/EWI";// 本地建立的文件夹的根目录
    // 错误标识码
    private final String NOFILEERROR = "1";// 远程无文件
    private final String NOREGISTEDEVICE = "2";// 设备没有注册
    private final String NONETWORK = "3";// 设备没有注册
    private final String FILECHECKOK = "4";// FileCheckOK
    private final String FILEDOWNOK = "5";// FILEDOWNOK
    private final String FILEDOWNERROR = "6";// FILEDOWNOKEOOR
    private final String PDFNERROR = "7";// PDF error
    private final String NullPointerInputStream = "8";// inputstream is null

    private static final int SENDFLAG = 0x10;// 发送请求的标志码
    private static final int TEMPERATURE = 0x11;// 发送温度的请求标志码
    private static final int UPDATEFLAG = 0x12;// 更新的请求标志

    private String currentErrorFlag = "";// 当前的错误类型

    @Bind(R.id.progress)
    MaterialProgressBar progress;
    @Bind(R.id.ll_pdf)
    LinearLayout ll_pdf;
    @Bind(R.id.tv_custname)
    TextView tv_custname;
    @Bind(R.id.tv_machinename)
    TextView tv_machinename;
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
    @Bind(R.id.recyclerViewMain)
    RecyclerView recyclerViewMain;
    @Bind(R.id.indeterminate_progress_large_library)
    MaterialProgressBar indeterminate_progress_large_library;
    @Bind(R.id.iv_lbpicpath)
    ImageView iv_lbpicpath;

    private MyRecyclerAdapter adapter;
    private FTPFile[] files;
    private FTPFile defaultFile = new FTPFile();

    // 接口请求的数据
    private String MachineCode = "";//设备代码
    private String PN = "";//零件号
    private String PO = "";//订单号
    private String CustName = "";
    private String MachineName = "";
    private String MachineDocPath = "";
    private String LBPicPath = "";

    private String FTPPath = "";
    private String deviceId = "";

    private MuPDFCore core;
    private int totalPage = 0;
    private int currentPage = 1;

    private TitleLineView titleLineView;

    private int picWidth = 0;// 图片的宽度

    // 定时器操作
    private static RequestHandler requestHandler = null;

    private ContentObserver contentObserver;
    private boolean isDownloading = false;


    private class RequestHandler extends Handler {

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case SENDFLAG:
                    asyncRequest();
                    break;
                case TEMPERATURE:
                    requestTemperature();
                    break;
                case UPDATEFLAG:
                    requestUpdate();
                    break;
            }
        }
    }

    /**
     * 请求温度接口
     */
    private void requestTemperature() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Host.HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        EwiService service = retrofit.create(EwiService.class);
        Call<TempResBody> tempResBodyCall = service.tempRequest("Srv", "BaseInfo.svc", "QueryWeather");
        tempResBodyCall.enqueue(new Callback<TempResBody>() {
            @Override
            public void onResponse(Call<TempResBody> call, Response<TempResBody> response) {
                if (response != null && response.body() != null && response.body().d != null && response.body().d.Data != null) {
                    if (titleLineView != null && !TextUtils.isEmpty(response.body().d.Data.TodayTemp)) {
                        titleLineView.setTv_temperature(response.body().d.Data.TodayTemp);
                        titleLineView.setIv_weather_icon(response.body().d.Data.Icon);
                        titleLineView.setTv_weather(response.body().d.Data.WeatherInfo);
                    }
                }
                requestHandler.sendEmptyMessageDelayed(TEMPERATURE, Host.TEMPLOOPER * 1000);
            }

            @Override
            public void onFailure(Call<TempResBody> call, Throwable throwable) {
                requestHandler.sendEmptyMessageDelayed(TEMPERATURE, Host.TEMPLOOPER * 1000);
            }
        });
    }

    private void initHOST() {
        String updateHost = getSP(Host.UPDATEKEYHOST);
        if (!TextUtils.isEmpty(updateHost) && updateHost.startsWith("http://") && updateHost.startsWith("/")) {
            Host.UpdateHost = updateHost;
        }

        String host = getSP(Host.KWYHOST);
        if (!TextUtils.isEmpty(host) && host.startsWith("http://") && host.endsWith("/")) {
            Host.HOST = host;
        }

        String ftphost = getSP(Host.KWYFTPHOST);
        if (!TextUtils.isEmpty(ftphost) && ftphost.split(".").length == 4) {
            Host.FTPHOST = ftphost;
        }

        String ftphostiptables = getSP(Host.KWYFTPHOSTIPTABLES);
        if (!TextUtils.isEmpty(ftphostiptables)) {
            try {
                Host.FTPHOSTIPTABLES = Integer.parseInt(ftphostiptables);
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    private String getSP(String key) {
        //同样，在读取SharedPreferences数据前要实例化出一个SharedPreferences对象
        SharedPreferences sharedPreferences = getSharedPreferences("test",
                Activity.MODE_PRIVATE);
        // 使用getString方法获得value，注意第2个参数是value的默认值
        String value = sharedPreferences.getString(key, "");
        return value;
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
        int widthpix = wm.getDefaultDisplay().getWidth();
        picWidth = widthpix / 6;
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        initHOST();

        // init height
        titleLineView = new TitleLineView(MainActivity.this);
        titleLineView.setTitle("E-WI");
        titleLineView.setImageLogoClickListen(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SecretActivity.class));
            }
        });

        ll_title.addView(titleLineView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (heightpix - 18) / 9));
        ll_bottom.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 18));
        ll_content.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightpix + Host.BOTTOMHEIGHT - 18 - (heightpix - 18) / 9));

        recyclerViewMain.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerAdapter(this);
        recyclerViewMain.setAdapter(adapter);
        adapter.setOnClickListener(new MyRecyclerAdapter.FileClickListen() {
            @Override
            public void filePdfClick(final String pdfPath) {
                if (core != null) {
                    core.onDestroy();
                    core = null;
                }
                ll_pdf.removeAllViews();
                totalPage = 1;
                currentPage = 1;
                tv_countpage.setText("");
                requestHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        switch (currentErrorFlag) {
                            case NOREGISTEDEVICE: {
                                iv_logo.setVisibility(View.VISIBLE);
                                tv_progress_desc.setText("联系管理员关联该设备\n设备唯一码:" + deviceId);
                            }
                            case NONETWORK: {
                                iv_logo.setVisibility(View.VISIBLE);
                                tv_progress_desc.setText("网络出现异常,请检查网络链接...");
                            }
                        }
                        openPdf(pdfPath);
                    }
                }, 100);
            }
        });
        requestHandler = new RequestHandler();
        requestHandler.sendEmptyMessage(SENDFLAG);
        requestHandler.sendEmptyMessage(TEMPERATURE);
        requestHandler.sendEmptyMessage(UPDATEFLAG);
    }

    private void initPDF(String mFilePath) {
        core = openFile(Uri.decode(mFilePath));
        if (core != null && core.countPages() == 0) {
            return;
        }
        if (core == null || core.countPages() == 0 || core.countPages() == -1) {
            return;
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

    private void asyncRequest() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Host.HOST)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        EwiService service = retrofit.create(EwiService.class);
        Call<EwiResBody> allMachineResBodyCall = service.allMachineList("Srv", "EWI.svc", "EWILogin", deviceId);
        allMachineResBodyCall.enqueue(new Callback<EwiResBody>() {
            @Override
            public void onResponse(Call<EwiResBody> call, Response<EwiResBody> response) {
                if (response != null && response.body() != null && response.body().d != null && response.body().d.Data != null) {
                    MachineCode = response.body().d.Data.MachineCode;
                    String tempPN = response.body().d.Data.PN;
                    CustName = response.body().d.Data.CustName;
                    FTPPath = response.body().d.Data.FTPPath;
                    String tempMachineDocPath = response.body().d.Data.MachineDocPath;
                    if (!MachineDocPath.equals(tempMachineDocPath)) {
                        MachineDocPath = tempMachineDocPath;
                        defaultFile.setLink(MachineDocPath);
                        defaultFile.setName("首页.pdf");
                        indeterminate_progress_large_library.setVisibility(View.INVISIBLE);
                        recyclerViewMain.setVisibility(View.VISIBLE);
                        adapter.setDefaultFTPFile(defaultFile);
                        adapter.setFiles(files);
                        DownloadFile(MachineDocPath);// download default pdf file
                    }
                    String tempLBPicPath = response.body().d.Data.LBPicPath;
                    if (!LBPicPath.equals(tempLBPicPath)) {
                        LBPicPath = tempLBPicPath;
                        DownloadFile(LBPicPath);// download pic file
                    }
                    MachineName = response.body().d.Data.MachineName;
                    PO = response.body().d.Data.PO;
                    String CustLogo = response.body().d.Data.CustLogo;
                    tv_custname.setText(CustName);
                    tv_machinename.setText(MachineName);
                    tv_product.setText(tempPN);
                    tv_order.setText(PO);
                    if (TextUtils.isEmpty(CustLogo)) {
                        iv_show_fresco.setVisibility(View.GONE);
                    } else {
                        iv_show_fresco.setVisibility(View.VISIBLE);
                        Uri uri = Uri.parse(Host.HOST + "res/customer/" + CustLogo);
                        iv_show_fresco.setImageURI(uri);
                    }
                    handleMarqueeText(response.body().d.Data.Msg);
                    // 判断pn是否发生变化，有变化需要更新远程files
                    if (TextUtils.isEmpty(tempPN)) {
                        tv_progress_desc.setText("设备空闲，无展示信息。");
                        requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                    } else if (!PN.equals(tempPN) || files == null || files.length == 0) {
                        PN = tempPN;
                        // delete file
                        deleteDirectory(Environment.getExternalStorageDirectory() + EWIPATH + File.separator);
                        // 创建目录
                        File pnFile = new File(Environment.getExternalStorageDirectory() + EWIPATH);
                        pnFile.mkdirs();
                        // 检查远程指定文件夹的file list
                        checkFileList(FTPPath);
                    } else {
                        requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                    }
                } else {
                    currentErrorFlag = NOREGISTEDEVICE;
                    tv_progress_desc.setText("联系管理员关联该设备\n设备唯一码:" + deviceId);
                    requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                }
            }

            @Override
            public void onFailure(Call<EwiResBody> call, Throwable throwable) {
                currentErrorFlag = NONETWORK;
                tv_progress_desc.setText("网络出现异常，请检查网络链接...");
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

    private void DownloadFile(String pdfFilePath) {
        AssyncFtpTaskActions assyncFtpTaskActions = new AssyncFtpTaskActions();
        assyncFtpTaskActions.execute("DOWNLOAD", pdfFilePath);
    }

    private void checkFileList(String FTPPath) {
        AssyncFtpTaskActions assyncFtpTaskActions = new AssyncFtpTaskActions();
        assyncFtpTaskActions.execute("CHECKFILELIST", FTPPath);
    }

    private void openPdf(String pdfPath) {
        iv_logo.setVisibility(View.INVISIBLE);
        // 判断本地是否有文件
        String localPdfFile = Environment.getExternalStorageDirectory() + pdfPath;
        File file = new File(localPdfFile);
        if (file.exists() && file.length() > 0) {
            initPDF(localPdfFile);
        } else {
            Toast.makeText(MainActivity.this, "文件暂不存在...", Toast.LENGTH_SHORT).show();
            tv_progress_desc.setText("文件暂不存在...");
        }
    }

    /**
     * ftp task 的post的值
     */
    public class TaskDate {
        public String resultFlag;
        public String pdfPath;
    }

    public class AssyncFtpTaskActions extends AsyncTask<String, Void, TaskDate> {
        @Override
        protected TaskDate doInBackground(String... params) {
            FTPClient mFTPClient = new FTPClient();
            TaskDate taskDate = new TaskDate();
            try {
                mFTPClient.setControlEncoding("UTF-8");
                mFTPClient.connect(Host.FTPHOST, Host.FTPHOSTIPTABLES);
                mFTPClient.login(FTP_USER, FTP_PASS);
                mFTPClient.enterLocalPassiveMode();
                mFTPClient.changeWorkingDirectory(EWIPATH);
                mFTPClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                mFTPClient.setFileTransferMode(FTPClient.STREAM_TRANSFER_MODE);
                mFTPClient.enterLocalPassiveMode(); //开启本地被动模式
                switch (params[0]) {
                    case "CHECKFILELIST": {
                        String FTPPath = params[1];
                        if (mFTPClient.changeWorkingDirectory(FTPPath)) {
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
                        String pdfFilePath = params[1];
                        String filePath = pdfFilePath.substring(0, pdfFilePath.lastIndexOf("/"));
                        File filepaths = new File(Environment.getExternalStorageDirectory() + filePath);
                        filepaths.mkdirs();

                        // 创建pdf
                        File pdfFile = new File(Environment.getExternalStorageDirectory() + pdfFilePath);
                        taskDate.pdfPath = pdfFilePath;
                        // task调用下载，即需要重新进行下载
                        if (pdfFile.exists()) {
                            pdfFile.delete();
                        }
                        pdfFile.createNewFile();

                        FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + pdfFilePath, true);
                        InputStream inputStream = mFTPClient.retrieveFileStream(pdfFilePath);
                        long currentSize = 0;
                        byte[] b = new byte[1024];
                        int length = 0;
                        while ((length = inputStream.read(b)) != -1) {
                            fos.write(b, 0, length);
                            currentSize = currentSize + length;
                        }
                        inputStream.close();
                        fos.flush();
                        fos.close();
                        if (mFTPClient.completePendingCommand()) {
                            taskDate.resultFlag = FILEDOWNOK;
                        } else {
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
            } catch (NullPointerException e) {
                taskDate.resultFlag = NullPointerInputStream;
            }
            return taskDate;
        }

        @Override
        protected void onPreExecute() {
            progress.setVisibility(View.VISIBLE);
            tv_progress_desc.setVisibility(View.VISIBLE);
            setProgressBarIndeterminateVisibility(true);
        }

        @Override
        protected void onPostExecute(TaskDate taskDate) {
            switch (taskDate.resultFlag) {
                case NullPointerInputStream: {
                    Toast.makeText(MainActivity.this, "文件 首页.pdf 不存在!", Toast.LENGTH_SHORT).show();
                    break;
                }
                case NOFILEERROR: {
                    // 远程文件不存在的情况下，10s后进行重新请求pn号
                    iv_logo.setVisibility(View.VISIBLE);
                    tv_progress_desc.setText("远程文件资料不存在（" + FTPPath + ")");
                    requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                    break;
                }
                case FILECHECKOK: {
                    // 远程文件有数据do nothing
                    tv_progress_desc.setText("获取远程列表成功，开始进行下载文件...");
                    indeterminate_progress_large_library.setVisibility(View.INVISIBLE);
                    recyclerViewMain.setVisibility(View.VISIBLE);
                    for (int i = 0; files != null && i < files.length; i++) {
                        files[i].setLink(FTPPath + files[i].getName());
                        DownloadFile(FTPPath + files[i].getName());
                    }
                    adapter.setFiles(files);
                    requestHandler.sendEmptyMessageDelayed(SENDFLAG, Host.TENLOOPER * 1000);
                    break;
                }
                case FILEDOWNOK: {
                    // 文件下载完成并且打开
                    tv_progress_desc.setText("文件：" + taskDate.pdfPath + ",下载完成.");
                    if (!TextUtils.isEmpty(defaultFile.getLink()) && !TextUtils.isEmpty(taskDate.pdfPath) && defaultFile.getLink().equals(taskDate.pdfPath)) {
                        openPdf(defaultFile.getLink());
                    }
                    if (LBPicPath.equals(taskDate.pdfPath)) {
                        // 显示图片
                        try {
                            FileInputStream f = new FileInputStream(Environment.getExternalStorageDirectory() + LBPicPath);
                            Bitmap bm = null;
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 1;//图片的长宽都是原来的1/8
                            BufferedInputStream bis = new BufferedInputStream(f);
                            bm = BitmapFactory.decodeStream(bis, null, options);
                            float scale = bm.getWidth() * 1.0f / bm.getHeight() * 1.0f;
                            float height = picWidth / scale;
                            RelativeLayout.LayoutParams rl = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) height);
                            rl.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                            iv_lbpicpath.setLayoutParams(rl);
                            iv_lbpicpath.setImageBitmap(bm);
                        } catch (Exception e) {
                            // do nothing
                        }
                    }
                    break;
                }
                case FILEDOWNERROR: {
                    // 下载失败，删除本地文件
                    if (!TextUtils.isEmpty(taskDate.pdfPath)) {
                        File file = new File(Environment.getExternalStorageDirectory() + taskDate.pdfPath);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    tv_progress_desc.setText("远程文件(" + taskDate.pdfPath + ")下载失败，请重新尝试...");
                    break;
                }
                case NONETWORK: {
                    tv_progress_desc.setText("网络出现异常，请检查网络链接...");
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
                    tv_progress_desc.setText("文件下载出现异常.");
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
        if (contentObserver != null) {
            getContentResolver().unregisterContentObserver(contentObserver);
        }
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


    public void requestUpdate() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Host.UpdateHost)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        EwiService service = retrofit.create(EwiService.class);
        Call<UpdaeResBody> updaeResBodyCall = service.updateVersion("Srv", "Base.svc", "CheckUpdate", "MubeaEWI", String.valueOf(getVersionCode(MainActivity.this)));
        updaeResBodyCall.enqueue(new Callback<UpdaeResBody>() {
            @Override
            public void onResponse(Call<UpdaeResBody> call, Response<UpdaeResBody> response) {
                if (response != null && response.body() != null && response.body().d != null && response.body().d.Data != null) {
                    Update update = response.body().d.Data;
                    if (update != null && !TextUtils.isEmpty(update.FileUrl) && !TextUtils.isEmpty(update.Ver) && Integer.parseInt(update.Ver) > getVersionCode(MainActivity.this)) {
                        // 下载
                        Toast.makeText(MainActivity.this, "Downloding EWI.apk ... ", Toast.LENGTH_SHORT).show();
                        if (isDownloading) {
                            return;
                        }
                        downloadApk(Host.UpdateHost + response.body().d.Data.FileUrl);
                    }
                }
                requestHandler.sendEmptyMessageDelayed(UPDATEFLAG, Host.UPDATELOOPER * 1000);

            }

            @Override
            public void onFailure(Call<UpdaeResBody> call, Throwable throwable) {
                Toast.makeText(MainActivity.this, "更新接口异常，请联系维护人员 ！", Toast.LENGTH_SHORT).show();
                requestHandler.sendEmptyMessageDelayed(UPDATEFLAG, Host.UPDATELOOPER * 1000);
            }
        });
    }

    private void downloadApk(String apkUrl) {
        isDownloading = true;
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
                                    int fileSize = my.getInt(my
                                            .getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                                    int bytesDL = my.getInt(my
                                            .getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                                    int percent = bytesDL * 100 / fileSize;

                                    if (percent == 100) {
                                        isDownloading = false;
                                    }
                                }
                                my.close();
                            } else {
                                isDownloading = false;
                            }
                        }
                    });
        } catch (Exception e) {
            isDownloading = false;
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