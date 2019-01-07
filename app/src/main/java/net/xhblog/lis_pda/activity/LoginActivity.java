package net.xhblog.lis_pda.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import net.xhblog.lis_pda.R;
import net.xhblog.lis_pda.utils.ConnectionTools;
import net.xhblog.lis_pda.utils.EditTextClearTools;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    //登录请求的url
    private static final String URL = "http://192.168.50.198:8080/Lis_Pda/LoginServlet";
    // 退出时间
    private long currentBackPressedTime = 0;
    // 退出间隔
    private static final int BACK_PRESSED_INTERVAL = 2000;

    private EditText userName;
    private EditText password;
    private ImageView unameClear;
    private ImageView pwdClear;
    private Button login_btn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_activty);
        init();

        //点击事件,子线程请求网络
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
                            Toast.makeText(getApplicationContext(), (String) resultmap.get("username") + ": 登录成功", Toast.LENGTH_LONG).show();
                            Looper.loop();
                            LoginActivity.this.finish();
                        }
                    }
                });
            }
        });
    }

    //初始化控件
    private void init(){
        userName = (EditText) findViewById(R.id.et_userName);
        password = (EditText) findViewById(R.id.et_password);
        unameClear = (ImageView) findViewById(R.id.iv_unameClear);
        pwdClear = (ImageView) findViewById(R.id.iv_pwdClear);
        login_btn = (Button) findViewById(R.id.btn_login);

        EditTextClearTools.addClearListener(userName, unameClear);
        EditTextClearTools.addClearListener(password, pwdClear);
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


    private void userLogin(final String username, final String password, final ICallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ConnectionTools ct = new ConnectionTools(URL,
                            "&username=" + URLEncoder.encode(username, "utf-8") +
                            "&password=" + URLEncoder.encode(password, "utf-8"));
                    String respResult = ct.getDataFromServer();

                    JSONObject json = new JSONObject(respResult);

                    Map<String, Object> results = new HashMap<>();
                    results.put("id", json.optInt("id"));
                    results.put("code", json.optInt("code"));
                    results.put("username", json.optString("username"));
                    results.put("userno", json.optInt("userno"));
                    callback.callback(results);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
