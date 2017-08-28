package com.qingting.iot.service;


import com.qingting.iot.model.Equip;
import com.smart.mvc.service.mybatis.Service;

public interface EquipService extends Service<Equip, Integer>{
	void addEquip(Equip equip);
	Equip checkValid(String equipCode,String username, String password);
}
