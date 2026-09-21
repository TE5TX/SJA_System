package com.sja.system.framework.web;

import com.sja.system.application.model.CurrentActor;
import com.sja.system.common.ApiException;
import com.sja.system.framework.security.SjaUserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 当前操作人Web参数解析测试。 */
class CurrentActorArgumentResolverTest {

    private final CurrentActorArgumentResolver resolver = new CurrentActorArgumentResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesVerifiedPrincipalIntoApplicationModel() throws Exception {
        SjaUserPrincipal principal = new SjaUserPrincipal(7L, "20230001", "", 4, 0, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));

        CurrentActor actor = (CurrentActor) resolver.resolveArgument(parameter(), null, null, null);

        // 202609211059 - 验证Controller获得的身份只来自已验证的SecurityContext主体。
        assertEquals(new CurrentActor(7L, "20230001", 4), actor);
    }

    @Test
    void rejectsMissingAuthenticatedPrincipal() throws Exception {
        assertThrows(ApiException.class,
                () -> resolver.resolveArgument(parameter(), null, null, null));
    }

    private MethodParameter parameter() throws Exception {
        Method method = Fixture.class.getDeclaredMethod("handle", CurrentActor.class);
        return new MethodParameter(method, 0);
    }

    private static final class Fixture {
        @SuppressWarnings("unused")
        void handle(CurrentActor actor) {
        }
    }
}
