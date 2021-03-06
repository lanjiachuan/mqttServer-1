package com.qingting.iot.protocol.mqttImp.process;

import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import com.qingting.iot.common.HandleResult;
import com.qingting.iot.common.RuleEngine;
import com.qingting.iot.common.TestRuleEngine;
import com.qingting.iot.kafka.ProducerBase;
import com.qingting.iot.model.Equip;
import com.qingting.iot.protocol.mqttImp.MQTTMesageFactory;
import com.qingting.iot.protocol.mqttImp.message.ConnAckMessage;
import com.qingting.iot.protocol.mqttImp.message.ConnAckVariableHeader;
import com.qingting.iot.protocol.mqttImp.message.ConnectMessage;
import com.qingting.iot.protocol.mqttImp.message.FixedHeader;
import com.qingting.iot.protocol.mqttImp.message.Message;
import com.qingting.iot.protocol.mqttImp.message.PackageIDManager;
import com.qingting.iot.protocol.mqttImp.message.PackageIdVariableHeader;
import com.qingting.iot.protocol.mqttImp.message.PubAckCode;
import com.qingting.iot.protocol.mqttImp.message.PublishMessage;
import com.qingting.iot.protocol.mqttImp.message.PublishVariableHeader;
import com.qingting.iot.protocol.mqttImp.message.QoS;
import com.qingting.iot.protocol.mqttImp.message.SubAckMessage;
import com.qingting.iot.protocol.mqttImp.message.SubAckPayload;
import com.qingting.iot.protocol.mqttImp.message.SubscribeMessage;
import com.qingting.iot.protocol.mqttImp.message.TopicSubscribe;
import com.qingting.iot.protocol.mqttImp.message.UnSubscribeMessage;
import com.qingting.iot.protocol.mqttImp.message.ConnAckMessage.ConnectionStatus;
import com.qingting.iot.protocol.mqttImp.process.Impl.IdentityAuthenticator;
import com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler.Get;
import com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler.MapDBPersistentStore;
import com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler.RequestType;
import com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler.Shadow;
import com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler.ShadowError;
import com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler.ShadowRequest;
import com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler.ShadowResponse;
import com.qingting.iot.protocol.mqttImp.process.Impl.dataHandler.ShadowStore;
import com.qingting.iot.protocol.mqttImp.process.Interface.IAuthenticator;
import com.qingting.iot.protocol.mqttImp.process.Interface.IMessagesStore;
import com.qingting.iot.protocol.mqttImp.process.Interface.ISessionStore;
import com.qingting.iot.protocol.mqttImp.process.event.PubRelEvent;
import com.qingting.iot.protocol.mqttImp.process.event.PublishEvent;
import com.qingting.iot.protocol.mqttImp.process.event.job.RePubRelJob;
import com.qingting.iot.protocol.mqttImp.process.event.job.RePublishJob;
import com.qingting.iot.protocol.mqttImp.process.handler.ConnectTimeOutHandler;
import com.qingting.iot.protocol.mqttImp.process.subscribe.SubscribeStore;
import com.qingting.iot.protocol.mqttImp.process.subscribe.Subscription;
import com.qingting.iot.service.EquipService;
import com.qingting.iot.util.QuartzManager;
import com.qingting.iot.util.RuleEngineObjectUtil;
import com.qingting.iot.util.StringTool;
import com.smart.mvc.util.SpringUtils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;



/**
 *  协议所有的业务处理都在此类，注释中所指协议为MQTT3.3.1协议英文版
 * 
 * @author zer0
 * @version 1.0
 * @date 2015-2-16
 */
public class ProtocolProcess {
	//设备验证用
	protected static EquipService equipService = SpringUtils
			.getBean("equipService");
	
	//遗嘱信息类
	static final class WillMessage {
        private final String topic;
        private final ByteBuf payload;
        private final boolean retained;
        private final QoS qos;

        public WillMessage(String topic, ByteBuf payload, boolean retained, QoS qos) {
            this.topic = topic;
            this.payload = payload;
            this.retained = retained;
            this.qos = qos;
        }

        public String getTopic() {
            return topic;
        }

        public ByteBuf getPayload() {
            return payload;
        }

        public boolean isRetained() {
            return retained;
        }

        public QoS getQos() {
            return qos;
        }
    }
	
	private final static Logger Log = Logger.getLogger(ProtocolProcess.class);
	
	//private ConcurrentHashMap<Object, ConnectionDescriptor> clients = new ConcurrentHashMap<Object, ConnectionDescriptor>();// 客户端链接映射表
	private static ConcurrentHashMap<Object, ConnectionDescriptor> clients = new ConcurrentHashMap<Object, ConnectionDescriptor>();// 客户端链接映射表
    //存储遗嘱信息，通过ID映射遗嘱信息
	private ConcurrentHashMap<String, WillMessage> willStore = new ConcurrentHashMap<>();
	
	//equipService代替了authenticator
	//private IAuthenticator authenticator;
	//private IMessagesStore messagesStore;
	private static IMessagesStore messagesStore;
	private ISessionStore sessionStore;
	//private SubscribeStore subscribeStore;
	private static SubscribeStore subscribeStore;
	
	public ProtocolProcess(){
		MapDBPersistentStore storge = new MapDBPersistentStore();
		//this.authenticator = new IdentityAuthenticator();
		//this.messagesStore = storge;
		//this.messagesStore.initStore();//初始化存储
		if(messagesStore==null){
			messagesStore = storge;
			messagesStore.initStore();//初始化存储
		}
		this.sessionStore = storge;
		//this.subscribeStore = new SubscribeStore();
		if(subscribeStore==null)
			subscribeStore = new SubscribeStore();
	}
	
	//将此类单例
	private static ProtocolProcess INSTANCE;
	public static ProtocolProcess getInstance(){
		if (INSTANCE == null) {
			INSTANCE = new ProtocolProcess();
		}
		return INSTANCE;
	}
	
	
    public static ConcurrentHashMap<Object, ConnectionDescriptor> getClients() {
		return clients;
	}



