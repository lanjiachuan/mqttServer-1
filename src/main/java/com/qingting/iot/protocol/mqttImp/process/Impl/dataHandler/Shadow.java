package com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qingting.iot.util.MqttTool;

public class Shadow implements Serializable{
	
	private final static Logger Log = Logger.getLogger(Shadow.class);
	
	private static final String STORAGE_FILE_PATH =  System.getProperty("user.dir") + File.separator + MqttTool.getProperty("shadow");
	
	private State state;
	private Metadata metadata;
	private Long timestamp;
	private Long version;
	
    public ShadowResponse updateMetadata(ShadowRequest shadowRequest){
    	Log.info("更新前影子文档"+this.toString());
    	Log.info("更新的shadowRequest:"+shadowRequest);
    	//版本判断
    	if(shadowRequest.getVersion()>version){
    		if(shadowRequest.getRequestType().equals(RequestType.REPORTED)){
    			//设备上报状态
    			JSONObject reported=shadowRequest.getState().getReported();
    			for (String attribute : reported.keySet()) {//修改影子文档state和metadata
    				this.state.getReported().put(attribute, reported.get(attribute));
    				this.metadata.getReported().put(attribute, Calendar.getInstance().getTimeInMillis());
				}
    			
    		}else if(shadowRequest.getRequestType().equals(RequestType.CONTROL)){
    			//服务器主动改变设备状态
    			JSONObject desired=shadowRequest.getState().getDesired();
    			for (String attribute : desired.keySet()) {//修改影子文档state和metadata
    				this.state.getDesired().put(attribute, desired.get(attribute));
    				this.metadata.getDesired().put(attribute, Calendar.getInstance().getTimeInMillis());
				}
    		}else if(shadowRequest.getRequestType().equals(RequestType.UPEND)){
    			//设备状态更新结束,清除desired
    			shadowRequest.getState().setDesired(null);
    		}
    		this.version=shadowRequest.getVersion();
    		this.timestamp=Calendar.getInstance().getTimeInMillis();
    		
    		Log.info("更新后影子文档"+this.toString());
	    	return null;
    	}else{
    		return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.VERSION_CONFLICT), null);
    	}
    }
    
	
	public State getState() {
		return state;
	}

	public void setState(State state) {
		this.state = state;
	}

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Long timestamp) {
		this.timestamp = timestamp;
	}

	public Long getVersion() {
		return version;
	}

	public void setVersion(Long version) {
		this.version = version;
	}


	@Override
	public String toString() {
		return "Shadow [state=" + state + ", metadata=" + metadata + ", timestamp=" + timestamp + ", version=" + version
				+ "]";
	}

	/*@Override
	public String toString() {
		return
		"{"+
			"\"state\":"+
				state.toString()+
			","+
			"\"metadata\":"+
				metadata.toString()+
			","+
			"\"timestamp\":"+Calendar.getInstance().getTimeInMillis()+","+
			"\"version\":"+version+
		"}";
	}*/
    
}
