package cn.lomis.data;

import android.util.Log;

import java.util.List;

/**
 * Created by Lomis on 18-3-25.
 */

public class Data {
    private String hour;   // 功能
    private String minute;
    private List<MsgData> datas;

    public void setHour(String hour) {
        this.hour = hour;
    }

    public String getHour() {
        return hour;
    }

    public void setMinute(String minute) {
        this.minute = minute;
    }

    public String getMinute() {
        return minute;
    }

    public void setDatas(List<MsgData> datas) {
        this.datas = datas;
    }

    public List<MsgData> getDatas() {
        return datas;
    }
}
