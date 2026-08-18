package com.justeam.rdp.dataset;

import com.fasterxml.jackson.core.type.TypeReference;
import com.justeam.rdp.audit.AuditService;
import com.justeam.rdp.common.BusinessException;
import com.justeam.rdp.common.JsonSupport;
import com.justeam.rdp.security.CurrentUser;
import com.justeam.rdp.security.UserPrincipal;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.Set;

@Service
public class DatasetImportService {
    private static final int MAX_ROWS=5000;
    private final DatasetService datasets;private final JdbcClient jdbc;private final JsonSupport json;private final AuditService audit;
    public DatasetImportService(DatasetService datasets,JdbcClient jdbc,JsonSupport json,AuditService audit){this.datasets=datasets;this.jdbc=jdbc;this.json=json;this.audit=audit;}

    public Map<String,Object> importFile(long datasetId,MultipartFile file){
        Map<String,Object> dataset=datasets.get(datasetId);datasets.requireWriteAccess(dataset);if(file==null||file.isEmpty())throw BusinessException.badRequest("请选择非空导入文件");
        String name=safeName(file.getOriginalFilename()),type=type(name);UserPrincipal user=CurrentUser.require();
        Long jobId=jdbc.sql("INSERT INTO data_import_job(dataset_id,file_name,file_type,status,created_by,created_by_name) VALUES (:dataset,:name,:type,'RUNNING',:user,:userName) RETURNING id")
                .param("dataset",datasetId).param("name",name).param("type",type).param("user",user.id()).param("userName",user.realName()).query(Long.class).single();
        int success=0;List<ImportRow> rows;List<Map<String,Object>> generatedRecords=new ArrayList<>();
        try{rows=parse(file,type,fields(dataset));if(rows.size()>MAX_ROWS)throw BusinessException.badRequest("单次导入最多"+MAX_ROWS+"行，请拆分文件");
            for(ImportRow row:rows){
                if(row.error()!=null){recordError(jobId,row.rowNumber(),row.data(),row.error());continue;}
                try{Map<String,Object> created=datasets.createRecord(datasetId,UUID.randomUUID(),row.data());success++;if(generatedRecords.size()<100){Map<String,Object> generated=new LinkedHashMap<>();generated.put("rowNumber",row.rowNumber());generated.put("recordId",created.get("id"));generated.put("businessCode",created.get("businessCode"));generatedRecords.add(generated);}}
                catch(Exception ex){recordError(jobId,row.rowNumber(),row.data(),message(ex));}
            }
            int failures=rows.size()-success;String status=failures==0?"COMPLETED":success==0?"FAILED":"PARTIAL";
            finish(jobId,status,rows.size(),success,failures);audit.record("IMPORT","DATASET","批量导入数据集",Map.of("datasetId",datasetId,"jobId",jobId,"fileName",name,"total",rows.size(),"success",success,"failure",failures));Map<String,Object> result=job(jobId);result.put("generatedRecords",generatedRecords);result.put("generatedRecordsTruncated",success>generatedRecords.size());return result;
        }catch(Exception ex){finish(jobId,"FAILED",0,0,1);recordError(jobId,0,Map.of("fileName",name),message(ex));if(ex instanceof BusinessException business)throw business;throw BusinessException.badRequest("导入文件解析失败："+message(ex));}
    }

    public void writeTemplate(Map<String,Object> dataset,String format,OutputStream output)throws IOException{
        List<Map<String,Object>> definitions=fields(dataset);String normalized=format.toLowerCase(Locale.ROOT);
        switch(normalized){
            case "csv"->writeCsvTemplate(definitions,output);
            case "json"->writeJsonTemplate(definitions,output);
            case "xlsx"->writeXlsxTemplate(definitions,output);
            default->throw BusinessException.badRequest("导入模板仅支持 csv、json、xlsx");
        }
    }

