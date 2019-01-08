package net.xhblog.lis_pda.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.*;
import net.xhblog.lis_pda.R;
import net.xhblog.lis_pda.utils.ConnectionTools;
import net.xhblog.lis_pda.utils.EditTextClearTools;
import net.xhblog.lis_pda.utils.NetworkUtils;
import org.json.JSONException;
import org.json.JSONObject;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks {
    private static final int CAMERA_REQ_CODE = 111;
    // 退出时间
    private long currentBackPressedTime = 0;
    // 退出间隔
    private static final int BACK_PRESSED_INTERVAL = 2000;

    private EditText userName;
    private EditText password;
    private ImageView unameClear;
    private ImageView pwdClear;
    private Button login_btn;
    private ImageButton settingbtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_activty);

        init();
        firstTimeUseApp();
        settingbtn.setOnClickListener(LoginActivity.this);
        login_btn.setOnClickListener(LoginActivity.this);
        getPermission();
    }

    //初始化控件
    private void init(){
        userName = (EditText) findViewById(R.id.et_userName);
        password = (EditText) findViewById(R.id.et_password);
        unameClear = (ImageView) findViewById(R.id.iv_unameClear);
        pwdClear = (ImageView) findViewById(R.id.iv_pwdClear);
        login_btn = (Button) findViewById(R.id.btn_login);
        settingbtn = (ImageButton) findViewById(R.id.settingbtn);

        EditTextClearTools.addClearListener(userName, unameClear);
        EditTextClearTools.addClearListener(password, pwdClear);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login :
                userLoginCheck();
                break;
            case R.id.settingbtn :
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), SettingActivity.class);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
        // 判断时间间隔
        if (System.currentTimeMillis() - currentBackPressedTime > BACK_PRESSED_INTERVAL) {
            currentBackPressedTime = System.currentTimeMillis();
            Toast.makeText(this, "再按一次返回键退出程序", Toast.LENGTH_SHORT).show();
        } else {
            // 退出
            finish();
            System.exit(0);
        }
    }

    //判断程序是否第一次启动,第一次启动需要生成数据库连接参数文件
    private void firstTimeUseApp() {
        SharedPreferences sharedPreferences = getSharedPreferences("FirstRun",0);
        boolean first_run = sharedPreferences.getBoolean("First",true);
        if (first_run){
            try {
                sharedPreferences.edit().putBoolean("First",false).commit();

                //拼接connect.properties内容
                String fileContent = "ip=192.168.50.198\n" + "port=8080\n" + "projectname=Lis_Pda";
                FileOutputStream fos = openFileOutput("connect.properties", Context.MODE_PRIVATE);
                fos.write(fileContent.getBytes());
                fos.flush();
                fos.close();
            } catch (Exception e) {
                Log.w("error", e.getLocalizedMessage());
            }
        }
    }


    private void userLogin(final String username, final String password, final ICallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                //发生异常时检测网络用
                String ip = null;

                try {
                    ConnectionTools tools = new ConnectionTools(getApplicationContext());
                    ip = tools.getConfigByPropertyName("ip");
                    String port = tools.getConfigByPropertyName("port");
                    String projectname = tools.getConfigByPropertyName("projectname");

                    String respResult = tools.getDataFromServer(
                            "http://" + ip + ":" + port + "/" + projectname + "/LoginServlet",
                            "&username=" + URLEncoder.encode(username, "utf-8") + "&password=" +
                                    URLEncoder.encode(password, "utf-8"));

                    JSONObject json = new JSONObject(respResult);

                    Map<String, Object> results = new HashMap<>();
                    results.put("id", json.optInt("id"));
                    results.put("code", json.optInt("code"));
                    results.put("username", json.optString("username"));
                    results.put("userno", json.optInt("userno"));
                    callback.callback(results);
                } catch (JSONException e1) {
                    Log.w("JSONException", e1.getLocalizedMessage());
                    //检测网络
                    boolean isNetworkOk = NetworkUtils.isAvailableByPing(ip);
                    if(! isNetworkOk) {
                        Looper.prepare();
                        Toast.makeText(getApplicationContext(), "当前与服务器连接不通,请检查网络", Toast.LENGTH_LONG).show();
                        Looper.loop();
                    }
                } catch (UnsupportedEncodingException e2) {
                    Log.w("encode error", e2.getLocalizedMessage());
                }
            }
        }).start();
    }

    private void userLoginCheck() {
        String usr = userName.getText().toString();
        String psw = password.getText().toString();
        if(usr.trim().isEmpty() || psw.trim().isEmpty()) {
            Toast.makeText(getApplicationContext(), "工号或密码不能为空", Toast.LENGTH_LONG).show();
            return;
        }
        //跳转到首页
        userLogin(usr, psw, new ICallback() {
            @Override
            public void callback(Map<String, Object> resultmap) {
                int loginstatus = (int) resultmap.get("code");
                if(-1 == loginstatus) {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), "用户名或密码不正确", Toast.LENGTH_LONG).show();
                    Looper.loop();
                } else if (1 == loginstatus) {
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), MainPageActivity.class);

                    SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("username", (String) resultmap.get("username"));
                    editor.putString("shortcode", (String) resultmap.get("shortcode"));
                    editor.putInt("userno", (int) resultmap.get("userno"));
                    editor.commit();

                    startActivity(intent);
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), (String) resultmap.get("username") +
                            ": 登录成功", Toast.LENGTH_LONG).show();
                    Looper.loop();
                    LoginActivity.this.finish();
                }
            }
        });
    }

    private void getPermission() {
        //检查是否存在请求的权限
        String[] permissions = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};
        if(EasyPermissions.hasPermissions(LoginActivity.this, permissions)) {

        } else {
            EasyPermissions.requestPermissions(LoginActivity.this,
                    "再次请求应用权限,请允许请求的权限,否则应用功能将不可用", 6, permissions);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        new AppSettingsDialog
                .Builder(this)
                .setRationale("本应用需要相机以及存储的使用权限，否则应用的部分功能将不可用，是否前往设置")
                .setPositiveButton("是")
                .setNegativeButton("否")
                .build()
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }
}
