package net.xhblog.lis_pda.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import net.xhblog.lis_pda.R;

public class TransActivity extends AppCompatActivity implements View.OnClickListener {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transactivity);

        findViewById(R.id.btn_trans_back).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), MainPageActivity.class);
        this.startActivity(intent);
        this.finish();
    }
}
