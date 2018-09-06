package com.htge.login.controller;

import com.htge.login.config.LoginProperties;
import com.htge.login.model.UserData;
import com.htge.login.model.Userinfo;
import com.htge.login.model.UserinfoDao;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.KeyPair;
import java.security.PrivateKey;

@RequestMapping("/auth")
public class UserController {
    private UserinfoDao userinfoDao;
    private LoginProperties properties;
    private LoginManager loginManager;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }
    public void setProperties(LoginProperties properties) {
        this.properties = properties;
    }
    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }

    /**
     * 登录
     * @param request 基础数据
     * @param userData 加密后的登录请求 {
     * "encryptedData": xxx, AES加密后的数据
     * "encryptedKey": xxx AES秘钥用RSA加密后的信息
     * }
     * @return {
     * "url", xxx 可选，告诉页面操作成功后跳转的路径
     * "message", xxx 可选，错误信息
     * }
     * 状态码：成功为200，其他情况为400和500
     * @throws IOException encode可能存在异常
     */
    @ResponseBody
    @GetMapping("/user")
    public Object login(HttpServletRequest request, @ModelAttribute UserData userData) throws IOException {
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        KeyPair keyPair = (KeyPair) session.getAttribute(LoginManager.SESSION.KEYPAIR_KEY);
        String uuid = (String) session.getAttribute(LoginManager.SESSION.UUID_KEY);
        if (keyPair == null || uuid == null) {
            return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.NOT_ACCEPTABLE);
        }

        PrivateKey privateKey = keyPair.getPrivate();
        userData.decryptDatas(privateKey);

        String clientUuid = userData.getUuid();
        if (loginManager.isInvalidTimestamp(userData.getTimestamp()) || clientUuid == null || !clientUuid.equals(uuid)) {
            return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.NOT_ACCEPTABLE);
        }

        String username = userData.getUsername();
        String ip = LoginManager.getIpAddress(request);
        if (ip == null) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "登录错误，请换浏览器后再试。");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }

        //加密用户信息
        UsernamePasswordToken token = new UsernamePasswordToken(userData.getUsername(), userData.getPassword(), ip);
        try {
            subject.login(token);

            //登录成功后，执行以下操作
            loginManager.updateSessionInfo(username, ip, properties.getSessionTimeout());

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

    /**
     * 修改密码
     * @param bodyJson 加密后的修改请求 {
     * "encryptedData": xxx, AES加密后的数据
     * "encryptedKey": xxx AES秘钥用RSA加密后的信息
     * }
     * @return {
     * "message", xxx 可选，错误信息
     * }
     * 状态码：成功为200，其他情况为400和500
     */
    @ResponseBody
    @PutMapping("/user")
    public Object change(@RequestBody JSONObject bodyJson) {
        UserData userData = new UserData(bodyJson);

        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        KeyPair keyPair = (KeyPair) session.getAttribute(LoginManager.SESSION.KEYPAIR_KEY);
        String uuid = (String) session.getAttribute(LoginManager.SESSION.UUID_KEY);
        if (keyPair == null || uuid == null) {
            return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.NOT_ACCEPTABLE);
        }

        PrivateKey privateKey = keyPair.getPrivate();
        userData.decryptDatas(privateKey);

        String clientUuid = userData.getUuid();
        if (loginManager.isInvalidTimestamp(userData.getTimestamp()) || clientUuid == null || !clientUuid.equals(uuid)) {
            return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.NOT_ACCEPTABLE);
        }

        String newPassword = userData.getNewPassword();
        String validation = userData.getValidation();
        if (newPassword == null || newPassword.length() < 8 || newPassword.length() > 32) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "密码长度必须在8～32之间");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }
        if (!newPassword.equals(validation)) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "密码输入不匹配");
            return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
        }
        String username = (String)session.getAttribute(LoginManager.SESSION.USER_KEY);
        String password = userData.getPassword();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        try {
            subject.login(token);

            //授权成功
            session.removeAttribute(LoginManager.SESSION.KEYPAIR_KEY);
            session.removeAttribute(LoginManager.SESSION.UUID_KEY);

            //修改用户信息
            Userinfo userinfo = userinfoDao.findUser(username);
            userinfo.setUserdata(Crypto.generateUserData(username, newPassword));
            if (userinfoDao.update(userinfo)) {
                loginManager.Logout();
                return new JSONObject();
            }
        } catch (UnknownAccountException e) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("message", "修改密码失败");
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", "修改密码失败");
        return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
    }

    /**
     * 注册新账户
     * @param bodyJson 加密后的注册请求 {
     * "encryptedData": xxx, AES加密后的数据
     * "encryptedKey": xxx AES秘钥用RSA加密后的信息
     * }
     * @return {
     * "message", xxx 可选，错误信息
     * }
     * 状态码：成功为200，其他情况为400和500
     */
    @ResponseBody
    @PostMapping("/user")
    public Object register(@RequestBody JSONObject bodyJson) {
        UserData userData = new UserData(bodyJson);

        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        KeyPair keyPair = (KeyPair) session.getAttribute(LoginManager.SESSION.KEYPAIR_KEY);
        String uuid = (String) session.getAttribute(LoginManager.SESSION.UUID_KEY);
        if (keyPair == null || uuid == null) {
            return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.NOT_ACCEPTABLE);
        }

        PrivateKey privateKey = keyPair.getPrivate();
        userData.decryptDatas(privateKey);

        String clientUuid = userData.getUuid();
        if (loginManager.isInvalidTimestamp(userData.getTimestamp()) || clientUuid == null || !clientUuid.equals(uuid)) {
            return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.NOT_ACCEPTABLE);
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
        String username = (String)session.getAttribute(LoginManager.SESSION.USER_KEY);
        String password = userData.getPassword();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        try {
            subject.login(token);

            //校验成功
            session.removeAttribute(LoginManager.SESSION.KEYPAIR_KEY);
            session.removeAttribute(LoginManager.SESSION.UUID_KEY);

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
            return new JSONObject();
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

    /**
     * 删除用户
     * @param bodyJson 加密后的删除请求，body: {
     * "encryptedData": xxx, AES加密后的数据
     * "encryptedKey": xxx AES秘钥用RSA加密后的信息
     * }
     * @return {
     * "message", xxx 可选，错误信息
     * }
     * 状态码：成功为200，其他情况为400和500
     */
    @ResponseBody
    @DeleteMapping("/user")
    public Object delete(@RequestBody JSONObject bodyJson) {
        UserData userData = new UserData(bodyJson);

        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession(); //经过条件判断，session一定不为空
        KeyPair keyPair = (KeyPair) session.getAttribute(LoginManager.SESSION.KEYPAIR_KEY);
        String uuid = (String) session.getAttribute(LoginManager.SESSION.UUID_KEY);
        if (keyPair == null || uuid == null) {
            return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.NOT_ACCEPTABLE);
        }

        PrivateKey privateKey = keyPair.getPrivate();
        userData.decryptDatas(privateKey);

        String clientUuid = userData.getUuid();
        if (loginManager.isInvalidTimestamp(userData.getTimestamp()) || clientUuid == null || !clientUuid.equals(uuid)) {
            return ResponseGeneration.ResponseEntityWithJsonObject(new JSONObject(), HttpStatus.NOT_ACCEPTABLE);
        }

        //登录校验
        String username = (String)session.getAttribute(LoginManager.SESSION.USER_KEY);
        String password = userData.getPassword();
        UsernamePasswordToken token = new UsernamePasswordToken(username, password);
        try {
            subject.login(token);

            //校验成功
            session.removeAttribute(LoginManager.SESSION.KEYPAIR_KEY);
            session.removeAttribute(LoginManager.SESSION.UUID_KEY);

            //删除用户
            boolean isDelete = userinfoDao.delete(userData.getUsername());
            if (!isDelete) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("message", "删除用户'"+userData.getUsername()+"'失败");
                return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
            }
            return new JSONObject();
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
