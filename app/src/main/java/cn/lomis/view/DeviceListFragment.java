package cn.lomis.view;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cn.lomis.R;
import cn.lomis.data.JDY_type;
import cn.lomis.data.Params;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;

/**
 * 设备列表页面
 */
public class DeviceListFragment extends Fragment {

    final String TAG = "DeviceListFragment";

    private static final long SCAN_PERIOD = 20000;  // 10m后停止搜索
    private boolean scanning;                       // 搜索状态

    ListView listView;
    MyListAdapter listAdapter;
    List<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();

    BluetoothAdapter bluetoothAdapter;
    MyBtReceiver btReceiver;
    IntentFilter intentFilter;

    MainActivity mainActivity;
    Handler uiHandler;
    byte dev_bid ;

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case Params.MY_PERMISSION_REQUEST_CONSTANT: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 运行时权限已授权
                }
                return;
            }
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        //bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        final BluetoothManager bluetoothManager = (BluetoothManager)getActivity().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAdapter == null) {
            toast("未找到可用蓝牙设备");
            getActivity().finish();
            return;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_bt_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        listView = (ListView) view.findViewById(R.id.device_list_view);
        listAdapter = new MyListAdapter();
        dev_bid = (byte)0x88;//88 是JDY厂家VID码
        listView.setAdapter(listAdapter);
        listAdapter.notifyDataSetChanged();

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        uiHandler = mainActivity.getUiHandler();

    }

    @Override   // activity启动后
    public void onResume() {
        super.onResume();
        // 蓝牙未打开，询问打开
        if (!bluetoothAdapter.isEnabled()) {
            Intent turnOnBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnBtIntent, Params.REQUEST_ENABLE_BT);
        }
        /*intentFilter = new IntentFilter();
        btReceiver = new MyBtReceiver();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        getActivity().registerReceiver(btReceiver, intentFilter);*/

        // 蓝牙已开启
        if (bluetoothAdapter.isEnabled()) {
            showBondDevice();
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 通知 ui 连接的服务器端设备
                BluetoothDevice device = deviceList.get(position);
                Message message = new Message();
                message.what = Params.MSG_CONNECT_TO_SERVER;
                message.obj = device;
                uiHandler.sendMessage(message);
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
        } else {
            //相当于Fragment的onPause
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        //scanDevice(false);
        deviceList.clear();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        scanDevice(false);
        //getActivity().unregisterReceiver(btReceiver);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.enable_visibility:
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                enableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
                startActivityForResult(enableIntent, Params.REQUEST_ENABLE_VISIBILITY);
                break;
            case R.id.discovery:
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                if (Build.VERSION.SDK_INT >= 6.0) {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Params.MY_PERMISSION_REQUEST_CONSTANT);
                }

                scanDevice(true);
                //bluetoothAdapter.startDiscovery();
                break;
            case R.id.disconnect:
                bluetoothAdapter.disable();
                deviceList.clear();
                listAdapter.notifyDataSetChanged();
                toast("蓝牙已关闭");
                break;
        }
        return super.onOptionsItemSelected(item);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case Params.REQUEST_ENABLE_BT: {
                if (resultCode == RESULT_OK) {
                    showBondDevice();
                }
                break;
            }
            case Params.REQUEST_ENABLE_VISIBILITY: {
                if (resultCode == 600) {
                    toast("蓝牙已设置可见");
                } else if (resultCode == RESULT_CANCELED) {
                    toast("蓝牙设置可见失败,请重试");
                }
                break;
            }
        }
    }

    /**
     * 用户打开蓝牙后，显示已绑定的设备列表
     */
    private void showBondDevice() {
        deviceList.clear();
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice d : bondedDevices) {
            deviceList.add(d);
        }
        listAdapter.notifyDataSetChanged();
    }

    /**
     * Toast 提示
     */
    public void toast(String str) {
        Toast.makeText(getContext(), str, Toast.LENGTH_SHORT).show();
    }

    /**
     * 设备列表的adapter
     */
    private class MyListAdapter extends BaseAdapter {
        public MyListAdapter() {}

        @Override
        public int getCount() {
            return deviceList.size();
        }

        @Override
        public Object getItem(int position) {
            return deviceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder viewHolder;
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(R.layout.layout_item_bt_device, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceName = (TextView) convertView.findViewById(R.id.device_name);
                viewHolder.deviceMac = (TextView) convertView.findViewById(R.id.device_mac);
                viewHolder.deviceState = (TextView) convertView.findViewById(R.id.device_state);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            int code = deviceList.get(position).getBondState();
            String name = deviceList.get(position).getName();
            String mac = deviceList.get(position).getAddress();
            String state;
            if (name == null || name.length() == 0) {
                name = "未命名设备";
            }
            if (code == BluetoothDevice.BOND_BONDED) {
                state = "ready";
                viewHolder.deviceState.setTextColor(getResources().getColor(R.color.green));
            } else {
                state = "new";
                viewHolder.deviceState.setTextColor(getResources().getColor(R.color.red));
            }
            if (mac == null || mac.length() == 0) {
                mac = "未知 mac 地址";
            }
            viewHolder.deviceName.setText(name);
            viewHolder.deviceMac.setText(mac);
            viewHolder.deviceState.setText(state);
            return convertView;
        }

    }

    /**
     * 与 adapter 配合的 viewholder
     */
    static class ViewHolder {
        public TextView deviceName;
        public TextView deviceMac;
        public TextView deviceState;
    }

    /**
     * 广播接受器
     */
    private class MyBtReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                toast("开始搜索 ...");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //toast("搜索结束");
            } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                //scanDevice(true);
               BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (isNewDevice(device)) {
                    Log.i("device:", String.valueOf(device.getType()) + ":" + device.getName() + ":" +  device.getUuids());
                    deviceList.add(device);
                    listAdapter.notifyDataSetChanged();
                }
            }
        }
    }

    /**
     * 判断搜索的设备是新蓝牙设备，且不重复
     * @param device
     * @return
     */
    private boolean isNewDevice(BluetoothDevice device){
        boolean repeatFlag = false;
        for (BluetoothDevice d : deviceList) {
            if (d.getAddress().equals(device.getAddress())){
                repeatFlag=true;
            }
        }
        //不是已绑定状态，且列表中不重复
        return device.getBondState() != BluetoothDevice.BOND_BONDED && !repeatFlag;
    }


    /**
     * 扫描设备
     * @param enable
     */
    private void scanDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    scanning = false;
                    bluetoothAdapter.stopLeScan(scanCallback);
                }
            }, SCAN_PERIOD);
            scanning = true;
            bluetoothAdapter.startLeScan(scanCallback);
        } else {
            scanning = false;
            bluetoothAdapter.stopLeScan(scanCallback);
        }
    }

    /**
     * 设备搜索回调
     */
    private BluetoothAdapter.LeScanCallback scanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            //if (scanRecord[13] == dev_bid) {
            JDY_type m_tyep = dv_type(scanRecord);
            if (m_tyep != JDY_type.UNKW && m_tyep != null) {
                uiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isNewDevice(device)) {
                            deviceList.add(device);
                            listAdapter.notifyDataSetChanged();
                        }
                    }
                });

            }
        }
    };

    /// ==========================================================================================

    public JDY_type dv_type(byte[] p) {
        if (p.length != 62) {
            return null;
        } else {
            byte m1 = (byte)(p[20] + 1 ^ 17);
            byte m2 = (byte)(p[19] + 1 ^ 34);
            boolean ib1_major = false;
            boolean ib1_minor = false;
            if (p[52] == -1 && p[53] == -1) {
                ib1_major = true;
            }

            if (p[54] == -1 && p[55] == -1) {
                ib1_minor = true;
            }

            if (p[5] == -32 && p[6] == -1 && p[11] == m1 && p[12] == m2 && dev_bid == p[13]) {
                if (p[14] == -96) {
                    return JDY_type.JDY;
                } else if (p[14] == -91) {
                    return JDY_type.JDY_AMQ;
                } else if (p[14] == -79) {
                    return JDY_type.JDY_LED1;
                } else if (p[14] == -78) {
                    return JDY_type.JDY_LED2;
                } else {
                    return p[14] == -60 ? JDY_type.JDY_KG : JDY_type.JDY;
                }
            } else if (p[44] == 16 && p[45] == 22 && (ib1_major || ib1_minor)) {
                return JDY_type.sensor_temp;
            } else if (p[44] == 16 && p[45] == 22) {
                if (p[57] == -32) {
                    return JDY_type.JDY_iBeacon;
                } else if (p[57] == -31) {
                    return JDY_type.sensor_temp;
                } else if (p[57] == -30) {
                    return JDY_type.sensor_humid;
                } else if (p[57] == -29) {
                    return JDY_type.sensor_temp_humid;
                } else if (p[57] == -28) {
                    return JDY_type.sensor_fanxiangji;
                } else if (p[57] == -27) {
                    return JDY_type.sensor_zhilanshuibiao;
                } else if (p[57] == -26) {
                    return JDY_type.sensor_dianyabiao;
                } else if (p[57] == -25) {
                    return JDY_type.sensor_dianliu;
                } else if (p[57] == -24) {
                    return JDY_type.sensor_zhonglian;
                } else {
                    return p[57] == -23 ? JDY_type.sensor_pm2_5 : JDY_type.JDY_iBeacon;
                }
            } else {
                return JDY_type.UNKW;
            }
        }
    }
}