	/**
   	 * 处理协议的CONNECT消息类型
   	 * @param clientID
   	 * @param connectMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-3-7
   	 */
	public void processConnect(Channel client, ConnectMessage connectMessage){
		Log.info("处理Connect的数据");
		//首先查看保留位是否为0，不为0则断开连接,协议P24
		if (!connectMessage.getVariableHeader().isReservedIsZero()) {
			client.close();
			return;
		}
		//处理protocol name和protocol version, 如果返回码!=0，sessionPresent必为0，协议P24,P32
		if (!connectMessage.getVariableHeader().getProtocolName().equals("MQTT") || 
				connectMessage.getVariableHeader().getProtocolVersionNumber() != 4 ) {
			
			ConnAckMessage connAckMessage = (ConnAckMessage) MQTTMesageFactory.newMessage(
					FixedHeader.getConnAckFixedHeader(), 
					new ConnAckVariableHeader(ConnectionStatus.UNACCEPTABLE_PROTOCOL_VERSION, false), 
					null);
			
			client.writeAndFlush(connAckMessage);
			client.close();//版本或协议名不匹配，则断开该客户端连接
			return;
		}
		
		//处理Connect包的保留位不为0的情况，协议P24
		if (!connectMessage.getVariableHeader().isReservedIsZero()) {
			client.close();
		}
		
		//处理clientID为null或长度为0的情况，协议P29
		if (connectMessage.getPayload().getClientId() == null || connectMessage.getPayload().getClientId().length() == 0) {
			//clientID为null的时候，cleanSession只能为1,此时给client设置一个随机的，不存在的mac地址为ID，否则，断开连接
			if (connectMessage.getVariableHeader().isCleanSession()) {
				boolean isExist = true;
				String macClientID = StringTool.generalMacString();
				while (isExist) {
					ConnectionDescriptor connectionDescriptor = clients.get(macClientID);
					if (connectionDescriptor == null) {
						connectMessage.getPayload().setClientId(macClientID);
						isExist = false;
					} else {
						macClientID = StringTool.generalMacString();
					}
				}
			} else {
				Log.info("客户端ID为空，cleanSession为0，根据协议，不接收此客户端");
				ConnAckMessage connAckMessage = (ConnAckMessage) MQTTMesageFactory.newMessage(
						FixedHeader.getConnAckFixedHeader(), 
						new ConnAckVariableHeader(ConnectionStatus.IDENTIFIER_REJECTED, false), 
						null);
				client.writeAndFlush(connAckMessage);
				client.close();
				return;
			}
		}
		
//		//检查clientID的格式符合与否
//		if (!StringTool.isMacString(connectMessage.getPayload().getClientId())) {
//			Log.info("客户端ID为{"+connectMessage.getPayload().getClientId()+"}，拒绝此客户端");
//			ConnAckMessage connAckMessage = (ConnAckMessage) MQTTMesageFactory.newMessage(
//					FixedHeader.getConnAckFixedHeader(), 
//					new ConnAckVariableHeader(ConnectionStatus.IDENTIFIER_REJECTED, false), 
//					null);
//			client.writeAndFlush(connAckMessage);
//			client.close();
//			return;
//		}
		
		//处理身份验证（userNameFlag和passwordFlag）
		if (connectMessage.getVariableHeader().isHasUsername() && 
				connectMessage.getVariableHeader().isHasPassword()) {
			String userName = connectMessage.getPayload().getUsername();
			String pwd = connectMessage.getPayload().getPassword();
			//此处对用户名和密码做验证
			Log.info("equipService:"+equipService);
			Equip equip = equipService.checkValid(connectMessage.getPayload().getClientId(),userName, pwd);
			Log.info("客户端账号："+connectMessage.getPayload().getClientId()+","+userName+","+pwd+".");
			if (equip==null) {
				ConnAckMessage connAckMessage = (ConnAckMessage) MQTTMesageFactory.newMessage(
						FixedHeader.getConnAckFixedHeader(), 
						new ConnAckVariableHeader(ConnectionStatus.BAD_USERNAME_OR_PASSWORD, false), 
						null);
				client.writeAndFlush(connAckMessage);
				client.close();
				Log.info("用户名或密码不正确");
				return;
			}
		}
		
		//如果会话中已经存储了这个新连接的ID，就关闭之前的clientID
		if (clients.containsKey(connectMessage.getPayload().getClientId())) {
			Log.error("客户端ID{"+connectMessage.getPayload().getClientId()+"}已存在，强制关闭老连接");
			Channel oldChannel = clients.get(connectMessage.getPayload().getClientId()).getClient();
			boolean cleanSession = NettyAttrManager.getAttrCleanSession(oldChannel);
			if (cleanSession) {
				cleanSession(connectMessage.getPayload().getClientId());
			}
			oldChannel.close();
		}
		
		//若至此没问题，则将新客户端连接加入client的维护列表中
		ConnectionDescriptor connectionDescriptor = 
				new ConnectionDescriptor(connectMessage.getPayload().getClientId(), 
						client, connectMessage.getVariableHeader().isCleanSession());
		this.clients.put(connectMessage.getPayload().getClientId(), connectionDescriptor);
		//处理心跳包时间，把心跳包时长和一些其他属性都添加到会话中，方便以后使用
		int keepAlive = connectMessage.getVariableHeader().getKeepAlive();
		Log.debug("连接的心跳包时长是 {" + keepAlive + "} s");
		NettyAttrManager.setAttrClientId(client, connectMessage.getPayload().getClientId());
		NettyAttrManager.setAttrCleanSession(client, connectMessage.getVariableHeader().isCleanSession());
		//协议P29规定，在超过1.5个keepAlive的时间以上没收到心跳包PingReq，就断开连接(但这里要注意把单位是s转为ms)
		float keepAliveTemp=(((float)keepAlive)*1.5f);
		NettyAttrManager.setAttrKeepAlive(client, (int)keepAliveTemp);
		//添加心跳机制处理的Handler
		client.pipeline().addFirst("idleStateHandler", new IdleStateHandler((int)keepAliveTemp, Integer.MAX_VALUE, Integer.MAX_VALUE, TimeUnit.SECONDS));
		client.pipeline().addAfter("idleStateHandler", "connectTimeOutHandler", new ConnectTimeOutHandler());
		//处理Will flag（遗嘱信息）,协议P26
		if (connectMessage.getVariableHeader().isHasWill()) {
			QoS willQos = connectMessage.getVariableHeader().getWillQoS();
			ByteBuf willPayload = Unpooled.buffer().writeBytes(connectMessage.getPayload().getWillMessage().getBytes());//获取遗嘱信息的具体内容
			WillMessage will = new WillMessage(connectMessage.getPayload().getWillTopic(),
					willPayload, connectMessage.getVariableHeader().isWillRetain(),willQos);
			//把遗嘱信息与和其对应的的clientID存储在一起
			willStore.put(connectMessage.getPayload().getClientId(), will);
		}
		
		//处理cleanSession为1的情况
        if (connectMessage.getVariableHeader().isCleanSession()) {
            //移除所有之前的session并开启一个新的，并且原先保存的subscribe之类的都得从服务器删掉
            cleanSession(connectMessage.getPayload().getClientId());
        }
        
        //TODO 此处生成一个token(以后每次客户端每次请求服务器，都必须先验证此token正确与否)，并把token保存到本地以及传回给客户端
        //鉴权获取不应该在这里做
        
//        String token = StringTool.generalRandomString(32);
//        sessionStore.addSession(connectMessage.getClientId(), token);
//        //把荷载封装成json字符串
//        JSONObject jsonObject = new JSONObject();
//        try {
//			jsonObject.put("token", token);
//		} catch (JSONException e) {
//			e.printStackTrace();
//		}
        
        //处理回写的CONNACK,并回写，协议P29
        ConnAckMessage okResp = null;
        //协议32,session present的处理
        if (!connectMessage.getVariableHeader().isCleanSession() && 
        		sessionStore.searchSubscriptions(connectMessage.getPayload().getClientId())) {
        	okResp = (ConnAckMessage) MQTTMesageFactory.newMessage(
					FixedHeader.getConnAckFixedHeader(), 
					new ConnAckVariableHeader(ConnectionStatus.ACCEPTED, true), 
					null);
		}else{
			okResp = (ConnAckMessage) MQTTMesageFactory.newMessage(
					FixedHeader.getConnAckFixedHeader(), 
					new ConnAckVariableHeader(ConnectionStatus.ACCEPTED, false), 
					null);
		}
        
        client.writeAndFlush(okResp);
        Log.info("CONNACK处理完毕并成功发送");
        Log.info("连接的客户端clientID="+connectMessage.getPayload().getClientId()+", " +
        		"cleanSession为"+connectMessage.getVariableHeader().isCleanSession());
        
        //如果cleanSession=0,需要在重连的时候重发同一clientID存储在服务端的离线信息
        if (!connectMessage.getVariableHeader().isCleanSession()) {
            //force the republish of stored QoS1 and QoS2
        	republishMessage(connectMessage.getPayload().getClientId());
        }
        
        /**
         * 默认订阅主题，这里需要在完善产品管理后，移动到产品管理中去
         */
        //get主题默认订阅
        String topicFilter = "get/"+connectMessage.getPayload().getClientId();//topicSubscribe.getTopicFilter();
		QoS qos = QoS.AT_LEAST_ONCE;//topicSubscribe.getQos();
		Subscription newSubscription = new Subscription(connectMessage.getPayload().getClientId(), topicFilter, qos, false);
		//订阅新的订阅
		subscribeSingleTopic(newSubscription, topicFilter);
		
		//current主题默认订阅
		String topicFilter2 = "current/"+connectMessage.getPayload().getClientId();
		QoS qos2 = QoS.AT_LEAST_ONCE;//topicSubscribe.getQos();
		Subscription newSubscription2 = new Subscription(connectMessage.getPayload().getClientId(), topicFilter2, qos2, false);
		//订阅新的订阅
		subscribeSingleTopic(newSubscription2, topicFilter2);
	}
	
