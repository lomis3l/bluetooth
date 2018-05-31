package cn.lomis.data;

import android.util.Log;

/**
 * Created by Lomis on 18-3-25.
 */

public class MsgData {
    private int function;   // 功能
    private int hour;
    private int minute;
    private int wave;
    private int rate;
    private int power;
    private int check;

    public int getFunction() {
        return function;
    }

    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getWave() {
        return wave;
    }

    public int getRate() {
        return rate;
    }

    public int getPower() {
        return power;
    }

    public int getCheck() {
        return check;
    }

    public void setFunction(int function) {
        this.function = function;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setWave(int wave) {
        this.wave = wave;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public void setCheck(int check) {
        this.check = check;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("55");
        sb.append(format(Integer.toHexString(this.function)));
        sb.append(format(Integer.toHexString(this.hour)));
        sb.append(format(Integer.toHexString(this.minute)));
        sb.append(format(Integer.toHexString(this.wave)));
        sb.append(format(Integer.toHexString(this.rate)));
        sb.append(format(Integer.toHexString(this.power)));
        sb.append(format(Integer.toHexString((/*this.function + */this.hour + this.minute + this.wave + this.rate + this.power) % 0xff)));
        sb.append("AA");
        Log.i("data:", sb.toString());
        return sb.toString();
    }

    public String format(String string ) {
        if (string.length() > 1) {
            return string;
        } else {
            return "0" + string;
        }
    }
}
