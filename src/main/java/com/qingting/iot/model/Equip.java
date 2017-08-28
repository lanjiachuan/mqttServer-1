package com.qingting.iot.model;

import com.smart.mvc.model.PersistentObject;

public class Equip extends PersistentObject{
	
	private static final long serialVersionUID = 2455806594154069463L;
	
	private String equipCode;
	private String username;
	private String password;
	
	public Equip() {
		super();
	}

	public Equip(String equipCode,String username,String password){
		this.equipCode=equipCode;
		this.username=username;
		this.password=password;
	}
	
	public String getEquipCode() {
		return equipCode;
	}
	public void setEquipCode(String equipCode) {
		this.equipCode = equipCode;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public String toString() {
		return "Equip [equipCode=" + equipCode + ", username=" + username + ", password=" + password + "]";
	}
	
	
	
}
