package com.qingting.iot.controller;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.qingting.iot.model.Equip;
import com.qingting.iot.protocol.mqttImp.process.ProtocolProcess;
import com.qingting.iot.service.EquipService;
import com.smart.mvc.controller.BaseController;
import com.smart.mvc.model.Pagination;
import com.smart.mvc.model.ResultCode;
import com.smart.mvc.model.WebResult;
import com.smart.mvc.validator.Validator;
import com.smart.mvc.validator.annotation.ValidateParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = "设备相关")
@Controller
@RequestMapping("/equip")
public class EquipController extends BaseController{
	@Resource
	EquipService equipService;
	@ApiOperation("添加设备")
	@RequestMapping(value="/addEquip",method = RequestMethod.POST,produces = "application/json; charset=utf-8")
	public @ResponseBody WebResult<String> addEquip(
			@ApiParam(value = "设备编号", required = true) @RequestParam @ValidateParam({ Validator.NOT_BLANK })String equipCode,
			@ApiParam(value = "用户名", required = true) @RequestParam @ValidateParam({ Validator.NOT_BLANK })String username,
			@ApiParam(value = "密码", required = true) @RequestParam @ValidateParam({ Validator.NOT_BLANK })String password
			){
		System.out.println("equipCode:"+equipCode+".username:"+username+".password:"+password);
		equipService.addEquip(new Equip(equipCode,username,password));
		WebResult<String> result=new WebResult<String>(ResultCode.SUCCESS);
		return result;
	}
	@ApiOperation("删除设备")
	@RequestMapping(value="/deleteEquip",method = RequestMethod.POST,produces = "application/json; charset=utf-8")
	public @ResponseBody WebResult<String> deleteEquip(
			@ApiParam(value = "ids", required = true) @ValidateParam({ Validator.NOT_BLANK }) String ids
			){
		System.out.println("ids:"+ids);
		equipService.deleteById(getAjaxIds(ids));
		WebResult<String> result=new WebResult<String>(ResultCode.SUCCESS);
		return result;
	}
	@ApiOperation("查询设备")
	@RequestMapping(value="/listEquip",method = RequestMethod.GET,produces = "application/json; charset=utf-8")
	public @ResponseBody WebResult<Pagination<Equip>> listEquip(
			@ApiParam(value = "开始页码", required = true) @ValidateParam({ Validator.NOT_BLANK }) Integer pageNo,
			@ApiParam(value = "显示条数", required = true) @ValidateParam({ Validator.NOT_BLANK }) Integer pageSize
			){
		WebResult<Pagination<Equip>> result=new WebResult<Pagination<Equip>>(ResultCode.SUCCESS);
		result.setData(equipService.findByAllPagination(new Pagination<Equip>(pageNo, pageSize)));
		return result;
	}
	@ApiOperation("影子请求")
	@RequestMapping(value="/shadow",method = RequestMethod.GET,produces = "application/json; charset=utf-8")
	public @ResponseBody WebResult<String> shadow(
			@ApiParam(value = "设备编号", required = true) @RequestParam @ValidateParam({ Validator.NOT_BLANK }) String equipCode,
			@ApiParam(value = "影子文档", required = true) @RequestParam @ValidateParam({ Validator.NOT_BLANK }) String shadow
			){
		new ProtocolProcess().shadowWebRequest(equipCode, shadow, 1);
		WebResult<String> result=new WebResult<String>(ResultCode.SUCCESS);
		result.setMessage("处理成功");
		return result;
	}
}
