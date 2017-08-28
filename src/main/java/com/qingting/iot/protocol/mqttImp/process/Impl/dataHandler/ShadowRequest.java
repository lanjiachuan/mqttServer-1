package com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class ShadowRequest{
	private String method;
	private State state;
	private Long version;
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public State getState() {
		return state;
	}
	public void setState(State state) {
		this.state = state;
	}
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}
	
	private RequestType requestType;
	
	public RequestType getRequestType() {
		return requestType;
	}
	public ShadowResponse verifyRequest(String jsonString){
		boolean containDesired=false;
		//boolean containReported=false;
		
		if(method.equals("get")){
			requestType=RequestType.GET;//设备主动获取影子内容
			return null;
		}else if(method==null && state==null && version==null){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.CONTENT_NULL), null);
		}else if(method==null){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.MISS_METHOD), null);
		}else if(state==null){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.MISS_STATE), null);
		}else if(version==null){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.VERSION_CONFLICT), null);
		}else if(!method.equals("update") && !method.equals("get")){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.METHOD_INVALID), null);
		}else if(method.equals("update") && 
				!( containDesired=((JSONObject)JSON.parseObject(jsonString).get("state")).containsKey("desired") )
					&& state.getReported()==null){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.MISS_REPORTED), null);
		}else if(state.getReported()!=null && state.getReported().isEmpty()){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.REPORTED_NULL), null);
		}else if(state.getReported()!=null && !state.getReported().isEmpty()){
			requestType=RequestType.REPORTED;//设备上报状态
			return null;
		}else if(state.getDesired()!=null && !state.getDesired().isEmpty()){
			requestType=RequestType.CONTROL;//服务器主动改变设备状态
			return null;
		}else if(state.getReported()==null && containDesired){
			requestType=RequestType.UPEND;//设备响应服务器状态改变
			return null;
		}
		
		
		return null;
	}
	@Override
	public String toString() {
		return "ShadowRequest [method=" + method + ", state=" + state + ", version=" + version + ", requestType="
				+ requestType + "]";
	}
	
	/*public ShadowRequest(String jsonString){
		parserJsonToUpdate(jsonString);
	}
	
	private void parserJsonToUpdate(String jsonString){
		JSONObject jsStr = JSON.parseObject(jsonString);
		method=(String) jsStr.get("method");
		state=(JSONObject) jsStr.get("state");
		version=jsStr.getLong("version");
	}
	
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	public JSONObject getState() {
		return state;
	}
	public void setState(JSONObject state) {
		this.state = state;
	}
	public Long getVersion() {
		return version;
	}
	public void setVersion(Long version) {
		this.version = version;
	}
	//以下为判断和流程处理用
	private JSONObject desired;
	private JSONObject reported;
	
	private boolean nullOfDesired=true;//state不存在desired属性
	private boolean emptyOfDesired=false;//state存在desired属性，但desired中无属性
	private boolean nullOfReported=true;
	
	public JSONObject getDesired() {
		return desired;
	}
	
	public JSONObject getReported() {
		return reported;
	}
	
	
	public boolean isNullOfDesired() {
		return nullOfDesired;
	}
	
	public boolean isEmptyOfDesired() {
		return emptyOfDesired;
	}

	public boolean isNullOfReported() {
		return nullOfReported;
	}

	public ShadowResponse parserState(){
		//解析desired
		
		//判断desired和reported
		boolean nullOfDesired=state.containsKey("desired");
		boolean nullOfReported=state.containsKey("reported");
		if( (nullOfDesired && nullOfReported) || (!nullOfDesired && !nullOfReported) )
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.MISS_REPORTED), null);
		if(!nullOfDesired){
			Object obj=state.get("desired");
			if(obj==null || obj.equals("null")){
				emptyOfDesired=true;
			}else{
				try{
					desired=(JSONObject)state.get("desired");
				}catch(Exception e){
					return new ShadowResponse("reply").
							updateShadowResponse(new Get("error",ShadowError.DESIRED_FORMAT_ERROR), null);
				}
			}
		}
		
		if(!nullOfReported){
			try{
				reported=(JSONObject) state.get("reported");
			}catch(Exception e){
				return new ShadowResponse("reply").
						updateShadowResponse(new Get("error",ShadowError.REPORTED_FORMAT_ERROR), null);
			}
		}
			
		string=(String)state.get("desired");
		if(string==null || string.equals("null")){
			emptyOfDesired=true;
		}else{
			try{
				desired=(JSONObject)state.get("desired");
			}catch(Exception e){
				return new ShadowResponse("reply").
						updateShadowResponse(new Get("success",ShadowError.DESIRED_FORMAT_ERROR), null);
			}
		}
		//解析reported
		try{
			reported=(JSONObject) state.get("reported");
		}catch(Exception e){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("success",ShadowError.REPORTED_FORMAT_ERROR), null);
		}
		return null;
	}*/
	/*@Override
	public String toString() {
		return "ShadowRequest [method=" + method + ", state=" + state + ", version=" + version + "]";
	}*/
	
}
