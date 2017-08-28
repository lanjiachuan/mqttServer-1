package com.qingting.iot.protocol.mqttImp.message;

public enum PubAckCode {
	SUCCESS  ((byte)0),
	DATA_VALUE_ERROR ((byte)1),
	VERIFY_ERROR  ((byte)2),
	DATA_LENGTH_ERROR ((byte)3),
	TIME_ERROR ((byte)4);
	
	final public byte code;
	
	PubAckCode(byte code) {
		this.code = code;
	}
	
	/**
	 * 获取类型对应的值
	 * @return int
	 * @author zer0
	 * @version 1.0
	 * @date 2016-3-3
	 */
	public byte getCode() {
		return code;
	}
}
