package org.example.just;

import org.example.just.dto.databaseDto.DataBasePageInitInfoVO;
import org.example.just.service.DataBaseService;
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

    @Test
    void test2(){
        Result<DataBasePageInitInfoVO> re = dataBaseService.getPageInitInfo();
        List<DataBaseServiceImpl.ClassificationTreeNode> tree = re.getData().getTree();
        for (DataBaseServiceImpl.ClassificationTreeNode classificationTreeNode : tree){
            System.out.println(classificationTreeNode.getId());
            System.out.println(classificationTreeNode.getLabel());
            for (DataBaseServiceImpl.ClassificationTreeNode child : classificationTreeNode.getChildren()){
                System.out.println(child.getId());
                System.out.println(child.getLabel());
                for (DataBaseServiceImpl.ClassificationTreeNode child1 : child.getChildren()){
                    System.out.println(child1.getId());
                    System.out.println(child1.getLabel());
                }
            }
        }
    }

}
