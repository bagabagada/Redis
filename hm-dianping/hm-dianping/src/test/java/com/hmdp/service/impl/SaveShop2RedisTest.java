package com.hmdp.service.impl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;


@SpringBootTest
public class SaveShop2RedisTest {
    @Resource
    ShopServiceImpl shopService;


    @Test
    public void saveShop2Redis() {
        shopService.saveShop2Redis(1L,20L);
    }
}
