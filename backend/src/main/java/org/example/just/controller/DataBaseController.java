package org.example.just.controller;


import jakarta.annotation.Resource;
import org.example.just.dto.databaseDto.DataBasePageInitInfoVO;
import org.example.just.service.DataBaseService;
import org.example.just.utils.Result;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/DataBase")
public class DataBaseController {

    @Resource
    private DataBaseService dataBaseService;
    @RequestMapping("/getPageInitInfo")
    public Result getPageInitInfo() {
        return dataBaseService.getPageInitInfo();
    }
}
