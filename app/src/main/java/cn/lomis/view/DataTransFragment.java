package cn.lomis.view;

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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import cn.lomis.R;
import cn.lomis.service.BluetoothLeService;
import cn.lomis.data.MsgData;
import cn.lomis.data.Params;
import com.weiwangcn.betterspinner.library.BetterSpinner;

import java.util.ArrayList;
import java.util.List;


/**
 * 数据传输页面
 */
public class DataTransFragment extends Fragment {

    /* 功能组件 */
    //private TextView connectNameTv;
    private Spinner  spinner;    // 波形
    private SeekBar  rate;       // 频率
    private SeekBar  power;      // 功率
    private TextView rateShow;  // 频率展示
    private TextView powerShow; // 功率展示
    private BetterSpinner timeHour;
    private BetterSpinner  timeMinute;
    private BetterSpinner  timerOneHour;
    private BetterSpinner  timerOneMinute;
    private Switch   timerOneSwith;
    private BetterSpinner  timerTwoHour;
    private BetterSpinner  timerTwoMinute;
    private Switch   timerTwoSwith;
    private BetterSpinner  timerThreeHour;
    private BetterSpinner  timerThreeMinute;
    private Switch   timerThreeSwith;
    private BetterSpinner  timerFourHour;
    private BetterSpinner  timerFourMinute;
    private Switch   timerFourSwith;
    private BetterSpinner  timerFiveHour;
    private BetterSpinner  timerFiveMinute;
    private Switch   timerFiveSwith;
    private Button   modifyTimeBt;    // 修改事件
    private Button   sendBt;          // 设置按钮

