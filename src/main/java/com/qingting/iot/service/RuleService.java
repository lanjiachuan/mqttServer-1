package com.qingting.iot.service;


import com.qingting.iot.model.Rule;
import com.smart.mvc.service.mybatis.Service;

public interface RuleService extends Service<Rule, Integer>{
	void loadAllRule();
}
