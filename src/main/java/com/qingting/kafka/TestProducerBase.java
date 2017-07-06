package com.qingting.kafka;

public class TestProducerBase {
	//ProducerBase();
	public static void main(String[] args){
		ProducerBase producerBase=new ProducerBase();
		for(int i=60;i<90;i++){
			producerBase.send("monitor", 0, Integer.toString(i), new byte[]{1,2,3,4});
		}
		
		producerBase.close();
		
	}
}
