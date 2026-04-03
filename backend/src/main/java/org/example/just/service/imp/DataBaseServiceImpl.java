package org.example.just.service.imp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Resource;
import org.example.just.dao.IndustryClassificationDao;
import org.example.just.dto.databaseDto.DataBasePageInitInfoVO;
import org.example.just.entity.IndustryClassificationEntity;
import org.example.just.service.DataBaseService;
import org.example.just.utils.Result;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DataBaseServiceImpl implements DataBaseService {

    @Resource
    private IndustryClassificationDao industryClassificationDao;

    @Override
    public Result<DataBasePageInitInfoVO> getPageInitInfo() {
        DataBasePageInitInfoVO pageInitInfoVO = new DataBasePageInitInfoVO();
        pageInitInfoVO.setTree(getClassificationTree());
        return Result.success(pageInitInfoVO);
    }


    public List<ClassificationTreeNode> getClassificationTree() {
        // 1. 从数据库获取所有分类数据 (假设返回的是实体列表)
        List<IndustryClassificationEntity> allList = industryClassificationDao.selectList(null);

        // 2. 创建根节点 "全部"
        ClassificationTreeNode rootAll = new ClassificationTreeNode("all", "全部");

        // 3. 查找 "生物医用材料（科学）" 节点，构建其子树
        // 注意：这里硬编码了ID 'bio_med_science'，因为它是树构建的入口
        Optional<IndustryClassificationEntity> sciRootOpt = allList.stream()
                .filter(item -> "bio_med_science".equals(item.getId()))
                .findFirst();

        if (sciRootOpt.isPresent()) {
            IndustryClassificationEntity sciRootEntity = sciRootOpt.get();
            ClassificationTreeNode sciRootNode = new ClassificationTreeNode(sciRootEntity.getId(), sciRootEntity.getName());

            // 递归构建 "科学" 分支的子节点
            buildTree(sciRootNode, allList);

            // 将构建好的科学分支添加到 "全部" 下面
            rootAll.addChild(sciRootNode);
        }

        // 4. 如果有其他顶级节点，也可以在这里继续添加...

        // 5. 封装成 List 返回
        List<ClassificationTreeNode> result = new ArrayList<>();
        result.add(rootAll);
        return result;
    }

    /**
     * 递归构建树形结构
     * @param parentNode 当前父节点对象
     * @param allNodes   数据库全量列表
     */
    private void buildTree(ClassificationTreeNode parentNode, List<IndustryClassificationEntity> allNodes) {
        // 找出所有 parent_id 等于当前节点 id 的子节点
        List<IndustryClassificationEntity> children = allNodes.stream()
                .filter(node -> Objects.equals(node.getParentId(), parentNode.getId()))
                .collect(Collectors.toList());

        for (IndustryClassificationEntity child : children) {
            ClassificationTreeNode childNode = new ClassificationTreeNode(child.getId(), child.getName());
            parentNode.addChild(childNode);

            // 递归调用，继续找下一层
            buildTree(childNode, allNodes);
        }
    }

    public static class ClassificationTreeNode {
        private String id;
        private String label; // 对应数据库的 name
        private List<ClassificationTreeNode> children = new ArrayList<>();

        // 构造函数
        public ClassificationTreeNode(String id, String label) {
            this.id = id;
            this.label = label;
        }

        // Getters and Setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public List<ClassificationTreeNode> getChildren() { return children; }
        public void setChildren(List<ClassificationTreeNode> children) { this.children = children; }

        // 辅助方法：添加子节点
        public void addChild(ClassificationTreeNode node) {
            this.children.add(node);
        }
    }
}
