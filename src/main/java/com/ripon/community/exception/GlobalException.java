package com.ripon.community.exception;

import com.ripon.community.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

//@ControllerAdvice(annotations = Controller.class)
//public class GlobalException {
//    private static final Logger logger = LoggerFactory.getLogger(GlobalException.class);
//    @ExceptionHandler(Exception.class)
//    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
//        for (StackTraceElement element : e.getStackTrace()) {
//            logger.error(element.toString());
//        }
//
//        String xRequestedWith = request.getHeader("x-requested-with");
//        if ("XMLHttpRequest".equals(xRequestedWith)) {
//            response.setContentType("application/plain;charset=utf-8");
//            PrintWriter writer = response.getWriter();
//            writer.write(JsonUtils.getJSONString(1, "服务器异常!"));
//        } else {
//            response.sendRedirect(request.getContextPath() + "/error");
//        }
//    }
//}
