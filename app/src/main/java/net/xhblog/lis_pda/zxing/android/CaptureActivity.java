package net.xhblog.lis_pda.zxing.android;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.LinearLayoutCompat;
import android.util.Log;
import android.view.*;
import android.widget.TextView;
import android.widget.Toast;
import com.google.zxing.Result;
import net.xhblog.lis_pda.R;
import net.xhblog.lis_pda.activity.CollectActivity;
import net.xhblog.lis_pda.activity.TransActivity;
import net.xhblog.lis_pda.zxing.bean.ZxingConfig;
import net.xhblog.lis_pda.zxing.camera.CameraManager;
import net.xhblog.lis_pda.zxing.common.Constant;
import net.xhblog.lis_pda.zxing.decode.DecodeImgCallback;
import net.xhblog.lis_pda.zxing.decode.DecodeImgThread;
import net.xhblog.lis_pda.zxing.decode.ImageUtil;
import net.xhblog.lis_pda.zxing.view.ViewfinderView;

import java.io.IOException;


public class CaptureActivity extends AppCompatActivity implements SurfaceHolder.Callback, View.OnClickListener {

    public static final String ACTION = "net.xhblog.lis_pda.zxing.android.intent.action.CaptureActivity";
    private static final String TAG = CaptureActivity.class.getSimpleName();
    public ZxingConfig config;
    private SurfaceView previewView;
    private ViewfinderView viewfinderView;
    private AppCompatImageView flashLightIv;
    private TextView flashLightTv;
    private AppCompatImageView backIv;
    private LinearLayoutCompat flashLightLayout;
    private LinearLayoutCompat albumLayout;
    private LinearLayoutCompat bottomLayout;
    private boolean hasSurface;
    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private SurfaceHolder surfaceHolder;


    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }


    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 保持Activity处于唤醒状态
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setStatusBarColor(Color.BLACK);
        }

        /*先获取配置信息*/
        try {
            config = (ZxingConfig) getIntent().getExtras().get(Constant.INTENT_ZXING_CONFIG);
        } catch (Exception e) {

            Log.i("config", e.toString());
        }

        if (config == null) {
            config = new ZxingConfig();
        }


        setContentView(R.layout.activity_capture);

        //requestPermission();
        initView();

        hasSurface = false;

        inactivityTimer = new InactivityTimer(this);
        beepManager = new BeepManager(this);
        beepManager.setPlayBeep(config.isPlayBeep());
        beepManager.setVibrate(config.isShake());

        /**
         * 注册广播接收器
         */
        IntentFilter collect_filter = new IntentFilter(CollectActivity.ACTION);
        registerReceiver(broadcastReceiver, collect_filter);

        IntentFilter trans_filter = new IntentFilter(TransActivity.ACTION);
        registerReceiver(broadcastReceiver, trans_filter);
    }


    private void initView() {
        previewView = (SurfaceView) findViewById(R.id.preview_view);
        previewView.setOnClickListener(this);

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);
        viewfinderView.setZxingConfig(config);


        backIv = (AppCompatImageView) findViewById(R.id.backIv);
        backIv.setOnClickListener(this);

        flashLightIv = (AppCompatImageView) findViewById(R.id.flashLightIv);
        flashLightTv = (TextView) findViewById(R.id.flashLightTv);

        flashLightLayout = (LinearLayoutCompat) findViewById(R.id.flashLightLayout);
        flashLightLayout.setOnClickListener(this);
        albumLayout = (LinearLayoutCompat) findViewById(R.id.albumLayout);
        albumLayout.setOnClickListener(this);
        bottomLayout = (LinearLayoutCompat) findViewById(R.id.bottomLayout);


        switchVisibility(bottomLayout, config.isShowbottomLayout());
//        switchVisibility(flashLightLayout, config.isShowFlashLight());
//        switchVisibility(albumLayout, config.isShowAlbum());


        /*有闪光灯就显示手电筒按钮  否则不显示*/
