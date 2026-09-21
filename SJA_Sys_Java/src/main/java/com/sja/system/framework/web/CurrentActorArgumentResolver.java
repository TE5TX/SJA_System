package com.sja.system.framework.web;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.ApiException;
import com.sja.system.framework.security.SjaUserPrincipal;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 将 Spring Security 主体转换为传输无关的当前操作人模型。
 */
@Component
// 202609211059 - 完成了受保护接口当前操作人参数的统一解析和未认证兜底。
public class CurrentActorArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return CurrentActor.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof SjaUserPrincipal principal)) {
            throw ApiException.unauthorized("未授权");
        }
        return new CurrentActor(principal.getUserId(), principal.getUsername(), principal.getHighestRoleCode());
    }
}