    MainActivity mainActivity;
    Handler uiHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_data_trans, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //connectNameTv = (TextView) view.findViewById(R.id.device_name_tv);
        spinner = (Spinner) view.findViewById(R.id.spinner);
        rate    = (SeekBar) view.findViewById(R.id.rate_val);
        rateShow = (TextView) view.findViewById(R.id.rate_show);
        rate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override // 进度条发生改变
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                rateShow.setText(String.valueOf(progress));
            }
            @Override // 开始拖动时候
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override // 停止拖动
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        power   = (SeekBar) view.findViewById(R.id.power_val);
        powerShow = (TextView) view.findViewById(R.id.power_show);
        power.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
                powerShow.setText(String.valueOf(progress));
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        String[] hourList = getResources().getStringArray(R.array.time_hour);
        ArrayAdapter<String> adapterHour = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, hourList);
        String[] minuteList = getResources().getStringArray(R.array.time_minute);
        ArrayAdapter<String> adapterMinute = new ArrayAdapter<String>(getContext(), android.R.layout.simple_dropdown_item_1line, minuteList);
        timeHour        = (BetterSpinner)view.findViewById(R.id.time_val_hour);             timeHour.setAdapter(adapterHour);
        timeMinute      = (BetterSpinner) view.findViewById(R.id.time_val_minute);          timeMinute.setAdapter(adapterMinute);
        timerOneHour    = (BetterSpinner) view.findViewById(R.id.timer_one_val_hour);       timerOneHour.setAdapter(adapterHour);
        timerOneMinute  = (BetterSpinner) view.findViewById(R.id.timer_one_val_minute);     timerOneMinute.setAdapter(adapterMinute);
        timerOneSwith   = (Switch) view.findViewById(R.id.timer_one_switch);
        timerTwoHour    = (BetterSpinner) view.findViewById(R.id.timer_two_val_hour);       timerTwoHour.setAdapter(adapterHour);
        timerTwoMinute  = (BetterSpinner) view.findViewById(R.id.timer_two_val_minute);     timerTwoMinute.setAdapter(adapterMinute);
        timerTwoSwith   = (Switch) view.findViewById(R.id.timer_two_switch);;
        timerThreeHour  = (BetterSpinner) view.findViewById(R.id.timer_three_val_hour);     timerThreeHour.setAdapter(adapterHour);
        timerThreeMinute= (BetterSpinner) view.findViewById(R.id.timer_three_val_minute);   timerThreeMinute.setAdapter(adapterMinute);
        timerThreeSwith = (Switch) view.findViewById(R.id.timer_three_switch);;
        timerFourHour   = (BetterSpinner) view.findViewById(R.id.timer_four_val_hour);      timerFourHour.setAdapter(adapterHour);
        timerFourMinute = (BetterSpinner) view.findViewById(R.id.timer_four_val_minute);    timerFourMinute.setAdapter(adapterMinute);
        timerFourSwith  = (Switch) view.findViewById(R.id.timer_four_switch);;
        timerFiveHour   = (BetterSpinner) view.findViewById(R.id.timer_five_val_hour);      timerFiveHour.setAdapter(adapterHour);
        timerFiveMinute = (BetterSpinner) view.findViewById(R.id.timer_five_val_minute);    timerFiveMinute.setAdapter(adapterMinute);
        timerFiveSwith  = (Switch) view.findViewById(R.id.timer_five_switch);;
        modifyTimeBt    = (Button) view.findViewById(R.id.modify_time_bt);  // 修改事件
        modifyTimeBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String timeHourVal    = timeHour.getText().toString(); //timeHour.getSelectedItem().toString();
                String timeMinuteVal = timeMinute.getText().toString();
                Log.i("timeOurVal:", timeHourVal);
                Log.i("timeMinuteVal:", timeMinuteVal);
                MsgData data = new MsgData();
                data.setFunction(Integer.parseInt("01", 16));
                data.setHour(Integer.valueOf(timeHourVal));
                data.setMinute(Integer.valueOf(timeMinuteVal));
                Message message = new Message();
                message.what = Params.MSG_WRITE_DATA;
                message.obj = data;
                uiHandler.sendMessage(message);
            }
        });

        sendBt = (Button) view.findViewById(R.id.send_bt);
        sendBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*int waveVal     = spinner.getSelectedItemPosition();
                String rateVal  = rateShow.getText().toString();
                String powerVal = powerShow.getText().toString();

                String timerOneValHour     = timerOneHour.getText().toString();
                String timerOneValMinute   = timerOneMinute.getText().toString();
                boolean timerOneValSwith   = timerOneSwith.isSelected();
                String timerTwoValHour     = timerTwoHour.getText().toString();
                String timerTwoValMinute   = timerTwoMinute.getText().toString();
                boolean timerTwoValSwith   = timerTwoSwith.isSelected();
                String timerThreeValHour   = timerThreeHour.getText().toString();
                String timerThreeValMinute = timerThreeMinute.getText().toString();
                boolean timerThreeValSwith = timerThreeSwith.isSelected();
                String timerFourValHour    = timerFourHour.getText().toString();
                String timerFourValMinute  = timerFourMinute.getText().toString();
                boolean timerFourValSwith  = timerFourSwith.isSelected();
                String timerFiveValHour    = timerFiveHour.getText().toString();
                String timerFiveValMinute  = timerFiveMinute.getText().toString();
                boolean timerFiveValSwith  = timerFiveSwith.isSelected();

                for (int i = 0; i < 5; i++) {
                    MsgData data = new MsgData();
                    data.setFunction(i + 1);
                    switch (i) {
                        case 0:
                            data.setHour(Integer.valueOf(timerOneValHour));
                            data.setMinute(Integer.valueOf(timerOneValMinute));
                            break;
                        case 1:
                            data.setHour(Integer.valueOf(timerTwoValHour));
                            data.setMinute(Integer.valueOf(timerTwoValMinute));
                            break;
                        case 2:
                            data.setHour(Integer.valueOf(timerThreeValHour));
                            data.setMinute(Integer.valueOf(timerThreeValMinute));
                            break;
                        case 3:
                            data.setHour(Integer.valueOf(timerFourValHour));
                            data.setMinute(Integer.valueOf(timerFourValMinute));
                            break;
                        case 4:
                            data.setHour(Integer.valueOf(timerFiveValHour));
                            data.setMinute(Integer.valueOf(timerFiveValMinute));
                            break;
                    }
                    data.setWave(Integer.valueOf(waveVal + 1));
                    data.setRate(Integer.valueOf(rateVal));
                    data.setPower(Integer.valueOf(powerVal));

                    List<MsgData> datas = new ArrayList<>();
                    datas.add(data);
                    Message message = new Message();
                    message.what = Params.MSG_WRITE_DATA;
                    message.obj = datas;
                    uiHandler.sendMessage(message);
                    //mainActivity.writeData(datas);
                }
                mainActivity.toast("操作完成");*/
                Message message = new Message();
                message.what = Params.MSG_TEST;
                uiHandler.sendMessage(message);
            }
        });

       /* dataListAdapter = new ArrayAdapter<String>(getContext(), R.layout.layout_item_new_data);
       showDataLv.setAdapter(dataListAdapter);*/

    }

   @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        //menu.add("测试").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        menu.add("连接").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String title = item.getTitle().toString();
        /*if ("测试".equals(title)) {
            MsgData data = new MsgData();
            Message message = new Message();
            message.what = Params.MSG_WRITE_DATA;
            message.obj = writeData(data);
            uiHandler.sendMessage(message);
        } else */if ("连接".equals(title)) {
            if (!mainActivity.connected) {
                mainActivity.connection();
            }
            //item.setTitle("断开");
        } else if ("断开".equals(title)) {
            mainActivity.updateConnectionState("已断开");
            item.setTitle("连接");
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mainActivity = (MainActivity) getActivity();
        uiHandler    = mainActivity.getUiHandler();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(mainActivity.gattUpdateReceiver, mainActivity.makeGattUpdateIntentFilter());
        mainActivity.connection();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}