package com.qingting.kafka;

public class TestProducerBase {
	//ProducerBase();
	public static void main(String[] args){
		ProducerBase producerBase=new ProducerBase();
		for(int i=60;i<90;i++){
			producerBase.send("monitor", 0, Integer.toString(i), Integer.toString(i+1));
		}
		
		producerBase.close();
		
	}
}
