ALTER TABLE `manu_dataset`
  ADD COLUMN `summary` varchar(500) NOT NULL DEFAULT '' COMMENT '数据集摘要' AFTER `name`,
  ADD COLUMN `cover_url` varchar(1024) DEFAULT NULL COMMENT '封面图URL' AFTER `summary`,
  ADD COLUMN `science_category_id` int DEFAULT NULL COMMENT '科学分类ID' AFTER `module`,
  ADD COLUMN `product_category_id` int DEFAULT NULL COMMENT '产业/产品分类ID' AFTER `science_category_id`,
  ADD COLUMN `data_level` varchar(32) DEFAULT NULL COMMENT '数据级别' AFTER `product_category_id`,
  ADD COLUMN `data_category` varchar(32) NOT NULL DEFAULT 'dataset' COMMENT '数据类别' AFTER `data_level`,
  ADD COLUMN `template_tag_id` int DEFAULT NULL COMMENT '模板标签ID' AFTER `data_category`,
  ADD COLUMN `dataset_tag_ids` varchar(255) DEFAULT NULL COMMENT '数据集标签ID列表JSON' AFTER `template_tag_id`;
