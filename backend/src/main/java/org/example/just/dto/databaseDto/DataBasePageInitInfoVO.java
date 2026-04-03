//package org.example.just.dto.databaseDto;
//
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Data;
//import org.example.just.service.imp.DataBaseServiceImpl;
//
//import java.util.List;
//
//@Data
//@Schema(name = "DataBasePageInitInfoVO", description = "数据库页面初始化信息响应")
//public class DataBasePageInitInfoVO {
//
//    @Schema(description = "左侧目录树")
//    private List<DataBaseServiceImpl.ClassificationTreeNode> tree;
//}
package org.example.just.dto.databaseDto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(name = "DataBasePageInitInfoVO", description = "数据库页面初始化信息响应")
public class DataBasePageInitInfoVO {

    @Schema(description = "左侧目录树")
    private List<ClassificationTreeNode> tree;

    @Schema(description = "行业分类下拉选项")
    private List<OptionItem> industryCategories;

    @Schema(description = "数据分类下拉选项")
    private List<OptionItem> dataCategories;

    @Schema(description = "部门下拉选项")
    private List<OptionItem> departments;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "数据集列表")
    private List<DatasetItem> list;


        @Data
    @Schema(name = "ClassificationTreeNode", description = "分类树节点")
    public static class ClassificationTreeNode {

        @Schema(description = "节点 ID")
        private String id;

        @Schema(description = "节点标签")
        private String label;

        @Schema(description = "子节点列表")
        private List<ClassificationTreeNode> children;

        // 构造函数
        public ClassificationTreeNode(String id, String label) {
            this.id = id;
            this.label = label;
            this.children = new ArrayList<>();
        }

        // 无参构造函数（Lombok 的@Data 不会自动生成）
        public ClassificationTreeNode() {
            this.children = new ArrayList<>();
        }

        // 辅助方法：添加子节点
        public void addChild(ClassificationTreeNode node) {
            this.children.add(node);
        }
    }


    @Data
    @Schema(name = "OptionItem", description = "下拉选项项")
    public static class OptionItem {

        @Schema(description = "选项标签")
        private String label;

        @Schema(description = "选项值")
        private String value;
    }

    @Data
    @Schema(name = "DatasetItem", description = "数据集项")
    public static class DatasetItem {

        @Schema(description = "数据集 ID")
        private Integer id;

        @Schema(description = "数据集名称")
        private String datasetName;

        @Schema(description = "学科分类列表")
        private List<String> scienceCategories;

        @Schema(description = "行业分类列表")
        private List<String> industryCategories;

        @Schema(description = "产品代码列表")
        private List<String> productCodes;

        @Schema(description = "数据级别：公益、内部等")
        private String dataLevel;

        @Schema(description = "数据条数")
        private Integer dataCount;

        @Schema(description = "模板名称")
        private String templateName;

        @Schema(description = "数据分类")
        private String dataCategory;

        @Schema(description = "所属部门")
        private String department;

        @Schema(description = "创建人")
        private String creator;

        @Schema(description = "创建时间")
        private java.time.LocalDateTime createTime;

        @Schema(description = "更新时间")
        private java.time.LocalDateTime updateTime;

        @Schema(description = "是否已收藏")
        private Boolean favorited;
    }
}
