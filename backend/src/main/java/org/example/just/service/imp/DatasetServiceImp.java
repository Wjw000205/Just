package org.example.just.service.imp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.just.dao.DatasetColumnDao;
import org.example.just.dao.ManuDatasetDao;
import org.example.just.dao.DatasetDataDao;
import org.example.just.dao.ModuleDao;
import org.example.just.dto.datasetDto.*;
import org.example.just.entity.DatasetColumnEntity;
import org.example.just.entity.DatabaseDataEntity;
import org.example.just.entity.ManuDatabaseEntity;
import org.example.just.entity.ModuleEntity;
import org.example.just.service.DatasetService;
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
public class DatasetServiceImp implements DatasetService {

    private final ManuDatasetDao DatasetDao;
    private final DatasetColumnDao DatasetColumnDao;
    private final DatasetDataDao DatasetDataDao;
    private final ModuleDao moduleDao;

    public DatasetServiceImp(ManuDatasetDao DatasetDao,
                            DatasetColumnDao DatasetColumnDao,
                            DatasetDataDao DatasetDataDao,
                             ModuleDao moduleDao) {
        this.DatasetDao = DatasetDao;
        this.DatasetColumnDao = DatasetColumnDao;
        this.DatasetDataDao = DatasetDataDao;
        this.moduleDao = moduleDao;
    }

    @Transactional
    @Override
    public Result<String> createMenu(CreateMenuDatasetDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            return Result.fail("目录名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCreator())) {
            return Result.fail("创建人不能为空");
        }

        String name = dto.getName().trim();
        String creator = dto.getCreator().trim();
        String parent = StringUtils.hasText(dto.getParent()) ? dto.getParent().trim() : null;

        // 1. 如果是创建次级目录，先校验父目录是否存在
        if (StringUtils.hasText(parent)) {
            LambdaQueryWrapper<ManuDatabaseEntity> parentWrapper = new LambdaQueryWrapper<>();
            parentWrapper.eq(ManuDatabaseEntity::getName, parent)
                    .eq(ManuDatabaseEntity::getIsMenu, 1)
                    .last("limit 1");

            ManuDatabaseEntity parentDataset = DatasetDao.selectOne(parentWrapper);
            if (parentDataset == null) {
                return Result.fail("父级目录不存在");
            }
        }

        // 2. 校验同级目录名称不能重复
        LambdaQueryWrapper<ManuDatabaseEntity> sameLevelWrapper = new LambdaQueryWrapper<>();
        sameLevelWrapper.eq(ManuDatabaseEntity::getName, name)
                .eq(ManuDatabaseEntity::getIsMenu, 1);

        if (StringUtils.hasText(parent)) {
            sameLevelWrapper.eq(ManuDatabaseEntity::getParent, parent);
        } else {
            sameLevelWrapper.isNull(ManuDatabaseEntity::getParent);
        }

        if (DatasetDao.selectCount(sameLevelWrapper) > 0) {
            return Result.fail("同级目录下已存在同名目录");
        }

        // 3. 插入目录
        ManuDatabaseEntity Dataset = new ManuDatabaseEntity();
        Dataset.setName(name);
        Dataset.setCreator(creator);
        Dataset.setCreateTime(LocalDateTime.now());
        Dataset.setParent(parent);
        Dataset.setIsMenu(1);
        Dataset.setDeleted(0);

        int rows = DatasetDao.insert(Dataset);
        if (rows <= 0) {
            return Result.fail("创建目录失败");
        }

        return Result.success("创建目录成功");
    }

