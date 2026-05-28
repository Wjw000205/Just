package org.example.just;

import org.example.just.dto.databaseDto.DataBasePageInitInfoVO;
import org.example.just.dto.moduleDto.ModuleBaseInfoVO;
import org.example.just.service.DataBaseService;
import org.example.just.service.DatasetService;
import org.example.just.service.ModuleService;
import org.example.just.service.imp.DataBaseServiceImpl;
import org.example.just.utils.JwtUtil;
import org.example.just.utils.Result;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.List;

@SpringBootTest
class JustApplicationTests {

    @Test
    void contextLoads() {
    }


    @Autowired
    private JwtUtil jwtUtil;
    @Test
    void testJwt(){
        String s = jwtUtil.generateToken(1, "admin", 1);
        System.out.println(s);
        System.out.println(jwtUtil.getUserIdFromToken(s));
        System.out.println(jwtUtil.getUserNameFromToken(s));
        System.out.println(jwtUtil.getRoleFromToken(s));
        System.out.println(" -----");
        System.out.println(jwtUtil.validateToken(s));
        System.out.println(jwtUtil.getExpirationDateFromToken(s));

        System.out.println(jwtUtil.isTokenBlacklisted(s));
        jwtUtil.blacklistToken(s);
        System.out.println(" -----");
        System.out.println(jwtUtil.refreshToken(s));
        System.out.println(" -----");


    }

    @Autowired
    private DataBaseService dataBaseService;

    @Autowired
//    private DataBaseServiceImpl dataBaseServiceImpl;
    private ModuleService moduleService;

    @Test
    void test2(){
        Result<ModuleBaseInfoVO> moduleBaseInfo = moduleService.getModuleBaseInfo(1);
        System.out.println(moduleBaseInfo.getData().getModuleName());
    }

}
