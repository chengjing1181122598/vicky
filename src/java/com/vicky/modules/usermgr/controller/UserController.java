/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vicky.modules.usermgr.controller;

import com.vicky.common.utils.controller.EntityController;
import com.vicky.common.utils.encodepwd.EncodePassword;
import com.vicky.common.utils.sendemail.Mail;
import com.vicky.common.utils.sendemail.SendEmailImpl;
import com.vicky.common.utils.sendemail.SendEmailable;
import com.vicky.common.utils.service.BaseService;
import com.vicky.common.utils.statusmsg.StatusMsg;
import com.vicky.common.utils.statusmsg.StatusMsgException;
import com.vicky.modules.usermgr.entity.User;
import com.vicky.modules.usermgr.service.UserService;
import com.vicky.modules.usermgr.utils.ActivateMail;
import java.util.Date;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Vicky
 */
@Controller
@RequestMapping("user")
public class UserController extends EntityController<User, String> {

    @Autowired
    private HttpServletRequest request;

    private User getUser() {
        return (User) request.getSession().getAttribute("user");
    }

    @Autowired
    private UserService userService;

    @Override
    protected BaseService<User, String> getBaseService() {
        return this.userService;
    }

    @RequestMapping("update")
    @ResponseBody
    public StatusMsg update(HttpServletRequest request, User updateUser) throws StatusMsgException {
        User user = this.getUser();
        updateUser.setUsername(user.getUsername());
        updateUser.setPassword(null);
        updateUser.setEmail(null);
        updateUser.setCreateTime(null);
        this.userService.update(user);

        StatusMsg statusMsg = new StatusMsg(StatusMsg.SUCCESS);
        statusMsg.getMessage().put(StatusMsg.MESSAGE, "修改信息成功");
        return statusMsg;
    }

    @RequestMapping("updatePWD")
    @ResponseBody
    public StatusMsg updatePWD(HttpServletRequest request, String frontPWD, String afterPWD) throws StatusMsgException {
        User user = this.getUser();
        if (!EncodePassword.checkPassword(frontPWD, user.getPassword())) {
            throw new StatusMsgException("原来密码不正确");
        } else {
            user.setPassword(EncodePassword.encodePassword(afterPWD));
        }
        StatusMsg statusMsg = new StatusMsg(StatusMsg.SUCCESS);
        statusMsg.getMessage().put(StatusMsg.MESSAGE, "修改密码成功");
        return statusMsg;
    }

    @Override
    @RequestMapping("getById")
    @ResponseBody
    public User getById(HttpServletRequest request, HttpServletResponse response, String username) {
        return this.userService.getByIdNoPassword(username);
    }

    @RequestMapping("getUser")
    @ResponseBody
    public User getUser(HttpServletRequest request) throws CloneNotSupportedException {
        User user = (User) this.getUser().clone();
        user.setPassword(null);
        return user;
    }

    @RequestMapping("login")
    @ResponseBody
    public StatusMsg login(HttpServletRequest request, HttpServletResponse response, User paramUser) throws Exception {
        User user;
        if (paramUser.getUsername().contains("@")) {
            user = this.userService.selectOne(new User(paramUser.getUsername()));
        } else {
            user = this.userService.selectByPrimaryKey(paramUser.getUsername());
        }
        if (user == null) {
            throw new StatusMsgException("找不到用户,请检查用户名是否正确");
        }
        if (!EncodePassword.checkPassword(paramUser.getPassword(), user.getPassword())) {
            throw new StatusMsgException("密码错误");
        }
        user = this.userService.selectByPrimaryKey(user.getUsername());
        User returnUser = (User) user.clone();
        returnUser.setPassword(null);
        request.getSession().setAttribute("user", user);
        StatusMsg statusMsg = new StatusMsg(StatusMsg.SUCCESS);
        statusMsg.getMessage().put("user", returnUser);
        return statusMsg;
    }

    @RequestMapping("finishRegister")
    @ResponseBody
    public StatusMsg prepareRegister(HttpServletRequest request, String activateCode) throws Exception {
        System.out.println(request.getSession().getAttribute("avtivateCode"));
        if (activateCode.equals(request.getSession().getAttribute("avtivateCode"))) {
            User user = (User) request.getSession().getAttribute("registerUser");
            this.userService.save(user);
        } else {
            throw new StatusMsgException("激活码不正确!");
        }
        StatusMsg statusMsg = new StatusMsg(StatusMsg.SUCCESS);
        statusMsg.getMessage().put(StatusMsg.MESSAGE, "用户激活成功!");
        return statusMsg;
    }

    @RequestMapping("prepareRegister")
    @ResponseBody
    public StatusMsg prepareRegister(HttpServletRequest request, User u) throws Exception {
        request.getSession(true);
        String username = u.getUsername();
        String email = u.getEmail();
        if (this.userService.selectByPrimaryKey(username) != null) {
            throw new StatusMsgException("该用户已存在");
        }
        User queryUser = new User();
        queryUser.setEmail(email);
        if (this.userService.selectOne(queryUser) != null) {
            throw new StatusMsgException("该邮箱已注册");
        }

        String avtivateCode = (int) (Math.random() * 900000 + 100000) + "";
        Mail activateMail = new ActivateMail(email, avtivateCode);
        SendEmailable sendEmailable = new SendEmailImpl();
        sendEmailable.send(activateMail);

        u.setCreateTime(new Date());
        u.setPassword(EncodePassword.encodePassword(u.getPassword()));
        request.getSession().setAttribute("registerUser", u);
        request.getSession().setAttribute("avtivateCode", avtivateCode);
        System.out.println(request.getSession().getAttribute("avtivateCode"));
        StatusMsg statusMsg = new StatusMsg(StatusMsg.SUCCESS);
        statusMsg.getMessage().put(StatusMsg.MESSAGE, "发送邮件成功,请前往邮箱获取验证码!");
        return statusMsg;
    }

}