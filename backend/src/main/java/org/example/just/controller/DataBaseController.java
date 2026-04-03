package org.example.just.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.just.dto.databaseDto.DataBasePageInitInfoVO;
import org.example.just.service.DataBaseService;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/DataBase")
@Tag(name = "数据库操作接口", description = "数据库相关操作")
public class DataBaseController {

    @Resource
    private DataBaseService dataBaseService;
    
    @GetMapping("/getPageInitInfo")
    @Operation(summary = "获取页面初始化信息", description = "获取数据库页面的初始化信息")
    public Result getPageInitInfo() {
        return dataBaseService.getPageInitInfo();
    }
}
