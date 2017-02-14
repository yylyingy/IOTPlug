package com.silverlit.onenetedp;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.chinamobile.iot.onenet.edp.toolbox.EdpClient;
import com.silverlit.onenetedp.activities.MainActivity;
import com.silverlit.onenetedp.utils.Constants;
import com.silverlit.onenetedp.utils.Util;


public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        checkPermissions();
    }

    private void checkPermissions(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
//            首先检测是否获得了权限
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED){
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setTitle("Warning!")
                        .setMessage("本程序需要获得存储权限！")
                        .setPositiveButton(android.R.string.ok, null)
//                        dialog消失时开始申请权限
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                requestPermissions(new String[]{
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                }, Constants.REQUEST_STORAGE_CODE);
                            }
                        })
                        .show();
            }
        }
    }
//申请权限的结果
    @Override
    public void onRequestPermissionsResult(int requestCode
            , @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case Constants.REQUEST_STORAGE_CODE:
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    finish();
                break;
            default:
                break;
        }
    }

    public void startMainActivity(android.view.View source){
        if (Util.isNetWorkAvailable(this)) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
//            Bundle bundle = new Bundle();
//            intent.putExtra(START_MAIN_BUNDLE_NAME,bundle);
            intent.putExtra(Constants.EXTRA_CONNECT_TYPE, EdpClient.CONNECT_TYPE_1);
            intent.putExtra(Constants.EXTRA_ENCRYPT_TYPE,-1);// 1 AEC  -1不加密
            intent.putExtra(Constants.EXTRA_ID,Util.DEV_ID);
            intent.putExtra(Constants.EXTRA_AUTH_INFO,Util.API_KEY);
            startActivity(intent);
            finish();
        }else {
            Toast.makeText(this,"网络不可用,请连接网络后再试",Toast.LENGTH_SHORT).show();
        }

    }

}
