package com.qingting.iot.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.qingting.iot.common.RuleEngine;
import com.qingting.iot.protocol.mqttImp.message.PubAckCode;

public class TestRuleEngine implements RuleEngine{
	private static final int dataLength=19;
	private static final boolean verifyFlag=true;
	private static final int verifyByteLength=2;
	@Override
	public HandleResult convertToJsonString(byte[] bytes) {
		
		if(verifyFlag){
			//数据长度验证
			if( (bytes.length-verifyByteLength)%dataLength!=0 ){
				return HandleResult.createFailureResult(PubAckCode.DATA_LENGTH_ERROR);
			}
			//数据和校验
			int sum=0;
			for(int i=0;i<bytes.length-verifyByteLength;i++){
				sum+=(bytes[i]&0xFF);
			}
			System.out.println("求得的和:"+sum);
			System.out.println("传输的校验值:"+( ((bytes[bytes.length-2]&0xFF)<<8) | (bytes[bytes.length-1]&0xFF) ));
			if( sum!=( ((bytes[bytes.length-2]&0xFF)<<8) | (bytes[bytes.length-1]&0xFF) ) ){
				return HandleResult.createFailureResult(PubAckCode.VERIFY_ERROR);
			}
		}else{
			//数据长度验证
			if( bytes.length%dataLength!=0 ){
				return HandleResult.createFailureResult(PubAckCode.DATA_LENGTH_ERROR);
			}
		}
		
		HandleResult result=new HandleResult();
		int dataSize= verifyFlag ? bytes.length-verifyByteLength : bytes.length;
		String jsonDate=new String();
		System.out.println("dataSize:"+dataSize);
		System.out.println(dataSize/dataLength);
		jsonDate+="[";
		for(int i=0;i<dataSize/dataLength;i++){
			int startIndex=dataLength*i;
			System.out.println("采集时间:"+bytes[startIndex+0]+" "+bytes[startIndex+1]+" "+bytes[startIndex+2]+" "+bytes[startIndex+3]+" "+bytes[startIndex+4]+" "+bytes[startIndex+5]);
			if( getDate(bytes[startIndex+0]+2000, bytes[startIndex+1], bytes[startIndex+2], bytes[startIndex+3], bytes[startIndex+4], bytes[startIndex+5]).getTimeInMillis()>Calendar.getInstance().getTimeInMillis()
					){
				return HandleResult.createFailureResult(PubAckCode.TIME_ERROR);
			}else if(
					(( ((bytes[startIndex+15]&0xff)<<8) | (bytes[startIndex+16]&0xff) )/10f)>100 ||
					((byte)(bytes[startIndex+17]-50))>80 ||
					bytes[startIndex+18]>100
					){
				return HandleResult.createFailureResult(PubAckCode.DATA_VALUE_ERROR);
			}
				
			if(i!=0){
				jsonDate+=",";
			}
			jsonDate+=
				"{"
				//时间
				+ "collectTime:"+getDate(bytes[startIndex+0]+2000, bytes[startIndex+1], bytes[startIndex+2], bytes[startIndex+3], bytes[startIndex+4], bytes[startIndex+5]).getTimeInMillis()+","
				+ "createTime:"+Calendar.getInstance().getTimeInMillis()+","
				//继电器
				+ "leak:"+((bytes[startIndex+6]&0x08)==0x08)+","
				+ "magnetic:"+((bytes[startIndex+6]&0x04)==0x04)+","
				+ "outRelay:"+((bytes[startIndex+6]&0x02)==0x02)+","
				+ "powerRelay:"+((bytes[startIndex+6]&0x01)==0x01)+","
				//流量
				+ "flow:"+bytesToLong(bytes,startIndex+7,6)+","
				//原水TDS
				+ "rawTds:"+ (( ((bytes[startIndex+13]&0xff)<<8) | (bytes[startIndex+14]&0xff) )/10f)+","
				//净水TDS
				+ "purTds:"+ (( ((bytes[startIndex+15]&0xff)<<8) | (bytes[startIndex+16]&0xff) )/10f)+","
				//温度
				+ "temp:"+((byte)(bytes[startIndex+17]-50))+","
				//湿度
				+ "humidity:"+bytes[startIndex+18]
				+ "}";
		}
		jsonDate+="]";
		result.setCode(PubAckCode.SUCCESS);
		result.setJsonData(jsonDate);
		return result;
	}
	public Calendar getDate(int year, int month, int date,
			int hourOfDay, int minute, int second) {
		Date d = null;
		try {
			d = new SimpleDateFormat("SSS").parse("000");
		} catch (ParseException e) {
			e.printStackTrace();
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(d);
		cal.set(year, month - 1, date, hourOfDay, minute, second);
		return cal;
	}
	public static long bytesToLong(byte[] byteNum,int index,int length) {  
	    long num = 0;  
	    for (int i = 0; i < length; i++) {  
	        num <<= 8;  
	        num |= ( byteNum[index+i] & 0xff); 
	    }  
	    return num;  
	} 
}
