package com.example.aicodeproductbackend.aop;

import com.example.aicodeproductbackend.annotation.AuthCheck;
import com.example.aicodeproductbackend.exception.BusinessException;
import com.example.aicodeproductbackend.exception.ErrorCode;
import com.example.aicodeproductbackend.model.entity.User;
import com.example.aicodeproductbackend.model.enums.UserRoleEnum;
import com.example.aicodeproductbackend.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuthInterceptor {
    @Resource
    private UserService userService;

    @Around("@annotation(authCheck)")
    public Object authCheck(ProceedingJoinPoint joinPoint , AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        //获取当前登录用户
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        //当前用户不需要权限校验，放行
        if (mustRoleEnum== null) {
            joinPoint.proceed();
        }
        UserRoleEnum userRoleEnum= UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        //无权限
        if (userRoleEnum==null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"无权限");
        }
        //管理员权限
        if(UserRoleEnum.ADMIN.equals(mustRoleEnum)&&!UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"权限不足");
        }
        return joinPoint.proceed();
    }
}
