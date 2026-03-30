-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: localhost    Database: just
-- ------------------------------------------------------
-- Server version	8.0.42

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `dataset_column`
--

DROP TABLE IF EXISTS `dataset_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dataset_column` (
  `id` int NOT NULL AUTO_INCREMENT,
  `column_name` varchar(255) NOT NULL COMMENT '列名',
  `column_type` varchar(255) NOT NULL COMMENT '列类型',
  `dataset_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '所属的模板名',
  `deleted` int NOT NULL DEFAULT '0' COMMENT '是否被删除',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dataset_column`
--

LOCK TABLES `dataset_column` WRITE;
/*!40000 ALTER TABLE `dataset_column` DISABLE KEYS */;
INSERT INTO `dataset_column` VALUES (1,'角色名称','varchar','人物细节模板',0),(2,'职业','varchar','人物细节模板',0),(3,'等级','int','人物细节模板',0),(4,'武器名称','varchar','武器具体模板',0),(5,'攻击力','varchar','武器具体模板',0),(6,'生命值','int','人物细节模板',0);
/*!40000 ALTER TABLE `dataset_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dataset_data`
--

DROP TABLE IF EXISTS `dataset_data`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dataset_data` (
  `id` int NOT NULL AUTO_INCREMENT,
  `column_id` int NOT NULL COMMENT '对应列id',
  `row_id` int NOT NULL COMMENT '行号',
  `data_type` varchar(255) NOT NULL COMMENT '数据类型',
  `data` varchar(255) NOT NULL COMMENT '数据',
  `deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dataset_data`
--

LOCK TABLES `dataset_data` WRITE;
/*!40000 ALTER TABLE `dataset_data` DISABLE KEYS */;
INSERT INTO `dataset_data` VALUES (1,1,1,'string','1',0),(2,2,1,'string','1',0),(3,3,1,'string','1',0),(4,1,2,'string','2',0),(5,2,2,'string','2',0),(6,3,2,'string','2',0);
/*!40000 ALTER TABLE `dataset_data` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `department`
--

DROP TABLE IF EXISTS `department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `department` (
  `id` int NOT NULL AUTO_INCREMENT,
  `department_name` varchar(255) NOT NULL COMMENT '部门名称',
  `leader_id` int NOT NULL COMMENT '负责人id',
  `leader_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '部门负责人姓名',
  `deleted` int NOT NULL COMMENT '0',
  `created_time` datetime NOT NULL COMMENT '信息创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `department`
--

LOCK TABLES `department` WRITE;
/*!40000 ALTER TABLE `department` DISABLE KEYS */;
/*!40000 ALTER TABLE `department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `manu_dataset`
--

DROP TABLE IF EXISTS `manu_dataset`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `manu_dataset` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `creator` varchar(255) NOT NULL,
  `create_time` datetime NOT NULL,
  `parent` int NOT NULL DEFAULT '0' COMMENT '上级模板目录，0代表位第一级目录',
  `is_menu` int NOT NULL DEFAULT '0' COMMENT '0:不是目录，是数据集；1：是目录',
  `deleted` int NOT NULL DEFAULT '0' COMMENT '0：未删除；1：已删除',
  `module` int DEFAULT NULL COMMENT '在是数据库时字段才有意义',
  `audit_status` int NOT NULL DEFAULT '0' COMMENT '审核状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `manu_dataset`
--

LOCK TABLES `manu_dataset` WRITE;
/*!40000 ALTER TABLE `manu_dataset` DISABLE KEYS */;
INSERT INTO `manu_dataset` VALUES (1,'基础模板','admin','2026-03-24 17:11:23',0,1,0,NULL,0),(2,'人物模板','admin','2026-03-24 17:11:43',1,1,0,NULL,0),(3,'人物细节模板','admin','2026-03-24 17:31:07',2,0,0,NULL,0),(4,'武器模板','admin','2026-03-24 19:27:09',1,1,0,NULL,0),(5,'武器具体模板','admin','2026-03-24 19:32:35',4,0,0,NULL,0);
/*!40000 ALTER TABLE `manu_dataset` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `module`
--

DROP TABLE IF EXISTS `module`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `module` (
  `id` int NOT NULL AUTO_INCREMENT,
  `module_name` varchar(255) NOT NULL COMMENT '模板名称',
  `tag` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '模板标签',
  `description` varchar(255) DEFAULT NULL COMMENT '模板说明',
  `creator` int NOT NULL COMMENT '创建者id',
  `visible_area` int NOT NULL COMMENT '1:public;0:private',
  `agree` int NOT NULL COMMENT '1:yes;0:no',
  `deleted` int NOT NULL DEFAULT '0' COMMENT '0:no;1:yes',
  `create_time` datetime NOT NULL,
  `audit_state` int NOT NULL DEFAULT '0' COMMENT '审核状态',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `module`
--

LOCK TABLES `module` WRITE;
/*!40000 ALTER TABLE `module` DISABLE KEYS */;
/*!40000 ALTER TABLE `module` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `module_column`
--

DROP TABLE IF EXISTS `module_column`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `module_column` (
  `id` int NOT NULL AUTO_INCREMENT,
  `module_id` int NOT NULL COMMENT '模板id，属于哪个模板',
  `column_name` varchar(255) NOT NULL COMMENT '列名',
  `type` varchar(255) NOT NULL COMMENT '列类型',
  `belong` varchar(255) NOT NULL COMMENT '属于Object、Operation还是Result',
  `create_time` datetime NOT NULL,
  `deleted` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `module_column`
--

LOCK TABLES `module_column` WRITE;
/*!40000 ALTER TABLE `module_column` DISABLE KEYS */;
/*!40000 ALTER TABLE `module_column` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `telephone` varchar(255) NOT NULL,
  `email` varchar(255) NOT NULL,
  `role` int NOT NULL DEFAULT '0' COMMENT '角色身份：0：普通用户；1：管理员；2：超级管理员',
  `create_time` datetime NOT NULL,
  `deleted` int NOT NULL DEFAULT '0' COMMENT '人员是否被删除占位符',
  `real_name` varchar(255) DEFAULT NULL,
  `second_password` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (2,'admin','$2a$10$0u.8vbi3kcd1zIFl6mg4..siT3zWzTtT4pttMlVog8EhtqKQS6LP6','13800138000','admin@qq.com',0,'2026-03-16 20:30:37',0,NULL,NULL);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_department`
--

DROP TABLE IF EXISTS `user_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_department` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL,
  `department` varchar(255) NOT NULL,
  `user_state` int NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_department`
--

LOCK TABLES `user_department` WRITE;
/*!40000 ALTER TABLE `user_department` DISABLE KEYS */;
/*!40000 ALTER TABLE `user_department` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-30 22:21:01
