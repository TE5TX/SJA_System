package com.sja.system.application.model;

/**
 * 当前已认证操作人的应用层快照。
 *
 * <p>该模型不依赖 Servlet 或 Spring Security，应用服务可以在不感知传输层的情况下
 * 取得审计和数据权限所需的最小身份信息。</p>
 */
// 202609211059 - 完成了Controller身份解析结果向传输无关应用模型的统一收口。
public record CurrentActor(Long userId, String username, Integer roleCode) {

    /** 返回现有审计记录使用的角色文本格式。 */
    public String roleText() {
        return roleCode == null ? "" : String.valueOf(roleCode);
    }
}
