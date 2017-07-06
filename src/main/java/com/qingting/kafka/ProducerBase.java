package com.qingting.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class ProducerBase {
	private Properties props =null;
	private Producer<String, byte[]> producer = null;
	public ProducerBase(){
		props = new Properties();
		//props.put("bootstrap.servers", "localhost:9092");
		
		props.put("bootstrap.servers", "39.108.52.201:9092");
		
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
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

        //创建kafka的生产者类
		producer = new KafkaProducer<String, byte[]>(props);
	}
	public void send(String topic, Integer partition, String key, byte[] value){
		ProducerRecord<String, byte[]> record =new ProducerRecord<String,byte[]>(topic,partition, key,value);
		producer.send(record);
		//producer.send(new ProducerRecord<String, String>(topic,partition, key, value));
		
	}
	public void close(){
		producer.close();
		//producer.close(timeout, unit);
	}
}