    private void writeCsvTemplate(List<Map<String,Object>> definitions,OutputStream output)throws IOException{
        String[] headers=definitions.stream().map(field->String.valueOf(field.get("key"))).toArray(String[]::new);
        try(var writer=new OutputStreamWriter(output,StandardCharsets.UTF_8);var printer=new CSVPrinter(writer,CSVFormat.DEFAULT.builder().setHeader(headers).get())){printer.flush();}
    }

    private void writeJsonTemplate(List<Map<String,Object>> definitions,OutputStream output)throws IOException{
        List<Map<String,Object>> descriptions=definitions.stream().map(field->{Map<String,Object> item=new LinkedHashMap<>();item.put("key",field.get("key"));item.put("label",field.get("label"));item.put("type",field.get("type"));item.put("required",Boolean.TRUE.equals(field.get("required")));if(field.get("unit")!=null)item.put("unit",field.get("unit"));if(field.get("options")!=null)item.put("options",field.get("options"));return item;}).toList();
        Map<String,Object> template=new LinkedHashMap<>();template.put("records",List.of());template.put("fieldDefinitions",descriptions);json.mapper().writerWithDefaultPrettyPrinter().writeValue(output,template);
    }

    private void writeXlsxTemplate(List<Map<String,Object>> definitions,OutputStream output)throws IOException{
        try(var workbook=new XSSFWorkbook()){
            var dataSheet=workbook.createSheet("数据");Row header=dataSheet.createRow(0);
            for(int i=0;i<definitions.size();i++){header.createCell(i).setCellValue(String.valueOf(definitions.get(i).get("key")));dataSheet.setColumnWidth(i,Math.min(40,Math.max(14,String.valueOf(definitions.get(i).get("label")).length()*2+6))*256);}
            dataSheet.createFreezePane(0,1);
            var guide=workbook.createSheet("字段说明");String[] guideHeaders={"字段 Key","字段名称","数据类型","是否必填","单位","可选值"};Row guideHeader=guide.createRow(0);for(int i=0;i<guideHeaders.length;i++)guideHeader.createCell(i).setCellValue(guideHeaders[i]);
            for(int rowIndex=0;rowIndex<definitions.size();rowIndex++){Map<String,Object> field=definitions.get(rowIndex);Row row=guide.createRow(rowIndex+1);row.createCell(0).setCellValue(String.valueOf(field.get("key")));row.createCell(1).setCellValue(String.valueOf(field.get("label")));row.createCell(2).setCellValue(String.valueOf(field.get("type")));row.createCell(3).setCellValue(Boolean.TRUE.equals(field.get("required"))?"是":"否");row.createCell(4).setCellValue(field.get("unit")==null?"":String.valueOf(field.get("unit")));row.createCell(5).setCellValue(field.get("options")==null?"":String.valueOf(field.get("options")));}
            for(int i=0;i<guideHeaders.length;i++)guide.setColumnWidth(i,(i==5?36:18)*256);guide.createFreezePane(0,1);workbook.write(output);
        }
    }

    public List<Map<String,Object>> jobs(long datasetId){Map<String,Object> dataset=datasets.get(datasetId);requireImportVisibility(dataset);return jdbc.sql("SELECT * FROM data_import_job WHERE dataset_id=:dataset ORDER BY started_time DESC LIMIT 100").param("dataset",datasetId).query((rs,n)->{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("fileName",rs.getString("file_name"));v.put("fileType",rs.getString("file_type"));v.put("status",rs.getString("status"));v.put("totalCount",rs.getInt("total_count"));v.put("successCount",rs.getInt("success_count"));v.put("failureCount",rs.getInt("failure_count"));v.put("createdByName",rs.getString("created_by_name"));v.put("startedTime",rs.getObject("started_time"));v.put("finishedTime",rs.getObject("finished_time"));return v;}).list();}
    public Map<String,Object> job(long jobId){Map<String,Object> summary=jdbc.sql("SELECT * FROM data_import_job WHERE id=:id").param("id",jobId).query((rs,n)->{Map<String,Object> v=new LinkedHashMap<>();v.put("id",rs.getLong("id"));v.put("datasetId",rs.getLong("dataset_id"));v.put("fileName",rs.getString("file_name"));v.put("fileType",rs.getString("file_type"));v.put("status",rs.getString("status"));v.put("totalCount",rs.getInt("total_count"));v.put("successCount",rs.getInt("success_count"));v.put("failureCount",rs.getInt("failure_count"));v.put("startedTime",rs.getObject("started_time"));v.put("finishedTime",rs.getObject("finished_time"));return v;}).optional().orElseThrow(()->BusinessException.notFound("导入作业不存在"));Map<String,Object> dataset=datasets.get(((Number)summary.get("datasetId")).longValue());requireImportVisibility(dataset);List<Map<String,Object>> errors=jdbc.sql("SELECT row_number,raw_data::text,error_message FROM data_import_error WHERE job_id=:id ORDER BY row_number LIMIT 1000").param("id",jobId).query((rs,n)->Map.<String,Object>of("rowNumber",rs.getInt("row_number"),"rawData",json.map(rs.getString("raw_data")),"errorMessage",rs.getString("error_message"))).list();summary.put("errors",errors);return summary;}

