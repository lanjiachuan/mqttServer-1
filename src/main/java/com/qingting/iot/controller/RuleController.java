package com.qingting.iot.controller;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.qingting.iot.model.Rule;
import com.qingting.iot.service.RuleService;
import com.smart.mvc.model.ResultCode;
import com.smart.mvc.model.WebResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "设备相关")
@Controller
@RequestMapping("/rule")
public class RuleController {
	@Resource
	RuleService ruleService;
	@ApiOperation("添加设备")
	@RequestMapping(value="/insert",method = RequestMethod.POST,produces = "application/json; charset=utf-8")
	public @ResponseBody WebResult<Object> insert(
			@ApiParam @RequestBody Rule rule
			){
		ruleService.save(rule);
		WebResult<Object> result=new WebResult<Object>(ResultCode.SUCCESS);
		return result;
	}
}
