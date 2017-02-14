package com.silverlit.onenetedp.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import com.chinamobile.iot.onenet.edp.CmdRespMsg;
import com.chinamobile.iot.onenet.edp.Common;
import com.chinamobile.iot.onenet.edp.ConnectMsg;
import com.chinamobile.iot.onenet.edp.EdpKit;
import com.chinamobile.iot.onenet.edp.EdpMsg;
import com.chinamobile.iot.onenet.edp.EncryptMsg;
import com.chinamobile.iot.onenet.edp.EncryptRespMsg;
import com.chinamobile.iot.onenet.edp.PingMsg;
import com.chinamobile.iot.onenet.edp.PushDataMsg;
import com.chinamobile.iot.onenet.edp.SaveDataMsg;
import com.chinamobile.iot.onenet.edp.toolbox.EdpClient;
//import com.chinamobile.iot.onenet.edp.toolbox.ServiceOneNET;
import com.chinamobile.iot.onenet.edp.toolbox.Listener;
import com.chinamobile.iot.onenet.edp.toolbox.RSAUtils;
import com.silverlit.onenetedp.services.ServiceOneNET;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * * 该类封装了EDP协议中规定的所有操作。EDP客户端使用该类发送请求，并通过{@link #setListener(Listener)}注册
 * {@link Listener}接收响应。其中接收到连接响应后，会自动启动{@link ServiceOneNET}定时发送心跳，默认为3min，
 * 可以通过{@link #setPingInterval(long)}来自定义心跳发送的周期。
 * <p/>
 *
 * 调用{@link #connect()}方法后，会自动启动发送和接收两个子线程，接收到响应后，会post到主线程中，所以在
 * {@link Listener}中的回调方法都在主线程中执行。
 * <p/>
 *
 * 如果使用加密通信，则在调用{@link #connect()}方法后调用{@link #requestEncrypt(int)}，然后在加密响应回调中
 * 再调用{@link #sendConnectReq()}。
 * <p/>
 *
 * 如果使用明文通信，则在{@link #connect()}之后直接调用{@link #sendConnectReq()}发送连接请求。
 * <p/>
 * Created by Yangyl on 2016/10/22.
 */

public class OneNetEDPClient {
    /**
     * 平台默认超时时间为4min，超过该时间没有发送心跳请求则断开连接
     */
    public static final long DEFAULT_TIMEOUT_MILLI_SECONDS = 4 * 60 * 1000;

    /**
     * 默认自动发送心跳的间隔
     */
    private static final long DEFAULT_PING_INTERVAL = 3 * 60 * 1000;

    /**
     * 连接认证类型1：设备ID + 鉴权信息(apikey)
     */
    public static final int CONNECT_TYPE_1 = 1;

    /**
     * 连接认证类型2：项目ID + 鉴权信息(auth info)
     */
    public static final int CONNECT_TYPE_2 = 2;

    /**
     * 服务器主机名
     */
    private String mHost = "jjfaedp.hedevice.com";

    /**
     * 端口
     */
    private int mPort = 876;

    /**
     * 备用端口
     */
    private int mPort2 = 29876;

    /**
     * 心跳周期
     */
    private long mPingIntervalMilli = DEFAULT_PING_INTERVAL;

    private Socket mEdpSocket;

    private boolean mConnected;

    /**
     * 请求消息队列
     */
    private LinkedBlockingQueue<OneNetEDPClient.Message> mMessageQueue = new LinkedBlockingQueue<OneNetEDPClient.Message>();

    /**
     * 认证类型
     */
    private int mConnectType;

    private String mDeviceId;
    private String mApikey;
    private String mProjectId;
    private String mAuthInfo;

    private Listener mListener;

    /**
     * 心跳广播的Action
     */
    public static final String ACTION_HEARTBEAT = "onenet.edp.intent.ACTION_HEARTBEAT";

    private static OneNetEDPClient sInstance;
    private Context mApplicationContext;

    private Handler mHandler = new Handler();

    private int mEncryptAlgorithm;
    private EdpKit mEdpKit = new EdpKit();

    private String mSectetKey;

    /**
     * 初始化 OnetNetEDPClient
     * @param context   上下文
     * @param type      连接类型
     * @param id        设备或项目id
     * @param authinfo  鉴权信息(apikey或authinfo)
     */
    public static void initialize(Context context, int type, String id, String authinfo) {
        if (sInstance == null) {
            synchronized (OneNetEDPClient.class) {
                if (sInstance == null) {
                    sInstance = new OneNetEDPClient(context, type, id, authinfo);
                }
            }
        }
    }

    public static OneNetEDPClient getInstance() {
        if (null == sInstance) {
            throw new RuntimeException("You must call OnetNetEDPClient.initialize() first");
        }
        return sInstance;
    }

    private OneNetEDPClient(Context context, int type, String id, String authinfo) {
        mApplicationContext = context.getApplicationContext();
        mConnectType = type;
        if (CONNECT_TYPE_1 == mConnectType) {
            mDeviceId = id;
            mApikey = authinfo;
        } else if (CONNECT_TYPE_2 == mConnectType) {
            mProjectId = id;
            mAuthInfo = authinfo;
        }
    }

    /**
     * 设置心跳请求周期，默认3min
     *
     * @param milliseconds
     */
    public void setPingInterval(long milliseconds) {
        if (milliseconds >= DEFAULT_TIMEOUT_MILLI_SECONDS) {
            throw new IllegalArgumentException("ping interval can not be longer than 4 min");
        }
        mPingIntervalMilli = milliseconds;
    }

    private static class ConnectThreat extends Thread {
//        保存外部类的弱引用，防止内存泄漏
        private WeakReference<OneNetEDPClient> mWeakReference;
        OneNetEDPClient mOneNetEDPClient;
        ConnectThreat(OneNetEDPClient client){
            mWeakReference = new WeakReference<OneNetEDPClient>(client);
            mOneNetEDPClient = mWeakReference.get();
        }
        @Override
        public void run() {

            try {
                mOneNetEDPClient.mEdpSocket = new Socket(mOneNetEDPClient.mHost, mOneNetEDPClient.mPort);
                new OneNetEDPClient.RecvMessageThread(mOneNetEDPClient).start();
                new OneNetEDPClient.SendMessageThread(mOneNetEDPClient).start();

            } catch (final IOException e) {
                e.printStackTrace();
                //连接网络失败的回调
                mOneNetEDPClient.mListener.connectFail();
                if (mOneNetEDPClient.mListener != null && mOneNetEDPClient.mConnected) {
                    mOneNetEDPClient.mHandler.post(new OneNetEDPClient.FailedEvent(e));
                }
            }

        }
    }

    private static class SendMessageThread extends Thread {
        private WeakReference<OneNetEDPClient> mWeakReference;
        private Exception mException;
        OneNetEDPClient mOneNetEDPClient;
        SendMessageThread(OneNetEDPClient client){
            mWeakReference = new WeakReference<OneNetEDPClient>(client);
            mOneNetEDPClient = mWeakReference.get();
        }

        @Override
        public void run() {
            OutputStream outputStream = null;
            try {
                while (true) {
                    if (null == mOneNetEDPClient.mEdpSocket) {
                        break;
                    }
                    outputStream = mOneNetEDPClient.mEdpSocket.getOutputStream();
                    mOneNetEDPClient.mConnected = true;
                    OneNetEDPClient.Message msg = mOneNetEDPClient.mMessageQueue.take();
                    if (msg.getType() == OneNetEDPClient.Message.BYTES) {
                        outputStream.write(msg.getPacket());
                    } else if (msg.getType() == OneNetEDPClient.Message.FILE) {
                        mOneNetEDPClient.sendFile(msg.getFilePath(), outputStream);
                    } else if (msg.getType() == OneNetEDPClient.Message.DISCONNECT) {
                        if (mOneNetEDPClient.mListener != null) {
                            mOneNetEDPClient.mHandler.post(new OneNetEDPClient.DisconnectEvent(mOneNetEDPClient));
                        }
                        break;
                    }
                }
            } catch (IOException e) {
                mException = e;
            } catch (InterruptedException e) {
                mException = e;
            } finally {
                if (mException != null && mOneNetEDPClient.mListener != null && mOneNetEDPClient.mConnected) {
                    mOneNetEDPClient.mHandler.post(new OneNetEDPClient.FailedEvent(mException));
                }
                mOneNetEDPClient.mConnected = false;
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private void sendFile(String filePath, OutputStream outputStream) throws IOException {
        FileInputStream fis = new FileInputStream(filePath);
        byte[] buffer = new byte[1024];
        int size;
        while ((size = fis.read(buffer)) > 0) {
            outputStream.write(buffer, 0, size);
        }
        outputStream.flush();
        fis.close();
    }

    private static class RecvMessageThread extends Thread {
        private WeakReference<OneNetEDPClient> mWeakReference;
        OneNetEDPClient mOneNetEDPClient;
        RecvMessageThread(OneNetEDPClient client){
            mWeakReference = new WeakReference<OneNetEDPClient>(client);
            mOneNetEDPClient = mWeakReference.get();
        }

        @Override
        public void run() {

            InputStream inputStream = null;
            try {
                while (true) {
                    if (null == mOneNetEDPClient.mEdpSocket) {
                        break;
                    }
                    inputStream = mOneNetEDPClient.mEdpSocket.getInputStream();
                    mOneNetEDPClient.mConnected = true;
                    byte[] recvPacket = mOneNetEDPClient.readRecvPacket(inputStream);
                    if (null == recvPacket) {
                        break;
                    }
                    List<EdpMsg> msgList = mOneNetEDPClient.mEdpKit.unpack(recvPacket
                            , mOneNetEDPClient.mEncryptAlgorithm, mOneNetEDPClient.mSectetKey);
                    if (mOneNetEDPClient.mListener != null && msgList != null) {
                        for (EdpMsg msg : msgList) {
                            if (msg != null) {
                                byte type = msg.getMsgType();
                                if (Common.MsgType.CONNRESP == type) {
                                    ServiceOneNET.start(mOneNetEDPClient.mApplicationContext.getApplicationContext());
                                } else if (Common.MsgType.ENCRYPTRESP == type) {
                                    EncryptRespMsg encryptRespMsg = (EncryptRespMsg) msg;
                                    try {
                                        mOneNetEDPClient.mSectetKey =
                                                mOneNetEDPClient.rsaDecrypt(encryptRespMsg.getEncryptSecretKey());
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                        mOneNetEDPClient.mHandler.post(new OneNetEDPClient.ReceiveEvent(mOneNetEDPClient,msgList));
                    }
                }
            } catch (IOException e) {
                if (mOneNetEDPClient.mListener != null && mOneNetEDPClient.mConnected) {
                    mOneNetEDPClient.mHandler.post(new OneNetEDPClient.FailedEvent(e));
                }
            } finally {
                OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
                msg.setType(OneNetEDPClient.Message.DISCONNECT);
                mOneNetEDPClient.enqueueMsg(msg);
                mOneNetEDPClient.mConnected = false;
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
    }

    private byte[] readRecvPacket(InputStream inputStream) throws IOException {
        byte[] buffer = new byte[1024];
        int size;
        byte[] recvPacket = null;
        int pos = 0;
        while ((size = inputStream.read(buffer)) > 0) {
            if (null == recvPacket) {
                recvPacket = new byte[size];
                System.arraycopy(buffer, 0, recvPacket, pos, size);
            } else {
                byte[] temp = new byte[pos + size];
                System.arraycopy(recvPacket, 0, temp, 0, pos);
                System.arraycopy(buffer, 0, temp, pos, size);
                recvPacket = temp;
            }
            if (size < 1024) {
                break;
            }
            pos += size;
        }
        if (size <= 0) {
            recvPacket = null;
        }
        return recvPacket;
    }

    /**
     * 建立TCP连接
     */
    public void connect() {
        if (mConnected) {
            return;
        }

        new OneNetEDPClient.ConnectThreat(this).start();
    }

    /**
     * 建立TCP连接，指定主机名和端口
     */
    public void connect(String host, int port) {
        if (TextUtils.isEmpty(host)) {
            throw new IllegalArgumentException("host can not be empty");
        }
        if (port <= 0) {
            throw new IllegalArgumentException("port " + port + " is illegal");
        }
        mHost = host;
        mPort = port;
        connect();
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        mConnected = false;
        if (mEdpSocket != null) {
            try {
                mEdpSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                mEdpSocket = null;
            }
        }
        ServiceOneNET.stop(mApplicationContext);
        OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
        msg.setType(OneNetEDPClient.Message.DISCONNECT);
        enqueueMsg(msg);
        mMessageQueue.clear();
        sInstance = null;
    }

    public void setListener(Listener l) {
        mListener = l;
    }

    /**
     * 发送连接请求
     */
    public void sendConnectReq() {
        ConnectMsg connectMsg = new ConnectMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            connectMsg.setAlgorithm(mEncryptAlgorithm);
            connectMsg.setSecretKey(mSectetKey);
        }
        OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
        msg.setType(OneNetEDPClient.Message.BYTES);
        byte[] packet = null;
        switch (mConnectType) {
            case CONNECT_TYPE_1:
                packet = connectMsg.packMsg(mDeviceId, mApikey);
                break;

            case CONNECT_TYPE_2:
                packet = connectMsg.packMsg("0", mProjectId, mAuthInfo);
                break;

            default:
                throw new IllegalArgumentException("Unknown connect type: " + mConnectType);
        }

        msg.setPacket(packet);
        // 发送连接请求
        enqueueMsg(msg);
    }

    /**
     * 心跳请求
     */
    public void sendHeartbeat() {
        PingMsg pingMsg = new PingMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            pingMsg.setAlgorithm(mEncryptAlgorithm);
            pingMsg.setSecretKey(mSectetKey);
        }
        OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
        msg.setType(OneNetEDPClient.Message.BYTES);
        msg.setPacket(pingMsg.packMsg());
        enqueueMsg(msg);
    }

    /**
     * 转发（透传）数据
     */
    public void pushData(long deviceId, byte[] data) {
        OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
        PushDataMsg pushDataMsg = new PushDataMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            pushDataMsg.setAlgorithm(mEncryptAlgorithm);
            pushDataMsg.setSecretKey(mSectetKey);
        }
        try {
            msg.setType(OneNetEDPClient.Message.BYTES);
            msg.setPacket(pushDataMsg.packMsg(deviceId, data));
            enqueueMsg(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 转发（透传）数据
     */
    public void pushData(long deviceId, String data) {
        OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
        PushDataMsg pushDataMsg = new PushDataMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            pushDataMsg.setAlgorithm(mEncryptAlgorithm);
            pushDataMsg.setSecretKey(mSectetKey);
        }
        try {
            msg.setType(OneNetEDPClient.Message.BYTES);
            msg.setPacket(pushDataMsg.packMsg(deviceId, data));
            enqueueMsg(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 存储（&转发）数据
     */
    public void saveData(String desDeviceId, int dataType, String tokenStr, byte[] data) {
        OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
        SaveDataMsg saveDataMsg = new SaveDataMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            saveDataMsg.setAlgorithm(mEncryptAlgorithm);
            saveDataMsg.setSecretKey(mSectetKey);
        }
        msg.setType(OneNetEDPClient.Message.BYTES);
        boolean flag = saveDataMsg.packMsg(desDeviceId, dataType, tokenStr, data);
        if (flag) {
            msg.setPacket(saveDataMsg.commit());
            enqueueMsg(msg);
        }
    }

    /**
     * 发送命令响应
     */
    public void sendCmdResp(String cmdid, byte[] data) {
        OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
        CmdRespMsg cmdRespMsg = new CmdRespMsg();
        if (mEncryptAlgorithm > Common.Algorithm.NO_ALGORITHM && !TextUtils.isEmpty(mSectetKey)) {
            cmdRespMsg.setAlgorithm(mEncryptAlgorithm);
            cmdRespMsg.setSecretKey(mSectetKey);
        }
        msg.setType(OneNetEDPClient.Message.BYTES);
        msg.setPacket(cmdRespMsg.packMsg(cmdid, data));
        enqueueMsg(msg);
    }

    /**
     * 发送加密请求
     */
    public void requestEncrypt(int algorithm) {
        if (algorithm > Common.Algorithm.NO_ALGORITHM) {
            mEncryptAlgorithm = algorithm;
            try {
                generateEncryptInfo();
                EncryptMsg encryptMsg = new EncryptMsg();
                OneNetEDPClient.Message msg = new OneNetEDPClient.Message();
                msg.setType(OneNetEDPClient.Message.BYTES);
                msg.setPacket(encryptMsg.packMsg(mModulus, mPublicExponent, algorithm));
                enqueueMsg(msg);
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 消息队列中的消息
     */
    static class Message {

        /**
         * 消息体为文字的类型
         */
        public static final int BYTES = 1;

        /**
         * 消息体为文件
         */
        public static final int FILE = 2;

        /**
         * 断开连接
         */
        public static final int DISCONNECT = 3;

        /**
         * 类型
         */
        private int type;

        /**
         * 完整的EDP消息，文件类型的消息此项无效
         */
        private byte[] packet;

        /**
         * 文件路径
         */
        private String filePath;

        public int getType() {
            return type;
        }

        public OneNetEDPClient.Message setType(int type) {
            this.type = type;
            return this;
        }

        public byte[] getPacket() {
            return packet;
        }

        public OneNetEDPClient.Message setPacket(byte[] packet) {
            this.packet = packet;
            return this;
        }

        public String getFilePath() {
            return filePath;
        }

        public OneNetEDPClient.Message setFilePath(String filePath) {
            this.filePath = filePath;
            return this;
        }
    }

    /**
     * 设置发送心跳的闹钟
     */
    public void setupAlarm(Context context) {
        Intent intent = new Intent(ACTION_HEARTBEAT);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        setupAlarm(context, mPingIntervalMilli, pendingIntent);
    }

    /**
     * 设置闹钟
     */
    public void setupAlarm(Context context, long timeIntervalMillis, PendingIntent pendingIntent) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long now = System.currentTimeMillis();
        am.cancel(pendingIntent);
        am.set(AlarmManager.RTC, now + timeIntervalMillis, pendingIntent);
    }

    /**
     * 停止发送心跳的闹钟
     */
    public void cancelAlarm(Context context) {
        Intent intent = new Intent(ACTION_HEARTBEAT);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        cancelAlarm(context, pendingIntent);
    }

    /**
     * 停止闹钟
     */
    public void cancelAlarm(Context context, PendingIntent pendingIntent) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }

    /**
     * 加入消息到发送队列
     * @param msg
     */
    private void enqueueMsg(OneNetEDPClient.Message msg) {
        mMessageQueue.offer(msg);
    }

    /**
     * 设备是否在线
     */
    public boolean isConnected() {
        return mConnected;
    }

    private static class ReceiveEvent implements Runnable {
        private WeakReference<OneNetEDPClient> mWeakReference ;
        OneNetEDPClient mOneNetEDPClient;
        private List<EdpMsg> mMsgList;

        public ReceiveEvent(OneNetEDPClient client,List<EdpMsg> msgList) {
            mWeakReference = new WeakReference<OneNetEDPClient>(client);
            mOneNetEDPClient = mWeakReference.get();
            mMsgList = msgList;
        }

        @Override
        public void run() {
            if (mOneNetEDPClient.mListener != null) {
                mOneNetEDPClient.mListener.onReceive(mMsgList);
            }
        }
    }

    private static class FailedEvent implements Runnable {

        private Exception mException;
        private WeakReference<OneNetEDPClient> mWeakReference;
        OneNetEDPClient mOneNetEDPClient;
        FailedEvent(OneNetEDPClient client){
            mWeakReference = new WeakReference<OneNetEDPClient>(client);
            mOneNetEDPClient = mWeakReference.get();
        }

        public FailedEvent(Exception e) {
            mException = e;
        }

        @Override
        public void run() {
            if (mOneNetEDPClient.mListener != null) {
                mOneNetEDPClient.mListener.onFailed(mException);
            }
        }
    }

    private static class DisconnectEvent implements Runnable {
        private WeakReference<OneNetEDPClient> mWeakReference;
        OneNetEDPClient mOneNetEDPClient;
        DisconnectEvent(OneNetEDPClient client){
            mWeakReference = new WeakReference<OneNetEDPClient>(client);
            mOneNetEDPClient = mWeakReference.get();
        }
        @Override
        public void run() {
            if (mOneNetEDPClient.mListener != null) {
                mOneNetEDPClient.mListener.onDisconnect();
            }
        }
    }

    private BigInteger mModulus;
    private BigInteger mPublicExponent;
    private BigInteger mPrivateExponent;
    private RSAPublicKey mPublicKey;
    private RSAPrivateKey mPrivateKey;

    private void generateEncryptInfo() throws NoSuchAlgorithmException {
        HashMap<String, Object> map = RSAUtils.getKeys();
        //生成公钥和私钥
        mPublicKey = (RSAPublicKey) map.get("public");
        mPrivateKey = (RSAPrivateKey) map.get("private");

        //模
        mModulus = mPublicKey.getModulus();
        Log.i("OneNetApp", "n = " + mModulus.toString());
        //公钥指数
        mPublicExponent = mPublicKey.getPublicExponent();
        Log.i("OneNetApp", "e = " + mPublicExponent.toString());
        //私钥指数
        mPrivateExponent = mPrivateKey.getPrivateExponent();
        Log.i("OneNetApp", "d = " + mPrivateExponent.toString());
    }

    /**
     * RSA加密
     * @param plainText 明文
     */
    public String rsaEncrypt(String plainText) throws Exception {
        if (mPublicKey != null) {
            return RSAUtils.encryptByPublicKey(plainText, mPublicKey);
        } else {
            return plainText;
        }
    }

    /**
     * RSA解密
     * @param cipherText 密文
     */
    public String rsaDecrypt(String cipherText) throws Exception {
        if (mPrivateKey != null) {
            return RSAUtils.decryptByPrivateKey(cipherText, mPrivateKey);
        } else {
            return cipherText;
        }
    }
}
