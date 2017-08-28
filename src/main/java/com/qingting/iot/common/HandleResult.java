package com.qingting.iot.common;

import com.qingting.iot.protocol.mqttImp.message.PubAckCode;
import com.smart.mvc.model.ResultCode;

public class HandleResult {
	
	private PubAckCode code;
	private String jsonData;
	
	
	
	public HandleResult() {
		super();
	}
	public HandleResult(PubAckCode code) {
		super();
		this.code = code;
	}
	public PubAckCode getCode() {
		return code;
	}
	public void setCode(PubAckCode code) {
		this.code = code;
	}
	public String getJsonData() {
		return jsonData;
	}
	public void setJsonData(String jsonData) {
		this.jsonData = jsonData;
	}
	
	public boolean isSuccess() {
		return code != null && code.equals(PubAckCode.SUCCESS);
	}
	
	public static HandleResult createSuccessResult(){
		return new HandleResult(PubAckCode.SUCCESS);
	}
	public static HandleResult createFailureResult(PubAckCode code){
		return new HandleResult(code);
	}
}
