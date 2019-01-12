package net.xhblog.lis_pda.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import net.xhblog.lis_pda.R;
import net.xhblog.lis_pda.adapter.MyAdapter;
import net.xhblog.lis_pda.entity.Sample;
import net.xhblog.lis_pda.entity.User;
import net.xhblog.lis_pda.utils.ConnectionTools;
import net.xhblog.lis_pda.zxing.android.CaptureActivity;
import net.xhblog.lis_pda.zxing.common.Constant;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.*;

public class TransActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String ACTION = "net.xhblog.lis_pda.intent.action.TransActivity";
    private static final int REQUEST_CODE_SCAN = 1;
    private AppCompatImageView btn_trans_back;
    private ImageButton barcode_scan;
    private TextView barcode_tv;
    private GridView grid_sampleinfo;
    //更新样本信息线程
    private Thread updateTransInfoThread = null;
    //存放样本信息
    private List<Sample> sampleList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactivity);

        initView();
        btn_trans_back.setOnClickListener(this);
        barcode_scan.setOnClickListener(this);

        IntentFilter filter = new IntentFilter(CaptureActivity.ACTION);
        registerReceiver(broadcastReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void initView() {
        btn_trans_back = (AppCompatImageView) findViewById(R.id.btn_trans_back);
        barcode_scan = (ImageButton) findViewById(R.id.barcode_scan);
        barcode_tv = (TextView) findViewById(R.id.tbarcode_content);
        grid_sampleinfo = (GridView) findViewById(R.id.grid_sampleinfo);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.btn_trans_back :
                finish();
                break;
            case R.id.barcode_scan :
                intent.setClass(getApplicationContext(), CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SCAN);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (data != null) {

                String content = data.getStringExtra(Constant.CODED_CONTENT);
                barcode_tv.setText(content);
            }
        }
    }


    //广播接收器 接收实时扫描的条码号 然后发送给服务器进行查询
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                String current_barcode = intent.getStringExtra("current_barcode");
                queryAndUpdateTransInfo(current_barcode, new ICallback() {
                    @Override
                    public void callback(Map<String, Object> resultmap) {
                        if(sampleList.size() > 5) {
                            sampleList.remove(0);
                        }
                        Sample sample = new Sample();
                        sample.setLine1("姓名:" + resultmap.get("cname") + " " + "条码号:" +
                                resultmap.get("serialno"));
                        sample.setLine2("病历号:" + resultmap.get("patno") + " " + "性别:" +
                                resultmap.get("sex") + " " + "科室:" + resultmap.get("dept") + " " + "床号:" +
                                resultmap.get("bed"));
                        sample.setLine3("送检人及时间:" + resultmap.get("nursesender") + "," +
                                resultmap.get("transdatetime"));
                        sample.setItem((String) resultmap.get("item"));
                        sampleList.add(sample);
                    }
                });
                /**
                 * 此处很重要,一定要等更新的那个线程结束并返回结果之后主线程在进行下一步操作,否则在扫描之后界面不显示样本的信息
                 */
                updateTransInfoThread.join();
                if(!sampleList.isEmpty()) {
                    ArrayList<Sample> mData = new ArrayList<>();
                    mData.addAll(sampleList);
                    //反转list,让后送检的显示在上
                    Collections.reverse(mData);

                    BaseAdapter mAdapter = new MyAdapter<Sample>(mData, R.layout.item_grid_sampleinfo) {
                        @Override
                        public void bindView(ViewHolder holder, Sample obj) {
                            holder.setText(R.id.sampleinfo_l1, obj.getLine1());
                            holder.setText(R.id.sampleinfo_l2, obj.getLine2());
                            holder.setText(R.id.sampleinfo_l3, obj.getLine3());
                            holder.setText(R.id.sampleinfo_l4, obj.getItem());
                        }
                    };
                    grid_sampleinfo.setAdapter(mAdapter);
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    };


    /**
     *查询并更新送检信息
     */
    private void queryAndUpdateTransInfo(final String barcode, final ICallback callback) {
        updateTransInfoThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Intent mainintent = getIntent();
                    User loginuser = (User) mainintent.getSerializableExtra("loginuser");

                    ConnectionTools tools = new ConnectionTools(getApplicationContext());
                    String ip = tools.getConfigByPropertyName("ip");
                    String port = tools.getConfigByPropertyName("port");
                    String projectname = tools.getConfigByPropertyName("projectname");
                    String jsonresult = tools.getDataFromServer(
                            "http://" + ip + ":" + port + "/" + projectname + "/UpdateNurseSendDateTimeServlet",
                            "&SerialNo=" + URLEncoder.encode(barcode, "utf-8") +
                                    "&currentUser=" + URLEncoder.encode(loginuser.getCname(), "utf-8"));
                    JSONObject json = new JSONObject(jsonresult);
                    //获取服务器返回的状态code
                    String code = json.optString("code");

                    /**
                     *      * 约定
                     *      * 1 --- 为设置采样成功
                     *      * 2 --- 为设置送检成功
                     *      * -1 --- 为未找到条码
                     *      * -2 --- 为传入给服务器的条码号有误,需联系管理员检查
                     *      * -3 --- 为未设置采样信息送检
                     *      * -4 --- 为服务器端发生错误,需联系管理员检查
                     *      * -6 --- 为采样信息已经设置,此时需要提示是否重新设置
                     *      * -8 --- 为送检信息已经设置,此时需要提示是否重新设置
                     *      */

                    if("1".equals(code) || "2".equals(code)) {
                        Map<String, Object> result_map = saveSampleInfo(json);

                        callback.callback(result_map);

                        Intent intent = new Intent(ACTION);
                        intent.putExtra("code", code);
                        intent.putExtra("barcode", (String) result_map.get("serialno"));
                        sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION);
                        intent.putExtra("code", code);
                        intent.putExtra("barcode", barcode);
                        sendBroadcast(intent);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        updateTransInfoThread.start();
    }

    /**
     * 保存返回的信息到map
     */
    private Map<String, Object> saveSampleInfo(@org.jetbrains.annotations.NotNull JSONObject json) throws Exception {
        Map<String, Object> resultmap = new HashMap<>();
        resultmap.put("serialno", json.optString("serialno"));
        resultmap.put("patno", json.optString("patno"));
        resultmap.put("cname", json.optString("cname"));
        resultmap.put("sex", json.optString("sex"));
        resultmap.put("dept", json.optString("dept"));
        resultmap.put("bed", json.optString("bed"));
        resultmap.put("nursesender", json.optString("nursesender"));
        resultmap.put("transdatetime", json.optString("transdatetime"));

        /*
            检验项目
         */
        JSONArray itemjson = json.optJSONArray("sampleitem");
        StringBuilder itemnames = new StringBuilder();
        for (int i = 0; i < itemjson.length(); i++) {
            JSONObject preitemjson = itemjson.getJSONObject(i);
            String paritename = preitemjson.optString("paritemname");
            if(i == itemjson.length() - 1) {
                itemnames.append(paritename);
                break;
            }
            itemnames.append(paritename + ",");
        }
        resultmap.put("item", itemnames.toString());
        return resultmap;
    }
}
