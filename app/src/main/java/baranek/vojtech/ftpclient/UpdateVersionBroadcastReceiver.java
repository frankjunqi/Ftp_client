package baranek.vojtech.ftpclient;

/**
 * Created by kjh08490 on 2016/10/11.
 */

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.widget.Toast;

/**
 * 监听系统DownloadManager下载完成的广播，然后进行引导安装
 */
public class UpdateVersionBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {// 下载完成
            long re = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(re);
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            Cursor my = downloadManager.query(query);
            if (my != null) {
                if (my.moveToFirst()) {
                    String fileUri = my.getString(my.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    String filePath = my.getString(my.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME));
                    install(context, fileUri, filePath);
                }
                my.close();
            }
        } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED.equals(action)) {
//            Intent appIntent = new Intent(context, LoadingActivity.class);
//            // 关键的一步，设置启动模式
//            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
//            context.startActivity(appIntent);

            Intent mIntent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
            mIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (mIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(mIntent);
            }
        }
    }


    /**
     * 安装最新版本
     */
    private void install(Context context, String url, String filePath) {
        if (!TextUtils.isEmpty(url)) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.setDataAndType(Uri.parse(url), "application/vnd.android.package-archive");
            context.startActivity(i);
        } else {
            Toast.makeText(context, "安装文件不存在", Toast.LENGTH_LONG).show();
        }
    }
}
