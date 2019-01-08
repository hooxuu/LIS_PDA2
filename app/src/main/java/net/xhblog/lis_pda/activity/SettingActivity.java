package net.xhblog.lis_pda.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import net.xhblog.lis_pda.R;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SettingActivity extends AppCompatActivity implements View.OnClickListener {
    private String FILENAME = "connect.properties";

    private AppCompatImageView backbtn;
    private Button savebtn;
    private EditText serverip;
    private EditText serverport;
    private EditText serverproject;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        init();
        backbtn.setOnClickListener(SettingActivity.this);
        savebtn.setOnClickListener(SettingActivity.this);
        getPropertyFromFile();
    }

    private void init() {
        backbtn = (AppCompatImageView) findViewById(R.id.btn_setting_back);
        savebtn = (Button) findViewById(R.id.btn_setting_save);
        serverip = (EditText) findViewById(R.id.ip);
        serverport = (EditText) findViewById(R.id.port);
        serverproject = (EditText) findViewById(R.id.pname);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_setting_back :
                SettingActivity.this.finish();
                break;
            case R.id.btn_setting_save :
                saveConfig2File();
                break;
        }
    }

    private void saveConfig2File() {
        try {
            String ip = serverip.getText().toString();
            String port = serverport.getText().toString();
            String project = serverproject.getText().toString();

            if(ip.isEmpty() || port.isEmpty() || project.isEmpty()) {
                Toast.makeText(getApplicationContext(), "请填写正确的连接参数再保存", Toast.LENGTH_LONG).show();
                return;
            }

            //拼接connect.properties内容
            String fileContent = "ip=" + ip + "\n" + "port=" + port + "\n" + "projectname=" + project;

            FileOutputStream fos = openFileOutput(FILENAME, Context.MODE_PRIVATE);
            fos.write(fileContent.getBytes());
            fos.flush();
            fos.close();

            Toast.makeText(getApplicationContext(), "保存成功", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.w("error", e.getLocalizedMessage());
        }
    }

    private void getPropertyFromFile() {
        InputStream in = null;
        try {
            Properties properties = new Properties();
            in = openFileInput("connect.properties");
            properties.load(in);

            serverip.setText(properties.getProperty("ip", ""));
            serverport.setText(properties.getProperty("port",""));
            serverproject.setText(properties.getProperty("projectname",""));
        } catch (IOException e) {
            Log.w("IOerror", e.getLocalizedMessage());
        } finally {
            if(null != in) {
                try {
                    in.close();
                } catch (IOException ie) {
                    Log.w("IOerror", ie.getLocalizedMessage());
                }
            }
        }

    }
}
