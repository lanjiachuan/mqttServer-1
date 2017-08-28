package com.qingting.iot.protocol.mqttImp.process.handler;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.qingting.iot.protocol.mqttImp.process.ConnectionDescriptor;
import com.qingting.iot.protocol.mqttImp.process.ProtocolProcess;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

public class ConnectTimeOutHandler extends ChannelHandlerAdapter {
	
	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
		if(IdleStateEvent.class.isAssignableFrom(evt.getClass())){
			IdleStateEvent event = (IdleStateEvent) evt;  
            if (event.state() == IdleState.READER_IDLE){ // 一段时间内没有收到任何数据
                System.out.println("================read idle=================="); 
                /*for (ConnectionDescriptor cd:ProtocolProcess.getClients().values()) {
					if(cd.getClient().equals(ctx.channel())){
						ProtocolProcess.getClients().
						ProtocolProcess.getClients().remove("");
					}
				}*/
                ConcurrentHashMap<Object, ConnectionDescriptor> clients = ProtocolProcess.getClients();
                for (Entry<Object, ConnectionDescriptor> m :clients.entrySet())  {
        			System.out.println("删除前客户端维护表:"+m.getKey()+":"+m.getValue());
                	if(m.getValue().getClient().equals(ctx.channel())){
                		clients.remove(m.getKey());
                	}
        		}
                for (Entry<Object, ConnectionDescriptor> m: clients.entrySet()) {
                	System.out.println("删除后客户端维护表:"+m.getKey()+":"+m.getValue());
				}
            }else if (event.state() == IdleState.WRITER_IDLE) { 
                System.out.println("====================write idle==============");  
            }else if (event.state() == IdleState.ALL_IDLE)  {
                System.out.println("======================all idle===================");
            }
		}
	}
}
