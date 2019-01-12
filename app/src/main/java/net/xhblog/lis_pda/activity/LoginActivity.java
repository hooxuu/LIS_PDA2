package net.xhblog.lis_pda.activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.*;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import net.xhblog.lis_pda.R;
import net.xhblog.lis_pda.entity.User;
import net.xhblog.lis_pda.utils.*;
import net.xhblog.lis_pda.view.CommonProgressDialog;
import org.json.JSONException;
import org.json.JSONObject;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks {
    // 下载存储的文件名
    private static final String DOWNLOAD_NAME = "tmsk_pda.apk";
    private EditText userName;
    private EditText password;
    private ImageView unameClear;
    private ImageView pwdClear;
    private Button login_btn;
    private ImageButton settingbtn;
    private CheckBox saveinfo;
    private CheckBox autologin;
    private SharedPreferences.Editor editor;
    private CommonProgressDialog pBar;
    // 退出时间
    private long currentBackPressedTime = 0;
    // 退出间隔
    private static final int BACK_PRESSED_INTERVAL = 2000;
    private SharedPreferences preferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_activty);

        init();
        firstTimeUseApp();
        settingbtn.setOnClickListener(LoginActivity.this);
        login_btn.setOnClickListener(LoginActivity.this);
        autologin.setOnClickListener(LoginActivity.this);
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
        saveinfo = (CheckBox) findViewById(R.id.cb_checkbox);
        autologin = (CheckBox) findViewById(R.id.autologin_checkbox);
        EditTextClearTools.addClearListener(userName, unameClear);
        EditTextClearTools.addClearListener(password, pwdClear);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1500);
                    //检测更新
                    int versioncode = VersionSDUtils.getLocalVersion(LoginActivity.this);
                    getNewVersion(versioncode);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();

        //关于自动登录和记住账户密码的操作
        preferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
        String shortcode = preferences.getString("shortcode", "");
        String psw = preferences.getString("password", "");

        boolean choseRemember = preferences.getBoolean("remember", false);
        boolean choseAutoLogin = preferences.getBoolean("autologin", false);

        if(choseRemember) {
            userName.setText(shortcode);
            password.setText(psw);
            saveinfo.setChecked(true);
        }

        if(choseAutoLogin){
            autologin.setChecked(true);
            autoLogin();
        }
    }



    //检测新版本
    private void getNewVersion(int versioncode) {
        //发生异常时检测网络用
        String ip = "";
        try {
            ConnectionTools tools = new ConnectionTools(getApplicationContext());
            ip = tools.getConfigByPropertyName("ip");
            String port = tools.getConfigByPropertyName("port");
            String projectname = tools.getConfigByPropertyName("projectname");

            String respResult = tools.getDataFromServer(
                    "http://" + ip + ":" + port + "/" + projectname + "/GetLastVersionServlet",
                    "version=" + versioncode);
            JSONObject object = new JSONObject(respResult);
            if(object.getInt("code") == 0) {
                String version_name = object.getString("version_name");
                String update_description = object.getString("ModifyContent");
                String apk_path = object.getString("DownloadUrl");
                String serverFileMD5 = object.getString("ApkMd5");
                ShowDialog(versioncode, version_name, update_description, apk_path, serverFileMD5);
            }

        } catch (JSONException e1) {
            Log.w("JSONException", e1.getLocalizedMessage());
            //检测网络
            boolean isNetworkOk = NetworkUtils.isAvailableByPing(ip);
            if(! isNetworkOk) {
                Looper.prepare();
                Toast.makeText(getApplicationContext(), "当前与服务器连接不通,请检查网络或连接配置", Toast.LENGTH_LONG).show();
                Looper.loop();
            } else {
                Toast.makeText(getApplicationContext(), "返回更新数据出错,请联系管理员", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void ShowDialog(int version, String newversion, String content, final String url, final String serverFileMD5) {
        Looper.prepare();
        new android.app.AlertDialog.Builder(this)
                .setTitle("有新版本")
                .setMessage("更新内容: " + content)
                .setPositiveButton("更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        pBar = new CommonProgressDialog(LoginActivity.this);
                        //pBar.setCanceledOnTouchOutside(false);
                        //pBar.setTitle("正在下载");
                        pBar.setCustomTitle(LayoutInflater.from(
                                LoginActivity.this).inflate(
                                R.layout.title_dialog, null));
                        pBar.setMessage("正在下载");
                        pBar.setIndeterminate(true);
                        pBar.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        pBar.setCancelable(false);
                        // downFile(URLData.DOWNLOAD_URL);
                        final DownloadTask downloadTask = new DownloadTask(
                                LoginActivity.this, serverFileMD5);
                        //downloadTask.execute(url);
                        LinkedBlockingQueue<Runnable> blockingQueue = new LinkedBlockingQueue<Runnable>();
                        ExecutorService exec = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, blockingQueue);
                        downloadTask.executeOnExecutor(exec, url);
                        pBar.setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
//                                downloadTask.cancel(true);
                            }
                        });
                    }
                })