	/**
   	 * 处理协议的publish消息类型,该方法先把public需要的事件提取出来
   	 * @param clientID
   	 * @param publishMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-18
   	 */
	public void processPublic(Channel client, PublishMessage publishMessage){
		Log.info("处理publish的数据");
		Log.info("publishMessage:"+publishMessage.toString());
		String clientID = NettyAttrManager.getAttrClientId(client);
		final String topic = publishMessage.getVariableHeader().getTopic();
	    final QoS qos = publishMessage.getFixedHeader().getQos();
	    final ByteBuf message = publishMessage.getPayload();
	    System.out.println(message);
	    final int packgeID = publishMessage.getVariableHeader().getPackageID();
	    final boolean retain = publishMessage.getFixedHeader().isRetain();
	    Log.info("messageLength:"+message.capacity());
	    processPublic(clientID, topic, qos, retain, message, packgeID);
	}
	
	/**
   	 * 处理遗言消息的发送
   	 * @param clientID
   	 * @param willMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-26
   	 */
	public void processPublic(Channel client, WillMessage willMessage){
		Log.info("处理遗言的publish数据");
		String clientID = NettyAttrManager.getAttrClientId(client);
		final String topic = willMessage.getTopic();
	    final QoS qos = willMessage.getQos();
	    final ByteBuf message = willMessage.getPayload();
	    final boolean retain = willMessage.isRetained();
	    
	    processPublic(clientID, topic, qos, retain, message, null);
	}
	
	/**  
	    * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用 
	    *   
	    * @param src  
	    *            byte数组  
	    * @param offset  
	    *            从数组的第offset位开始  
	    * @param length
	    * 			   长度
	    * @return int数值  
	    */    
	public static int bytesToInt(byte[] src, int offset,int length) {  
	    int value=0;  
	    int count=0;
	    if(length>4) throw new RuntimeException("输入的length大于4无法装换成int类型");
	    for(int i=length+offset-1;i>(offset-1);i--){
	    	value=(int)(
	    			value|(
	    			(src[i]&0xFF)<<(8*count)
	    			)
	    			);
	    	count++;
	    }
	    /*value = (int) ((src[offset] & 0xFF)   
	            | ((src[offset+1] & 0xFF)<<8)   
	            | ((src[offset+2] & 0xFF)<<16)   
	            | ((src[offset+3] & 0xFF)<<24)); */ 
	    return value;  
	}
	
	/**
   	 * 根据协议进行具体的处理，处理不同的Qos等级下的public事件
   	 * @param clientID
   	 * @param topic
   	 * @param qos
   	 * @param recRetain
   	 * @param message
   	 * @param recPackgeID 此包ID只是客户端传过来的，用于发回pubAck用，发送给其他客户端的包ID，需要重新生成
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-19
   	 */
	private void processPublic(String clientID, String topic, QoS qos, boolean recRetain, ByteBuf message, Integer recPackgeID){
		Log.info("接收public消息:{clientID="+clientID+",Qos="+qos+",topic="+topic+",packageID="+recPackgeID+"}");
		String publishKey = null;
//		int sendPackageID = PackageIDManager.getNextMessageId();
		
		//根据协议P34，Qos=3的时候，就关闭连接
		if (qos == QoS.RESERVE) {
			clients.get(clientID).getClient().close();
		}
		
		//根据协议P52，qos=0, Dup=0, 则把消息发送给所有注册的客户端即可
		if (qos == QoS.AT_MOST_ONCE) {
			boolean dup = false;
			boolean retain = false;
			sendPublishMessage(topic, qos, message, retain, dup);
		}
		
		//根据协议P53，publish的接受者需要发送该publish(Qos=1,Dup=0)消息给其他客户端，然后发送pubAck给该客户端。
		//发送该publish消息时候，按此流程： 存储消息→发送给所有人→等待pubAck到来→删除消息
		if (qos == QoS.AT_LEAST_ONCE) {
			boolean retain = false;
			boolean dup = false;
			System.out.println("Qos=1,Dup=0");
			
			if(topic.equals("update")){//影子消息
				sendPubAck(clientID, recPackgeID);//这里换个位置，先发送pubAck给该客户端
				
				ShadowResponse shadowResponse = processShadow(clientID,qos,recRetain,message,recPackgeID);
				if(shadowResponse!=null){
					/*sendPublishMessageOfMyself("get/"+clientID, qos, 
							shadowResponse.toString().getBytes(),
							retain, dup);*/
					
					
					sendPublishMessage("get/"+clientID, qos, 
							ByteBufUtil.encodeString(PooledByteBufAllocator.DEFAULT, CharBuffer.wrap(shadowResponse.toString()), CharsetUtil.US_ASCII),
							retain, dup);
				}
			}else if(topic.equals("current")){//实时消息
				sendPubAck(clientID, recPackgeID);//先发送pubAck给该客户端
				
				Calendar time=Calendar.getInstance();
				/*byte[] bytes=new byte[]{ 
						(byte)(time.get(Calendar.YEAR)%100),    //获取年
						(byte)(time.get(Calendar.MONTH)+1),    //获取月
						(byte)(time.get(Calendar.DAY_OF_MONTH)),    //获取日
						(byte)(time.get(Calendar.HOUR_OF_DAY)),    //获取时
						(byte)(time.get(Calendar.MINUTE)),    //获取分
						(byte)(time.get(Calendar.SECOND)),    //获取秒
					};*/
				byte[] bytes=new byte[8];
				bytes[0]=(byte)(time.get(Calendar.YEAR)%100);   //获取年
				bytes[1]=(byte)(time.get(Calendar.MONTH)+1);    //获取月
				bytes[2]=(byte)(time.get(Calendar.DAY_OF_MONTH));    //获取日
				bytes[3]=(byte)(time.get(Calendar.HOUR_OF_DAY));    //获取时
				bytes[4]=(byte)(time.get(Calendar.MINUTE));    //获取分
				bytes[5]=(byte)(time.get(Calendar.SECOND));    //获取秒
				int sum=0;
				for(int i=0;i<bytes.length;i++){
					sum+=bytes[i];
				}
				bytes[6]=(byte)( (sum>>>8) & 0xFF );
				bytes[7]=(byte)( sum & 0xFF );
				System.out.println("推送实时消息:");
				for (byte b : bytes) {
					System.out.print(b+" ");
				}
				char[] chars=new char[8];
				for(int i=0;i<8;i++){
					chars[i]=(char) bytes[i];
				}
				
				//sendPublishMessageByByte("current/"+clientID,qos,bytes,retain,dup);
				sendPublishMessage("current/"+clientID, QoS.AT_MOST_ONCE,//qos, 
						ByteBufUtil.encodeString(PooledByteBufAllocator.DEFAULT, CharBuffer.wrap(chars), CharsetUtil.US_ASCII),
						retain, dup);
			}else{//其他消息
				PubAckCode pubAckCode=processGeneralPublicMessage(clientID,topic,qos,recRetain,message,recPackgeID);
				
				sendPubAck(clientID, recPackgeID,pubAckCode);//先发送pubAck给该客户端
				
				sendPublishMessage(topic, qos, message, retain, dup);
			}
			
		}
		
		//根据协议P54，P55
		//接收端：publish接收消息→存储包ID→发给其他客户端→发回pubRec→收到pubRel→抛弃第二步存储的包ID→发回pubcomp
		//发送端：存储消息→发送publish(Qos=2,Dup=0)→收到pubRec→抛弃第一步存储的消息→存储pubRec的包ID→发送pubRel→收到pubcomp→抛弃pubRec包ID的存储
		if (qos == QoS.EXACTLY_ONCE) {
			boolean dup = false;
			boolean retain = false;
			messagesStore.storePublicPackgeID(clientID, recPackgeID);
			sendPublishMessage(topic, qos, message, retain, dup);
			sendPubRec(clientID, recPackgeID);
		}
		
		//处理消息是否保留，注：publish报文中的主题名不能包含通配符(协议P35)，所以retain中保存的主题名不会有通配符
		if (recRetain) {
			if (qos == QoS.AT_MOST_ONCE) {
				messagesStore.cleanRetained(topic);
			} else {
				messagesStore.storeRetained(topic, message, qos);
			}
		}
	}
	/**
	 * 
	 * @Title: processGeneralPublicMessage
	 * @Description: 按照规则引擎规则转换消息，最后将消息发送到消息服务器
	 * @param clientID
	 * @param topic
	 * @param qos
	 * @param recRetain
	 * @param message
	 * @param recPackgeID 
	 * @return void
	 * @throws
	 */
	public PubAckCode processGeneralPublicMessage(String clientID, String topic, QoS qos, boolean recRetain, ByteBuf message, Integer recPackgeID){
		//将ByteBuf转变为byte[]
		byte[] messageBytes = new byte[message.readableBytes()];
		message.getBytes(message.readerIndex(), messageBytes);
		
		ProducerBase<String,String> producerBase=new ProducerBase<String,String>("String","String");
		System.out.println("The key:"+clientID+":"+recPackgeID+".The value:"+message);
		String str=topic.replaceAll("/", "_");
		str=str.replace(str.substring(0, 1), str.substring(0, 1).toUpperCase());
		RuleEngine ruleEngine = RuleEngineObjectUtil.getRuleEngine(str);
		
		//HandleResult handleResult=ruleEngine.convertToJsonString(messageBytes);
		HandleResult handleResult=new TestRuleEngine().convertToJsonString(messageBytes);
		
		if(handleResult.isSuccess()){
			System.out.println("convertString:"+handleResult.getJsonData());
			producerBase.send(str, 0, clientID, handleResult.getJsonData());
			producerBase.close();
			JSONArray array = JSON.parseArray(handleResult.getJsonData()); 
			System.out.println(array);
		}
		return handleResult.getCode();
	}
	
