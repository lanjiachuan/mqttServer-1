package com.qingting.protocol.mqttImp;

import com.qingting.protocol.mqttImp.message.ConnAckMessage;
import com.qingting.protocol.mqttImp.message.ConnAckVariableHeader;
import com.qingting.protocol.mqttImp.message.ConnectMessage;
import com.qingting.protocol.mqttImp.message.ConnectPayload;
import com.qingting.protocol.mqttImp.message.ConnectVariableHeader;
import com.qingting.protocol.mqttImp.message.FixedHeader;
import com.qingting.protocol.mqttImp.message.Message;
import com.qingting.protocol.mqttImp.message.PackageIdVariableHeader;
import com.qingting.protocol.mqttImp.message.PublishMessage;
import com.qingting.protocol.mqttImp.message.PublishVariableHeader;
import com.qingting.protocol.mqttImp.message.SubAckMessage;
import com.qingting.protocol.mqttImp.message.SubAckPayload;
import com.qingting.protocol.mqttImp.message.SubscribeMessage;
import com.qingting.protocol.mqttImp.message.SubscribePayload;
import com.qingting.protocol.mqttImp.message.UnSubscribeMessage;
import com.qingting.protocol.mqttImp.message.UnSubscribePayload;

import io.netty.buffer.ByteBuf;

public final class MQTTMesageFactory {

    public static Message newMessage(FixedHeader fixedHeader, Object variableHeader, Object payload) {
        switch (fixedHeader.getMessageType()) {
            case CONNECT :
                return new ConnectMessage(fixedHeader, 
                		(ConnectVariableHeader)variableHeader, 
                		(ConnectPayload)payload);

            case CONNACK:
                return new ConnAckMessage(fixedHeader, (ConnAckVariableHeader) variableHeader);

            case SUBSCRIBE:
                return new SubscribeMessage(
                        fixedHeader,
                        (PackageIdVariableHeader) variableHeader,
                        (SubscribePayload) payload);

            case SUBACK:
                return new SubAckMessage(
                        fixedHeader,
                        (PackageIdVariableHeader) variableHeader,
                        (SubAckPayload) payload);

            case UNSUBSCRIBE:
                return new UnSubscribeMessage(
                        fixedHeader,
                        (PackageIdVariableHeader) variableHeader,
                        (UnSubscribePayload) payload);

            case PUBLISH:
            	System.out.println("--------switch-PUBLISH---------");
                if(payload instanceof ByteBuf)
	            	return new PublishMessage(
	                        fixedHeader,
	                        (PublishVariableHeader) variableHeader,
	                        (ByteBuf) payload);
                else
                	return new PublishMessage(
	                        fixedHeader,
	                        (PublishVariableHeader) variableHeader,
	                        (byte[]) payload);
            case PUBACK:
            	System.out.println("--------switch-PUBACK---------");
            case UNSUBACK:
            	System.out.println("--------switch-UNSUBACK---------");
            case PUBREC:
            	System.out.println("--------switch-PUBREC---------");
            case PUBREL:
            	System.out.println("--------switch-PUBREL---------");
            case PUBCOMP:
            	System.out.println("--------switch-PUBCOMP---------");
                return new Message(fixedHeader, variableHeader);

            case PINGREQ:
            	System.out.println("--------switch-PINGREQ---------");
            case PINGRESP:
            	return new Message(fixedHeader);
            case DISCONNECT:
                return new Message(fixedHeader);

            default:
                throw new IllegalArgumentException("unknown message type: " + fixedHeader.getMessageType());
        }
    }

    private MQTTMesageFactory() { }
}