package baranek.vojtech.ftpclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by Frank on 16/4/23.
 */
public class SecretActivity extends Activity implements View.OnClickListener {


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
                break;
        }
    }
}
