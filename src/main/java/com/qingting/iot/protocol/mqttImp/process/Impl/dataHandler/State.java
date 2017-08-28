package com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler;

import com.alibaba.fastjson.JSONObject;

public class State {
	private JSONObject desired;
	private JSONObject reported;
	public JSONObject getDesired() {
		return desired;
	}
	public void setDesired(JSONObject desired) {
		this.desired = desired;
	}
	public JSONObject getReported() {
		return reported;
	}
	public void setReported(JSONObject reported) {
		this.reported = reported;
	}
	@Override
	public String toString() {
		return "State [desired=" + desired + ", reported=" + reported + "]";
	}
	
}
