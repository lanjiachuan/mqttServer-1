package com.qingting.mqtt.server;


import java.io.UnsupportedEncodingException;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.qingting.kafka.ProducerBase;
import com.qingting.protocol.mqttImp.process.Impl.dataHandler.Shadow;
import com.qingting.protocol.mqttImp.process.Impl.dataHandler.ShadowRequest;
import com.qingting.protocol.mqttImp.process.Impl.dataHandler.ShadowResponse;
import com.qingting.protocol.mqttImp.process.Impl.dataHandler.ShadowStore;

import io.netty.channel.ChannelFuture;

/**
 * 启动服务器，主线程所在
 * 
 * @author zer0
 * @version 1.0
 * @date 2015-2-14
 */
public class StartServer {
	
	public static void main(String[] args){
		/*ShadowStore shadowStore = new ShadowStore("123",true);
		System.out.println(shadowStore.toString());
		
		ShadowStore temp = new ShadowStore("123",true);
		JSONObject reported = new JSONObject();
		reported.put("attribute", "string");
		temp.putState("reported", reported);
		
		shadowStore.setVersion(4l);
		shadowStore.updateMetadata(temp);
		System.out.println(shadowStore.toString());*/
		
		/*System.out.println(JSONObject.toJSON("{report:null}"));
		String str1=new ShadowStore().getShadowStoreText("211701000024");
		System.out.println(str1);
		Shadow shadow=JSON.parseObject(str1,Shadow.class);
		System.out.println(shadow);
		String str2=JSON.toJSONString(shadow);
				//SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty);
		System.out.println(str2);*/
		/*String jsonString=
			"{ method:\"update\",state:{ desired:{color:\"red\"} },version:0 }";*/
		/*String jsonString=
				"{ method:\"update\",state:{ desired:{color:\"red\"} },version:0 }";
		
		ShadowRequest shadowRequest =JSON.parseObject(jsonString,ShadowRequest.class); 
		System.out.println(shadowRequest);
		ShadowResponse shadowResponse = shadowRequest.verifyRequest(jsonString);
		System.out.println(shadowResponse);*/
		final TcpServer tcpServer = new TcpServer();
		ChannelFuture future = tcpServer.startServer();
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				tcpServer.destory();
			}
		});
		future.channel().closeFuture().syncUninterruptibly();
	}
}
