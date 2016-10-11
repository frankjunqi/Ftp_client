package baranek.vojtech.ftpclient;

/**
 * Created by kjh08490 on 2016/10/11.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import java.io.File;

/**
 * Created by kjh08490 on 2015/7/25.
 * 针对内部下载的action进行安装的广播，而非系统下载器的完成的广播的接收者只在当前进程中接收action
 */
public class TCTInsatllActionBroadcastReceiver extends BroadcastReceiver {


    public static final String INSTALL_APK_ACTION = "baranek.vojtech.ftpclient.install";

    // 安装文件的路径
    public static final String FILE_PATH = "filePath";

    // 更新的apk的文件名称
    public static final String APK_FILE_NAME = "EWI.apk";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (INSTALL_APK_ACTION.equals(action)) {
            String filePath = intent.getStringExtra(FILE_PATH);
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    install(context, file, filePath + File.separator + APK_FILE_NAME);
                } else {
                    Toast.makeText(context, "下载文件已不存在", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, "下载文件已不存在", Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * 安装最新版本
     */
    private void install(Context context, File file, String filePath) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            context.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(context, "安装文件出错", Toast.LENGTH_LONG).show();
        }

    }
}