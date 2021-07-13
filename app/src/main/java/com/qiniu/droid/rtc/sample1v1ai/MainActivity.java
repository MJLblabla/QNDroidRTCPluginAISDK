package com.qiniu.droid.rtc.sample1v1ai;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;

public class MainActivity extends AppCompatActivity {

    private static final int QRCODE_RESULT_REQUEST_CODE = 1;
    private EditText mRoomTokenEditText;
    String token1 = "QxZugR8TAhI38AiJ_cptTl3RbzLyca3t-AAiH-Hh:IjZQk1oD46GxXma6lpZ0KNi5x8Q=:eyJhcHBJZCI6ImQ4ZHJlOHcxcCIsInJvb21OYW1lIjoiMjM0cyIsInVzZXJJZCI6InNzYWRzYWQiLCJleHBpcmVBdCI6MTYzMDA2MjEwNSwicGVybWlzc2lvbiI6InVzZXIifQ==";
    String token2 = "QxZugR8TAhI38AiJ_cptTl3RbzLyca3t-AAiH-Hh:KhqM2SKquI0_eoYI0r7dlOffwV8=:eyJhcHBJZCI6ImQ4ZHJlOHcxcCIsInJvb21OYW1lIjoiMjM0cyIsInVzZXJJZCI6ImFzZHNhMiIsImV4cGlyZUF0IjoxNjIzOTI3NzI2LCJwZXJtaXNzaW9uIjoidXNlciJ9";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRoomTokenEditText = findViewById(R.id.room_token_edit_text);

        findViewById(R.id.buttonToken1).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRoomTokenEditText.setText(token1);
                joinRoom(v);
            }
        });
        findViewById(R.id.buttonToken2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRoomTokenEditText.setText(token2);
                joinRoom(v);
            }
        });
    }

    public void joinRoom(View view) {
        // 在进入房间前，必须有相对应的权限，在 Android 6.0 后除了在 Manifest 文件中声明外还需要动态申请权限。
        if (!isPermissionOK()) {
            Toast.makeText(this, "Some permissions is not approved !!!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!TextUtils.isEmpty(mRoomTokenEditText.getText())) {
            Intent intent = new Intent(this, RoomActivity.class);
            intent.putExtra("roomToken", mRoomTokenEditText.getText().toString());
            startActivity(intent);
        }
    }

    public void clickToScanQRCode(View view) {
        // 扫码也用到了相机权限
        if (!isPermissionOK()) {
            Toast.makeText(this, "Some permissions is not approved !!!", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CaptureActivity.class);
        startActivityForResult(intent, QRCODE_RESULT_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // 处理扫描结果
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    mRoomTokenEditText.setText(result);
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private boolean isPermissionOK() {
        PermissionChecker checker = new PermissionChecker(this);
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checker.checkPermission();
    }
}
