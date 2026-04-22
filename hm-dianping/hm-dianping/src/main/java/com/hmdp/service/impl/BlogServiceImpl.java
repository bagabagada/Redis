package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBlogById(Long id) {
            //  1 . 查询blog
            Blog blog = getById(id);
            if(blog == null){
                return Result.fail("笔记不存在!") ;
            }
            // 2 . 查询blog有关的用户
            queryBlogUser(blog);
            // 3 . 查询blog是否被点赞
            isBlogLiked(blog);
            return Result.ok(blog);
        }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog -> {
            this.queryBlogUser(blog);
            this.isBlogLiked(blog);
        });
        return Result.ok(records);
    }


    @Override
    public Result likeBlog(Long id) {
        // 1 . 获取登录用户
        Long userId = UserHolder.getUser().getId() ;
        // 2 . 判断当前用户是否点赞
        String key = "blog:liked:" + id ; // 笔记id作为key
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score == null){
            // 3 . 不存在,未点过赞,可以点赞
            // 3 . 1 数据库点赞数 + 1
            boolean isOk = update().setSql("liked = liked + 1").eq("id",id).update() ;
            // 3 . 2 保存数据库到Redis的Set集合
            if(isOk){
                // 数据库更新成功，更新缓存 zadd key value score
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());
            }
        }else{
            // 4 如果已经点赞，取消点赞
            // 4 . 1 数据库点赞数 -1
            boolean isOk = update().setSql("liked = liked - 1").eq("id",id).update() ;
            // 4 . 2 把用户从Redis的set集合移去
            if(isOk){
                // 数据更新成功，更新缓存 zrem key value
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok() ;
    }

    @Override
    public Result queryBlogLikes(Long id) {
        String key = "blog:liked:" + id ;
        // 1 . 查询top5的点赞用户 zrange key 0 4
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key,0,4) ;
        if(top5 == null || top5.isEmpty()){
            return Result.ok(Collections.emptyList()) ;
        }
        // 2 . 解析出其中的用户id
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String idStr = StrUtil.join(",",ids) ;
        // 3 . 根据用户id查询用户
        List<UserDTO> userDTOS = userService.query().in("id",ids)
                .last("ORDER BY FIELD(id," + idStr + ")")
                .list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        // 4 . 返回
        return Result.ok(userDTOS) ;
    }

    private void queryBlogUser(Blog blog) {
            Long userId = blog.getUserId();
            User user = userService.getById(userId) ;
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon()) ;// 用户图标
    }
    private void isBlogLiked(Blog blog) {
        // 1 . 获取登录用户
        UserDTO user = UserHolder.getUser();
        if(user == null)
        {
            // 当前用户未登录，无需查询点赞
            return;
        }
        Long userId = user.getId();
        // 2 . 判断当前用户是否点赞
        String key = "blog:liked:" + blog.getId() ; // 笔记id作为key
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(Objects.nonNull(score)) ;
    }
}
