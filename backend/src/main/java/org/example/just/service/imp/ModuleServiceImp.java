package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.just.dao.ModuleColumnDao;
import org.example.just.dao.ModuleDao;
import org.example.just.dao.ModuleDataDao;
import org.example.just.dto.moduleDto.*;
import org.example.just.entity.ModuleColumnEntity;
import org.example.just.entity.ModuleDataEntity;
import org.example.just.entity.ModuleEntity;
import org.example.just.service.ModuleService;
import org.example.just.utils.PageQuery;
import org.example.just.utils.PageResult;
import org.example.just.utils.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ModuleServiceImp implements ModuleService {

    private final ModuleDao moduleDao;
    private final ModuleColumnDao moduleColumnDao;
    private final ModuleDataDao moduleDataDao;

    public ModuleServiceImp(ModuleDao moduleDao,
                            ModuleColumnDao moduleColumnDao,
                            ModuleDataDao moduleDataDao) {
        this.moduleDao = moduleDao;
        this.moduleColumnDao = moduleColumnDao;
        this.moduleDataDao = moduleDataDao;
    }

    @Transactional
    @Override
    public Result<String> createMenu(CreateModuleMenuDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getModuleName())) {
            return Result.fail("目录名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCreator())) {
            return Result.fail("创建人不能为空");
        }

        String moduleName = dto.getModuleName().trim();
        String creator = dto.getCreator().trim();
        String parent = StringUtils.hasText(dto.getParent()) ? dto.getParent().trim() : null;

        // 1. 如果是创建次级目录，先校验父目录是否存在
        if (StringUtils.hasText(parent)) {
            LambdaQueryWrapper<ModuleEntity> parentWrapper = new LambdaQueryWrapper<>();
            parentWrapper.eq(ModuleEntity::getModuleName, parent)
                    .eq(ModuleEntity::getIsMenu, 1)
                    .last("limit 1");

            ModuleEntity parentModule = moduleDao.selectOne(parentWrapper);
            if (parentModule == null) {
                return Result.fail("父级目录不存在");
            }
        }

        // 2. 校验同级目录名称不能重复
        LambdaQueryWrapper<ModuleEntity> sameLevelWrapper = new LambdaQueryWrapper<>();
        sameLevelWrapper.eq(ModuleEntity::getModuleName, moduleName)
                .eq(ModuleEntity::getIsMenu, 1);

        if (StringUtils.hasText(parent)) {
            sameLevelWrapper.eq(ModuleEntity::getParent, parent);
        } else {
            sameLevelWrapper.isNull(ModuleEntity::getParent);
        }

        if (moduleDao.selectCount(sameLevelWrapper) > 0) {
            return Result.fail("同级目录下已存在同名目录");
        }

        // 3. 插入目录
        ModuleEntity module = new ModuleEntity();
        module.setModuleName(moduleName);
        module.setCreator(creator);
        module.setCreateTime(LocalDateTime.now());
        module.setParent(parent);
        module.setIsMenu(1);
        module.setDeleted(0);

        int rows = moduleDao.insert(module);
        if (rows <= 0) {
            return Result.fail("创建目录失败");
        }

        return Result.success("创建目录成功");
    }

    @Override
    public Result<List<ModuleMenuVO>> getModuleTree() {
        LambdaQueryWrapper<ModuleEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper
                .orderByAsc(ModuleEntity::getId);

        List<ModuleEntity> moduleList = moduleDao.selectList(wrapper);

        if (moduleList == null || moduleList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 先转成 VO
        List<ModuleMenuVO> voList = moduleList.stream().map(module -> {
            ModuleMenuVO vo = new ModuleMenuVO();
            vo.setId(module.getId());
            vo.setModuleName(module.getModuleName());
            vo.setCreator(module.getCreator());
            vo.setCreateTime(module.getCreateTime());
            vo.setParent(module.getParent());
            vo.setIsMenu(module.getIsMenu());
            return vo;
        }).collect(Collectors.toList());

        // 用 moduleName 建索引，方便挂子节点
        Map<String, ModuleMenuVO> moduleMap = voList.stream()
                .collect(Collectors.toMap(ModuleMenuVO::getModuleName, item -> item, (a, b) -> a));

        List<ModuleMenuVO> rootList = new ArrayList<>();

        for (ModuleMenuVO node : voList) {
            if (!StringUtils.hasText(node.getParent())) {
                // 一级目录
                rootList.add(node);
            } else {
                // 子目录挂到父目录下面
                ModuleMenuVO parentNode = moduleMap.get(node.getParent());
                if (parentNode != null) {
                    parentNode.getChildren().add(node);
                } else {
                    // 如果父目录不存在，兜底当根节点返回，避免数据丢失
                    rootList.add(node);
                }
            }
        }

        return Result.success(rootList);
    }

    @Transactional
    @Override
    public Result<String> createModule(CreateModuleDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getModuleName())) {
            return Result.fail("模板名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCreator())) {
            return Result.fail("创建人不能为空");
        }
        if (!StringUtils.hasText(dto.getParent())) {
            return Result.fail("父目录不能为空");
        }
        if (dto.getColumns() == null || dto.getColumns().isEmpty()) {
            return Result.fail("模板列不能为空");
        }

        String moduleName = dto.getModuleName().trim();
        String creator = dto.getCreator().trim();
        String parent = dto.getParent().trim();
        List<ModuleColumnDTO> columns = dto.getColumns();

        // 1. 校验父目录存在，并且必须是目录
        LambdaQueryWrapper<ModuleEntity> parentWrapper = new LambdaQueryWrapper<>();
        parentWrapper.eq(ModuleEntity::getModuleName, parent)
                .eq(ModuleEntity::getIsMenu, 1)
                .last("limit 1");

        ModuleEntity parentModule = moduleDao.selectOne(parentWrapper);
        if (parentModule == null) {
            return Result.fail("父目录不存在");
        }

        // 2. 校验同一父目录下模板名称不能重复
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getModuleName, moduleName)
                .eq(ModuleEntity::getParent, parent)
                .eq(ModuleEntity::getIsMenu, 0);

        if (moduleDao.selectCount(moduleWrapper) > 0) {
            return Result.fail("同一目录下已存在同名模板");
        }

        // 3. 校验列定义
        Set<String> columnNameSet = new HashSet<>();
        for (ModuleColumnDTO column : columns) {
            if (column == null) {
                return Result.fail("模板列参数不能为空");
            }
            if (!StringUtils.hasText(column.getColumnName())) {
                return Result.fail("列名不能为空");
            }
            if (!StringUtils.hasText(column.getColumnType())) {
                return Result.fail("列类型不能为空");
            }

            String columnName = column.getColumnName().trim();
            if (!columnNameSet.add(columnName)) {
                return Result.fail("模板列名称不能重复: " + columnName);
            }
        }

        // 4. 插入 module 表
        ModuleEntity module = new ModuleEntity();
        module.setModuleName(moduleName);
        module.setCreator(creator);
        module.setCreateTime(LocalDateTime.now());
        module.setParent(parent);
        module.setIsMenu(0);
        module.setDeleted(0);

        int moduleRows = moduleDao.insert(module);
        if (moduleRows <= 0) {
            return Result.fail("创建模板失败");
        }

        // 5. 插入 module_column 表
        for (ModuleColumnDTO column : columns) {
            ModuleColumnEntity moduleColumn = new ModuleColumnEntity();
            moduleColumn.setColumnName(column.getColumnName().trim());
            moduleColumn.setColumnType(column.getColumnType().trim());
            moduleColumn.setModuleName(moduleName);
            moduleColumn.setDeleted(0);

            int columnRows = moduleColumnDao.insert(moduleColumn);
            if (columnRows <= 0) {
                throw new RuntimeException("插入模板列失败");
            }
        }

        return Result.success("创建模板成功");
    }

    @Transactional
    @Override
    public Result<String> importModuleData(String moduleName, MultipartFile file) {
        if (!StringUtils.hasText(moduleName)) {
            return Result.fail("模板名称不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getModuleName, moduleName.trim())
                .eq(ModuleEntity::getIsMenu, 0)
                .last("limit 1");
        ModuleEntity module = moduleDao.selectOne(moduleWrapper);
        if (module == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板对应列
        LambdaQueryWrapper<ModuleColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(ModuleColumnEntity::getModuleName, moduleName.trim());
        List<ModuleColumnEntity> columnList = moduleColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        Map<String, ModuleColumnEntity> columnMap = columnList.stream()
                .collect(Collectors.toMap(ModuleColumnEntity::getColumnName, item -> item, (a, b) -> a));

        List<Integer> columnIds = columnList.stream()
                .map(ModuleColumnEntity::getId)
                .collect(Collectors.toList());

        Integer maxRowId = moduleDataDao.selectMaxRowIdByColumnIds(columnIds);
        int nextRowId = (maxRowId == null ? 0 : maxRowId) + 1;

        List<ModuleDataEntity> batchList = new ArrayList<>();
        DataFormatter dataFormatter = new DataFormatter();

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return Result.fail("Excel中没有可读取的Sheet");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return Result.fail("Excel表头不能为空");
            }

            // 3. 读取表头，并建立 Excel 列索引 -> 模板列 的映射
            Map<Integer, ModuleColumnEntity> matchedColumnIndexMap = new LinkedHashMap<>();
            int lastCellNum = headerRow.getLastCellNum();
            for (int i = 0; i < lastCellNum; i++) {
                Cell cell = headerRow.getCell(i);
                String headerName = dataFormatter.formatCellValue(cell).trim();
                if (!StringUtils.hasText(headerName)) {
                    continue;
                }

                ModuleColumnEntity matchedColumn = columnMap.get(headerName);
                if (matchedColumn != null) {
                    matchedColumnIndexMap.put(i, matchedColumn);
                }
            }

            if (matchedColumnIndexMap.isEmpty()) {
                return Result.fail("Excel表头与模板列没有任何匹配项");
            }

            // 4. 遍历数据行
            int lastRowNum = sheet.getLastRowNum();
            int importedRowCount = 0;

            for (int rowIndex = 1; rowIndex <= lastRowNum; rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (dataRow == null) {
                    continue;
                }

                boolean rowHasValue = false;
                List<ModuleDataEntity> currentRowDataList = new ArrayList<>();

                for (Map.Entry<Integer, ModuleColumnEntity> entry : matchedColumnIndexMap.entrySet()) {
                    Integer cellIndex = entry.getKey();
                    ModuleColumnEntity moduleColumn = entry.getValue();

                    Cell cell = dataRow.getCell(cellIndex);
                    String cellValue = dataFormatter.formatCellValue(cell);
                    if (cellValue != null) {
                        cellValue = cellValue.trim();
                    }

                    if (StringUtils.hasText(cellValue)) {
                        rowHasValue = true;
                    }

                    ModuleDataEntity moduleData = new ModuleDataEntity();
                    moduleData.setColumnId(moduleColumn.getId());
                    moduleData.setRowId(nextRowId);
                    moduleData.setDataType(moduleColumn.getColumnType());
                    moduleData.setData(cellValue);
                    moduleData.setDeleted(0);

                    currentRowDataList.add(moduleData);
                }

                // 整行都为空，跳过，不占 row_id
                if (!rowHasValue) {
                    continue;
                }

                batchList.addAll(currentRowDataList);
                importedRowCount++;
                nextRowId++;
            }

            if (batchList.isEmpty()) {
                return Result.fail("没有可导入的数据");
            }

            moduleDataDao.insertBatch(batchList);
            return Result.success("导入成功，共导入 " + importedRowCount + " 行数据");

        } catch (Exception e) {
            throw new RuntimeException("导入模板数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Result<ModuleDataPageVO> getModuleDataPage(String moduleName, PageQuery pageQuery) {
        if (!StringUtils.hasText(moduleName)) {
            return Result.fail("模板名称不能为空");
        }
        if (pageQuery == null) {
            return Result.fail("分页参数不能为空");
        }

        // 1. 校验模板存在，并且必须是模板而不是目录
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getModuleName, moduleName.trim())
                .eq(ModuleEntity::getIsMenu, 0)
                .last("limit 1");
        ModuleEntity module = moduleDao.selectOne(moduleWrapper);
        if (module == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板列
        LambdaQueryWrapper<ModuleColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(ModuleColumnEntity::getModuleName, moduleName.trim())
                .orderByAsc(ModuleColumnEntity::getId);

        List<ModuleColumnEntity> columnList = moduleColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        List<Integer> columnIds = columnList.stream()
                .map(ModuleColumnEntity::getId)
                .toList();

        // 3. 先统计总行数
        Long total = moduleDataDao.countDistinctRowIds(columnIds);
        if (total == null) {
            total = 0L;
        }

        List<ModuleRowVO> rowVOList = new ArrayList<>();

        if (total > 0) {
            // 4. 分页取 row_id
            List<Integer> rowIds = moduleDataDao.selectPagedRowIds(
                    columnIds,
                    pageQuery.getOffset(),
                    pageQuery.getRealPageSize()
            );

            if (rowIds != null && !rowIds.isEmpty()) {
                // 5. 查这一页 row_id 下的所有单元格数据
                List<ModuleDataEntity> cellList = moduleDataDao.selectByColumnIdsAndRowIds(columnIds, rowIds);

                // 列ID -> 列定义
                Map<Integer, ModuleColumnEntity> columnMap = columnList.stream()
                        .collect(Collectors.toMap(ModuleColumnEntity::getId, item -> item, (a, b) -> a));

                // row_id -> 当前行数据
                Map<Integer, ModuleRowVO> rowMap = new LinkedHashMap<>();

                // 先按分页顺序建立行，保证返回顺序稳定
                for (Integer rowId : rowIds) {
                    ModuleRowVO rowVO = new ModuleRowVO();
                    rowVO.setRowId(rowId);
                    rowVO.setData(new LinkedHashMap<>());
                    rowMap.put(rowId, rowVO);
                }

                // 把 cell 填充进每一行
                for (ModuleDataEntity cell : cellList) {
                    ModuleRowVO rowVO = rowMap.get(cell.getRowId());
                    ModuleColumnEntity column = columnMap.get(cell.getColumnId());
                    if (rowVO != null && column != null) {
                        rowVO.getData().put(column.getColumnName(), cell.getData());
                    }
                }

                rowVOList = new ArrayList<>(rowMap.values());
            }
        }

        // 6. 组装列定义
        List<ModuleColumnVO> columnVOList = columnList.stream().map(column -> {
            ModuleColumnVO vo = new ModuleColumnVO();
            vo.setId(column.getId());
            vo.setColumnName(column.getColumnName());
            vo.setColumnType(column.getColumnType());
            return vo;
        }).toList();

        // 7. 组装分页结果
        PageResult<ModuleRowVO> pageResult = PageResult.of(
                pageQuery.getRealPageNum(),
                pageQuery.getRealPageSize(),
                total,
                rowVOList
        );

        ModuleDataPageVO resultVO = new ModuleDataPageVO();
        resultVO.setModuleName(moduleName.trim());
        resultVO.setColumns(columnVOList);
        resultVO.setPageData(pageResult);

        return Result.success(resultVO);
    }

    @Override
    public void exportModuleTemplate(String moduleName, HttpServletResponse response) {
        if (!StringUtils.hasText(moduleName)) {
            throw new RuntimeException("模板名称不能为空");
        }

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getModuleName, moduleName.trim())
                .eq(ModuleEntity::getIsMenu, 0)
                .last("limit 1");

        ModuleEntity module = moduleDao.selectOne(moduleWrapper);
        if (module == null) {
            throw new RuntimeException("模板不存在");
        }

        // 2. 查询模板列
        LambdaQueryWrapper<ModuleColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(ModuleColumnEntity::getModuleName, moduleName.trim())
                .orderByAsc(ModuleColumnEntity::getId);

        List<ModuleColumnEntity> columnList = moduleColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            throw new RuntimeException("当前模板未定义列，无法导出模板结构");
        }

        // 3. 创建 Excel，只写表头，不写数据
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("模板结构");

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < columnList.size(); i++) {
                ModuleColumnEntity column = columnList.get(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(column.getColumnName());
                sheet.autoSizeColumn(i);
            }

            // 4. 设置响应头
            String fileName = moduleName.trim() + "_模板结构.xlsx";
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);

            // 5. 输出到前端
            OutputStream outputStream = response.getOutputStream();
            workbook.write(outputStream);
            outputStream.flush();

        } catch (Exception e) {
            throw new RuntimeException("导出模板结构失败: " + e.getMessage(), e);
        }
    }

    @Transactional
    @Override
    public Result<String> addModuleColumn(AddModuleColumnDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getModuleName())) {
            return Result.fail("模板名称不能为空");
        }
        if (!StringUtils.hasText(dto.getColumnName())) {
            return Result.fail("列名称不能为空");
        }
        if (!StringUtils.hasText(dto.getColumnType())) {
            return Result.fail("列类型不能为空");
        }

        String moduleName = dto.getModuleName().trim();
        String columnName = dto.getColumnName().trim();
        String columnType = dto.getColumnType().trim();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getModuleName, moduleName)
                .eq(ModuleEntity::getIsMenu, 0)
                .last("limit 1");

        ModuleEntity module = moduleDao.selectOne(moduleWrapper);
        if (module == null) {
            return Result.fail("模板不存在");
        }

        // 2. 校验当前模板下列名不能重复
        LambdaQueryWrapper<ModuleColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(ModuleColumnEntity::getModuleName, moduleName)
                .eq(ModuleColumnEntity::getColumnName, columnName)
                .last("limit 1");

        ModuleColumnEntity existedColumn = moduleColumnDao.selectOne(columnWrapper);
        if (existedColumn != null) {
            return Result.fail("当前模板下已存在同名列");
        }

        // 3. 插入模板列
        ModuleColumnEntity moduleColumn = new ModuleColumnEntity();
        moduleColumn.setModuleName(moduleName);
        moduleColumn.setColumnName(columnName);
        moduleColumn.setColumnType(columnType);
        moduleColumn.setDeleted(0);

        int rows = moduleColumnDao.insert(moduleColumn);
        if (rows <= 0) {
            return Result.fail("新增模板列失败");
        }

        return Result.success("新增模板列成功");
    }

    @Transactional
    @Override
    public Result<String> deleteModuleColumn(DeleteModuleColumnDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (dto.getColumnId() == null) {
            return Result.fail("模板列ID不能为空");
        }

        // 1. 校验列存在
        ModuleColumnEntity moduleColumn = moduleColumnDao.selectById(dto.getColumnId());
        if (moduleColumn == null) {
            return Result.fail("模板列不存在");
        }

        // 2. 逻辑删除模板列
        int columnRows = moduleColumnDao.deleteById(dto.getColumnId());
        if (columnRows <= 0) {
            return Result.fail("删除模板列失败");
        }

        // 3. 逻辑删除该列对应的数据
        LambdaUpdateWrapper<ModuleDataEntity> dataUpdateWrapper = new LambdaUpdateWrapper<>();
        dataUpdateWrapper.eq(ModuleDataEntity::getColumnId, dto.getColumnId())
                .set(ModuleDataEntity::getDeleted, 1);

        moduleDataDao.update(null, dataUpdateWrapper);

        return Result.success("删除模板列成功");
    }

    @Transactional
    @Override
    public Result<String> deleteModuleRow(DeleteModuleRowDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getModuleName())) {
            return Result.fail("模板名称不能为空");
        }
        if (dto.getRowId() == null) {
            return Result.fail("行ID不能为空");
        }

        String moduleName = dto.getModuleName().trim();
        Integer rowId = dto.getRowId();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getModuleName, moduleName)
                .eq(ModuleEntity::getIsMenu, 0)
                .last("limit 1");

        ModuleEntity module = moduleDao.selectOne(moduleWrapper);
        if (module == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板下所有列
        LambdaQueryWrapper<ModuleColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(ModuleColumnEntity::getModuleName, moduleName);

        List<ModuleColumnEntity> columnList = moduleColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        List<Integer> columnIds = columnList.stream()
                .map(ModuleColumnEntity::getId)
                .toList();

        // 3. 先检查这一行数据是否存在
        LambdaQueryWrapper<ModuleDataEntity> dataCheckWrapper = new LambdaQueryWrapper<>();
        dataCheckWrapper.eq(ModuleDataEntity::getRowId, rowId)
                .in(ModuleDataEntity::getColumnId, columnIds);

        Long count = moduleDataDao.selectCount(dataCheckWrapper);
        if (count == null || count == 0) {
            return Result.fail("当前行数据不存在");
        }

        // 4. 逻辑删除这一整行
        LambdaUpdateWrapper<ModuleDataEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ModuleDataEntity::getRowId, rowId)
                .in(ModuleDataEntity::getColumnId, columnIds)
                .set(ModuleDataEntity::getDeleted, 1);

        int rows = moduleDataDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("删除行数据失败");
        }

        return Result.success("删除行数据成功");
    }


    @Transactional
    @Override
    public Result<AuditModuleResultVO> auditModule(AuditModuleDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getModuleName())) {
            return Result.fail("模板名称不能为空");
        }
        if (dto.getStatus() == null) {
            return Result.fail("审核状态不能为空");
        }
        if (!StringUtils.hasText(dto.getAuditor())) {
            return Result.fail("审核人不能为空");
        }

        String moduleName = dto.getModuleName().trim();
        Integer status = dto.getStatus();
        String remark = dto.getRemark() != null ? dto.getRemark().trim() : null;
        String auditor = dto.getAuditor().trim();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getModuleName, moduleName)
                .eq(ModuleEntity::getIsMenu, 0)
                .last("limit 1");

        ModuleEntity module = moduleDao.selectOne(moduleWrapper);
        if (module == null) {
            return Result.fail("模板不存在");
        }

        // 2. 校验审核状态是否合法
        if (status != 0 && status != 1) {
            return Result.fail("审核状态必须为 0（驳回）或 1（通过）");
        }

        // 3. 更新审核信息
        LocalDateTime auditTime = LocalDateTime.now();

        LambdaUpdateWrapper<ModuleEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ModuleEntity::getModuleName, moduleName)
                .set(ModuleEntity::getAuditStatus, status == 0 ? 2 : status); // 0 转为 2（驳回），1 保持（通过）
        int rows = moduleDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("更新审核信息失败");
        }

        // 4. 返回审核结果
        AuditModuleResultVO resultVO = new AuditModuleResultVO();
        resultVO.setId(module.getId());
        resultVO.setModuleName(module.getModuleName());
        resultVO.setStatus(status == 0 ? 2 : status);
        resultVO.setRemark(remark);
        resultVO.setAuditor(auditor);
        resultVO.setAuditTime(auditTime);

        return Result.success(resultVO);
    }

}