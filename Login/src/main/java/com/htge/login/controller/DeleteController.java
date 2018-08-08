package com.htge.login.controller;

import com.htge.login.model.UserData;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import java.security.KeyPair;
import java.security.PrivateKey;

@RequestMapping("/auth")
public class DeleteController {
    private UserinfoDao userinfoDao;
    private LoginManager loginManager;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }
    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }

    @RequestMapping(value="/delete/{username}", method= RequestMethod.GET)
    public Object deletePage(@PathVariable String username) {
        KeyPair keyPair = loginManager.generateKeyPair();
        if (keyPair == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "keyPair无法生成");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        String rsaPublicKey = Crypto.getPublicKey(keyPair);
        String uuid = loginManager.generateUUID();

        ModelAndView view = new ModelAndView("delete");
        view.addObject("username", username);
        view.addObject("rsa", rsaPublicKey);
        view.addObject("uuid", uuid);
        return view;
    }

    @RequestMapping(value="/delete", method=RequestMethod.POST)
    public ResponseEntity delete(@ModelAttribute UserData userData) {
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession(); //经过条件判断，session一定不为空
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

        //登录校验
        String username = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
        String password = userData.getPassword();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        try {
            subject.login(token);

            //校验成功
            session.removeAttribute(LoginManager.SESSION_KEYPAIR_KEY);
            session.removeAttribute(LoginManager.UUID_KEY);

            //删除用户
            boolean isDelete = userinfoDao.delete(userData.getUsername());
            if (!isDelete) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("message", "删除用户'"+ userData.getUsername()+"'失败");
                return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>("{}", HttpStatus.OK);
        } catch (UnknownAccountException e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "删除用户'"+ userData.getUsername()+"'失败");
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
