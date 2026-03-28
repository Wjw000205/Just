/*
 Navicat Premium Dump SQL

 Source Server         : MySQL_Connection
 Source Server Type    : MySQL
 Source Server Version : 80026 (8.0.26)
 Source Host           : localhost:3306
 Source Schema         : just

 Target Server Type    : MySQL
 Target Server Version : 80026 (8.0.26)
 File Encoding         : 65001

 Date: 28/03/2026 12:18:01
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for department
-- ----------------------------
DROP TABLE IF EXISTS `department`;
CREATE TABLE `department`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `department_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门名称',
  `leader_id` int NOT NULL COMMENT '负责人id',
  `leader_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门负责人姓名',
  `deleted` int NOT NULL COMMENT '0',
  `created_time` datetime NOT NULL COMMENT '信息创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of department
-- ----------------------------


-- ----------------------------
-- Table structure for module
-- ----------------------------
DROP TABLE IF EXISTS `module`;
CREATE TABLE `module`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `module_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime NOT NULL,
  `parent` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '上级模板目录，null代表位第一级目录',
  `is_menu` int NOT NULL DEFAULT 0 COMMENT '0:不是目录，是模板名；1：是目录',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '0：未删除；1：已删除',
  `audit_status` int NULL DEFAULT NULL COMMENT '审核状态：0未审核，1审核通过，2审核未通过',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of module
-- ----------------------------
INSERT INTO `module` VALUES (1, '基础模板', 'admin', '2026-03-24 17:11:23', NULL, 1, 0, NULL);
INSERT INTO `module` VALUES (2, '人物模板', 'admin', '2026-03-24 17:11:43', '基础模板', 1, 0, NULL);
INSERT INTO `module` VALUES (3, '人物细节模板', 'admin', '2026-03-24 17:31:07', '人物模板', 0, 0, 2);

-- ----------------------------
-- Table structure for module_column
-- ----------------------------
DROP TABLE IF EXISTS `module_column`;
CREATE TABLE `module_column`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `column_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '列名',
  `column_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '列类型',
  `module_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属的模板名',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否被删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of module_column
-- ----------------------------
INSERT INTO `module_column` VALUES (1, '角色名称', 'varchar', '人物细节模板', 0);
INSERT INTO `module_column` VALUES (2, '职业', 'varchar', '人物细节模板', 0);
INSERT INTO `module_column` VALUES (3, '等级', 'int', '人物细节模板', 0);

-- ----------------------------
-- Table structure for module_data
-- ----------------------------
DROP TABLE IF EXISTS `module_data`;
CREATE TABLE `module_data`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `column_id` int NOT NULL COMMENT '对应列id',
  `row_id` int NOT NULL COMMENT '行号',
  `data_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据类型',
  `data` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据',
  `deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of module_data
-- ----------------------------
INSERT INTO `module_data` VALUES (1, 1, 1, 'string', '1', 0);
INSERT INTO `module_data` VALUES (2, 2, 1, 'string', '1', 0);
INSERT INTO `module_data` VALUES (3, 3, 1, 'string', '1', 0);
INSERT INTO `module_data` VALUES (4, 1, 2, 'string', '2', 0);
INSERT INTO `module_data` VALUES (5, 2, 2, 'string', '2', 0);
INSERT INTO `module_data` VALUES (6, 3, 2, 'string', '2', 0);

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `telephone` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `role` int NOT NULL DEFAULT 0 COMMENT '角色身份：0：普通用户；1：管理员；2：超级管理员',
  `create_time` datetime NOT NULL,
  `deleted` int NOT NULL DEFAULT 0 COMMENT '人员是否被删除占位符',
  `real_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `second_password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (2, 'admin', '$2a$10$0u.8vbi3kcd1zIFl6mg4..siT3zWzTtT4pttMlVog8EhtqKQS6LP6', '13800138000', 'admin@qq.com', 0, '2026-03-16 20:30:37', 0, NULL, NULL);

-- ----------------------------
-- Table structure for user_department
-- ----------------------------
DROP TABLE IF EXISTS `user_department`;
CREATE TABLE `user_department`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `department` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `user_state` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_department
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
