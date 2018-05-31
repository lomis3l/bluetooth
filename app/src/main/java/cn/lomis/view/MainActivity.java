package cn.lomis.view;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import cn.lomis.R;
import cn.lomis.data.Data;
import cn.lomis.data.MsgData;
import cn.lomis.data.Params;
import cn.lomis.data.ResultData;
import cn.lomis.service.BluetoothLeService;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MainActivity";

    ViewPager viewPager;
    MyPagerAdapter pagerAdapter;
    String[] titleList=new String[]{"设备列表","数据传输"};
    List<Fragment> fragmentList=new ArrayList<>();

    DeviceListFragment deviceListFragment;
    DataTransFragment dataTransFragment;
    OperateFragment operateFragment;
    DoneFragment doneFragment;

    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice remoteDevice;
    boolean connected = false;
    private String deviceName;

    BluetoothLeService bluetoothLeService;
    private List<BluetoothGattCharacteristic> gattCharacteristics = new ArrayList<BluetoothGattCharacteristic>();

    private String baseURL = "http://rem.lomis.cn/rem/post2.jspa";

    private String getURL = "http://rem.lomis.cn/rem/datas.jspa";

    Handler uiHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case Params.MSG_CONNECT_TO_SERVER:
                    BluetoothDevice serverDevice = (BluetoothDevice) msg.obj;
                    connectServer(serverDevice);
                    viewPager.setCurrentItem(1);
                    break;
                case Params.MSG_SET_DONE:
                    Data data = (Data) msg.obj;
                    writeData(data.getDatas());
                    doneFragment.setText(data.getHour(), data.getMinute());
                    viewPager.setCurrentItem(2);
                    break;
                case Params.MSG_REV_DATA:
                    break;
                case Params.MSG_NET_SEND:
                    String netData = (String)msg.obj;
                    netSend(netData);
                    break;
                case Params.MSG_WRITE_DATA:
                    List<MsgData> datas = (List<MsgData>) msg.obj;
                    if(writeData(datas)) {
                        toast("操作成功");
                    } else {
                        toast("操作失败");
                    }
                    break;
                case Params.MSG_TOAST_DATA:
                    toast((String)msg.obj);
                    break;
                case Params.MSG_TEST :

                    new Thread(){
                        @Override
                        public void run() {
                            sendTest();
                        }
                    }.start();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        initUI();
        //registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
    }

    /**
     * 返回 uiHandler
     *
     * @return
     */
    public Handler getUiHandler(){
        return uiHandler;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * 初始化界面
     */
    private void initUI() {
        viewPager= (ViewPager) findViewById(R.id.view_pager);

        deviceListFragment=new DeviceListFragment();
        operateFragment = new OperateFragment();
        dataTransFragment=new DataTransFragment();
        doneFragment = new DoneFragment();
        fragmentList.add(deviceListFragment);
        fragmentList.add(operateFragment);
        fragmentList.add(dataTransFragment);
        fragmentList.add(doneFragment);

        pagerAdapter=new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
    }

    /**
     * ViewPager 适配器
     */
    public class MyPagerAdapter extends FragmentPagerAdapter{
        public MyPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return titleList[position];
        }
    }

    /**
     * Toast 提示
     */
    public void toast(String str){
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    /**
     * 服务绑定回调
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                Log.e("msg", "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            bluetoothLeService.connect(remoteDevice.getAddress());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    /**
     * Handles various events fired by the Service.
     * ACTION_GATT_CONNECTED: connected to a GATT server.
     * ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
     * ACTION_DATA_AVAILABLE: received data from the device.
     * This can be a result of read or notification operations.
     */
    public final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (bluetoothLeService != null) {
                final String action = intent.getAction();
                if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                    connected = true;
                    updateConnectionState("已连接");
                } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                    connected = false;
                    updateConnectionState("已断开");
                } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                    // 显示用户界面上所有支持的服务和特性。
                    //getAllGattServices(bluetoothLeService.getSupportedGattServices());
                    bluetoothLeService.setCharacteristicNotification(true);
                } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                    // 收到数据
                    displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                }
            }
        }
    };

    private void getAllGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            updateConnectionState("无可用服务");
            return;
        }
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> bluetoothGattCharacteristics = gattService.getCharacteristics();
            if (bluetoothGattCharacteristics != null && bluetoothGattCharacteristics.size() > 0) {
                gattCharacteristics.addAll(bluetoothGattCharacteristics);
            }
        }
    }

    public void connection () {
        registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
        if (bluetoothLeService != null) {
            boolean result = bluetoothLeService.connect(remoteDevice.getAddress());
            if (result) {
                updateConnectionState("已连接");
            } else {
                updateConnectionState("连接失败");
            }
            Log.d("msg", "Connect request result=" + result);
        }
    }

    public IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    /**
     * 客户端连接服务器端设备后，显示
     * @param serverDevice
     */
    public void connectServer(BluetoothDevice serverDevice) {
        this.remoteDevice = serverDevice;
        if (serverDevice.getName() == null || "".equals(serverDevice.getName().trim())) {
            this.deviceName = "未命名设备";
        } else {
            this.deviceName = serverDevice.getName();
        }
        updateConnectionState("未连接");
		registerReceiver(gattUpdateReceiver, makeGattUpdateIntentFilter());
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    /**
     * 更新当前设备状态
     * @param state
     */
    public void updateConnectionState(final String state) {
        if (remoteDevice != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                setTitle(deviceName + "(" + state + ")");
                }
            });
        }
    }

    public int[] bin2hex(String str) {
        str = str.trim();
        if (str.length() > 0) {
            String[] strs = str.split(" ");
            int[] data = new int[strs.length];
            for (int i = 0; i < strs.length; i++) {
                data[i] = Integer.parseInt(strs[i], 16);
            }
            return data;
        }
        return null;
    }

    public static byte[] hex2byte(byte[] b) {
        if ((b.length % 2) != 0) {
            throw new IllegalArgumentException("长度不是偶数");
        }
        byte[] b2 = new byte[b.length / 2];
        for (int n = 0; n < b.length; n += 2) {
            String item = new String(b, n, 2);
            // 两位一组，表示一个字节,把这样表示的16进制字符串，还原成一个进制字节
            b2[n / 2] = (byte) Integer.parseInt(item, 16);
        }
        b = null;
        return b2;
    }

    /**
     * 写入数据
     * @param datas
     * @return
     */
    public boolean writeData (List<MsgData> datas) {
        for (int i = 0; i < datas.size(); i++) {
            MsgData data = datas.get(i);
            if (i > 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            bluetoothLeService.writeData(hex2byte(data.toString().getBytes()));
        }
        return  true;
    }

    /**
     * 显示连接远端(客户端)设备
     */
    public void receiveClient(BluetoothDevice clientDevice) {
        this.remoteDevice = clientDevice;
    }

    /**
     * 显示获取的数据
     * @param dataStr
     */
    private void displayData(final String dataStr) {
        if (dataStr != null && dataStr.length() > 0) {
            if (!dataStr.startsWith("55")) {
                return;
            }
            //byte[] databyte = hex2byte(dataStr.getBytes());
            final int[] data = bin2hex(dataStr);
            if (data != null && data.length >=9) {
                for (int i = 0; i < data.length; i+=9) {
                    if (data[i] == 0x55 && data[i + 8] == 0xaa) {
                        Integer dataAll = 0;
                        for (int j = 2; j < 7; j++) {
                            dataAll+=data[i + j];
                        }
                        if ((dataAll % 0xff) == data[i + 7]) {
                            if (data[i + 1] == 0x01) { // 设备时间
                                String time = "20" + String.valueOf(data[i + 2]) + "年"
                                        + String.format("%02d", (data[i + 3] >> 5)) + "月"
                                        + String.format("%02d", data[i + 4]) + "日 "
                                        + String.format("%02d", data[i + 3] & 0x1f) + ":"
                                        + String.format("%02d", data[i + 5]) + ":"
                                        + String.format("%02d", data[i + 6]);
                                operateFragment.setDevTime(time);
                            }
                        }
                    }
                }
            }
            new Thread(){
                @Override
                public void run() {
                   netSend(dataStr);
                }
            }.start();
        }
    }

    private void sendTest() {
        try {
            HttpGet httpGet = new HttpGet(getURL);
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(httpGet);
            HttpEntity httpEntity = response.getEntity();
            InputStream inputStream = httpEntity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String result = "";
            String line = "";
            while (null != (line = reader.readLine())) {
                result += line;
            }
            ResultData resultData = JSON.parseObject(result, ResultData.class);
            List<String> list = ((JSONArray) resultData.getData()).toJavaList(String.class);
            if (list != null && list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    bluetoothLeService.writeData(hex2byte(list.get(i).trim().replaceAll(" ", "").getBytes()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void netSend (String data) {
        try {
            NameValuePair nvp = new BasicNameValuePair("data", data);
            List<NameValuePair> pairList = new ArrayList<NameValuePair>();
            pairList.add(nvp);
            HttpEntity requestHttpEntity = new UrlEncodedFormEntity(pairList);
            HttpPost httpPost = new HttpPost(baseURL);
            httpPost.setEntity(requestHttpEntity);
            HttpClient httpClient = new DefaultHttpClient();
            HttpResponse response = httpClient.execute(httpPost);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
