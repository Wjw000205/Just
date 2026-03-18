package org.example.just;

import org.example.just.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

}
