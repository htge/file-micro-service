package com.htge.login.controller;

import com.htge.login.model.UserData;
import com.htge.login.model.Userinfo;
import com.htge.login.model.UserinfoDao;
import com.htge.login.util.Crypto;
import com.htge.login.util.LoginManager;
import com.htge.login.util.ResponseGeneration;
import net.sf.json.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.security.KeyPair;
import java.security.PrivateKey;

@RequestMapping("/auth")
public class RegisterController {
    private UserinfoDao userinfoDao;
    private LoginManager loginManager;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }
    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }

    @RequestMapping(value="/register", method= RequestMethod.GET)
    public Object registerPage() {
        KeyPair keyPair = loginManager.generateKeyPair();
        if (keyPair == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "keyPair无法生成");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String rsaPublicKey = Crypto.getPublicKey(keyPair);
        String uuid = loginManager.generateUUID();

        ModelAndView view = new ModelAndView("register");
        view.addObject("rsa", rsaPublicKey);
        view.addObject("uuid", uuid);
        return view;
    }

    @RequestMapping(value="/register", method=RequestMethod.POST)
    public ResponseEntity register(@ModelAttribute UserData userData) {
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        KeyPair keyPair = (KeyPair) session.getAttribute(LoginManager.SESSION_KEYPAIR_KEY);
        String uuid = (String) session.getAttribute(LoginManager.UUID_KEY);
        if (keyPair == null || uuid == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "不允许执行当前操作");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PrivateKey privateKey = keyPair.getPrivate();
        userData.decryptDatas(privateKey);

        String clientUuid = userData.getUuid();
        if (!loginManager.checkTimestamp(userData.getTimestamp()) || clientUuid == null || !clientUuid.equals(uuid)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "数据格式不正确");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }

        String newUsername = userData.getUsername();
        String newPassword = userData.getNewPassword();
        String validation = userData.getValidation();
        if (!newUsername.matches("^[a-zA-Z0-9]{4,20}$")) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "用户名只能允许字母和数字，长度在4~20之间");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 32) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "密码长度必须在8～32之间");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }
        if (!newPassword.equals(validation)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "两次输入的密码不匹配");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }

        //登录校验
        String username = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
        String password = userData.getPassword();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        try {
            subject.login(token);

            //校验成功
            session.removeAttribute(LoginManager.SESSION_KEYPAIR_KEY);
            session.removeAttribute(LoginManager.UUID_KEY);

            //注册新用户
            String userDataStr = Crypto.generateUserData(newUsername, newPassword);
            userData.setUserData(userDataStr);
            Userinfo userinfo = userinfoDao.findUser(newUsername);
            if (userinfo != null) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("message", "用户'"+newUsername+"'已存在");
                return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
            }
            userinfo = new Userinfo();
            userinfo.setUsername(newUsername);
            userinfo.setUserdata(userDataStr);
            String role = userData.getRole();
            if (role != null && role.equals("admin")) {
                userinfo.setRole(1);
            } else {
                userinfo.setRole(0);
            }
            if (!userinfoDao.create(userinfo)) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("message", "注册新用户'"+newUsername+"'失败");
                return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
            }
            session.removeAttribute("keypair");
            return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch (UnknownAccountException e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "注册新用户'"+ userData.getUsername()+"'失败");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        } catch (IncorrectCredentialsException e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "密码不正确，请重新输入");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        } catch (ExcessiveAttemptsException e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "输入密码错误次数过多，请过一会再试");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }
    }
}