//                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.dismiss();
//                    }
//                })
                .setCancelable(false)
                .show();
        Looper.loop();
    }


    class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;
        private File downLoadFile;
        private String serverFileMD5;

        public DownloadTask(Context context, String serverFileMD5) {
            this.context = context;
            this.serverFileMD5 = serverFileMD5;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                // expect HTTP 200 OK, so we don't mistakenly save error
                // report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP "
                            + connection.getResponseCode() + " "
                            + connection.getResponseMessage();
                }
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                downLoadFile = new File(Environment.getExternalStorageDirectory(), DOWNLOAD_NAME);

                if (!downLoadFile.exists()) {
                    // 判断父文件夹是否存在
                    if (!downLoadFile.getParentFile().exists()) {
                        downLoadFile.getParentFile().mkdirs();
                    }
                }

                input = connection.getInputStream();
                output = new FileOutputStream(downLoadFile);
                byte data[] = new byte[1024];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // 进度条
                    if (fileLength > 0) {
                        publishProgress((int) ((total / (double) fileLength) * 100));
                    }
                    output.write(data, 0, count);
                    if(total % 4096 == 0) {
                        Thread.sleep(2);
                    }
                }
                output.flush();
            } catch (Exception e) {
                Log.w("get file error", e.getLocalizedMessage());

            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context
                    .getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            pBar.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            //super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            pBar.setIndeterminate(false);
            pBar.setMax(100);
            pBar.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            mWakeLock.release();
            pBar.dismiss();
            String downloadFileMD5 = FileUtils.getFileMD5(downLoadFile);
            if(serverFileMD5.equals(downloadFileMD5)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(1000);
                            install();
                        } catch (InterruptedException e) {
                            Log.w("InterruptedException", e.toString());
                        }
                    }
                }).start();
            } else {
                Toast.makeText(LoginActivity.this.getApplicationContext(),
                        "更新的安装包不完整或被篡改,请联系管理员", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void install() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File apkFile = new File(Environment.getExternalStorageDirectory(), DOWNLOAD_NAME);
        //Android 7.0 系统共享文件需要通过 FileProvider 添加临时权限，否则系统会抛出 FileUriExposedException .
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.N){
            Uri contentUri = FileProvider.getUriForFile(LoginActivity.this,
                    "net.xhblog.lis_pda.fileprovider", apkFile);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
        }else {
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(apkFile),"application/vnd.android.package-archive");
        }
        LoginActivity.this.startActivity(intent);
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
            case R.id.autologin_checkbox :
                if(autologin.isChecked()) {
                    saveinfo.setChecked(true);
                }
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
                try {
                    ConnectionTools tools = new ConnectionTools(getApplicationContext());
                    String ip = tools.getConfigByPropertyName("ip");
                    String port = tools.getConfigByPropertyName("port");
                    String projectname = tools.getConfigByPropertyName("projectname");

                    String respResult = tools.getDataFromServer(
                            "http://" + ip + ":" + port + "/" + projectname + "/LoginServlet",
                            "username=" + URLEncoder.encode(username, "utf-8") + "&password=" +
                                    URLEncoder.encode(password, "utf-8"));

                    JSONObject json = new JSONObject(respResult);
                    Map<String, Object> results = new HashMap<>();
                    Iterator it = json.keys();
                    while (it.hasNext()) {
                        String key = String.valueOf(it.next());
                        Object value = json.get(key);
                        results.put(key, value);
                    }
                    callback.callback(results);
                }  catch (Exception e2) {
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

                    if(saveinfo.isChecked()) {
                        //SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                        editor = preferences.edit();
                        editor.putString("password", (String) resultmap.get("password"));
                        editor.putString("shortcode", (String) resultmap.get("shortCode"));
                        editor.putBoolean("remember", true);
                    } else {
                        editor = preferences.edit();
                        editor.clear();
                    }

                    if(autologin.isChecked()) {
                        editor.putBoolean("autologin", true);
                    } else {
                        editor.putBoolean("autologin", false);
                    }
                    editor.apply();

                    User loginuser = new User();
                    loginuser.setUserno((int) resultmap.get("userno"));
                    loginuser.setCname((String) resultmap.get("CName"));
                    loginuser.setShortcode((String) resultmap.get("shortCode"));
                    intent.putExtra("loginuser", loginuser);

                    startActivity(intent);
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), loginuser.getCname() +
                            ": 登录成功", Toast.LENGTH_LONG).show();
                    Looper.loop();
                }
            }
        });
    }

    private void autoLogin() {
        String shortcode = preferences.getString("shortcode", "");
        String password = preferences.getString("password", "");
        userLogin(shortcode, password, new ICallback() {
            @Override
            public void callback(Map<String, Object> resultmap) {
                int code = (int) resultmap.get("code");
                if(-1 == code) {
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), "自动登录失败", Toast.LENGTH_LONG).show();
                    Looper.loop();
                } else if (1 == code) {
                    Intent intent = new Intent();
                    intent.setClass(getApplicationContext(), MainPageActivity.class);
                    User loginuser = new User();
                    loginuser.setUserno((int) resultmap.get("userno"));
                    loginuser.setCname((String) resultmap.get("CName"));
                    loginuser.setShortcode((String) resultmap.get("shortCode"));
                    intent.putExtra("loginuser", loginuser);
                    startActivity(intent);
                    Looper.prepare();
                    Toast.makeText(getApplicationContext(), "自动登录成功", Toast.LENGTH_LONG).show();
                    Looper.loop();
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
