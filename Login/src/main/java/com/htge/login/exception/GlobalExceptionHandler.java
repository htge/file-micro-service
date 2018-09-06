package com.htge.login.exception;

import net.sf.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public Object defaultErrorHandler(Exception e, HttpServletRequest request) {
        if (request.getMethod().equals("GET")) {
            if (e instanceof HttpRequestMethodNotSupportedException) {
                return new ModelAndView("error", HttpStatus.NOT_FOUND);
            }
            ModelAndView view = new ModelAndView("error", HttpStatus.INTERNAL_SERVER_ERROR);
            view.addObject("title", "服务器出错了");
            view.addObject("errorMessage", e.getLocalizedMessage());
            return view;
        }
        //post请求只返回json格式的数据
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("message", e.getLocalizedMessage());
        return new ResponseEntity<>(jsonObject, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
