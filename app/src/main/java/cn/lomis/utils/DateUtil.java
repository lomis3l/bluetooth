package cn.lomis.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.DateFormat;
import java.text.ParseException;

public class DateUtil {
    /**
     * 根据日期获取当前计算机时间
     *
     * @param dateFormatString
     * @return
     */
    public static Long getLongDate(String dateFormatString) {
        Date resultDate = new Date();
        DateFormat dateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        try {
            if (dateFormatString != null && !"".equals(dateFormatString)) {
                resultDate = dateFormat.parse(dateFormatString);
            }
            return resultDate.getTime();
        } catch (ParseException e) {

        }
        return null;
    }

    public static String formatDate(Date date) {
        if (null == date) {
            return "";
        }
        SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm");
        return myFormatter.format(date);
    }

    public static String formatDate2(Date date) {
        if (null == date) {
            return "";
        }
        SimpleDateFormat myFormatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        return myFormatter.format(date);
    }

}
