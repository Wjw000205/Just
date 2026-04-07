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

 Date: 07/04/2026 15:59:29
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for database_page_static_data
-- ----------------------------
DROP TABLE IF EXISTS `database_page_static_data`;
CREATE TABLE `database_page_static_data`  (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` int NULL DEFAULT 0 COMMENT '父级ID，顶级节点为0',
  `category_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据类型标识: industry(产业), data_category(数据类别), department(所属部门)',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '显示名称',
  `sort_order` int NULL DEFAULT 0 COMMENT '排序字段',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_type_parent`(`category_type` ASC, `parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 38 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '前端数据库页面静态数据表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of database_page_static_data
-- ----------------------------
INSERT INTO `database_page_static_data` VALUES (1, 0, 'data_category', '数据集', 1);
INSERT INTO `database_page_static_data` VALUES (2, 0, 'data_category', '科技论文', 2);
INSERT INTO `database_page_static_data` VALUES (3, 0, 'data_category', '专利文献', 3);
INSERT INTO `database_page_static_data` VALUES (4, 0, 'data_category', '标准规范', 4);
INSERT INTO `database_page_static_data` VALUES (5, 0, 'data_category', '学术专著', 5);
INSERT INTO `database_page_static_data` VALUES (6, 0, 'data_category', '学位论文', 6);
INSERT INTO `database_page_static_data` VALUES (7, 0, 'department', '社会大众', 1);
INSERT INTO `database_page_static_data` VALUES (8, 0, 'department', '节点管理部', 2);
INSERT INTO `database_page_static_data` VALUES (9, 0, 'department', '知网', 3);
INSERT INTO `database_page_static_data` VALUES (10, 0, 'department', '川大生材中心', 4);
INSERT INTO `database_page_static_data` VALUES (11, 0, 'department', '中南大学', 5);
INSERT INTO `database_page_static_data` VALUES (12, 0, 'department', '解放军第四医院', 6);
INSERT INTO `database_page_static_data` VALUES (13, 0, 'department', '西南交大', 7);
INSERT INTO `database_page_static_data` VALUES (14, 0, 'department', '上硅所', 8);
INSERT INTO `database_page_static_data` VALUES (15, 0, 'department', '嘉思特', 9);
INSERT INTO `database_page_static_data` VALUES (16, 0, 'department', '锦波生物', 10);

-- ----------------------------
-- Table structure for dataset_column
-- ----------------------------
DROP TABLE IF EXISTS `dataset_column`;
CREATE TABLE `dataset_column`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `column_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '列名',
  `column_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '列类型',
  `dataset_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属的模板名',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '是否被删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dataset_column
-- ----------------------------
INSERT INTO `dataset_column` VALUES (1, '角色名称', 'varchar', '人物细节模板', 0);
INSERT INTO `dataset_column` VALUES (2, '职业', 'varchar', '人物细节模板', 0);
INSERT INTO `dataset_column` VALUES (3, '等级', 'int', '人物细节模板', 0);
INSERT INTO `dataset_column` VALUES (4, '武器名称', 'varchar', '武器具体模板', 0);
INSERT INTO `dataset_column` VALUES (5, '攻击力', 'varchar', '武器具体模板', 0);
INSERT INTO `dataset_column` VALUES (6, '生命值', 'int', '人物细节模板', 0);

-- ----------------------------
-- Table structure for dataset_data
-- ----------------------------
DROP TABLE IF EXISTS `dataset_data`;
CREATE TABLE `dataset_data`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `column_id` int NOT NULL COMMENT '对应列id',
  `row_id` int NOT NULL COMMENT '行号',
  `data_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据类型',
  `data` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '数据',
  `deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of dataset_data
-- ----------------------------
INSERT INTO `dataset_data` VALUES (1, 1, 1, 'string', '1', 0);
INSERT INTO `dataset_data` VALUES (2, 2, 1, 'string', '1', 0);
INSERT INTO `dataset_data` VALUES (3, 3, 1, 'string', '1', 0);
INSERT INTO `dataset_data` VALUES (4, 1, 2, 'string', '2', 0);
INSERT INTO `dataset_data` VALUES (5, 2, 2, 'string', '2', 0);
INSERT INTO `dataset_data` VALUES (6, 3, 2, 'string', '2', 0);

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of department
-- ----------------------------

-- ----------------------------
-- Table structure for industry_classification
-- ----------------------------
DROP TABLE IF EXISTS `industry_classification`;
CREATE TABLE `industry_classification`  (
  `id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类名称',
  `parent_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '父级分类 ID，null 代表顶级分类',
  `level` int NOT NULL DEFAULT 1 COMMENT '层级，从 1 开始',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序顺序',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE,
  INDEX `idx_level`(`level` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '工业战略性新兴产业分类目录' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of industry_classification
-- ----------------------------
INSERT INTO `industry_classification` VALUES ('advanced_material', '先进基础材料', NULL, 1, 1);
INSERT INTO `industry_classification` VALUES ('alpha_tcp', 'α-磷酸三钙', 'inorganic_materials', 4, 4);
INSERT INTO `industry_classification` VALUES ('beta_tcp', 'β-磷酸三钙', 'inorganic_materials', 4, 3);
INSERT INTO `industry_classification` VALUES ('bio_med_industry', '生物医用材料（产业）', 'advanced_material', 2, 2);
INSERT INTO `industry_classification` VALUES ('bio_med_science', '生物医用材料（科学）', 'advanced_material', 2, 1);
INSERT INTO `industry_classification` VALUES ('biological_materials', '生物衍生材料', 'bio_med_science', 3, 5);
INSERT INTO `industry_classification` VALUES ('biphasic_calcium_phosphate', '双相磷酸钙', 'inorganic_materials', 4, 5);
INSERT INTO `industry_classification` VALUES ('calcium_phosphate_ceramics', '磷酸钙陶瓷', 'inorganic_materials', 4, 1);
INSERT INTO `industry_classification` VALUES ('composite_materials', '生物医用复合材料', 'bio_med_science', 3, 4);
INSERT INTO `industry_classification` VALUES ('hydroxyapatite', '羟基磷灰石', 'inorganic_materials', 4, 2);
INSERT INTO `industry_classification` VALUES ('inorganic_materials', '生物医用无机材料', 'bio_med_science', 3, 1);
INSERT INTO `industry_classification` VALUES ('iron_alloys', '铁及铁合金', 'metal_materials', 4, 3);
INSERT INTO `industry_classification` VALUES ('magnesium_alloys', '镁及镁合金', 'metal_materials', 4, 2);
INSERT INTO `industry_classification` VALUES ('metal_materials', '生物医用金属材料', 'bio_med_science', 3, 2);
INSERT INTO `industry_classification` VALUES ('polymer_materials', '生物医用高分子材料', 'bio_med_science', 3, 3);
INSERT INTO `industry_classification` VALUES ('titanium_alloys', '钛及钛合金', 'metal_materials', 4, 1);
INSERT INTO `industry_classification` VALUES ('zinc_alloys', '锌及锌合金', 'metal_materials', 4, 4);

-- ----------------------------
-- Table structure for manu_dataset
-- ----------------------------
DROP TABLE IF EXISTS `manu_dataset`;
CREATE TABLE `manu_dataset`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `creator` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `create_time` datetime NOT NULL,
  `parent` int NOT NULL DEFAULT 0 COMMENT '上级模板目录，0代表位第一级目录',
  `is_menu` int NOT NULL DEFAULT 0 COMMENT '0:不是目录，是数据集；1：是目录',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '0：未删除；1：已删除',
  `module` int NULL DEFAULT NULL COMMENT '在是数据库时字段才有意义',
  `audit_status` int NOT NULL DEFAULT 0 COMMENT '审核状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of manu_dataset
-- ----------------------------
INSERT INTO `manu_dataset` VALUES (1, '基础模板', 'admin', '2026-03-24 17:11:23', 0, 1, 0, NULL, 0);
INSERT INTO `manu_dataset` VALUES (2, '人物模板', 'admin', '2026-03-24 17:11:43', 1, 1, 0, NULL, 0);
INSERT INTO `manu_dataset` VALUES (3, '人物细节模板', 'admin', '2026-03-24 17:31:07', 2, 0, 0, NULL, 0);
INSERT INTO `manu_dataset` VALUES (4, '武器模板', 'admin', '2026-03-24 19:27:09', 1, 1, 0, NULL, 0);
INSERT INTO `manu_dataset` VALUES (5, '武器具体模板', 'admin', '2026-03-24 19:32:35', 4, 0, 0, NULL, 0);

-- ----------------------------
-- Table structure for module
-- ----------------------------
DROP TABLE IF EXISTS `module`;
CREATE TABLE `module`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `module_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板名称',
  `tag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板标签',
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '模板说明',
  `creator` int NOT NULL COMMENT '创建者id',
  `visible_area` int NOT NULL COMMENT '1:public;0:private',
  `agree` int NOT NULL COMMENT '1:yes;0:no',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '0:no;1:yes',
  `create_time` datetime NOT NULL,
  `audit_state` int NOT NULL DEFAULT 0 COMMENT '审核状态',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of module
-- ----------------------------
INSERT INTO `module` VALUES (1, '人物细节模板', 'S', NULL, 1, 2, 1, 0, '2026-04-03 18:52:36', 0);

-- ----------------------------
-- Table structure for module_column
-- ----------------------------
DROP TABLE IF EXISTS `module_column`;
CREATE TABLE `module_column`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `module_id` int NOT NULL COMMENT '模板id，属于哪个模板',
  `column_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '列名',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '列类型',
  `belong` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '属于Object、Operation还是Result',
  `create_time` datetime NOT NULL,
  `deleted` int NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of module_column
-- ----------------------------

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
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_department
-- ----------------------------

-- ----------------------------
-- Table structure for user_favorite
-- ----------------------------
DROP TABLE IF EXISTS `user_favorite`;
CREATE TABLE `user_favorite`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `dataset_id` int NOT NULL COMMENT '数据集 ID',
  `favorited` tinyint NOT NULL DEFAULT 0 COMMENT '是否收藏：1 收藏，0 取消收藏',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` int NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username_dataset`(`username` ASC, `dataset_id` ASC) USING BTREE,
  INDEX `idx_dataset_id`(`dataset_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_favorite
-- ----------------------------
INSERT INTO `user_favorite` VALUES (1, 'admin', 1, 1, '2026-04-03 19:21:20', '2026-04-03 19:21:20', 0);

SET FOREIGN_KEY_CHECKS = 1;
