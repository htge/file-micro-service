package com.htge.download.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(value = Exception.class)
    public ModelAndView defaultErrorHandler(Exception e) {
        if (e instanceof HttpRequestMethodNotSupportedException) {
            return new ModelAndView("error", HttpStatus.NOT_FOUND);
        }
        ModelAndView view = new ModelAndView("error", HttpStatus.INTERNAL_SERVER_ERROR);
        view.addObject("title", "服务器出错了");
        view.addObject("errorMessage", e.getLocalizedMessage());
        return view;
    }
}
