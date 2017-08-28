/*
Navicat MySQL Data Transfer

Source Server         : aliyun-mysql
Source Server Version : 50718
Source Host           : 39.108.52.201:3306
Source Database       : zer0mqtt

Target Server Type    : MYSQL
Target Server Version : 50718
File Encoding         : 65001

Date: 2017-08-14 09:09:45
*/

SET FOREIGN_KEY_CHECKS=0;

-- ----------------------------
-- Table structure for EQUIP
-- ----------------------------
DROP TABLE IF EXISTS `EQUIP`;
CREATE TABLE `EQUIP` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `equipCode` varchar(20) NOT NULL COMMENT '设备编号',
  `password` varchar(20) DEFAULT NULL COMMENT '用户名',
  `username` varchar(20) DEFAULT NULL COMMENT '密码',
  PRIMARY KEY (`id`),
  UNIQUE KEY `equipCode` (`equipCode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
-- Records of EQUIP
-- ----------------------------
INSERT INTO `EQUIP` VALUES (1,'default id 0','test', 'test');

-- ----------------------------
-- Table structure for RULE
-- ----------------------------
DROP TABLE IF EXISTS `RULE`;
CREATE TABLE `RULE` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `topic` varchar(20) NOT NULL COMMENT '主题名',
  `func` varchar(2000) DEFAULT NULL COMMENT '对象字符串',
  PRIMARY KEY (`id`),
  UNIQUE KEY `topic` (`topic`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;