package cn.lomis.view;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.os.Message;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import cn.lomis.R;
import cn.lomis.data.Data;
import cn.lomis.data.MsgData;
import cn.lomis.data.Params;
import cn.lomis.utils.DateUtil;

public class OperateFragment extends Fragment {

    /* 功能组件 */
    private Spinner  powerLevel;            // 功率等级
    private Spinner  frequencyLevel;        // 频率等级
    private TextView timeDevVal;            // 设备时间
    private TextView timeBjVal;             // 北京时间
    private TextView timeSleepVal;          // 睡觉时间
    private TextView timeWakeupVal;         // 起床时间
    private RadioButton modelVal;           // 模式
    private Button powerTest;               // 测试按钮
    private Button timeGet;                 // 获取时间按钮
    private Button timeSet;                 // 设置时间按钮
    private Button confirm;                 // 设置按钮

    MainActivity mainActivity;
    Handler uiHandler;
    boolean setTime = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.layout_operate, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        //**********************************************************************
        // 设置组件
        //**********************************************************************
        powerLevel     = (Spinner)view.findViewById(R.id.operate_power_val);
        frequencyLevel = (Spinner)view.findViewById(R.id.operate_frequency_val);

        timeDevVal = (TextView)view.findViewById(R.id.operate_time_dev_val);
        timeBjVal  = (TextView)view.findViewById(R.id.operate_time_bj_val);

