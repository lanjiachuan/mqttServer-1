package com.qingting.iot.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
public class ProducerBase<K,V> {
	private Properties props =null;
	private Producer<K, V> producer = null;
	public ProducerBase(String kType,String vType){
		props = new Properties();
		//props.put("bootstrap.servers", "localhost:9092");
		
		props.put("bootstrap.servers", "39.108.131.8:9092");
		
        //props.put("zookeeper.connect", "119.29.225.162:2281");//声明zk
        //The "all" setting we have specified will result in blocking on the full commit of the record, the slowest but most durable setting.
        //“所有”设置将导致记录的完整提交阻塞，最慢的，但最持久的设置。
        props.put("acks", "all");
        //如果请求失败，生产者也会自动重试，即使设置成０ the producer can automatically retry.
        props.put("retries", 0);

        //The producer maintains buffers of unsent records for each partition. 
        props.put("batch.size", 16384);
        //默认立即发送，这里这是延时毫秒数
        props.put("linger.ms", 1);
        //生产者缓冲大小，当缓冲区耗尽后，额外的发送调用将被阻塞。时间超过max.block.ms将抛出TimeoutException
        props.put("buffer.memory", 33554432);
        //The key.serializer and value.serializer instruct how to turn the key and value objects the user provides with their ProducerRecord into bytes.
        if(kType.toUpperCase().equals("STRING")){ 
        	props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        }else if(kType.toUpperCase().equals("BYTEARRAY")){
        	props.put("key.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        }
        if(vType.toUpperCase().equals("STRING")){
        	props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        }else if(vType.toUpperCase().equals("BYTEARRAY")){
        	props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");
        }

        //创建kafka的生产者类
		producer = new KafkaProducer<K, V>(props);
	}
	public void send(String topic, Integer partition, K key, V value){
		ProducerRecord<K, V> record =new ProducerRecord<K,V>(topic,partition, key,value);
		producer.send(record);
	}
	public void close(){
		producer.close();
	}
}
