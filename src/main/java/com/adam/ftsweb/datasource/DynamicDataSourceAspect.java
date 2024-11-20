package com.adam.ftsweb.datasource;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * 读写分离切面配置
 */
@Aspect
@Component
@Slf4j
public class DynamicDataSourceAspect {

    @Pointcut("execution(public * com.adam.ftsweb.mapper..*.insert*(..))")
    public void savePointcut() {
    }
    @Pointcut("execution(public * com.adam.ftsweb.mapper..*.update*(..))")
    public void updatePointcut(){
    }
    @Pointcut("execution(public * com.adam.ftsweb.mapper..*.delete*(..))")
    public void deletePointcut(){
    }
    @Pointcut("execution(public * com.adam.ftsweb.mapper..*.*(..))")
    public void anyPointcut(){
    }

    @Before("savePointcut() || updatePointcut() || deletePointcut()")
    public void masterMethods(JoinPoint joinPoint) {
        log.debug(joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName() + ",using master");
        DynamicDataSourceKeyHolder.useMaster();
    }

    @Before("anyPointcut() && !savePointcut() && !updatePointcut() && !deletePointcut()")
    public void anyMethods(JoinPoint joinPoint) {
        log.debug(joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName() + ",using slave");
        DynamicDataSourceKeyHolder.useSlave();
    }

}