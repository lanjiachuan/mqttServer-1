package com.qingting.protocol.mqttImp.process.Impl.dataHandler;

public class Get {
	private String status;
	private String errorcode;
	private String errormessage;
	
	
	
	public Get(String status, ShadowError shadowError) {
		super();
		this.status = status;
		if(shadowError!=null){
			this.errorcode = shadowError.getErrorcode();
			this.errormessage = shadowError.getErrormessage();
		}
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
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
