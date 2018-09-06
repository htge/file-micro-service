package com.htge.upload.exception;

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
        boolean isGetMethod = request.getMethod().equals("GET");
        if (isGetMethod) {
            if (e instanceof HttpRequestMethodNotSupportedException) {
                return new ModelAndView("error", HttpStatus.NOT_FOUND);
            }
            ModelAndView view = new ModelAndView("error", HttpStatus.INTERNAL_SERVER_ERROR);
            view.addObject("title", "服务器出错了");
            view.addObject("errorMessage", e.getLocalizedMessage());
            return view;
        }
        //post请求只返回json格式的数据，TODO: 有空的时候时候针对全局的字段做一下统一定义，一些页面和Restful接口抽离拆分
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("error", e.getLocalizedMessage());
        jsonObject.put("errorCode", -1);
        return new ResponseEntity<>(jsonObject, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
