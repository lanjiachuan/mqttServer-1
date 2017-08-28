package com.qingting.iot.dao;


import org.apache.ibatis.annotations.Param;

import com.qingting.iot.model.Equip;
import com.smart.mvc.dao.mybatis.Dao;

public interface EquipDAO extends Dao<Equip, Integer>{
	int addEquip(Equip equip);
	Equip checkValid(@Param("equipCode") String equipCode,@Param("username") String username,@Param("password") String password);
}
