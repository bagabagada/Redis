package com.hmdp.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.List;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryType() {
        // 1 . 先查询redis缓存
        String typeList = stringRedisTemplate.opsForValue().get(CACHE_SHOP_TYPE_KEY) ;
        // 2 . 判断是否缓存命中
        if(StrUtil.isNotBlank(typeList)){
            // 2 . 1 存在，直接返回
            List<ShopType> list = JSONUtil.toList(typeList,ShopType.class) ;
            return Result.ok(list) ;
        }
        // 2 . 2 缓存未命中，查数据库
        List<ShopType> list = query().orderByAsc("sort").list() ;

        // 3 . 判断数据库中是否存在
        if(list == null){
            // 3 . 1 数据库也为空 ， 直接返回false ;
            return Result.fail("分类不存在") ;
        }
        // 3 . 2 数据库中存在 , 则将查询到的数据存入redis中
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_TYPE_KEY , JSONUtil.toJsonStr(list)) ;
        // 3 . 3 返回
        return Result.ok(list) ;
    }
}
