package com.qingting.protocol.mqttImp.process.Impl.dataHandler;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

import org.apache.log4j.Logger;

import com.qingting.util.MqttTool;

public class ShadowStore {
	private static final Logger Log = Logger.getLogger(Shadow.class);
	
	private static final String STORAGE_FILE_PATH =  System.getProperty("user.dir") + File.separator + MqttTool.getProperty("shadow");
	
	private File file;
	
	private String jsonString;
	
	static{
		File file=new File(STORAGE_FILE_PATH);
		//如果目录不存在，则新建一个
        if(!file.exists()){
        	file.mkdir();
        }
	}
	
	
	public String getJsonString() {
		return jsonString;
	}
	
	public ShadowStore(String fileName) {
		file = new File(STORAGE_FILE_PATH+"/"+fileName+".json");
		System.out.println("设备影子文件路径:"+STORAGE_FILE_PATH+"/"+fileName+".json");
      
    	boolean flag=false;
    	if(!file.exists()){
    		try {
				flag = file.createNewFile();
			} catch (IOException e) {
				Log.info("创建影子文档失败,影子文档已存在");
				e.printStackTrace();
			}
    	}
    	
		if(flag){//第一次创建，且成功
			jsonString=
				"{"+
					"\"state\":{"+
						"\"desired\":{"+
						
						"},"+
						"\"reported\":{"+
						
						"},"+
					"},"+
					"\"metadata\":{"+
						"\"desired\":{"+
						
						"},"+
						"\"reported\":{"+
						
						"},"+
					"},"+
					"\"timestamp\":"+Calendar.getInstance().getTimeInMillis()+","+
					"\"version\":0"+
				"}";
			writeJson(jsonString,file);
		}else{//已存在
			jsonString=readJson(file);
		}
	}
	/*public String getShadowStoreText(String fileName){
		file = new File(STORAGE_FILE_PATH+"/"+fileName+".json");
		System.out.println("设备影子文件路径:"+STORAGE_FILE_PATH+"/"+fileName+".json");
      
    	boolean flag=false;
    	if(!file.exists()){
    		try {
				flag = file.createNewFile();
			} catch (IOException e) {
				Log.info("创建影子文档失败,影子文档已存在");
				e.printStackTrace();
			}
    	}
    	
		if(flag){//第一次创建，且成功
			String str=
				"{"+
					"\"state\":{"+
						"\"desired\":{"+
						
						"},"+
						"\"reported\":{"+
						
						"},"+
					"},"+
					"\"metadata\":{"+
						"\"desired\":{"+
						
						"},"+
						"\"reported\":{"+
						
						"},"+
					"},"+
					"\"timestamp\":"+Calendar.getInstance().getTimeInMillis()+","+
					"\"version\":0"+
				"}";
			writeJson(str,file);
			return str;
		}else{//已存在
			return readJson(file);
		}
	}*/
	public void reWriteShadow(String string){
    	writeJson(string,file);
    }
	
	//从给定位置读取Json文件
    private static String readJson(File file){
        
        BufferedReader reader = null;
        //返回值,使用StringBuffer
        StringBuffer data = new StringBuffer();
        //
        try {
            reader = new BufferedReader(new FileReader(file));
            //每次读取文件的缓存
            String temp = null;
            while((temp = reader.readLine()) != null){
                data.append(temp);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            //关闭文件流
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data.toString();
    }
    //写Json文件到给定文件，存储到硬盘
    private static void writeJson(Object json,File file){
        BufferedWriter writer = null;
        //如果文件不存在，则新建一个
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //写入
        try {
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(json.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(writer != null){
                    writer.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
