package com.qingting.iot.service.impl;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qingting.iot.common.RuleEngine;
import com.qingting.iot.dao.RuleDAO;
import com.qingting.iot.dynamic.DynamicEngine;
import com.qingting.iot.model.Rule;
import com.qingting.iot.service.RuleService;
import com.qingting.iot.util.RuleEngineObjectUtil;
import com.smart.mvc.service.mybatis.impl.ServiceImpl;
@Service("ruleService")
public class RuleServiceImpl extends ServiceImpl<RuleDAO, Rule, Integer> implements RuleService {

	@Autowired
	public void setDao(RuleDAO dao) {
		this.dao=dao;
	}
	public void save(Rule rule){
	    try {
	    	DynamicEngine de = DynamicEngine.getInstance();
	    	RuleEngine ruleEngine = (RuleEngine) de.javaCodeToObject(rule.getTopic(),rule.getFunc());
			RuleEngineObjectUtil.saveRuleEngine(rule.getTopic(), ruleEngine);
			super.save(rule);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}
	}
	@Override
	public void loadAllRule() {
		List<Rule> listRule = dao.listAllRule();
		for (Rule rule : listRule) {
			try {
				DynamicEngine de = DynamicEngine.getInstance();
		    	RuleEngine ruleEngine = (RuleEngine) de.javaCodeToObject(rule.getTopic(),rule.getFunc());
				RuleEngineObjectUtil.saveRuleEngine(rule.getTopic(), ruleEngine);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InstantiationException e) {
				e.printStackTrace();
			}	
		}
	}
	
}
