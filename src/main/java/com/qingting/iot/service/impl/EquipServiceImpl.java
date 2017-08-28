package com.qingting.iot.service.impl;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qingting.iot.dao.EquipDAO;
import com.qingting.iot.model.Equip;
import com.qingting.iot.service.EquipService;
import com.smart.mvc.service.mybatis.impl.ServiceImpl;
@Service("equipService")
public class EquipServiceImpl extends ServiceImpl<EquipDAO, Equip, Integer> implements EquipService {
	
	@Override
	public void addEquip(Equip equip) {
		System.out.println("equipDAO:"+dao);
		dao.addEquip(equip);
	}

	@Autowired
	public void setDao(EquipDAO dao) {
		this.dao=dao;
	}

	@Override
	public Equip checkValid(String equipCode,String username, String password) {
		return dao.checkValid(equipCode,username, password);
	}

	public void deleteById(List<Integer> idList) {
		verifyRows(dao.deleteById(idList), idList.size(), "应用数据库删除失败");
	}

}
