package cn.lomis.data;

import java.io.Serializable;

public class ResultData implements Serializable {

	private static final long serialVersionUID = 1L;

	private int code;	// 结果状态码
	private String message = "请求成功";
	private Object data;	// 结果数据集
	
	public ResultData() {

	}

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}


}
