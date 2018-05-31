package cn.lomis.data;

public class Params {

    public static final int MY_PERMISSION_REQUEST_CONSTANT = 12;
    public static final int REQUEST_ENABLE_BT = 11;
    public static final int REQUEST_ENABLE_VISIBILITY = 22;

    /*public static final String UUID = "00001101-0000-1000-8000-00805F9B34FB";
    public static final String NAME = "RemBluetooth";
    public static final int MSG_REV_A_CLIENT = 33;
    public static final int MSG_SERVER_REV_NEW = 44;
    public static final int ME = 999;
    public static final int REMOTE = 998;
    public static final int MSG_SERVER_WRITE_NEW = 346;
    public static final int MSG_CLIENT_REV_NEW = 347;
    public static final int MSG_CLIENT_WRITE_NEW = 348;*/

    /** 连接到服务 */
    public static final int MSG_CONNECT_TO_SERVER = 1001;

     /** 断开连接 */
    public static final int MSG_DISCONNECT_TO_SERVER = 1002;

    /** 发送数据 */
    public static final int MSG_WRITE_DATA = 1101;

    /** 接收到数据 */
    public static final int MSG_REV_DATA = 1102;

    /** read data */
    public static final int MSG_READ_DATA = 1103;

    public static final int MSG_SET_DONE = 1104;
    public static final int MSG_NET_SEND = 1105;

    /** 提示消息 */
    public static final int MSG_TOAST_DATA = 1201;

    public static final int MSG_TEST = 1111;
}
