package com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler;

import java.util.Calendar;

import com.alibaba.fastjson.JSONObject;


public class ShadowResponse {
	private String method;
	private JSONObject payload;
	private Long timestamp;
	
	public ShadowResponse(String method){
		this.method=method;
		this.timestamp=Calendar.getInstance().getTimeInMillis();
	}
	
	public ShadowResponse updateShadowResponse(Get get,Shadow shadow){
		payload=new JSONObject();
		payload.put("status", get.getStatus());
		
		if(method.equals("reply")){//状态上报回复
			if(get.getStatus().equals("success")){//success
				payload.put("version", shadow.getVersion());
			}else{//error
				JSONObject content=new JSONObject();
				content.put("errorcode", get.getErrorcode());
				content.put("errormessage", get.getErrormessage());
				payload.put("content", content);
			}
		}else if(method.equals("control")){//服务器主动改变状态回复或设备主动获取状态回复
			payload.put("state", shadow.getState());
			
			payload.put("metadata", shadow.getMetadata());
			
			payload.put("version", shadow.getVersion());
			
		}
		return this;
	}
	
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public JSONObject getPayload() {
		return payload;
	}
	public void setPayload(JSONObject payload) {
		this.payload = payload;
	}
	public Long getTimestamp() {
		return timestamp;
	}
	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return  "{"+
					"\"method\":" + method + ","+ 
					"\"payload\":" + payload + ","+
					"\"timestamp\":" + timestamp + 
				"}";
	}
	
	
}
