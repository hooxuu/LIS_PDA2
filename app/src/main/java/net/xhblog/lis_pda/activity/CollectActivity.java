package net.xhblog.lis_pda.activity;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import net.xhblog.lis_pda.R;
import net.xhblog.lis_pda.adapter.MyAdapter;
import net.xhblog.lis_pda.entity.Sample;
import net.xhblog.lis_pda.utils.ConnectionTools;
import net.xhblog.lis_pda.zxing.android.CaptureActivity;
import net.xhblog.lis_pda.zxing.common.Constant;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CollectActivity extends AppCompatActivity implements View.OnClickListener {
    public static final String ACTION = "net.xhblog.lis_pda.intent.action.CollectActivity";

    private static final int REQUEST_CODE_SCAN = 1;
    private static final String REQUEST_URL = "http://192.168.50.198:8080/Lis_Pda/UpdateCollectTimeServlet";

    private AppCompatImageView btn_collect_back;
    private ImageButton barcode_scan;
    private TextView barcode_tv;
    private GridView grid_sampleinfo;
    private BaseAdapter mAdapter = null;
    private ArrayList<Sample> mData = null;
    private List<Map<String, Object>> sampleinfo;

    //存放样本信息
    private List<Sample> sampleList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collectactivity);
        initView();
        btn_collect_back.setOnClickListener(this);
        barcode_scan.setOnClickListener(this);

        IntentFilter filter = new IntentFilter(CaptureActivity.ACTION);
        registerReceiver(broadcastReceiver, filter);

        mData = new ArrayList<>();
    }

    //广播接收器 接收实时扫描的条码号 然后发送给服务器进行查询
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String current_barcode = intent.getStringExtra("current_barcode");
            queryAndUpdateCollectInfo(current_barcode, new ICallback() {
                @Override
                public void callback(Map<String, Object> resultmap) {
                    if(sampleList.size() > 6) {
                        sampleList.remove(0);
                    }

                    Sample sample = new Sample();
                    sample.setLine1("姓名:" + resultmap.get("cname") + " " + "条码号:" +
                            resultmap.get("serialno"));
                    sample.setLine2("病历号:" + resultmap.get("patno") + " " + "性别:" +
                            resultmap.get("sex") + " " + "科室:" + resultmap.get("dept") + " " + "床号:" +
                            resultmap.get("bed"));
                    sample.setLine3("采样人及时间:" + resultmap.get("collecter") + "," +
                            resultmap.get("collectdatetime"));
                    sample.setItem((String) resultmap.get("item"));
                    sampleList.add(sample);
                    System.out.println("========size" + sampleList.size());
                    Log.d("=========msg:" , "" + sampleList.size());
                    if(!sampleList.isEmpty()) {
                        mData.addAll(sampleList);
                        mAdapter = new MyAdapter<Sample>(mData, R.layout.item_grid_sampleinfo) {
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
                }
            });
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
    }

    private void initView() {
        btn_collect_back = (AppCompatImageView) findViewById(R.id.btn_collect_back);
        barcode_scan = (ImageButton) findViewById(R.id.barcode_scan);
        barcode_tv = (TextView) findViewById(R.id.cbarcode_content);
        grid_sampleinfo = (GridView) findViewById(R.id.grid_sampleinfo);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.btn_collect_back :
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
                //barcode_tv.setText(content);
            }
        }
    }

    /**
     * 利用得到的条码进行查询更新采样信息
     */
    private void queryAndUpdateCollectInfo(final String barcode, final ICallback callback) {
        //请求服务器查询条码是否存在
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    SharedPreferences preferences = getSharedPreferences("userInfo",
                            Activity.MODE_PRIVATE);
                    String username = preferences.getString("username", "");
                    ConnectionTools tools = new ConnectionTools(REQUEST_URL,
                            "&SerialNo=" + URLEncoder.encode(barcode, "utf-8") +
                                    "&currentUser=" + URLEncoder.encode(username, "utf-8"));
                    String jsonresult = tools.getDataFromServer();
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

                    if("1" == code) {
                        Map<String, Object> resultmap = saveSampleInfo(json);
                        callback.callback(resultmap);

                        Intent intent = new Intent(ACTION);
                        intent.putExtra("code", code);
                        intent.putExtra("barcode", (String) resultmap.get("serialno"));
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
        }).start();
    }

    /**
     * 保存返回的信息到map
     */
    private Map<String, Object> saveSampleInfo(JSONObject json) throws Exception {
        Map<String, Object> resultmap = new HashMap<>();
        resultmap.put("serialno", json.optString("serialno"));
        resultmap.put("patno", json.optString("patno"));
        resultmap.put("cname", json.optString("cname"));
        resultmap.put("sex", json.optString("sex"));
        resultmap.put("dept", json.optString("dept"));
        resultmap.put("collecter", json.optString("collecter"));
        resultmap.put("collectdatetime", json.optString("collectdatetime"));

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