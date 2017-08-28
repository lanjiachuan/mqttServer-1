package com.qingting.iot.dao;

import java.util.List;

import com.qingting.iot.model.Rule;
import com.smart.mvc.dao.mybatis.Dao;

public interface RuleDAO extends Dao<Rule, Integer>{
	List<Rule> listAllRule();
}
