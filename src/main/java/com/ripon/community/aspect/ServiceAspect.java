//package com.ripon.community.aspect;
//
//import com.alibaba.fastjson.JSONObject;
//import org.aspectj.lang.JoinPoint;
//import org.aspectj.lang.Signature;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Before;
//import org.aspectj.lang.annotation.Pointcut;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.stereotype.Component;
//@Aspect
//@Component
//public class ServiceAspect {
//
//    private static final Logger logger = LoggerFactory.getLogger(ServiceAspect.class);
//    @Pointcut("execution(* com.ripon.community.service.*.*(..))")
//    public void pointcut() {}
//
//    @Before("pointcut()")
//    public void doBefore(JoinPoint joinPoint){
//        //获取请求的方法
//        Signature sig = joinPoint.getSignature();
//        String method = joinPoint.getTarget().getClass().getName() + "." + sig.getName();
//
//        //获取请求的参数
//        Object[] args = joinPoint.getArgs();
//        //fastjson转换
//        String params = JSONObject.toJSONString(args);
//
//        //打印请求参数
//        logger.info(method + ":" + params);
//    }
//}