	public ShadowResponse processShadow(String clientID,QoS qos, boolean recRetain, ByteBuf message, Integer recPackgeID){
		//将ByteBuf转变为byte[]
		byte[] messageBytes = new byte[message.readableBytes()];
		message.getBytes(message.readerIndex(), messageBytes);
		System.out.println("接收的消息"+new String(messageBytes));
		
		
		//获取update请求数据
		String jsonString=null;
		try {
			jsonString = new String(messageBytes,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		Log.info("jsonString:"+jsonString);
		ShadowRequest shadowRequest=null;
		try { 
			shadowRequest =JSON.parseObject(jsonString,ShadowRequest.class); 
			ShadowResponse shadowResponse = shadowRequest.verifyRequest(jsonString);
			if(shadowResponse!=null){
				Log.info(shadowResponse);
				return shadowResponse;
			}
		} catch (JSONException e) { //json格式错误
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.JSON_FORMAT_ERROR), null);
		} catch (Exception e){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.SERVER_EXCEPTION), null);
		}
		Log.info("temp:"+shadowRequest);
		/*JSONObject desired=null;
		JSONObject reported=null;
		if(temp.getState().has("desired")){
			desired=(JSONObject) temp.getState().get("desired");
		}
		if(temp.getState().has("reported")){
			reported=(JSONObject) temp.getState().get("reported");
		}*/
		//解析请求state
		/*ShadowResponse shadowResponse = null;//temp.parserState();
		if(shadowResponse!=null)
			return shadowResponse;*/
		//获取服务器影子文档
		ShadowStore shadowStore=new ShadowStore(clientID);
		Shadow shadow = JSON.parseObject(shadowStore.getJsonString(),Shadow.class);
		
		//判断请求类型
		if(shadowRequest.getMethod().equals("update")){
			Log.info("更新update");
			if(shadowRequest.getRequestType().equals(RequestType.REPORTED)){
				Log.info("设备上报状态");
				//设备上报状态
				//流程：更新shadow——>发送响应到get主题(即响应设备)
				ShadowResponse temp=shadow.updateMetadata(shadowRequest);
				if(temp!=null)	return temp;
				shadowStore.reWriteShadow(JSON.toJSONString(shadow));
				return new ShadowResponse("reply").
						updateShadowResponse(new Get("success",null), shadow);
			}else if(shadowRequest.getRequestType().equals(RequestType.CONTROL)){
				Log.info("服务器主动改变设备状态");
				//服务器主动改变设备状态
				//流程：更新shadow——>发送状态到get主题(即响应设备)
				ShadowResponse temp=shadow.updateMetadata(shadowRequest);
				if(temp!=null)	return temp;
				shadowStore.reWriteShadow(JSON.toJSONString(shadow));
				return new ShadowResponse("control").
						updateShadowResponse(new Get("success",null), shadow);
			}else if(shadowRequest.getRequestType().equals(RequestType.UPEND)){
				Log.info("设备状态更新结束");
				ShadowResponse temp=shadow.updateMetadata(shadowRequest);
				if(temp!=null)	return temp;
				shadowStore.reWriteShadow(JSON.toJSONString(shadow));
				//更新结束无响应
			}
		}else if(shadowRequest.getMethod().equals("get")){
			Log.info("设备主动获取影子文档");
			return new ShadowResponse("control").
					updateShadowResponse(new Get("success",null), shadow);
		}
		//messagesStore.storeRetained("get"+"/"+clientID,message,qos);
		
		return null;
	}
	public ShadowResponse processShadowByJsonString(String clientID,QoS qos, boolean recRetain, String message, Integer recPackgeID){
		
		//获取update请求数据
		String jsonString=message;
		
		Log.info("jsonString:"+jsonString);
		ShadowRequest shadowRequest=null;
		try { 
			shadowRequest =JSON.parseObject(jsonString,ShadowRequest.class); 
			ShadowResponse shadowResponse = shadowRequest.verifyRequest(jsonString);
			if(shadowResponse!=null){
				Log.info(shadowResponse);
				return shadowResponse;
			}
		} catch (JSONException e) { //json格式错误
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.JSON_FORMAT_ERROR), null);
		} catch (Exception e){
			return new ShadowResponse("reply").
					updateShadowResponse(new Get("error",ShadowError.SERVER_EXCEPTION), null);
		}
		Log.info("temp:"+shadowRequest);
		/*JSONObject desired=null;
		JSONObject reported=null;
		if(temp.getState().has("desired")){
			desired=(JSONObject) temp.getState().get("desired");
		}
		if(temp.getState().has("reported")){
			reported=(JSONObject) temp.getState().get("reported");
		}*/
		//解析请求state
		/*ShadowResponse shadowResponse = null;//temp.parserState();
		if(shadowResponse!=null)
			return shadowResponse;*/
		//获取服务器影子文档
		ShadowStore shadowStore=new ShadowStore(clientID);
		Shadow shadow = JSON.parseObject(shadowStore.getJsonString(),Shadow.class);
		
		//判断请求类型
		if(shadowRequest.getMethod().equals("update")){
			Log.info("更新update");
			if(shadowRequest.getRequestType().equals(RequestType.REPORTED)){
				Log.info("设备上报状态");
				//设备上报状态
				//流程：更新shadow——>发送响应到get主题(即响应设备)
				ShadowResponse temp=shadow.updateMetadata(shadowRequest);
				if(temp!=null)	return temp;
				shadowStore.reWriteShadow(JSON.toJSONString(shadow));
				return new ShadowResponse("reply").
						updateShadowResponse(new Get("success",null), shadow);
			}else if(shadowRequest.getRequestType().equals(RequestType.CONTROL)){
				Log.info("服务器主动改变设备状态");
				//服务器主动改变设备状态
				//流程：更新shadow——>发送状态到get主题(即响应设备)
				ShadowResponse temp=shadow.updateMetadata(shadowRequest);
				if(temp!=null)	return temp;
				shadowStore.reWriteShadow(JSON.toJSONString(shadow));
				return new ShadowResponse("control").
						updateShadowResponse(new Get("success",null), shadow);
			}else if(shadowRequest.getRequestType().equals(RequestType.UPEND)){
				Log.info("设备状态更新结束");
				ShadowResponse temp=shadow.updateMetadata(shadowRequest);
				if(temp!=null)	return temp;
				shadowStore.reWriteShadow(JSON.toJSONString(shadow));
				//更新结束无响应
			}
		}else if(shadowRequest.getMethod().equals("get")){
			Log.info("设备主动获取影子文档");
			return new ShadowResponse("control").
					updateShadowResponse(new Get("success",null), shadow);
		}
		//messagesStore.storeRetained("get"+"/"+clientID,message,qos);
		
		return null;
	}
	/**
   	 * 处理协议的pubAck消息类型
   	 * @param client
   	 * @param pubAckMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-21
   	 */
	public void processPubAck(Channel client, PackageIdVariableHeader pubAckVariableMessage){		
		 String clientID = NettyAttrManager.getAttrClientId(client);
		 int pacakgeID = pubAckVariableMessage.getPackageID();
		 String publishKey = String.format("%s%d", clientID, pacakgeID);
		 //取消Publish重传任务
		 QuartzManager.removeJob(publishKey, "publish", publishKey, "publish");
		 //删除临时存储用于重发的Publish消息
		 messagesStore.removeQosPublishMessage(publishKey);
		 //最后把使用完的包ID释放掉
		 PackageIDManager.releaseMessageId(pacakgeID);
	}

	/**
   	 * 处理协议的pubRec消息类型
   	 * @param client
   	 * @param pubRecMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-23
   	 */
	public void processPubRec(Channel client, PackageIdVariableHeader pubRecVariableMessage){
		 String clientID = NettyAttrManager.getAttrClientId(client);
		 int packageID = pubRecVariableMessage.getPackageID();
		 String publishKey = String.format("%s%d", clientID, packageID);
		 
		 //取消Publish重传任务,同时删除对应的值
		 QuartzManager.removeJob(publishKey, "publish", publishKey, "publish");
		 messagesStore.removeQosPublishMessage(publishKey);
		 //此处须额外处理，根据不同的事件，处理不同的包ID
		 messagesStore.storePubRecPackgeID(clientID, packageID);
		 //组装PubRel事件后，存储PubRel事件，并发回PubRel
		 PubRelEvent pubRelEvent = new PubRelEvent(clientID, packageID);
		 //此处的Key和Publish的key一致
		 messagesStore.storePubRelMessage(publishKey, pubRelEvent);
		 //发回PubRel
		 sendPubRel(clientID, packageID);
		 //开启PubRel重传事件
		 Map<String, Object> jobParam = new HashMap<String, Object>();
		 jobParam.put("ProtocolProcess", this);
		 jobParam.put("pubRelKey", publishKey);
		 QuartzManager.addJob(publishKey, "pubRel", publishKey, "pubRel", RePubRelJob.class, 10, 2, jobParam);
	}
	
	/**
   	 * 处理协议的pubRel消息类型
   	 * @param client
   	 * @param pubRelMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-23
   	 */
	public void processPubRel(Channel client, PackageIdVariableHeader pubRelVariableMessage){
		 String clientID = NettyAttrManager.getAttrClientId(client);
		 //删除的是接收端的包ID
		 int pacakgeID = pubRelVariableMessage.getPackageID();
		 
		 messagesStore.removePublicPackgeID(clientID);
		 sendPubComp(clientID, pacakgeID);
	}
	
	/**
   	 * 处理协议的pubComp消息类型
   	 * @param client
   	 * @param pubcompMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-23
   	 */
	public void processPubComp(Channel client, PackageIdVariableHeader pubcompVariableMessage){
		 String clientID = NettyAttrManager.getAttrClientId(client);
		 int packaageID = pubcompVariableMessage.getPackageID();
		 String pubRelkey = String.format("%s%d", clientID, packaageID);
		 
		 //删除存储的PubRec包ID
		 messagesStore.removePubRecPackgeID(clientID);
		 //取消PubRel的重传任务，删除临时存储的PubRel事件
		 QuartzManager.removeJob(pubRelkey, "pubRel", pubRelkey, "pubRel");
		 messagesStore.removePubRelMessage(pubRelkey);
		 //最后把使用完的包ID释放掉
		 PackageIDManager.releaseMessageId(packaageID);
	}

	/**
   	 * 处理协议的subscribe消息类型
   	 * @param client
   	 * @param subscribeMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-24
   	 */
	public void processSubscribe(Channel client, SubscribeMessage subscribeMessage) { 
		 String clientID = NettyAttrManager.getAttrClientId(client);
		 boolean cleanSession = NettyAttrManager.getAttrCleanSession(client);
		 Log.info("处理subscribe数据包，客户端ID={"+clientID+"},cleanSession={"+cleanSession+"}");
		 //一条subscribeMessage信息可能包含多个Topic和Qos
		 List<TopicSubscribe> topicSubscribes = subscribeMessage.getPayload().getTopicSubscribes();
		
		 List<Integer> grantedQosLevel = new ArrayList<Integer>();
		 //依次处理订阅
		 for (TopicSubscribe topicSubscribe : topicSubscribes) {
			String topicFilter = topicSubscribe.getTopicFilter();
			QoS qos = topicSubscribe.getQos();
			Subscription newSubscription = new Subscription(clientID, topicFilter, qos, cleanSession);
			//订阅新的订阅
			subscribeSingleTopic(newSubscription, topicFilter);
			
			//生成suback荷载
			grantedQosLevel.add(qos.value());
		 }
		 
		 SubAckMessage subAckMessage = (SubAckMessage) MQTTMesageFactory.newMessage(
				 FixedHeader.getSubAckFixedHeader(), 
				 new PackageIdVariableHeader(subscribeMessage.getVariableHeader().getPackageID()), 
				 new SubAckPayload(grantedQosLevel));
		 
		 Log.info("回写subAck消息给订阅者，包ID={"+subscribeMessage.getVariableHeader().getPackageID()+"}");
		 client.writeAndFlush(subAckMessage);
	}
	

	/**
   	 * 处理协议的unSubscribe消息类型
   	 * @param client
   	 * @param unSubscribeMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-24
   	 */
	public void processUnSubscribe(Channel client, UnSubscribeMessage unSubscribeMessage){
		 String clientID = NettyAttrManager.getAttrClientId(client);
		 int packageID = unSubscribeMessage.getVariableHeader().getPackageID();
		 Log.info("处理unSubscribe数据包，客户端ID={"+clientID+"}");
		 List<String> topicFilters = unSubscribeMessage.getPayload().getTopics();
		 for (String topic : topicFilters) {
			//取消订阅树里的订阅
			subscribeStore.removeSubscription(topic, clientID);
			sessionStore.removeSubscription(topic, clientID);
		 }
		 
		 Message unSubAckMessage = MQTTMesageFactory.newMessage(
				 FixedHeader.getUnSubAckFixedHeader(), 
				 new PackageIdVariableHeader(packageID), 
				 null);
		 Log.info("回写unSubAck信息给客户端，包ID为{"+packageID+"}");
		 client.writeAndFlush(unSubAckMessage);
	}
	
	/**
   	 * 处理协议的pingReq消息类型
   	 * @param client
   	 * @param pingReqMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-24
   	 */
	public void processPingReq(Channel client, Message pingReqMessage){
		 Log.info("收到心跳包"+pingReqMessage.getFixedHeader().toString());
		 Message pingRespMessage = MQTTMesageFactory.newMessage(
				 FixedHeader.getPingRespFixedHeader(), 
				 null,
				 null);
		 //重置心跳包计时器
		 client.writeAndFlush(pingRespMessage);
	}
	
	/**
   	 * 处理协议的disconnect消息类型
   	 * @param client
   	 * @param disconnectMessage
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-5-24
   	 */
	public void processDisconnet(Channel client, Message disconnectMessage){
		 String clientID = NettyAttrManager.getAttrClientId(client);
		 boolean cleanSession = NettyAttrManager.getAttrCleanSession(client);
		 if (cleanSession) {
			cleanSession(clientID);
		 }
		 
		willStore.remove(clientID);

		 this.clients.remove(clientID);
		 client.close();
	}
	
	/**
   	 * 清除会话，除了要从订阅树中删掉会话信息，还要从会话存储中删除会话信息
   	 * @param client
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-05-07
   	 */
	private void cleanSession(String clientID) {
		subscribeStore.removeForClient(clientID);
		//从会话存储中删除信息
		sessionStore.wipeSubscriptions(clientID);
	}

	/**
   	 * 在客户端重连以后，针对QoS1和Qos2的消息，重发存储的离线消息
   	 * @param clientID
   	 * @author zer0
   	 * @version 1.0
   	 * @date 2015-05-18
   	 */
	private void republishMessage(String clientID){
		//取出需要重发的消息列表
		//查看消息列表是否为空，为空则返回
		//不为空则依次发送消息并从会话中删除此消息
		List<PublishEvent> publishedEvents = messagesStore.listMessagesInSession(clientID);
		if (publishedEvents.isEmpty()) {
			Log.info("没有客户端{"+clientID+"}存储的离线消息");
			return;
		}
		
		Log.info("重发客户端{"+ clientID +"}存储的离线消息");
		for (PublishEvent pubEvent : publishedEvents) {
			boolean dup = true;
			sendPublishMessage(pubEvent.getClientID(), 
							   pubEvent.getTopic(), 
							   pubEvent.getQos(), 
							   Unpooled.buffer().writeBytes(pubEvent.getMessage()), 
							   pubEvent.isRetain(), 
							   pubEvent.getPackgeID(),
							   dup);
			messagesStore.removeMessageInSessionForPublish(clientID, pubEvent.getPackgeID());
		}
	}
	
	/**
	 * 在未收到对应包的情况下，重传Publish消息
	 * @param publishKey
	 * @author zer0
	 * @version 1.0
	 * @date 2015-11-28
	 */
	public void reUnKnowPublishMessage(String publishKey){
		PublishEvent pubEvent = messagesStore.searchQosPublishMessage(publishKey);
		Log.info("重发PublishKey为{"+ publishKey +"}的Publish离线消息");
		boolean dup = true;
		PublishMessage publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
				FixedHeader.getPublishFixedHeader(dup, pubEvent.getQos(), pubEvent.isRetain()), 
				new PublishVariableHeader(pubEvent.getTopic(), pubEvent.getPackgeID()), 
				Unpooled.buffer().writeBytes(pubEvent.getMessage()));
		Log.info("重发的publish:"+publishMessage);
		Log.info("重发的pubEvent:"+pubEvent);
		//从会话列表中取出会话，然后通过此会话发送publish消息
		if(clients.get(pubEvent.getClientID())!=null)//这里是防止中途连接断开，客户端列表信息被清除了，抛空指针异常
			clients.get(pubEvent.getClientID()).getClient().writeAndFlush(publishMessage);
	}
	
	/**
	 * 在未收到对应包的情况下，重传PubRel消息
	 * @param pubRelKey
	 * @author zer0
	 * @version 1.0
	 * @date 2015-11-28
	 */
	public void reUnKnowPubRelMessage(String pubRelKey){
		PubRelEvent pubEvent = messagesStore.searchPubRelMessage(pubRelKey);
		Log.info("重发PubRelKey为{"+ pubRelKey +"}的PubRel离线消息");
		sendPubRel(pubEvent.getClientID(), pubEvent.getPackgeID());
//	    messagesStore.removeQosPublishMessage(pubRelKey);
	}
	
	
	
	/**
	  * 取出所有匹配topic的客户端，然后发送public消息给客户端(临时调试用)
	  * @param topic
	  * @param qos
	  * @param message
	  * @param retain
	  * @param PackgeID
	  * @author zer0
	  * @version 1.0
     * @date 2015-05-19
	  */
	private void sendPublishMessageOfMyself(String topic, QoS originQos, byte[] message, boolean retain, boolean dup){
		for (final Subscription sub : subscribeStore.getClientListFromTopic(topic)) {
			
			String clientID = sub.getClientID();
			Integer sendPackageID = PackageIDManager.getNextMessageId();
			String publishKey = String.format("%s%d", clientID, sendPackageID);
			QoS qos = originQos;
			
			//协议P43提到， 假设请求的QoS级别被授权，客户端接收的PUBLISH消息的QoS级别小于或等于这个级别，PUBLISH 消息的级别取决于发布者的原始消息的QoS级别
			if (originQos.ordinal() > sub.getRequestedQos().ordinal()) {
				qos = sub.getRequestedQos(); 
			}
			
			PublishMessage publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
					FixedHeader.getPublishFixedHeader(dup, qos, retain), 
					new PublishVariableHeader(topic, sendPackageID), 
					message);
			
			if (this.clients == null) {
				throw new RuntimeException("内部错误，clients为null");
			} else {
				Log.debug("clients为{"+this.clients+"}");
			}
			
			if (this.clients.get(clientID) == null) {
				throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
			} else {
				Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
			}
			
			if (originQos == QoS.AT_MOST_ONCE) {
				publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
						FixedHeader.getPublishFixedHeader(dup, qos, retain), 
						new PublishVariableHeader(topic), 
						message);
				//从会话列表中取出会话，然后通过此会话发送publish消息
				this.clients.get(clientID).getClient().writeAndFlush(publishMessage);
			}else {
				publishKey = String.format("%s%d", clientID, sendPackageID);//针对每个重生成key，保证消息ID不会重复
				//将ByteBuf转变为byte[]
				PublishEvent storePublishEvent = new PublishEvent(topic, qos, message, retain, clientID, sendPackageID);
				
				Log.info("将消息发送给订阅topic的客户端，消息如下:"+new String(message));
				Log.info("本次包ID为:"+sendPackageID);
				
				//从会话列表中取出会话，然后通过此会话发送publish消息
				this.clients.get(clientID).getClient().writeAndFlush(publishMessage);
				//存临时Publish消息，用于重发
				messagesStore.storeQosPublishMessage(publishKey, storePublishEvent);
				//开启Publish重传任务，在制定时间内未收到PubAck包则重传该条Publish信息
				Map<String, Object> jobParam = new HashMap<String, Object>();
				jobParam.put("ProtocolProcess", this);
				jobParam.put("publishKey", publishKey);
				QuartzManager.addJob(publishKey, "publish", publishKey, "publish", RePublishJob.class, 10, 2, jobParam);
			}
			
			Log.info("服务器发送消息给客户端{"+clientID+"},topic{"+topic+"},qos{"+qos+"}");
			
			if (!sub.isCleanSession()) {
				//将ByteBuf转变为byte[]
				
				PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, 
						 retain, sub.getClientID(), 
						 sendPackageID != null ? sendPackageID : 0);
               messagesStore.storeMessageToSessionForPublish(newPublishEvt);
			}
			
			
		}
	}
	
	
	
	/**
	  * 取出所有匹配topic的客户端，然后发送public消息给客户端
	  * @param topic
	  * @param qos
	  * @param message
	  * @param retain
	  * @param PackgeID
	  * @author zer0
	  * @version 1.0
      * @date 2015-05-19
	  */
	private void sendPublishMessage(String topic, QoS originQos, ByteBuf message, boolean retain, boolean dup){
		System.out.println("publish消息给订阅的客户端.");
		System.out.println("主题订阅客户端数量："+subscribeStore.getClientListFromTopic(topic).size());
		for (final Subscription sub : subscribeStore.getClientListFromTopic(topic)) {
			
			String clientID = sub.getClientID();
			Integer sendPackageID = PackageIDManager.getNextMessageId();
			String publishKey = String.format("%s%d", clientID, sendPackageID);
			QoS qos = originQos;
			
			//协议P43提到， 假设请求的QoS级别被授权，客户端接收的PUBLISH消息的QoS级别小于或等于这个级别，PUBLISH 消息的级别取决于发布者的原始消息的QoS级别
			if (originQos.ordinal() > sub.getRequestedQos().ordinal()) {
				qos = sub.getRequestedQos(); 
			}
			
			PublishMessage publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
					FixedHeader.getPublishFixedHeader(dup, qos, retain), 
					new PublishVariableHeader(topic, sendPackageID), 
					message);
			
			if (this.clients == null) {
				throw new RuntimeException("内部错误，clients为null");
			} else {
				Log.debug("clients为{"+this.clients+"}");
			}
			
			if (this.clients.get(clientID) == null) {
				throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
			} else {
				Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
			}
			
			if (originQos == QoS.AT_MOST_ONCE) {
				publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
						FixedHeader.getPublishFixedHeader(dup, qos, retain), 
						new PublishVariableHeader(topic), 
						message);
				//从会话列表中取出会话，然后通过此会话发送publish消息
				this.clients.get(clientID).getClient().writeAndFlush(publishMessage);
			}else {
				publishKey = String.format("%s%d", clientID, sendPackageID);//针对每个重生成key，保证消息ID不会重复
				//将ByteBuf转变为byte[]
				byte[] messageBytes = new byte[message.readableBytes()];
				message.getBytes(message.readerIndex(), messageBytes);
				PublishEvent storePublishEvent = new PublishEvent(topic, qos, messageBytes, retain, clientID, sendPackageID);
				
				Log.info("将消息发送给订阅topic的客户端，消息如下:"+new String(messageBytes));
				Log.info("本次包ID为:"+sendPackageID);
				
				//从会话列表中取出会话，然后通过此会话发送publish消息
				this.clients.get(clientID).getClient().writeAndFlush(publishMessage);
				//存临时Publish消息，用于重发
				messagesStore.storeQosPublishMessage(publishKey, storePublishEvent);
				//开启Publish重传任务，在制定时间内未收到PubAck包则重传该条Publish信息
				Map<String, Object> jobParam = new HashMap<String, Object>();
				jobParam.put("ProtocolProcess", this);
				jobParam.put("publishKey", publishKey);
				QuartzManager.addJob(publishKey, "publish", publishKey, "publish", RePublishJob.class, 10, 2, jobParam);
			}
			
			Log.info("服务器发送消息给客户端{"+clientID+"},topic{"+topic+"},qos{"+qos+"}");
			
			if (!sub.isCleanSession()) {
				//将ByteBuf转变为byte[]
				byte[] messageBytes = new byte[message.readableBytes()];
				message.getBytes(message.readerIndex(), messageBytes);
				PublishEvent newPublishEvt = new PublishEvent(topic, qos, messageBytes, 
						 retain, sub.getClientID(), 
						 sendPackageID != null ? sendPackageID : 0);
                messagesStore.storeMessageToSessionForPublish(newPublishEvt);
			}
			
			
		}
	}
	/**
	  * 取出所有匹配topic的客户端，然后发送public消息给客户端
	  * @param topic
	  * @param qos
	  * @param message
	  * @param retain
	  * @param PackgeID
	  * @author zer0
	  * @version 1.0
     * @date 2015-05-19
	  */
	public void shadowWebRequest(String clientID,String jsonRequest, int recPackgeID){
		ShadowResponse shadowResponse = processShadowByJsonString(clientID, QoS.AT_LEAST_ONCE, false, jsonRequest, recPackgeID);
		System.out.println("shadowResponse:"+shadowResponse);
		if(shadowResponse!=null){
			char[] chars=new char[5];
			if(shadowResponse.getMethod().equals("control")){
				JSONObject payload=shadowResponse.getPayload();
				JSONObject state=payload.getJSONObject("state");
				JSONObject desired=state.getJSONObject("desired");
				Boolean relay=desired.getBoolean("relay");
				/*Boolean relay=(Boolean)(
							(
								(JSONObject)(
										(
												(JSONObject)(
														shadowResponse.getPayload().get("state")
												)
										).get("desired")
								)
							).get("relay")
						);*/
				if(relay!=null){
					chars[0]=relay ? (char)1 : (char)2;
				}else{
					chars[0]=(char)0;
				}
			}
			sendPublishMessage("get/"+clientID, QoS.AT_LEAST_ONCE, 
					ByteBufUtil.encodeString(PooledByteBufAllocator.DEFAULT, CharBuffer.wrap(chars), CharsetUtil.US_ASCII),
					false, false);
		}
	}
	/**
	  * 取出所有匹配topic的客户端，然后发送public消息给客户端
	  * @param topic
	  * @param qos
	  * @param message
	  * @param retain
	  * @param PackgeID
	  * @author zer0
	  * @version 1.0
     * @date 2015-05-19
	  */
	private void sendPublishMessageByByte(String topic, QoS originQos, byte[] message, boolean retain, boolean dup){
		for (final Subscription sub : subscribeStore.getClientListFromTopic(topic)) {
			
			String clientID = sub.getClientID();
			Integer sendPackageID = PackageIDManager.getNextMessageId();
			String publishKey = String.format("%s%d", clientID, sendPackageID);
			QoS qos = originQos;
			
			//协议P43提到， 假设请求的QoS级别被授权，客户端接收的PUBLISH消息的QoS级别小于或等于这个级别，PUBLISH 消息的级别取决于发布者的原始消息的QoS级别
			if (originQos.ordinal() > sub.getRequestedQos().ordinal()) {
				qos = sub.getRequestedQos(); 
			}
			
			PublishMessage publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
					FixedHeader.getPublishFixedHeader(dup, qos, retain), 
					new PublishVariableHeader(topic, sendPackageID), 
					message);
			
			if (this.clients == null) {
				throw new RuntimeException("内部错误，clients为null");
			} else {
				Log.debug("clients为{"+this.clients+"}");
			}
			
			if (this.clients.get(clientID) == null) {
				throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
			} else {
				Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
			}
			
			if (originQos == QoS.AT_MOST_ONCE) {
				publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
						FixedHeader.getPublishFixedHeader(dup, qos, retain), 
						new PublishVariableHeader(topic), 
						message);
				//从会话列表中取出会话，然后通过此会话发送publish消息
				this.clients.get(clientID).getClient().writeAndFlush(publishMessage);
			}else {
				publishKey = String.format("%s%d", clientID, sendPackageID);//针对每个重生成key，保证消息ID不会重复
				//将ByteBuf转变为byte[]
				//byte[] messageBytes = new byte[message.readableBytes()];
				//message.getBytes(message.readerIndex(), messageBytes);
				PublishEvent storePublishEvent = new PublishEvent(topic, qos, message, retain, clientID, sendPackageID);
				
				Log.info("将消息发送给订阅topic的客户端，消息如下:"+new String(message));
				Log.info("本次包ID为:"+sendPackageID);
				
				//从会话列表中取出会话，然后通过此会话发送publish消息
				this.clients.get(clientID).getClient().writeAndFlush(publishMessage);
				//存临时Publish消息，用于重发
				messagesStore.storeQosPublishMessage(publishKey, storePublishEvent);
				//开启Publish重传任务，在制定时间内未收到PubAck包则重传该条Publish信息
				Map<String, Object> jobParam = new HashMap<String, Object>();
				jobParam.put("ProtocolProcess", this);
				jobParam.put("publishKey", publishKey);
				QuartzManager.addJob(publishKey, "publish", publishKey, "publish", RePublishJob.class, 10, 2, jobParam);
			}
			
			Log.info("服务器发送消息给客户端{"+clientID+"},topic{"+topic+"},qos{"+qos+"}");
			
			if (!sub.isCleanSession()) {
				//将ByteBuf转变为byte[]
				
				PublishEvent newPublishEvt = new PublishEvent(topic, qos, message, 
						 retain, sub.getClientID(), 
						 sendPackageID != null ? sendPackageID : 0);
               messagesStore.storeMessageToSessionForPublish(newPublishEvt);
			}
			
			
		}
	}
	/**
	  * 发送publish消息给指定ID的客户端
	  * @param clientID
	  * @param topic
	  * @param qos
	  * @param message
	  * @param retain
	  * @param PackgeID
	  * @param dup
	  * @author zer0
	  * @version 1.0
      * @date 2015-05-19
	  */
	private void sendPublishMessage(String clientID, String topic, QoS qos, ByteBuf message, boolean retain, Integer packageID, boolean dup){
		Log.info("发送pulicMessage给指定客户端");
		
		String publishKey = String.format("%s%d", clientID, packageID);
		
		PublishMessage publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
				FixedHeader.getPublishFixedHeader(dup, qos, retain), 
				new PublishVariableHeader(topic, packageID), 
				message);
		
		if (this.clients == null) {
			throw new RuntimeException("内部错误，clients为null");
		} else {
			Log.debug("clients为{"+this.clients+"}");
		}
		
		if (this.clients.get(clientID) == null) {
			throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
		} else {
			Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
		}
		
		if (qos == QoS.AT_MOST_ONCE) {
			publishMessage = (PublishMessage) MQTTMesageFactory.newMessage(
					FixedHeader.getPublishFixedHeader(dup, qos, retain), 
					new PublishVariableHeader(topic), 
					message);
			//从会话列表中取出会话，然后通过此会话发送publish消息
			this.clients.get(clientID).getClient().writeAndFlush(publishMessage);
		}else {
			publishKey = String.format("%s%d", clientID, packageID);//针对每个重生成key，保证消息ID不会重复
			//将ByteBuf转变为byte[]
			byte[] messageBytes = new byte[message.readableBytes()];
			message.getBytes(message.readerIndex(), messageBytes);
			PublishEvent storePublishEvent = new PublishEvent(topic, qos, messageBytes, retain, clientID, packageID);
			
			//从会话列表中取出会话，然后通过此会话发送publish消息
			this.clients.get(clientID).getClient().writeAndFlush(publishMessage);
			//存临时Publish消息，用于重发
			messagesStore.storeQosPublishMessage(publishKey, storePublishEvent);
			//开启Publish重传任务，在制定时间内未收到PubAck包则重传该条Publish信息
			Map<String, Object> jobParam = new HashMap<String, Object>();
			jobParam.put("ProtocolProcess", this);
			jobParam.put("publishKey", publishKey);
			QuartzManager.addJob(publishKey, "publish", publishKey, "publish", RePublishJob.class, 10, 2, jobParam);
		}
	}
	
	 /**
	  * 发送保存的Retain消息
	  * @param clientID
	  * @param topic
	  * @param qos
	  * @param message
	  * @param retain
	  * @author zer0
	  * @version 1.0
      * @date 2015-12-1
	  */
	private void sendPublishMessage(String clientID, String topic, QoS qos, ByteBuf message, boolean retain){
		int packageID = PackageIDManager.getNextMessageId();
		sendPublishMessage(clientID, topic, qos, message, retain, packageID, false);
	}
	
	/**
	 *回写PubAck消息给发来publish的客户端
	 * @param clientID
	 * @param packgeID
	 * @author zer0
	 * @version 1.0
	 * @date 2015-5-21
	 */
	private void sendPubAck(String clientID, Integer packageID) {
        Log.info("发送PubAck消息给客户端");

        Message pubAckMessage = MQTTMesageFactory.newMessage(
        		FixedHeader.getPubAckFixedHeader(), 
        		new PackageIdVariableHeader(packageID), 
        		null);
        
        try {
        	if (this.clients == null) {
				throw new RuntimeException("内部错误，clients为null");
			} else {
				Log.debug("clients为{"+this.clients+"}");
			}
        	
        	if (this.clients.get(clientID) == null) {
				throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
			} else {
				Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
			}	            
        	
			this.clients.get(clientID).getClient().writeAndFlush(pubAckMessage);
        }catch(Throwable t) {
            Log.error(null, t);
        }
    }
	/**
	 *回写PubAck消息给发来publish的客户端(包含返回编码)
	 * @param clientID
	 * @param packgeID
	 * @author zer0
	 * @version 1.0
	 * @date 2015-5-21
	 */
	private void sendPubAck(String clientID, Integer packageID,PubAckCode code) {
        Log.info("发送PubAck消息给客户端");

        Message pubAckMessage = MQTTMesageFactory.newMessage(
        		FixedHeader.getPubAckFixedHeader(code.getCode()), 
        		new PackageIdVariableHeader(packageID), 
        		null);
        
        try {
        	if (this.clients == null) {
				throw new RuntimeException("内部错误，clients为null");
			} else {
				Log.debug("clients为{"+this.clients+"}");
			}
        	
        	if (this.clients.get(clientID) == null) {
				throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
			} else {
				Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
			}	            
        	
			this.clients.get(clientID).getClient().writeAndFlush(pubAckMessage);
        }catch(Throwable t) {
            Log.error(null, t);
        }
    }
	
	/**
	 *回写PubRec消息给发来publish的客户端
	 * @param clientID
	 * @param packgeID
	 * @author zer0
	 * @version 1.0
	 * @date 2015-5-21
	 */
	private void sendPubRec(String clientID, Integer packageID) {
	        Log.trace("发送PubRec消息给客户端");

	        Message pubRecMessage = MQTTMesageFactory.newMessage(
	        		FixedHeader.getPubAckFixedHeader(), 
	        		new PackageIdVariableHeader(packageID), 
	        		null);
	        
	        try {
	        	if (this.clients == null) {
					throw new RuntimeException("内部错误，clients为null");
				} else {
					Log.debug("clients为{"+this.clients+"}");
				}
	        	
	        	if (this.clients.get(clientID) == null) {
					throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
				} else {
					Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
				}	            
	        	
	        	this.clients.get(clientID).getClient().writeAndFlush(pubRecMessage);
	        }catch(Throwable t) {
	            Log.error(null, t);
	        }
	    }
	
	/**
	 *回写PubRel消息给发来publish的客户端
	 * @param clientID
	 * @param packgeID
	 * @author zer0
	 * @version 1.0
	 * @date 2015-5-23
	 */
	private void sendPubRel(String clientID, Integer packageID) {
	        Log.trace("发送PubRel消息给客户端");

	        Message pubRelMessage = MQTTMesageFactory.newMessage(
	        		FixedHeader.getPubAckFixedHeader(), 
	        		new PackageIdVariableHeader(packageID), 
	        		null);
	        
	        try {
	        	if (this.clients == null) {
					throw new RuntimeException("内部错误，clients为null");
				} else {
					Log.debug("clients为{"+this.clients+"}");
				}
	        	
	        	if (this.clients.get(clientID) == null) {
					throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
				} else {
					Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
				}	            
	        	
	        	this.clients.get(clientID).getClient().writeAndFlush(pubRelMessage);
	        }catch(Throwable t) {
	            Log.error(null, t);
	        }
	    }
	
	/**
	 * 回写PubComp消息给发来publish的客户端
	 * @param clientID
	 * @param packgeID
	 * @author zer0
	 * @version 1.0
	 * @date 2015-5-23
	 */
	private void sendPubComp(String clientID, Integer packageID) {
	        Log.trace("发送PubComp消息给客户端");

	        Message pubcompMessage = MQTTMesageFactory.newMessage(
	        		FixedHeader.getPubAckFixedHeader(), 
	        		new PackageIdVariableHeader(packageID), 
	        		null);
	        
	        try {
	        	if (this.clients == null) {
					throw new RuntimeException("内部错误，clients为null");
				} else {
					Log.debug("clients为{"+this.clients+"}");
				}
	        	
	        	if (this.clients.get(clientID) == null) {
					throw new RuntimeException("不能从会话列表{"+this.clients+"}中找到clientID:{"+clientID+"}");
				} else {
					Log.debug("从会话列表{"+this.clients+"}查找到clientID:{"+clientID+"}");
				}	            
	        	
	        	this.clients.get(clientID).getClient().writeAndFlush(pubcompMessage);
	        }catch(Throwable t) {
	            Log.error(null, t);
	        }
	    }
	
	/**
	 * 处理一个单一订阅，存储到会话和订阅数
	 * @param newSubscription
	 * @param topic
	 * @author zer0
	 * @version 1.0
	 * @date 2015-5-25
	 */
	private void subscribeSingleTopic(Subscription newSubscription, final String topic){
		Log.info("订阅topic{"+topic+"},Qos为{"+newSubscription.getRequestedQos()+"}");
		String clientID = newSubscription.getClientID();
		sessionStore.addNewSubscription(newSubscription, clientID);
		subscribeStore.addSubscrpition(newSubscription);
		//TODO 此处还需要将此订阅之前存储的信息发出去
		 Collection<IMessagesStore.StoredMessage> messages = messagesStore.searchRetained(topic);
		 for (IMessagesStore.StoredMessage storedMsg : messages) {
	            Log.debug("send publish message for topic {" + topic + "}");
	            sendPublishMessage(newSubscription.getClientID(), storedMsg.getTopic(), storedMsg.getQos(), Unpooled.buffer().writeBytes(storedMsg.getPayload()), true);
	     }
	}
}
