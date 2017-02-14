package com.silverlit.onenetedp.activities;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.balysv.materialripple.MaterialRippleLayout;
import com.chinamobile.iot.onenet.edp.CmdMsg;
import com.chinamobile.iot.onenet.edp.Common;
import com.chinamobile.iot.onenet.edp.ConnectRespMsg;
import com.chinamobile.iot.onenet.edp.EdpMsg;
import com.chinamobile.iot.onenet.edp.EncryptRespMsg;
import com.chinamobile.iot.onenet.edp.PingRespMsg;
import com.chinamobile.iot.onenet.edp.PushDataMsg;
import com.chinamobile.iot.onenet.edp.SaveDataMsg;
import com.chinamobile.iot.onenet.edp.SaveRespMsg;
import com.chinamobile.iot.onenet.edp.toolbox.EdpClient;
import com.chinamobile.iot.onenet.edp.toolbox.Listener;
import com.google.gson.Gson;
import com.silverlit.onenetedp.R;
import com.silverlit.onenetedp.model.CtrlBean;
import com.silverlit.onenetedp.model.OneNetEDPClient;
import com.silverlit.onenetedp.utils.Constants;
import com.silverlit.onenetedp.views.DevsFragment;
import com.silverlit.onenetedp.views.TestFragment;

import java.lang.ref.WeakReference;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.netty.channel.Channel;

public class MainActivity extends AppCompatActivity implements Listener
        ,DevsFragment.OnFragmentInteractionListener
        ,TestFragment.OnFragmentInteractionListener{
    private static final String TAG = "MainActivity";

    private DevsFragment mDevsFragment;
    private TestFragment mTestFragment;
    private ActionBarDrawerToggle mActionBarDrawerToggle;
    Channel mChannel;
    @BindView(R.id.radioGroup)
    RadioGroup mRadioGroup;
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.drawerLayout)
    DrawerLayout mDrawerLayout;
    @BindView(R.id.devsFragmentBtn)
    RadioButton mDevsFragmentBtn;
    @BindView(R.id.testFragmentBtn)
    RadioButton mTestFragmentBtn;
    @BindView(R.id.exit)
    MaterialRippleLayout mRippleLayoutExit;
    @BindView(R.id.addDevice)
    MaterialRippleLayout mRippleLayoutAddDevice;
    private android.support.v4.app.FragmentManager mFragmentManager;
    private MHandler mHandler;

    @OnClick(R.id.exit)
    void exit(){
        finish();
//        Toast.makeText(this, "finish!" + R.id.exit, Toast.LENGTH_SHORT).show();
    }
    @OnClick(R.id.addDevice)
    void addDevice(){
        new AlertDialog.Builder(this)
                .setTitle("Add one dev")
                .setView(R.layout.add_dev_dialog)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText devId = ButterKnife.findById((Dialog) dialog,R.id.devId);
                        if (!mDevsFragment.addOneItem(devId.getText().toString())){
                            Toast.makeText(MainActivity.this,"The device id must be single!",
                                    Toast.LENGTH_SHORT).show();
                        }else {
                            mDrawerLayout.closeDrawer(GravityCompat.START);
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private static class MHandler extends Handler{
        WeakReference<MainActivity> mWeakReference;
        MainActivity mActivity;
        private MHandler(MainActivity activity){
            mWeakReference = new WeakReference<>(activity);
            mActivity       = mWeakReference.get();
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Constants.HANDLER_CONNECT_FAIl_CODE:
                    new AlertDialog.Builder(mActivity)
//                            .setView()
                            .setTitle("Invalid internet!")
                            .setMessage("Please reconnect to the valid internet and try again!")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    break;
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        Log.d(TAG,"onCreate");
        mHandler = new MHandler(this);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
            final ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setDisplayShowTitleEnabled(true);
                actionBar.setDisplayUseLogoEnabled(false);
                actionBar.setHomeButtonEnabled(true);
            }
        }
        init();
    }
    private void init(){
        mActionBarDrawerToggle = new ActionBarDrawerToggle(this,mDrawerLayout,R.string.drawer_open,
                R.string.drawer_close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
            }
        };
//        mRippleLayoutExit.setOnClickListener(this);
        mDrawerLayout.addDrawerListener(mActionBarDrawerToggle);
        mActionBarDrawerToggle.syncState();
        mDevsFragment = DevsFragment.newInstance(null,null);
        mTestFragment = TestFragment.newInstance(null,null);
        mFragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.fragmentContainer,mTestFragment
                ,"mTestFragment");
        transaction.add(R.id.fragmentContainer,mDevsFragment
                ,"mDevsFragment");
        transaction.hide(mTestFragment).show(mDevsFragment).commit();
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.devsFragmentBtn:
                        switchFragment(mFragmentManager.findFragmentByTag("mTestFragment")
                                ,mDevsFragment);