//        if (isSupportCameraLedFlash(getPackageManager())) {
//            flashLightLayout.setVisibility(View.VISIBLE);
//        } else {
//            flashLightLayout.setVisibility(View.GONE);
//        }

        //不显示手电按钮和相册按钮
        flashLightLayout.setVisibility(View.GONE);
        albumLayout.setVisibility(View.GONE);
    }


    /**
     * @return 是否有闪光灯
     */
    public static boolean isSupportCameraLedFlash(PackageManager pm) {
        if (pm != null) {
            FeatureInfo[] features = pm.getSystemAvailableFeatures();
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f != null && PackageManager.FEATURE_CAMERA_FLASH.equals(f.name)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * @param flashState 切换闪光灯图片
     */
    public void switchFlashImg(int flashState) {

        if (flashState == Constant.FLASH_OPEN) {
            flashLightIv.setImageResource(R.drawable.ic_open);
            flashLightTv.setText("关闭闪光灯");
        } else {
            flashLightIv.setImageResource(R.drawable.ic_close);
            flashLightTv.setText("打开闪光灯");
        }

    }

    /**
     * @param rawResult 返回的扫描结果
     */
    public void handleDecode(Result rawResult) {

        inactivityTimer.onActivity();

        beepManager.playBeepSoundAndVibrate();

        Intent intent = getIntent();
        intent.putExtra(Constant.CODED_CONTENT, rawResult.getText());
        setResult(RESULT_OK, intent);

        //发送广播,传递当前扫描的条码号
        Intent intent1 = new Intent(ACTION);
        intent1.putExtra("current_barcode", rawResult.getText());
        sendBroadcast(intent1);
        //this.finish();

        //实现连续扫描
        new Handler().postDelayed(new Runnable() {
            public void run() {
                SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);

                SurfaceHolder surfaceHolder = surfaceView.getHolder();

                initCamera(surfaceHolder);

                if (handler != null)

                    handler.restartPreviewAndDecode();
            }

        }, 3000);

    }


    private void switchVisibility(View view, boolean b) {
        if (b) {
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        cameraManager = new CameraManager(getApplication(), config);

        viewfinderView.setCameraManager(cameraManager);
        handler = null;

        surfaceHolder = previewView.getHolder();
        if (hasSurface) {

            initCamera(surfaceHolder);
        } else {
            // 重置callback，等待surfaceCreated()来初始化camera
            surfaceHolder.addCallback(this);
        }

        beepManager.updatePrefs();
        inactivityTimer.onResume();

    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            return;
        }
        try {
            // 打开Camera硬件设备
            cameraManager.openDriver(surfaceHolder);
            // 创建一个handler来打开预览，并抛出一个运行时异常
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager);
            }
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            //displayFrameworkBugMessageAndExit();
        } catch (RuntimeException e) {
            Log.w(TAG, "Unexpected error initializing camera", e);
            //displayFrameworkBugMessageAndExit();
        }
    }

    private void displayFrameworkBugMessageAndExit() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("条码扫描");
        builder.setMessage(getString(R.string.msg_camera_framework_bug));
        builder.setPositiveButton(R.string.button_ok, new FinishListener(this));
        builder.setOnCancelListener(new FinishListener(this));
        builder.show();
    }

    @Override
    protected void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();

        if (!hasSurface) {

            surfaceHolder.removeCallback(this);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        inactivityTimer.shutdown();
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (!hasSurface) {
            hasSurface = true;
            initCamera(holder);
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        hasSurface = false;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void onClick(View view) {

        int id = view.getId();
        if (id == R.id.flashLightLayout) {
            /*切换闪光灯*/
            cameraManager.switchFlashLight(handler);
        } else if (id == R.id.albumLayout) {
            /*打开相册*/
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, Constant.REQUEST_IMAGE);
        } else if (id == R.id.backIv) {
            finish();
        }


    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constant.REQUEST_IMAGE && resultCode == RESULT_OK) {
            String path = ImageUtil.getImageAbsolutePath(this, data.getData());

            new DecodeImgThread(path, new DecodeImgCallback() {
                @Override
                public void onImageDecodeSuccess(Result result) {
                    handleDecode(result);
                }

                @Override
                public void onImageDecodeFailed() {
                    Toast.makeText(CaptureActivity.this, "抱歉，解析失败,换个图片试试.", Toast.LENGTH_SHORT).show();
                }
            }).run();


        }
    }


    /**
     * 广播接收器 接收从CollectActivity发来的条码处理的结果
     * 约定
     * 1 --- 为设置采样成功
     * 2 --- 为设置送检成功
     * -1 --- 为未找到条码
     * -2 --- 为传入给服务器的条码号有误,需联系管理员检查
     * -3 --- 为未设置采样信息送检
     * -4 --- 为服务器端发生错误,需联系管理员检查
     * -6 --- 为采样信息已经设置,此时需要提示是否重新设置
     * -8 --- 为送检信息已经设置,此时需要提示是否重新设置
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(null != intent) {
                String code = intent.getStringExtra("code");
                String barcode = intent.getStringExtra("barcode");
                switch (code) {
                    case "1" :
                        Toast.makeText(getApplicationContext(), "条码:" +
                                barcode + "采样成功", Toast.LENGTH_LONG).show();
                        break;
                    case "2" :
                        Toast.makeText(getApplicationContext(), "条码:" +
                                barcode + "送检成功", Toast.LENGTH_LONG).show();
                        break;
                    case "-1" :
                        Toast.makeText(getApplicationContext(), "条码:" +
                                barcode + "不存在", Toast.LENGTH_LONG).show();
                        break;
                    case "-2" :
                        break;
                    case "-3" :
                        Toast.makeText(getApplicationContext(), "条码:" +
                                barcode + "未设置采样信息,不能送检", Toast.LENGTH_LONG).show();
                        break;
                    case "-4" :
                        break;
                    case "-6" :
                        Toast.makeText(getApplicationContext(), "条码:" +
                                barcode + "已采样,不允许重复采样", Toast.LENGTH_LONG).show();
                        break;
                    case "-8" :
                        Toast.makeText(getApplicationContext(), "条码:" +
                                barcode + "已送检,不允许重复送检", Toast.LENGTH_LONG).show();
                        break;
                }

            }

        }
    };
}
