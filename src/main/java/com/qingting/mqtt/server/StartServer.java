package com.qingting.mqtt.server;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.qingting.customer.baseserver.MonitorService;

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
		//String path=System.getProperty("user.dir");
		//ApplicationContext ct=new FileSystemXmlApplicationContext(path+"applicationContext.xml");
		ApplicationContext ct=new FileSystemXmlApplicationContext(Thread.currentThread().getContextClassLoader().getResource("").getPath()+"applicationContext.xml");
		MonitorService monitorService =(MonitorService) ct.getBean("monitorService");
		System.out.println("monitorService->"+monitorService);
		
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
