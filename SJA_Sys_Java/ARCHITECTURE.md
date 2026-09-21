# 后端目录与依赖规范

<!-- 202609211427 - 完成了参考 Form 项目分层方式的后端目录说明。 -->

## 主目录

```text
com.sja.system
├─ application   应用层共享 DTO、VO、端口和传输模型
├─ controller    HTTP 入口，按业务域分包
├─ service       业务与应用服务，按业务域分包
├─ dal
│  ├─ dataobject 数据库对象
│  └─ mysql      MyBatis Mapper、查询模型和 SQL Provider
├─ framework     Security、Web、Redis、Excel及配置实现
└─ common        通用结果、异常与无业务属性的工具
```

## 依赖方向

```text
controller → service → dal
     │           │
     └──────────→ application ← framework适配实现
```

- Controller 只允许参数绑定、鉴权声明和 HTTP 响应转换。
- Service 不得依赖 Controller、Servlet 或 Spring Web 上传类型。
- DAL 不得依赖 Service 或 Controller。
- Application 中的端口和模型不得依赖具体基础设施实现。
- Framework 负责实现安全、Web、Redis、Excel等技术细节。
- 新业务代码必须进入对应业务域目录，不得重新平铺到 `controller` 或 `service` 根目录。

以上约束由 `LayerDependencyTest` 持续检查。
