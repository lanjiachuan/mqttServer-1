package com.qingting.iot.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.qingting.iot.common.RuleEngine;
import com.qingting.iot.util.RuleEngineObjectUtil;
import com.smart.mvc.model.ResultCode;
import com.smart.mvc.model.WebResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(tags = "设备相关")
@Controller
@RequestMapping("/testRule")
public class TestRuleController {
	@ApiOperation("添加设备")
	@RequestMapping(value="/get",method = RequestMethod.GET,produces = "application/json; charset=utf-8")
	public @ResponseBody WebResult<Object> get(
			String topic
			){
		
		/*Class<?> c = Heros.class;
        try {
            Object object = c.newInstance();
            Field[] fields = c.getDeclaredFields();
            System.out.println("Heros所有属性：");
            for (Field f : fields) {
                System.out.println(f);
            }
             
            Field field = c.getDeclaredField("name");
            field.setAccessible(true);
            field.set(object, "炸弹人");
            System.out.println("修改后的属性值：");
            System.out.println(field.get(object));
            System.out.println("修改属性后的Heros：");
            System.out.println((Heros)object);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
		RuleEngine re = RuleEngineObjectUtil.getRuleEngine(topic);
		
		WebResult<Object> result=new WebResult<Object>(ResultCode.SUCCESS);
		result.setData(re);
		
		return result;
	}
}