    @Override
    public Result<List<ManuDatasetTreeVO>> getDatasetTree() {
        LambdaQueryWrapper<ManuDatabaseEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper
                .orderByAsc(ManuDatabaseEntity::getId);

        List<ManuDatabaseEntity> DatasetList = DatasetDao.selectList(wrapper);

        if (DatasetList == null || DatasetList.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        // 先转成 VO
        List<ManuDatasetTreeVO> voList = DatasetList.stream().map(Dataset -> {
            ManuDatasetTreeVO vo = new ManuDatasetTreeVO();
            vo.setId(Dataset.getId());
            vo.setName(Dataset.getName());
            vo.setCreator(Dataset.getCreator());
            vo.setCreateTime(Dataset.getCreateTime());
            vo.setParent(Dataset.getParent());
            vo.setIsMenu(Dataset.getIsMenu());
            return vo;
        }).collect(Collectors.toList());

        // 用 DatasetName 建索引，方便挂子节点
        Map<String, ManuDatasetTreeVO> DatasetMap = voList.stream()
                .collect(Collectors.toMap(ManuDatasetTreeVO::getName, item -> item, (a, b) -> a));

        List<ManuDatasetTreeVO> rootList = new ArrayList<>();

        for (ManuDatasetTreeVO node : voList) {
            if (!StringUtils.hasText(node.getParent())) {
                // 一级目录
                rootList.add(node);
            } else {
                // 子目录挂到父目录下面
                ManuDatasetTreeVO parentNode = DatasetMap.get(node.getParent());
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
    public Result<String> createDataset(CreateDatasetDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getName())) {
            return Result.fail("数据集名称不能为空");
        }
        if (!StringUtils.hasText(dto.getCreator())) {
            return Result.fail("创建人不能为空");
        }
        if (!StringUtils.hasText(dto.getParent())) {
            return Result.fail("父目录不能为空");
        }
        if (dto.getModule() == null) {
            return Result.fail("所属module不能为空");
        }
        if (dto.getColumns() == null || dto.getColumns().isEmpty()) {
            return Result.fail("数据集列不能为空");
        }

        String datasetName = dto.getName().trim();
        String creator = dto.getCreator().trim();
        String parent = dto.getParent().trim();
        Integer moduleId = dto.getModule();
        List<DatasetColumnDTO> columns = dto.getColumns();

        // 1. 校验父目录存在，并且必须是目录
        LambdaQueryWrapper<ManuDatabaseEntity> parentWrapper = new LambdaQueryWrapper<>();
        parentWrapper.eq(ManuDatabaseEntity::getName, parent)
                .eq(ManuDatabaseEntity::getIsMenu, 1)
                .eq(ManuDatabaseEntity::getDeleted, 0)
                .last("limit 1");

        ManuDatabaseEntity parentDataset = DatasetDao.selectOne(parentWrapper);
        if (parentDataset == null) {
            return Result.fail("父目录不存在");
        }

        // 2. 校验所属module存在
        LambdaQueryWrapper<ModuleEntity> moduleWrapper = new LambdaQueryWrapper<>();
        moduleWrapper.eq(ModuleEntity::getId, moduleId)
                .eq(ModuleEntity::getDeleted, 0)
                .last("limit 1");

        ModuleEntity moduleEntity = moduleDao.selectOne(moduleWrapper);
        if (moduleEntity == null) {
            return Result.fail("所属module不存在");
        }

        // 3. 校验同一父目录下、同一module下模板名称不能重复
        LambdaQueryWrapper<ManuDatabaseEntity> datasetWrapper = new LambdaQueryWrapper<>();
        datasetWrapper.eq(ManuDatabaseEntity::getName, datasetName)
                .eq(ManuDatabaseEntity::getParent, parent)
                .eq(ManuDatabaseEntity::getModule, moduleId)
                .eq(ManuDatabaseEntity::getIsMenu, 0)
                .eq(ManuDatabaseEntity::getDeleted, 0);

        if (DatasetDao.selectCount(datasetWrapper) > 0) {
            return Result.fail("同一目录下该module已存在同名模板");
        }

        // 4. 校验列定义
        Set<String> columnNameSet = new HashSet<>();
        for (DatasetColumnDTO column : columns) {
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

        // 5. 插入 manu_dataset 表
        ManuDatabaseEntity dataset = new ManuDatabaseEntity();
        dataset.setName(datasetName);
        dataset.setCreator(creator);
        dataset.setCreateTime(LocalDateTime.now());
        dataset.setParent(parent);
        dataset.setIsMenu(0);
        dataset.setDeleted(0);
        dataset.setModule(moduleId);

        int datasetRows = DatasetDao.insert(dataset);
        if (datasetRows <= 0) {
            return Result.fail("创建模板失败");
        }

        // 6. 插入 dataset_column 表
        for (DatasetColumnDTO column : columns) {
            DatasetColumnEntity datasetColumn = new DatasetColumnEntity();
            datasetColumn.setColumnName(column.getColumnName().trim());
            datasetColumn.setColumnType(column.getColumnType().trim());
            datasetColumn.setDatasetName(datasetName);
            datasetColumn.setDeleted(0);

            int columnRows = DatasetColumnDao.insert(datasetColumn);
            if (columnRows <= 0) {
                throw new RuntimeException("插入模板列失败");
            }
        }

        return Result.success("创建模板成功");
    }

    @Transactional
    @Override
    public Result<String> importDatasetData(String DatasetName, MultipartFile file) {
        if (!StringUtils.hasText(DatasetName)) {
            return Result.fail("模板名称不能为空");
        }
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatabaseEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatabaseEntity::getName, DatasetName.trim())
                .eq(ManuDatabaseEntity::getIsMenu, 0)
                .last("limit 1");
        ManuDatabaseEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板对应列
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName.trim());
        List<DatasetColumnEntity> columnList = DatasetColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        Map<String, DatasetColumnEntity> columnMap = columnList.stream()
                .collect(Collectors.toMap(DatasetColumnEntity::getColumnName, item -> item, (a, b) -> a));

        List<Integer> columnIds = columnList.stream()
                .map(DatasetColumnEntity::getId)
                .collect(Collectors.toList());

        Integer maxRowId = DatasetDataDao.selectMaxRowIdByColumnIds(columnIds);
        int nextRowId = (maxRowId == null ? 0 : maxRowId) + 1;

        List<DatabaseDataEntity> batchList = new ArrayList<>();
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
            Map<Integer, DatasetColumnEntity> matchedColumnIndexMap = new LinkedHashMap<>();
            int lastCellNum = headerRow.getLastCellNum();
            for (int i = 0; i < lastCellNum; i++) {
                Cell cell = headerRow.getCell(i);
                String headerName = dataFormatter.formatCellValue(cell).trim();
                if (!StringUtils.hasText(headerName)) {
                    continue;
                }

                DatasetColumnEntity matchedColumn = columnMap.get(headerName);
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
                List<DatabaseDataEntity> currentRowDataList = new ArrayList<>();

                for (Map.Entry<Integer, DatasetColumnEntity> entry : matchedColumnIndexMap.entrySet()) {
                    Integer cellIndex = entry.getKey();
                    DatasetColumnEntity DatasetColumn = entry.getValue();

                    Cell cell = dataRow.getCell(cellIndex);
                    String cellValue = dataFormatter.formatCellValue(cell);
                    if (cellValue != null) {
                        cellValue = cellValue.trim();
                    }

                    if (StringUtils.hasText(cellValue)) {
                        rowHasValue = true;
                    }

                    DatabaseDataEntity DatasetData = new DatabaseDataEntity();
                    DatasetData.setColumnId(DatasetColumn.getId());
                    DatasetData.setRowId(nextRowId);
                    DatasetData.setDataType(DatasetColumn.getColumnType());
                    DatasetData.setData(cellValue);
                    DatasetData.setDeleted(0);

                    currentRowDataList.add(DatasetData);
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

            DatasetDataDao.insertBatch(batchList);
            return Result.success("导入成功，共导入 " + importedRowCount + " 行数据");

        } catch (Exception e) {
            throw new RuntimeException("导入模板数据失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Result<DatasetDataPageVO> getDatasetDataPage(String DatasetName, PageQuery pageQuery) {
        if (!StringUtils.hasText(DatasetName)) {
            return Result.fail("模板名称不能为空");
        }
        if (pageQuery == null) {
            return Result.fail("分页参数不能为空");
        }

        // 1. 校验模板存在，并且必须是模板而不是目录
        LambdaQueryWrapper<ManuDatabaseEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatabaseEntity::getName, DatasetName.trim())
                .eq(ManuDatabaseEntity::getIsMenu, 0)
                .last("limit 1");
        ManuDatabaseEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板列
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName.trim())
                .orderByAsc(DatasetColumnEntity::getId);

        List<DatasetColumnEntity> columnList = DatasetColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        List<Integer> columnIds = columnList.stream()
                .map(DatasetColumnEntity::getId)
                .toList();

        // 3. 先统计总行数
        Long total = DatasetDataDao.countDistinctRowIds(columnIds);
        if (total == null) {
            total = 0L;
        }

        List<DatasetRowVO> rowVOList = new ArrayList<>();

        if (total > 0) {
            // 4. 分页取 row_id
            List<Integer> rowIds = DatasetDataDao.selectPagedRowIds(
                    columnIds,
                    pageQuery.getOffset(),
                    pageQuery.getRealPageSize()
            );

            if (rowIds != null && !rowIds.isEmpty()) {
                // 5. 查这一页 row_id 下的所有单元格数据
                List<DatabaseDataEntity> cellList = DatasetDataDao.selectByColumnIdsAndRowIds(columnIds, rowIds);

                // 列ID -> 列定义
                Map<Integer, DatasetColumnEntity> columnMap = columnList.stream()
                        .collect(Collectors.toMap(DatasetColumnEntity::getId, item -> item, (a, b) -> a));

                // row_id -> 当前行数据
                Map<Integer, DatasetRowVO> rowMap = new LinkedHashMap<>();

                // 先按分页顺序建立行，保证返回顺序稳定
                for (Integer rowId : rowIds) {
                    DatasetRowVO rowVO = new DatasetRowVO();
                    rowVO.setRowId(rowId);
                    rowVO.setData(new LinkedHashMap<>());
                    rowMap.put(rowId, rowVO);
                }

                // 把 cell 填充进每一行
                for (DatabaseDataEntity cell : cellList) {
                    DatasetRowVO rowVO = rowMap.get(cell.getRowId());
                    DatasetColumnEntity column = columnMap.get(cell.getColumnId());
                    if (rowVO != null && column != null) {
                        rowVO.getData().put(column.getColumnName(), cell.getData());
                    }
                }

                rowVOList = new ArrayList<>(rowMap.values());
            }
        }

        // 6. 组装列定义
        List<DatasetColumnVO> columnVOList = columnList.stream().map(column -> {
            DatasetColumnVO vo = new DatasetColumnVO();
            vo.setId(column.getId());
            vo.setColumnName(column.getColumnName());
            vo.setColumnType(column.getColumnType());
            return vo;
        }).toList();

        // 7. 组装分页结果
        PageResult<DatasetRowVO> pageResult = PageResult.of(
                pageQuery.getRealPageNum(),
                pageQuery.getRealPageSize(),
                total,
                rowVOList
        );

        DatasetDataPageVO resultVO = new DatasetDataPageVO();
        resultVO.setDatasetName(DatasetName.trim());
        resultVO.setColumns(columnVOList);
        resultVO.setPageData(pageResult);

        return Result.success(resultVO);
    }

    @Override
    public void exportDatasetTemplate(String DatasetName, HttpServletResponse response) {
        if (!StringUtils.hasText(DatasetName)) {
            throw new RuntimeException("模板名称不能为空");
        }

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatabaseEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatabaseEntity::getName, DatasetName.trim())
                .eq(ManuDatabaseEntity::getIsMenu, 0)
                .last("limit 1");

        ManuDatabaseEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            throw new RuntimeException("模板不存在");
        }

        // 2. 查询模板列
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName.trim())
                .orderByAsc(DatasetColumnEntity::getId);

