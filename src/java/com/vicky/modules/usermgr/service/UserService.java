/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vicky.modules.usermgr.service;

import com.vicky.common.service.MemcachedService;
import com.vicky.common.utils.service.MybatisBaseService;
import com.vicky.modules.usermgr.entity.User;
import com.vicky.modules.usermgr.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.common.Mapper;

/**
 *
 * @author Vicky
 */
@Service
public class UserService extends MybatisBaseService<User, String> {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private MemcachedService memcachedService;

    @Override
    protected Mapper<User> getMapper() {
        return this.userMapper;
    }

    @Override
    public void save(User t) {
        super.save(t); //To change body of generated methods, choose Tools | Templates.
        this.memcachedService.setMemcached(User.MEMCACHED_PREFFIX + t.getUsername(), User.MEMCACHED_TIME_SECOND, t);
    }

    @Override
    public User selectOne(User t) {
        User user = this.memcachedService.getMemcached(User.MEMCACHED_PREFFIX + t.getEmail());
        if (user == null) {
            user = super.selectOne(t);
            if (user != null) {
                this.memcachedService.setMemcached(User.MEMCACHED_PREFFIX + t.getEmail(), User.MEMCACHED_TIME_SECOND, user);
            }
            return user;
        } else {
            return user;
        }
    }

    @Override
    public void updateSelective(User t) {
        super.updateSelective(t);
        this.memcachedService.setMemcached(User.MEMCACHED_PREFFIX + t.getUsername(), User.MEMCACHED_TIME_SECOND, t);
        this.memcachedService.setMemcached(User.MEMCACHED_PREFFIX + t.getEmail(), User.MEMCACHED_TIME_SECOND, t);
    }

    @Override
    public User selectByPrimaryKey(String id) {
        User user = this.memcachedService.getMemcached(User.MEMCACHED_PREFFIX + id);
        if (user == null) {
            user = super.selectByPrimaryKey(id);
            if (user != null) {
                this.memcachedService.setMemcached(User.MEMCACHED_PREFFIX + user.getUsername(), User.MEMCACHED_TIME_SECOND, user);
            }
            return user;
        } else {
            return user;
        }
    }

}
