package com.qingting.iot.util;

import java.util.HashMap;
import java.util.Map;

import com.qingting.iot.common.RuleEngine;
import com.qingting.iot.service.RuleService;
import com.smart.mvc.util.SpringUtils;

public class RuleEngineObjectUtil {
	private static Map<String,RuleEngine> classMap=new HashMap<String,RuleEngine>();
	
	protected static RuleService ruleService = SpringUtils
			.getBean(RuleService.class);
	
	static{
		ruleService.loadAllRule();
	}
	
	public static void saveRuleEngine(String className,RuleEngine obj){
		classMap.put(className, obj);
	}
	
	public static RuleEngine getRuleEngine(String className){
		return classMap.get(className);
	}
}