        List<DatasetColumnEntity> columnList = DatasetColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            throw new RuntimeException("当前模板未定义列，无法导出模板结构");
        }

        // 3. 创建 Excel，只写表头，不写数据
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("模板结构");

            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < columnList.size(); i++) {
                DatasetColumnEntity column = columnList.get(i);
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(column.getColumnName());
                sheet.autoSizeColumn(i);
            }

            // 4. 设置响应头
            String fileName = DatasetName.trim() + "_模板结构.xlsx";
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
    public Result<String> addDatasetColumn(AddDatasetColumnDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDatasetName())) {
            return Result.fail("模板名称不能为空");
        }
        if (!StringUtils.hasText(dto.getColumnName())) {
            return Result.fail("列名称不能为空");
        }
        if (!StringUtils.hasText(dto.getColumnType())) {
            return Result.fail("列类型不能为空");
        }

        String DatasetName = dto.getDatasetName().trim();
        String columnName = dto.getColumnName().trim();
        String columnType = dto.getColumnType().trim();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatabaseEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatabaseEntity::getName, DatasetName)
                .eq(ManuDatabaseEntity::getIsMenu, 0)
                .last("limit 1");

        ManuDatabaseEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 校验当前模板下列名不能重复
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName)
                .eq(DatasetColumnEntity::getColumnName, columnName)
                .last("limit 1");

        DatasetColumnEntity existedColumn = DatasetColumnDao.selectOne(columnWrapper);
        if (existedColumn != null) {
            return Result.fail("当前模板下已存在同名列");
        }

        // 3. 插入模板列
        DatasetColumnEntity DatasetColumn = new DatasetColumnEntity();
        DatasetColumn.setDatasetName(DatasetName);
        DatasetColumn.setColumnName(columnName);
        DatasetColumn.setColumnType(columnType);
        DatasetColumn.setDeleted(0);

        int rows = DatasetColumnDao.insert(DatasetColumn);
        if (rows <= 0) {
            return Result.fail("新增模板列失败");
        }

        return Result.success("新增模板列成功");
    }

    @Transactional
    @Override
    public Result<String> deleteDatasetColumn(DeleteDatabaseColumnDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (dto.getColumnId() == null) {
            return Result.fail("模板列ID不能为空");
        }

        // 1. 校验列存在
        DatasetColumnEntity DatasetColumn = DatasetColumnDao.selectById(dto.getColumnId());
        if (DatasetColumn == null) {
            return Result.fail("模板列不存在");
        }

        // 2. 逻辑删除模板列
        int columnRows = DatasetColumnDao.deleteById(dto.getColumnId());
        if (columnRows <= 0) {
            return Result.fail("删除模板列失败");
        }

        // 3. 逻辑删除该列对应的数据
        LambdaUpdateWrapper<DatabaseDataEntity> dataUpdateWrapper = new LambdaUpdateWrapper<>();
        dataUpdateWrapper.eq(DatabaseDataEntity::getColumnId, dto.getColumnId())
                .set(DatabaseDataEntity::getDeleted, 1);

        DatasetDataDao.update(null, dataUpdateWrapper);

        return Result.success("删除模板列成功");
    }

    @Transactional
    @Override
    public Result<String> deleteDatasetRow(DeleteDatasetRowDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDatasetName())) {
            return Result.fail("模板名称不能为空");
        }
        if (dto.getRowId() == null) {
            return Result.fail("行ID不能为空");
        }

        String DatasetName = dto.getDatasetName().trim();
        Integer rowId = dto.getRowId();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatabaseEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatabaseEntity::getName, DatasetName)
                .eq(ManuDatabaseEntity::getIsMenu, 0)
                .last("limit 1");

        ManuDatabaseEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 查模板下所有列
        LambdaQueryWrapper<DatasetColumnEntity> columnWrapper = new LambdaQueryWrapper<>();
        columnWrapper.eq(DatasetColumnEntity::getDatasetName, DatasetName);

        List<DatasetColumnEntity> columnList = DatasetColumnDao.selectList(columnWrapper);
        if (columnList == null || columnList.isEmpty()) {
            return Result.fail("当前模板未定义列");
        }

        List<Integer> columnIds = columnList.stream()
                .map(DatasetColumnEntity::getId)
                .toList();

        // 3. 先检查这一行数据是否存在
        LambdaQueryWrapper<DatabaseDataEntity> dataCheckWrapper = new LambdaQueryWrapper<>();
        dataCheckWrapper.eq(DatabaseDataEntity::getRowId, rowId)
                .in(DatabaseDataEntity::getColumnId, columnIds);

        Long count = DatasetDataDao.selectCount(dataCheckWrapper);
        if (count == null || count == 0) {
            return Result.fail("当前行数据不存在");
        }

        // 4. 逻辑删除这一整行
        LambdaUpdateWrapper<DatabaseDataEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(DatabaseDataEntity::getRowId, rowId)
                .in(DatabaseDataEntity::getColumnId, columnIds)
                .set(DatabaseDataEntity::getDeleted, 1);

        int rows = DatasetDataDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("删除行数据失败");
        }

        return Result.success("删除行数据成功");
    }


    @Transactional
    @Override
    public Result<AuditDatasetResultVO> auditDataset(AuditDatasetDTO dto) {
        if (dto == null) {
            return Result.fail("请求参数不能为空");
        }
        if (!StringUtils.hasText(dto.getDatasetName())) {
            return Result.fail("模板名称不能为空");
        }
        if (dto.getStatus() == null) {
            return Result.fail("审核状态不能为空");
        }
        if (!StringUtils.hasText(dto.getAuditor())) {
            return Result.fail("审核人不能为空");
        }

        String DatasetName = dto.getDatasetName().trim();
        Integer status = dto.getStatus();
        String remark = dto.getRemark() != null ? dto.getRemark().trim() : null;
        String auditor = dto.getAuditor().trim();

        // 1. 校验模板存在，并且必须是模板，不是目录
        LambdaQueryWrapper<ManuDatabaseEntity> DatasetWrapper = new LambdaQueryWrapper<>();
        DatasetWrapper.eq(ManuDatabaseEntity::getName, DatasetName)
                .eq(ManuDatabaseEntity::getIsMenu, 0)
                .last("limit 1");

        ManuDatabaseEntity Dataset = DatasetDao.selectOne(DatasetWrapper);
        if (Dataset == null) {
            return Result.fail("模板不存在");
        }

        // 2. 校验审核状态是否合法
        if (status != 0 && status != 1) {
            return Result.fail("审核状态必须为 0（驳回）或 1（通过）");
        }

        // 3. 更新审核信息
        LocalDateTime auditTime = LocalDateTime.now();

        LambdaUpdateWrapper<ManuDatabaseEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ManuDatabaseEntity::getName, DatasetName)
                .set(ManuDatabaseEntity::getAuditStatus, status == 0 ? 2 : status); // 0 转为 2（驳回），1 保持（通过）
        int rows = DatasetDao.update(null, updateWrapper);
        if (rows <= 0) {
            return Result.fail("更新审核信息失败");
        }

        // 4. 返回审核结果
        AuditDatasetResultVO resultVO = new AuditDatasetResultVO();
        resultVO.setId(Dataset.getId());
        resultVO.setDatasetName(Dataset.getName());
        resultVO.setStatus(status == 0 ? 2 : status);
        resultVO.setRemark(remark);
        resultVO.setAuditor(auditor);
        resultVO.setAuditTime(auditTime);

        return Result.success(resultVO);
    }

}