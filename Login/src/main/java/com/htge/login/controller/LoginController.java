package com.htge.login.controller;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.UserData;
import com.htge.login.util.Crypto;
import com.htge.login.util.LoginManager;
import com.htge.login.util.ResponseGeneration;
import com.htge.login.util.StringHelper;
import net.sf.json.JSONObject;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.IncorrectCredentialsException;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.PrivateKey;

@RequestMapping("/auth")
public class LoginController {
    private LoginProperties properties;
    private LoginManager loginManager;

    public void setProperties(LoginProperties properties) {
        this.properties = properties;
    }

    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }

    @GetMapping("/")
    public Object loginPage() throws Exception {
        KeyPair keyPair = loginManager.generateKeyPair();
        String rsaPublicKey = Crypto.getPublicKey(keyPair);
        String uuid = loginManager.generateUUID();

        ModelAndView view = new ModelAndView("login");
        view.addObject("rsa", rsaPublicKey);
        view.addObject("uuid", uuid);
        return view;
    }

    @ResponseBody
    @PostMapping("/login")
    public Object login(HttpServletRequest request, @ModelAttribute UserData userData) throws IOException {
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        KeyPair keyPair = (KeyPair) session.getAttribute(LoginManager.SESSION.KEYPAIR_KEY);
        String uuid = (String) session.getAttribute(LoginManager.SESSION.UUID_KEY);
        if (keyPair == null || uuid == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "不允许执行当前操作");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        PrivateKey privateKey = keyPair.getPrivate();
        userData.decryptDatas(privateKey);

        String clientUuid = userData.getUuid();
        if (loginManager.isInvalidTimestamp(userData.getTimestamp()) || clientUuid == null || !clientUuid.equals(uuid)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "数据格式不正确");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }

        String username = userData.getUsername();

        //加密用户信息
        UsernamePasswordToken token = new UsernamePasswordToken(userData.getUsername(), userData.getPassword(),
                LoginManager.getIpAddress(request));
        try {
            subject.login(token);

            //登录成功后，执行以下操作
            loginManager.updateSessionInfo(username, properties.getSessionTimeout());

            //清理之前的旧信息
            session.removeAttribute(LoginManager.SESSION.KEYPAIR_KEY);
            session.removeAttribute(LoginManager.SESSION.UUID_KEY);

            String lastUri = (String) session.getAttribute(LoginManager.SESSION.URL_KEY);
            if (lastUri == null) {
                return new JSONObject();
            }
            JSONObject jsonObject = new JSONObject();
            //url地址编码的大致格式：//xxx/...或者/xxx/...，遇到中文或者特殊字符不乱码
            StringBuilder stringBuilder = new StringBuilder(URLEncoder.encode(lastUri, "UTF-8"));
            StringHelper.replaceString(stringBuilder, "%3A", ":");
            StringHelper.replaceString(stringBuilder, "%2F", "/");
            jsonObject.put("url", stringBuilder.toString());
            return jsonObject;
        } catch (UnknownAccountException | IncorrectCredentialsException ex) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "用户名或密码不正确，请重新输入");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        } catch (ExcessiveAttemptsException e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "输入密码错误次数过多，请过一会再试");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }
    }
}
