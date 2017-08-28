package com.qingting.iot.model;

import com.smart.mvc.model.PersistentObject;

public class Rule extends PersistentObject{

	private static final long serialVersionUID = -8625810203369599314L;
	private String topic;
	private String func;
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getFunc() {
		return func;
	}
	public void setFunc(String func) {
		this.func = func;
	}
	
}
