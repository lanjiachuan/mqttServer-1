package com.qingting.protocol.mqttImp.process.Impl.dataHandler;

public enum ShadowError {
	
	JSON_FORMAT_ERROR("400","不正确的JSON格式"),
	MISS_METHOD("401","影子json缺少method信息"),
	MISS_STATE("402","影子json缺少state字段"),
	VERSION_CLASS_EXCEPTION("403","影子json version不是数字"),
	MISS_REPORTED("404","影子json缺少reported字段"),
	REPORTED_NULL("405","影子json reported属性字段为空"),
	METHOD_INVALID("406","影子json method是无效的方法"),
	CONTENT_NULL("407","影子内容为空"),
	REPORTED_TOO_LARGE("408","影子reported属性个数超限"),
	VERSION_CONFLICT("409","影子版本冲突"),
	DESIRED_FORMAT_ERROR("410","影子json desired格式错误"),
	REPORTED_FORMAT_ERROR("411","影子json reported格式错误"),
	SERVER_EXCEPTION("500","服务端处理异常");
	
	private String errorcode;
	private String errormessage;
	
	private ShadowError(String errorcode,String errormessage){
		this.errorcode=errorcode;
		this.errormessage=errormessage;
	}
	public String getErrorcode() {
		return errorcode;
	}
	public void setErrorcode(String errorcode) {
		this.errorcode = errorcode;
	}
	public String getErrormessage() {
		return errormessage;
	}
	public void setErrormessage(String errormessage) {
		this.errormessage = errormessage;
	}
	
	
}
