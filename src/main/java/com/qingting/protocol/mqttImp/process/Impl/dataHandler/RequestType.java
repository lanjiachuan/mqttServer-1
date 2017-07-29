package com.qingting.protocol.mqttImp.process.Impl.dataHandler;

public enum RequestType {
	REPORTED("REPORTED"),//设备上报状态
	CONTROL("CONTROL"),//服务器主动改变设备状态
	UPEND("UPEND"),//设备更新完成
	GET("GET");//设备主动获取影子内容
	
	private String type;
	
	private RequestType(String type){
		this.type=type;
	}
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
}
