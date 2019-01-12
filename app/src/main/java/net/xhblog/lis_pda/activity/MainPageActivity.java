package net.xhblog.lis_pda.activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.view.View;
import android.widget.*;
import me.leefeng.promptlibrary.PromptButton;
import me.leefeng.promptlibrary.PromptButtonListener;
import me.leefeng.promptlibrary.PromptDialog;
import net.xhblog.lis_pda.R;
import net.xhblog.lis_pda.adapter.MyAdapter;
import net.xhblog.lis_pda.entity.Icon;
import net.xhblog.lis_pda.entity.User;

import java.util.ArrayList;

public class MainPageActivity extends AppCompatActivity implements View.OnClickListener {
    private Context mContext;
    private GridView grid_photo;
    private TextView tv;
    private AppCompatImageView btn_back;
    private User loginuser;
    private PromptDialog promptDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainpage);
        initView();

        btn_back.setOnClickListener(this);
        promptDialog = new PromptDialog(MainPageActivity.this);
        Intent intent = getIntent();
        loginuser = (User) intent.getSerializableExtra("loginuser");
        tv.setText(String.format(getResources().getString(R.string.currentuser), loginuser.getCname()));
        promptDialog.showSuccess("登陆成功");

        //设置功能图标等
        ArrayList<Icon> mData = new ArrayList<>();
        mData.add(new Icon(R.mipmap.iv_sample_collect1, "样本采集"));
        mData.add(new Icon(R.mipmap.iv_sample_trans1, "样本送检"));

        BaseAdapter mAdapter = new MyAdapter<Icon>(mData, R.layout.item_grid_icon) {
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
                //为了采样和送检传给客户端
                intent.putExtra("loginuser", loginuser);
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
        if(R.id.btn_back == view.getId()) {
            final Intent intent = new Intent();

            final PromptButton confirm = new PromptButton("确定", new PromptButtonListener() {
                @Override
                public void onClick(PromptButton button) {
                    SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
                    sharedPreferences.edit().putBoolean("autologin", false).apply();
                    Toast.makeText(mContext, "退出成功", Toast.LENGTH_SHORT).show();
                    intent.setClass(mContext, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
                    promptDialog.dismiss();
                    promptDialog.dismissImmediately();
                    startActivity(intent);
                }
            });
            final PromptButton cancelconfirm = new PromptButton("取消", new PromptButtonListener() {
                @Override
                public void onClick(PromptButton button) {
                }
            });

            confirm.setTextColor(Color.parseColor("#5EB7D5"));
            cancelconfirm.setTextColor(Color.parseColor("#5EB7D5"));

            promptDialog.showWarnAlert("你确定要退出登录吗？", cancelconfirm, confirm);

//            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//            AlertDialog alert = builder.setIcon(R.drawable.loginout)
//                    .setTitle("退出提示：")
//                    .setMessage("你确定要退出当前用户吗?")
//                    .setNegativeButton("取消", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {}
//                    })
//                    .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            SharedPreferences sharedPreferences = getSharedPreferences("userInfo", Context.MODE_PRIVATE);
//                            sharedPreferences.edit().putBoolean("autologin", false).apply();
//                            Toast.makeText(mContext, "退出成功", Toast.LENGTH_SHORT).show();
//                            intent.setClass(mContext, LoginActivity.class);
//                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
//                            startActivity(intent);
//                        }
//                    }).create();//创建AlertDialog对象
//            alert.show();
        }
    }

    @Override
    public void onBackPressed() {
//        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
//        builder.setTitle("提示");
//        builder.setMessage("是否退出系统?");
//        builder.setPositiveButton(R.string.confirm_btn, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                finishAffinity();
//                System.exit(0);
//            }
//        });
//
//        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {}
//        });
//
//        builder.show();

        final PromptButton confirm = new PromptButton("确定", new PromptButtonListener() {
            @Override
            public void onClick(PromptButton button) {
                finishAffinity();
                promptDialog.dismiss();
                promptDialog.dismissImmediately();
                System.exit(0);
            }
        });
        final PromptButton cancelconfirm = new PromptButton("取消", new PromptButtonListener() {
            @Override
            public void onClick(PromptButton button) {
            }
        });

        confirm.setTextColor(Color.parseColor("#EC1230"));
        cancelconfirm.setTextColor(Color.parseColor("#5EB7D5"));

        promptDialog.showWarnAlert("是否退出系统？", cancelconfirm, confirm);
    }

}