        modelVal = (RadioButton) view.findViewById(R.id.model_one);
        RadioGroup radioGroup = (RadioGroup)view.findViewById(R.id.operate_model);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                modelVal = (RadioButton) group.findViewById(checkedId);
            }
        });

        timeDevVal = (TextView)view.findViewById(R.id.operate_time_dev_val);
        final TextView operateBjTimeVal = (TextView)view.findViewById(R.id.operate_time_bj_val);
        Calendar mCalendar = Calendar.getInstance();
        operateBjTimeVal.setText(DateUtil.formatDate(mCalendar.getTime()));

        timeSleepVal = (TextView) view.findViewById(R.id.operate_time_sleep_val);
        timeSleepVal.setOnClickListener(new View.OnClickListener() {
            final Calendar mCalendar1 = Calendar.getInstance();
            @Override
            public void onClick(View v) {
                TimePickerDialog dialog = new TimePickerDialog(mainActivity, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int h, int m) {
                        mCalendar1.set(Calendar.HOUR_OF_DAY, h);
                        mCalendar1.set(Calendar.MINUTE, m);
                        long time = mCalendar1.getTime().getTime();
                        if (time < System.currentTimeMillis()){
                            time += (24 * 60 * 60 * 1000);
                        }
                        String wakeupTime = timeWakeupVal.getText().toString();
                        if (!"点击设置".equals(wakeupTime)) {
                            long wtime = DateUtil.getLongDate(wakeupTime);
                            if (time > wtime) {
                                time -= (24 * 60 * 60 * 1000);
                            }
                        }
                        timeSleepVal.setText(DateUtil.formatDate(new Date(time)));
                    }
                }, mCalendar1.get(Calendar.HOUR_OF_DAY), mCalendar1.get(Calendar.MINUTE), true);
                dialog.show();
            }
        });

        timeWakeupVal = (TextView) view.findViewById(R.id.operate_time_wakeup_val);
        timeWakeupVal.setOnClickListener(new View.OnClickListener() {
            final Calendar mCalendar2 = Calendar.getInstance();
            @Override
            public void onClick(View v) {
                TimePickerDialog dialog = new TimePickerDialog(mainActivity, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int h, int m) {
                        mCalendar2.set(Calendar.HOUR_OF_DAY, h);
                        mCalendar2.set(Calendar.MINUTE, m);
                        long time = mCalendar2.getTime().getTime();
                        if (time < System.currentTimeMillis()){
                            time += (24 * 60 * 60 * 1000);
                        }
                        String sleepTime = timeSleepVal.getText().toString();
                        if (!"点击设置".equals(sleepTime)) {
                            long stime = DateUtil.getLongDate(sleepTime);
                            if (stime > time) {
                                time += (24 * 60 * 60 * 1000);
                            }
                        }
                        timeWakeupVal.setText(DateUtil.formatDate(new Date(time)));
                    }
                }, mCalendar2.get(Calendar.HOUR_OF_DAY), mCalendar2.get(Calendar.MINUTE), true);
                dialog.show();
            }
        });

        //**********************************************************************
        // 组件监听
        //**********************************************************************
        // 功率测试
        powerTest = (Button)view.findViewById(R.id.operate_power_btn);
        powerTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MsgData data = new MsgData();
                data.setWave(0x01);
                String powerLevelVal = powerLevel.getSelectedItem().toString();
                //String frequencyLevelVal = frequencyLevel.getSelectedItem().toString();
                data.setPower(getPower(powerLevelVal));
                data.setRate(Integer.parseInt("40"));
                String testVal = powerTest.getText().toString();
                if ("开始测试".equals(testVal)) {
                    data.setFunction(0xF0);
                    powerTest.setText("关闭测试");
                } else if ("关闭测试".equals(testVal)) {
                    data.setFunction(0xF1);
                    powerTest.setText("开始测试");
                }

                List<MsgData> datas = new ArrayList<>();
                datas.add(data);
                Message message = new Message();
                message.what = Params.MSG_WRITE_DATA;
                message.obj = datas;
                uiHandler.sendMessage(message);
            }
        });

        // 获取时间
        timeGet = (Button)view.findViewById(R.id.operate_time_get_btn);
        timeGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MsgData data = new MsgData();
                data.setFunction(0x00);
                List<MsgData> datas = new ArrayList<>();
                datas.add(data);
                Message message = new Message();
                message.what = Params.MSG_WRITE_DATA;
                message.obj = datas;
                uiHandler.sendMessage(message);

            }
        });

        // 设置时间
        timeSet = (Button)view.findViewById(R.id.operate_time_set_btn);
        timeSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long now = System.currentTimeMillis();
                String time = DateUtil.formatDate(new Date(now));
                String year = time.substring(2, 4);
                String month = time.substring(5, 7);
                String day = time.substring(8, 10);
                String hour = time.substring(12, 14);
		        String minute = time.substring(15, 17);

                MsgData data = new MsgData();
                data.setFunction(0x01);
                data.setHour(Integer.parseInt(year));
                data.setMinute(Integer.parseInt(hour) | (Integer.parseInt(month) << 5));
                data.setWave(Integer.parseInt(day));
                data.setRate(Integer.parseInt(minute));
                data.setPower(0);

                List<MsgData> datas = new ArrayList<>();
                datas.add(data);
                Message message = new Message();
                message.what = Params.MSG_WRITE_DATA;
                message.obj = datas;
                uiHandler.sendMessage(message);
            }
        });

        // 设置定时
        confirm = (Button)view.findViewById(R.id.operate_confirm_btn);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message message = new Message();
                String sleepTime = timeSleepVal.getText().toString();
                if ("点击设置".equals(sleepTime)) {
                    message.what = Params.MSG_TOAST_DATA;
                    message.obj = "请设置睡觉时间";
                    uiHandler.sendMessage(message);
                    return ;
                }
                String wakeupTime = timeWakeupVal.getText().toString();
                if ("点击设置".equals(wakeupTime)) {
                    message.what = Params.MSG_TOAST_DATA;
                    message.obj = "请设置起床时间";
                    uiHandler.sendMessage(message);
                    return ;
                }

                message = new Message();
                message.what = Params.MSG_SET_DONE;
                String powerLevelVal = powerLevel.getSelectedItem().toString();
                String frequencyLevelVal = frequencyLevel.getSelectedItem().toString();
                String model = modelVal.getText().toString();
                long timing = System.currentTimeMillis();
                List<MsgData> datas = new ArrayList<>();
                int keepTime = 5;
                for(int i = 0; i < 5; i++) {
                    MsgData data = new MsgData();
                    data.setWave(0x01);
                    data.setPower(getPower(powerLevelVal));
                    data.setRate(getFrequency(frequencyLevelVal));
                    data.setFunction(Integer.parseInt(String.valueOf(i+2)));
                    if (i == 0) {
                        timing = DateUtil.getLongDate(sleepTime);
                        timing +=(1 * 60 * 60 * 1000);
                    } else if (i == 1) {
                        timing = DateUtil.getLongDate(sleepTime);
                        timing +=(2 * 60 * 60 * 1000);
                    } else if (i == 2) {
                        timing = DateUtil.getLongDate(wakeupTime);
                        timing -=(2 * 60 * 60 * 1000);
                    } else if (i == 3) {
                        if ("温和".equals(model)) {
                            timing = DateUtil.getLongDate(wakeupTime);
                            timing -=(1 * 60 * 60 * 1000);
                        } else if ("中等".equals(model)) {
                            keepTime = 10;
                            timing = DateUtil.getLongDate(wakeupTime);
                            timing -=(1 * 60 * 60 * 1000);
                        } else if ("疯狂".equals(model)) {
                            keepTime = 20;
                            timing = DateUtil.getLongDate(wakeupTime);
                            timing -=(1 * 80 * 60 * 1000);
                        }
                    } else if (i == 4) {
                        if ("温和".equals(model)) {
                            timing = DateUtil.getLongDate(wakeupTime);
                            timing -=(1 * 30 * 60 * 1000);
                        } else if ("中等".equals(model)) {
                            timing = DateUtil.getLongDate(wakeupTime);
                            timing -=(1 * 30 * 60 * 1000);
                        } else if ("疯狂".equals(model)) {
                             timing = DateUtil.getLongDate(wakeupTime);
                            timing -=(1 * 40 * 60 * 1000);
                        }
                    }
                    String time = DateUtil.formatDate(new Date(timing));
                    String hour = time.substring(12, 14);
                    String minute = time.substring(15, 17);
                    data.setHour(Integer.parseInt(hour));
                    data.setMinute(Integer.parseInt(minute));
                    datas.add(data);

                }
                MsgData keepData = new MsgData();
                keepData.setFunction(0x0f);
                keepData.setMinute(keepTime);
                datas.add(keepData);

                Data realData = new Data();
                realData.setDatas(datas);
                realData.setHour(sleepTime.substring(12, 14));
                realData.setMinute(sleepTime.substring(15, 17));
                message.obj = realData;
                uiHandler.sendMessage(message);
            }
        });
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            mainActivity.registerReceiver(mainActivity.gattUpdateReceiver, mainActivity.makeGattUpdateIntentFilter());

            if (mainActivity.bluetoothLeService != null) {
                mainActivity.connection();
            }
            //相当于Fragment的onResume
            setTime = true;
            new Thread(){ // 定时任务动态更新时间
                @Override
                public void run() {
                    super.run();
                    do{
                        try {
                            Thread.sleep(1000);
                            Message msg = new Message();
                            msg.what = 1;
                            mHandler.sendMessage(msg);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (setTime);
                }
            }.start();
       } else {
           //相当于Fragment的onPause
           setTime = false;
       }
   }

   @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.add("连接").setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String title = item.getTitle().toString();
        if ("连接".equals(title)) {
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
        //mainActivity.registerReceiver(mainActivity.gattUpdateReceiver, mainActivity.makeGattUpdateIntentFilter());
        //mainActivity.connection();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    // 动态更新本地时间
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case 1:
                    long time = System.currentTimeMillis();
                    timeBjVal.setText(DateUtil.formatDate2(new Date(time)));
                    break;
                default:
                    break;
            }
        }
    };

    private Integer getPower(String powerVal) {
		String power = "10";
		switch (powerVal) {
		case "二级":
			power = "20";
			break;
		case "三级":
			power = "30";
			break;
		case "四级":
			power = "45";
			break;
		case "五级":
			power = "60";
			break;
		case "六级":
			power = "70";
			break;
		case "七级":
			power = "80";
			break;
		case "八级":
			power = "88";
			break;
		case "九级":
			power = "95";
			break;
        case "十级谨慎选择!!!":
			power = "100";
			break;
		default:
			break;
		}
		return Integer.parseInt(power);
    }

    private Integer getFrequency(String frequencyLevelVal) {
        return Integer.parseInt(frequencyLevelVal.replaceAll("HZ", ""));
    }

    /**
     * 设置获取的设备时间
     * @param time
     */
    public void setDevTime(String time) {
        timeDevVal.setText(time);
    }
}