//                        mFragmentManager.beginTransaction().add(R.id.fragmentContainer,
//                                mDevsFragment,"").show(mDevsFragment).commit();
                        break;
                    case R.id.testFragmentBtn:
                        switchFragment(mFragmentManager.findFragmentByTag("mDevsFragment")
                                ,mTestFragment);
//                        mFragmentManager.beginTransaction().add(R.id.fragmentContainer,
//                                mTestFragment,"").show(mTestFragment).commit();
                        break;
                }
                mDrawerLayout.closeDrawer(GravityCompat.START);
            }

            private void switchFragment(android.support.v4.app.Fragment from
                    , android.support.v4.app.Fragment to){
                    if (to.isAdded()){
                        mFragmentManager.beginTransaction().hide(from).show(to).commit();
                    }else {
                        mFragmentManager.beginTransaction().hide(from).add(R.id.fragmentContainer
                                ,to).commit();
                    }
            }
        });
        Intent intent = getIntent();
        OneNetEDPClient.initialize(this,intent.getIntExtra(Constants.EXTRA_CONNECT_TYPE,1),
                intent.getStringExtra(Constants.EXTRA_ID),
                intent.getStringExtra(Constants.EXTRA_AUTH_INFO));
        OneNetEDPClient.getInstance().setListener(this);
        OneNetEDPClient.getInstance().setPingInterval(Constants.SEND_HEART_RATE);
        OneNetEDPClient.getInstance().connect();
        if (Common.Algorithm.NO_ALGORITHM == intent.getIntExtra(Constants.EXTRA_ENCRYPT_TYPE, -1)) {
            // 5、如果使用明文通信，则建立连接后直接发送连接请求
            OneNetEDPClient.getInstance().sendConnectReq();
        } else if (Common.Algorithm.ALGORITHM_AES == intent.getIntExtra(Constants.EXTRA_ENCRYPT_TYPE, -1)) {
            // 6、如果使用加密通信，则先发送加密请求，然后在加密响应回调中发送连接请求
            OneNetEDPClient.getInstance().requestEncrypt(Common.Algorithm.ALGORITHM_AES);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("isGC",true);
        Log.d(TAG,"onSaveInstanceState");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.d(TAG,"onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG,"onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG,"onRestart");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mTestFragment = null;
        mDevsFragment = null;
        Log.d(TAG,"onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG,"onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        OneNetEDPClient.getInstance().disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mTestFragment = (TestFragment) mFragmentManager.findFragmentByTag("mTestFragment");
        mDevsFragment = (DevsFragment) mFragmentManager.findFragmentByTag("mDevsFragment");
        Log.d(TAG,"onResume");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return mActionBarDrawerToggle.onOptionsItemSelected(item) ||
                super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout.isDrawerOpen(GravityCompat.START)){
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }else {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            startActivity(intent);
        }
    }

    /**
     * @see Listener from onenet edp sdk
     * @param msgList
     */
    @Override
    public void onReceive(List<EdpMsg> msgList) {
        if (null == msgList) {
            return;
        }
        for (EdpMsg msg : msgList) {
            if (null == msg) {
                continue;
            }
            switch (msg.getMsgType()) {

                // 连接响应
                case Common.MsgType.CONNRESP:

                    ConnectRespMsg connectRespMsg = (ConnectRespMsg) msg;
                    Log.d(TAG, "连接响应码: " + connectRespMsg.getResCode());
                    if (connectRespMsg.getResCode() == Common.ConnResp.ACCEPTED) {
                        Toast.makeText(getApplicationContext(), "连接成功", Toast.LENGTH_SHORT).show();
                    }
//                    //mLogList.add("连接响应\n\n响应码 = " + connectRespMsg.getResCode());

                    break;

                // 心跳响应
                case Common.MsgType.PINGRESP:

                    PingRespMsg pingRespMsg = (PingRespMsg) msg;
                    Log.d(TAG, "心跳响应");
                    Toast.makeText(getApplicationContext(), "心跳响应", Toast.LENGTH_SHORT).show();
                    //mLogList.add("心跳响应");

                    break;

                // 存储确认
                case Common.MsgType.SAVERESP:

                    SaveRespMsg saveRespMsg = (SaveRespMsg) msg;
                    Log.d(TAG, "存储确认: " + new String(saveRespMsg.getData()));
                    //mLogList.add("存储确认: " + new String(saveRespMsg.getData()));

//                    NotificationController.getInstance(MainActivity.this).notifyMessage("存储确认: " + new String(saveRespMsg.getData()));

                    break;

                // 转发（透传）
                case Common.MsgType.PUSHDATA:

                    PushDataMsg pushDataMsg = (PushDataMsg) msg;
                    Log.d(TAG, "透传数据: " + new String(pushDataMsg.getData()));
                    //mLogList.add("透传数据: " + new String(pushDataMsg.getData()));
//                    NotificationController.getInstance(MainActivity.this).notifyMessage("透传数据: " + new String(pushDataMsg.getData()));
                    break;

                // 存储（转发）
                case Common.MsgType.SAVEDATA:

                    SaveDataMsg saveDataMsg = (SaveDataMsg) msg;
                    for (byte[] bytes : saveDataMsg.getDataList()) {
                        Log.d(TAG, "存储数据: " + new String(bytes) + ((SaveDataMsg) msg).getSrcDeviceId());
                        Gson gson = new Gson();
                        CtrlBean receiveMessage = gson.fromJson(new String(bytes), CtrlBean.class);

                        ////mLogList.add("存储数据: " + new String(bytes));
//                        NotificationController.getInstance(MainActivity.this).notifyMessage("存储数据: " + new String(bytes));
                    }
                    break;

                // 命令请求
                case Common.MsgType.CMDREQ:

                    CmdMsg cmdMsg = (CmdMsg) msg;
                    Log.d(TAG, "cmdid: " + cmdMsg.getCmdId() + "\n命令请求内容: " + new String(cmdMsg.getData()));
                    Toast.makeText(getApplicationContext(), "cmdid: " + cmdMsg.getCmdId() + "\n命令请求内容: " + new String(cmdMsg.getData()), Toast.LENGTH_LONG).show();
                    //mLogList.add("命令请求：\ncmdid: " + cmdMsg.getCmdId() + "\ndata: " + new String(cmdMsg.getData()));

//                    NotificationController.getInstance(MainActivity.this).notifyMessage("命令请求：\ncmdid: " + cmdMsg.getCmdId() + "\ndata: " + new String(cmdMsg.getData()));

                    // 发送命令响应
                    EdpClient.getInstance().sendCmdResp(cmdMsg.getCmdId(), "发送命令成功".getBytes());
                    break;

                // 加密响应
                case Common.MsgType.ENCRYPTRESP:
                    Log.d(TAG, "加密响应");
                    EncryptRespMsg encryptRespMsg = (EncryptRespMsg) msg;
                    String key = encryptRespMsg.getEncryptSecretKey();
                    Log.d(TAG, "加密密钥 = " + key);
                    try {
                        String keyPlain = EdpClient.getInstance().rsaDecrypt(key);
                        Log.d(TAG, "原始密钥 = " + keyPlain);
                        //mLogList.add("加密响应\n\n加密密钥 = " + key + "\n\n原始密钥 = " + keyPlain);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    EdpClient.getInstance().sendConnectReq();
                    break;
            }
//            mRequestLogFragment.updateList(//mLogList);
        }
        
    }

    /**
     * @see Listener from onenet edp sdk
     * @param e
     */
    @Override
    public void onFailed(Exception e) {
        e.printStackTrace();
        Toast.makeText(getApplicationContext(), "网络异常", Toast.LENGTH_SHORT).show();
        new AlertDialog.Builder(MainActivity.this)
                .setMessage("网络发生异常，连接已断开，请重新连接")
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        SignInActivity.actionSignIn(MainActivity.this);
                        finish();
                    }
                })
                .show();
    }

    /**
     * @see Listener from onenet edp sdk
     */
    @Override
    public void onDisconnect() {
        Toast.makeText(getApplicationContext(), "连接断开", Toast.LENGTH_SHORT).show();
    }

    /**
     * @see DevsFragment.OnFragmentInteractionListener
     * @param uri
     */
    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /**
     * @see Listener from onenet edp sdk . Called from non ui thread
     */
    @Override
    public void connectFail() {
        mHandler.sendEmptyMessage(Constants.HANDLER_CONNECT_FAIl_CODE);
    }
}
