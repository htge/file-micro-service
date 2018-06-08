package com.htge.login.controller.Map;

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
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.security.KeyPair;
import java.security.PrivateKey;

@Component
public class DeleteMap {
    @Resource(name = "UserinfoDao")
    private UserinfoDao userinfoDao;

    public Object deletePage(String username, HttpServletRequest request) {
        Subject subject = SecurityUtils.getSubject();
        if (LoginManager.getUserRule(subject, userinfoDao) == LoginManager.LoginRole.Admin) {
            Session session = subject.getSession();
            if (session == null) {
                return new ResponseEntity<>("{}", HttpStatus.INTERNAL_SERVER_ERROR);
            }
            KeyPair keyPair = (KeyPair) session.getAttribute("keypair");
            if (keyPair == null) {
                keyPair = Crypto.getCachedKeyPair();
            }
            if (keyPair != null) {
                String rsaPublicKey = Crypto.getPublicKey(keyPair);
                session.setAttribute("keypair", keyPair);
                ModelAndView view = new ModelAndView("delete");
                view.addObject("username", username);
                view.addObject("rsa", rsaPublicKey);
                view.addObject("Path", request.getSession().getServletContext().getContextPath());
                return view;
            }
        }
        return new ModelAndView("redirect:/auth/");
    }

    public ResponseEntity deleteUser(UserData userData) {
        Subject subject = SecurityUtils.getSubject();
        if (LoginManager.getUserRule(subject, userinfoDao) == LoginManager.LoginRole.Admin) {
            Session session = subject.getSession(); //经过条件判断，session一定不为空
            KeyPair keyPair = (KeyPair) session.getAttribute("keypair");
            if (keyPair != null) {
                PrivateKey privateKey = keyPair.getPrivate();
                userData.decryptDatas(privateKey);

                //登录校验
                String username = (String)session.getAttribute(LoginManager.SESSION_USER_KEY);
                String password = userData.getPassword();
                UsernamePasswordToken token = new UsernamePasswordToken(username, password);
                try {
                    subject.login(token);

                    //校验成功
                    session.removeAttribute("keypair");
                    boolean isDelete = userinfoDao.delete(userData.getUsername());
                    if (isDelete) {
                        return new ResponseEntity<>("{}", HttpStatus.OK);
                    }
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", "删除用户'"+ userData.getUsername()+"'失败");
        return ResponseGeneration.ResponseEntityWithJsonObject(jsonObject, HttpStatus.BAD_REQUEST);
    }
}
