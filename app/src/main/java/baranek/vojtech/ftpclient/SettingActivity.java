package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.w3c.dom.Text;

import baranek.vojtech.ftpclient.api.Host;

/**
 * Created by Frank on 16/4/8.
 */
public class SettingActivity extends Activity implements View.OnClickListener {

    private EditText et_request_time;
    private EditText et_ftp_host;
    private EditText et_ftp_host_iptables;
    private EditText et_host;
    private EditText et_update_host;

    private Button activity_setting;

    private View.OnKeyListener onKeyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        activity_setting = (Button) findViewById(R.id.btn_setting);
        activity_setting.setOnClickListener(this);

        onKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    return true;
                }
                return false;
            }
        };
        et_update_host = (EditText) findViewById(R.id.et_update_host);
        et_host = (EditText) findViewById(R.id.et_host);
        et_ftp_host = (EditText) findViewById(R.id.et_ftp_host);
        et_ftp_host_iptables = (EditText) findViewById(R.id.et_ftp_host_iptables);
        et_request_time = (EditText) findViewById(R.id.et_request_time);

        et_update_host.setOnKeyListener(onKeyListener);
        et_host.setOnKeyListener(onKeyListener);
        et_ftp_host.setOnKeyListener(onKeyListener);
        et_ftp_host_iptables.setOnKeyListener(onKeyListener);
        et_request_time.setOnKeyListener(onKeyListener);

        String updateHost = getSP(Host.UPDATEKEYHOST);
        if (!TextUtils.isEmpty(updateHost) && updateHost.startsWith("http://") && updateHost.startsWith("/")) {
            et_update_host.setText(updateHost);
        } else {
            et_update_host.setText(String.valueOf(Host.UpdateHost));
        }

        String host = getSP(Host.KWYHOST);
        if (!TextUtils.isEmpty(host) && host.startsWith("http://") && host.endsWith("/")) {
            et_host.setText(host);
        } else {
            et_host.setText(Host.HOST);
        }

        String ftphost = getSP(Host.KWYFTPHOST);
        if (!TextUtils.isEmpty(ftphost) && ftphost.split(".").length == 4) {
            et_ftp_host.setText(ftphost);
        } else {
            et_ftp_host.setText(Host.FTPHOST);

        }

        String ftphostiptables = getSP(Host.KWYFTPHOSTIPTABLES);
        if (TextUtils.isEmpty(ftphostiptables)) {
            et_ftp_host_iptables.setText(String.valueOf(Host.FTPHOSTIPTABLES));
        } else {
            et_ftp_host_iptables.setText(ftphostiptables);
        }

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_setting:
                initSetting();
                break;
        }
    }

    private void savaSP(String key, String value) {
        //实例化SharedPreferences对象（第一步）
        SharedPreferences mySharedPreferences = getSharedPreferences("test",
                Activity.MODE_PRIVATE);
        //实例化SharedPreferences.Editor对象（第二步）
        SharedPreferences.Editor editor = mySharedPreferences.edit();
        //用putString的方法保存数据
        editor.putString(key, value);
        //提交当前数据
        editor.commit();
    }

    private String getSP(String key) {
        //同样，在读取SharedPreferences数据前要实例化出一个SharedPreferences对象
        SharedPreferences sharedPreferences = getSharedPreferences("test",
                Activity.MODE_PRIVATE);
        // 使用getString方法获得value，注意第2个参数是value的默认值
        String value = sharedPreferences.getString(key, "");
        return value;
    }

    private void initSetting() {
        String host = et_host.getText().toString();
        String ftphost = et_ftp_host.getText().toString();
        String ftphostiptables = et_ftp_host_iptables.getText().toString();
        String updateHost = et_update_host.getText().toString().replace(" ", "");
        String request_time = et_request_time.getText().toString();

        if (!TextUtils.isEmpty(updateHost) && updateHost.startsWith("http://") && updateHost.endsWith("/")) {
            Host.UpdateHost = updateHost;
            savaSP(Host.UPDATEKEYHOST, updateHost);
        } else {
            Toast.makeText(this, "更新地址：http://IP地址:端口/", Toast.LENGTH_LONG).show();
            return;
        }

        if (!TextUtils.isEmpty(host) && host.startsWith("http://") && host.endsWith("/")) {
            // do  something
            Host.HOST = host;
            savaSP(Host.KWYHOST, host);
        } else {
            Toast.makeText(this, "业务地址：http://IP地址:端口/", Toast.LENGTH_LONG).show();
            return;
        }

        if (!TextUtils.isEmpty(ftphost) && ftphost.split(".").length == 4) {
            Host.FTPHOST = ftphost;
            savaSP(Host.KWYFTPHOST, ftphost);
        } else {
            Toast.makeText(this, "FTP IP地址： XX.XX.XX.XX ", Toast.LENGTH_LONG).show();
            return;
        }

        if (!TextUtils.isEmpty(ftphostiptables)) {
            int ftphostIp = Host.FTPHOSTIPTABLES;
            try {
                ftphostIp = Integer.parseInt(ftphostiptables);
            } catch (Exception e) {
                Toast.makeText(this, "FTP 端口号： XXXX 为数字", Toast.LENGTH_LONG).show();
            }
            Host.FTPHOSTIPTABLES = ftphostIp;
            savaSP(Host.KWYFTPHOSTIPTABLES, ftphostiptables);
        } else {
            Toast.makeText(this, "FTP 端口号： XXXX 为数字", Toast.LENGTH_LONG).show();
            return;
        }

        int time = 10;
        if (!TextUtils.isEmpty(request_time)) {
            try {
                time = Integer.parseInt(request_time);
                if (time < 5) {
                    Toast.makeText(SettingActivity.this, "时间间隔必须大于5秒", Toast.LENGTH_LONG).show();
                    return;
                }
            } catch (Exception e) {
                time = 10;
            }
        }
        Host.TENLOOPER = time;

        this.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
