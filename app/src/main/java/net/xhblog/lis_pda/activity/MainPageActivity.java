package net.xhblog.lis_pda.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.*;
import net.xhblog.lis_pda.R;
import net.xhblog.lis_pda.adapter.MyAdapter;
import net.xhblog.lis_pda.entity.Icon;

import java.util.ArrayList;

public class MainPageActivity extends AppCompatActivity implements View.OnClickListener {
    private Context mContext;
    private GridView grid_photo;
    private BaseAdapter mAdapter = null;
    private ArrayList<Icon> mData = null;

    //退出确认框
    private AlertDialog alert = null;
    private AlertDialog.Builder builder = null;

    // 退出时间
    private long currentBackPressedTime = 0;
    // 退出间隔
    private static final int BACK_PRESSED_INTERVAL = 2000;
    private TextView tv;
    private AppCompatImageView btn_back;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        initView();

        btn_back.setOnClickListener(this);

        SharedPreferences preferences = getSharedPreferences("userInfo",
                Activity.MODE_PRIVATE);
        String username = preferences.getString("username", "");
        tv.setText(String.format(getResources().getString(R.string.currentuser), username));

        //设置功能图标等
        mData = new ArrayList<>();
        mData.add(new Icon(R.mipmap.iv_sample_collect, "样本采集"));
        mData.add(new Icon(R.mipmap.iv_sample_trans, "样本送检"));

        mAdapter = new MyAdapter<Icon>(mData, R.layout.item_grid_icon) {
            @Override
            public void bindView(ViewHolder holder, Icon obj) {
                holder.setImageResource(R.id.img_icon, obj.getId());
                holder.setText(R.id.txt_icon, obj.getIname());
            }
        };

        grid_photo.setAdapter(mAdapter);

        grid_photo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                if(0 == position) {
                    intent.setClass(mContext, CollectActivity.class);
                } else if(1 == position) {
                    intent.setClass(mContext, TransActivity.class);
                }
                startActivity(intent);
            }
        });

    }

    private void initView(){
        mContext = MainPageActivity.this;
        grid_photo = (GridView) findViewById(R.id.grid_photo);
        tv = (TextView) findViewById(R.id.currentuser);
        btn_back = (AppCompatImageView) findViewById(R.id.btn_back);
    }

    @Override
    public void onClick(View view) {
        final Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.btn_back :
                builder = new AlertDialog.Builder(mContext);
                alert = builder.setIcon(R.drawable.loginout)
                        .setTitle("退出提示：")
                        .setMessage("你确定要退出当前用户吗?")
                        .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
                        })
                        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Toast.makeText(mContext, "退出成功", Toast.LENGTH_SHORT).show();
                                intent.setClass(mContext, LoginActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        }).create();//创建AlertDialog对象
                alert.show();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("提示");
        builder.setMessage("是否退出系统?");
        builder.setPositiveButton(R.string.confirm_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finishAffinity();
                System.exit(0);
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.show();
    }

}