    private void requireImportVisibility(Map<String,Object> dataset){UserPrincipal user=CurrentUser.require();long scope=((Number)dataset.get("dataScopeId")).longValue();if(!user.admin()&&(!user.assignedScopes().contains(scope)||!user.permissions().contains("dataset:import")))throw BusinessException.forbidden("选择性共享不包含导入作业及失败原始数据");}

    @SuppressWarnings("unchecked") private List<Map<String,Object>> fields(Map<String,Object> dataset){return (List<Map<String,Object>>)dataset.get("fieldDefinition");}
    private List<ImportRow> parse(MultipartFile file,String type,List<Map<String,Object>> fields)throws Exception{return switch(type){case "CSV"->csv(file,fields);case "JSON"->jsonFile(file);case "XLSX"->excel(file,fields);default->throw BusinessException.badRequest("仅支持 CSV、JSON、XLSX 文件");};}
    private List<ImportRow> csv(MultipartFile file,List<Map<String,Object>> fields)throws Exception{try(var reader=new InputStreamReader(file.getInputStream(),StandardCharsets.UTF_8);var parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).get().parse(reader)){List<ImportRow> result=new ArrayList<>();for(CSVRecord record:parser){Map<String,Object> rawRow=new LinkedHashMap<>(),typedRow=new LinkedHashMap<>();try{for(Map<String,Object> field:fields){String key=String.valueOf(field.get("key"));String raw=value(record,key,String.valueOf(field.get("label")));if(raw!=null&&!raw.isBlank()){rawRow.put(key,raw);typedRow.put(key,convert(raw,field));}}result.add(new ImportRow(Math.toIntExact(record.getRecordNumber()+1),typedRow,null));}catch(Exception ex){result.add(new ImportRow(Math.toIntExact(record.getRecordNumber()+1),rawRow,message(ex)));}}return result;}}
    private String value(CSVRecord record,String key,String label){if(record.isMapped(key))return record.get(key);if(record.isMapped(label))return record.get(label);return null;}
    private List<ImportRow> jsonFile(MultipartFile file)throws Exception{Object root=json.mapper().readValue(file.getInputStream(),Object.class);Object records=root instanceof Map<?,?> map?map.get("records"):root;if(!(records instanceof List<?> list))throw BusinessException.badRequest("JSON 须为对象数组或包含 records 数组");List<ImportRow> result=new ArrayList<>();int index=2;for(Object value:list){if(value instanceof Map<?,?> map)result.add(new ImportRow(index,(Map<String,Object>)map,null));else result.add(new ImportRow(index,Map.of("value",String.valueOf(value)),"JSON 每条记录须为对象"));index++;}return result;}
    private List<ImportRow> excel(MultipartFile file,List<Map<String,Object>> fields)throws Exception{try(var workbook=WorkbookFactory.create(file.getInputStream())){var sheet=workbook.getSheetAt(0);if(sheet.getPhysicalNumberOfRows()<1)return List.of();Row header=sheet.getRow(sheet.getFirstRowNum());Map<Integer,Map<String,Object>> columns=new LinkedHashMap<>();DataFormatter formatter=new DataFormatter();for(Cell cell:header){String title=formatter.formatCellValue(cell).trim();fields.stream().filter(f->title.equals(String.valueOf(f.get("key")))||title.equals(String.valueOf(f.get("label")))).findFirst().ifPresent(f->columns.put(cell.getColumnIndex(),f));}if(columns.isEmpty())throw BusinessException.badRequest("Excel 首行未找到匹配的字段 key 或标签");List<ImportRow> result=new ArrayList<>();for(int i=header.getRowNum()+1;i<=sheet.getLastRowNum();i++){Row source=sheet.getRow(i);if(source==null)continue;Map<String,Object> rawRow=new LinkedHashMap<>(),typedRow=new LinkedHashMap<>();try{for(var entry:columns.entrySet()){Cell cell=source.getCell(entry.getKey());if(cell==null||cell.getCellType()==CellType.BLANK)continue;Map<String,Object> field=entry.getValue();String key=String.valueOf(field.get("key"));String formatted=formatter.formatCellValue(cell);rawRow.put(key,formatted);Object value;if("number".equals(String.valueOf(field.get("type")))&&cell.getCellType()==CellType.NUMERIC)value=cell.getNumericCellValue();else if("boolean".equals(String.valueOf(field.get("type")))&&cell.getCellType()==CellType.BOOLEAN)value=cell.getBooleanCellValue();else if("date".equals(String.valueOf(field.get("type")))&&cell.getCellType()==CellType.NUMERIC&&org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell))value=cell.getLocalDateTimeCellValue().toLocalDate().toString();else value=convert(formatted,field);typedRow.put(key,value);}if(!typedRow.isEmpty())result.add(new ImportRow(i+1,typedRow,null));}catch(Exception ex){result.add(new ImportRow(i+1,rawRow,message(ex)));}}return result;}}
    private Object convert(String raw,Map<String,Object> field){String type=String.valueOf(field.get("type"));try{return switch(type){case "number"->new java.math.BigDecimal(raw.trim());case "boolean"->parseBoolean(raw);case "date"->LocalDate.parse(raw.trim()).toString();default->raw;};}catch(Exception ex){throw BusinessException.badRequest(String.valueOf(field.get("label"))+"的值格式不正确："+raw);}}
    private boolean parseBoolean(String value){String normalized=value.trim().toLowerCase(Locale.ROOT);if(Set.of("true","1","yes","是").contains(normalized))return true;if(Set.of("false","0","no","否").contains(normalized))return false;throw new IllegalArgumentException();}
    private void finish(long id,String status,int total,int success,int failure){jdbc.sql("UPDATE data_import_job SET status=:status,total_count=:total,success_count=:success,failure_count=:failure,finished_time=now() WHERE id=:id").param("status",status).param("total",total).param("success",success).param("failure",failure).param("id",id).update();}
    private void recordError(long jobId,int row,Map<String,Object> data,String message){jdbc.sql("INSERT INTO data_import_error(job_id,row_number,raw_data,error_message) VALUES (:job,:row,CAST(:data AS jsonb),:message)").param("job",jobId).param("row",row).param("data",json.write(data)).param("message",message.substring(0,Math.min(message.length(),1000))).update();}
    private String message(Exception ex){return ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage();}
    private String type(String name){String lower=name.toLowerCase(Locale.ROOT);if(lower.endsWith(".csv"))return "CSV";if(lower.endsWith(".json"))return "JSON";if(lower.endsWith(".xlsx"))return "XLSX";throw BusinessException.badRequest("仅支持 .csv、.json、.xlsx 文件");}
    private String safeName(String value){String name=value==null?"import":value.replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\r\\n\\u0000-\\u001f]","_");return name.substring(0,Math.min(name.length(),255));}
    private record ImportRow(int rowNumber,Map<String,Object> data,String error){}
}
