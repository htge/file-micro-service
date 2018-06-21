package com.htge.login.controller.Map;

import com.htge.login.model.UserData;
import com.htge.login.model.UserinfoDao;
import com.htge.login.model.Userinfo;
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
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import java.security.KeyPair;
import java.security.PrivateKey;

public class ChangeMap {
    private UserinfoDao userinfoDao;

    public void setUserinfoDao(UserinfoDao userinfoDao) {
        this.userinfoDao = userinfoDao;
    }

    public Object changePage(HttpServletRequest request) {
        Subject subject = SecurityUtils.getSubject();
        if (LoginManager.isAuthorized(subject)) {
            Session session = subject.getSession();
            KeyPair keyPair = (KeyPair) session.getAttribute("keypair");
            if (keyPair == null) {
                keyPair = Crypto.getCachedKeyPair();
            }
            if (keyPair != null) {
                String rsaPublicKey = Crypto.getPublicKey(keyPair);
                session.setAttribute("keypair", keyPair);
                ModelAndView view = new ModelAndView("change");
                view.addObject("rsa", rsaPublicKey);
                view.addObject("Path", request.getSession().getServletContext().getContextPath());
                return view;
            }
        }
        return new ModelAndView("redirect:/auth/");
    }

    public ResponseEntity changeUserInfo(UserData userData) {
        Subject subject = SecurityUtils.getSubject();
        Session session = subject.getSession();
        if (session != null) {
            KeyPair keyPair = (KeyPair) session.getAttribute("keypair");
            if (keyPair != null) {
                PrivateKey privateKey = keyPair.getPrivate();
                userData.decryptDatas(privateKey);
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
                String username = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
                String password = userData.getPassword();
                UsernamePasswordToken token = new UsernamePasswordToken(username, password);
                try {
                    subject.login(token);

                    //修改用户信息
                    Userinfo userinfo = userinfoDao.findUser(username);
                    userinfo.setUserdata(Crypto.generateUserData(username, newPassword));
                    if (userinfoDao.update(userinfo)) {
                        LoginManager.Logout(subject);
                        return new ResponseEntity<>("{}", HttpStatus.OK);
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
            }
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", "修改密码失败");
        return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
    }
}
