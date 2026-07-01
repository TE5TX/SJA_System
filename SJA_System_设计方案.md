# SJA System — 校记者团综合管理系统设计方案

> **文档版本**：v1.0
> **数据库版本**：`SJA_System_db.sql`
> **最后更新**：2026-07

---

## 目录

- [一、整体系统架构](#一整体系统架构)
- [二、前端设计方案（Vite + Vue3）](#二前端设计方案vite--vue3)
- [三、后端设计方案（Spring Boot + JavaEE）](#三后端设计方案spring-boot--javaee)
- [四、数据库设计](#四数据库设计)

---

## 一、整体系统架构

### 1.1 架构概述

系统采用 **前后端分离** 架构，通过 **RESTful API** 进行数据交互，各层职责清晰、解耦部署。

```
┌─────────────────────────────────────────────────────────┐
│                      用户浏览器                          │
│              Vite + Vue3 + Element Plus                  │
│   Pinia 状态管理 │ Vue Router 动态路由 │ Axios 请求封装   │
└──────────────────────┬──────────────────────────────────┘
                       │  HTTP / JSON (JWT Token)
                       ▼
┌─────────────────────────────────────────────────────────┐
│                     Nginx 反向代理                       │
│              静态资源托管 │ API 转发 │ HTTPS               │
└──────────────────────┬──────────────────────────────────┘
                       │  /api/v1/**
                       ▼
┌─────────────────────────────────────────────────────────┐
│               Spring Boot 应用服务器                     │
│  Spring Security + JWT │ MyBatis-Plus │ Knife4j 文档     │
│  Controller → Service → Mapper → MySQL                   │
└──────────────────────┬──────────────────────────────────┘
                       │  JDBC
                       ▼
┌─────────────────────────────────────────────────────────┐
│                    MySQL 数据库                          │
│              sja_system_db (utf8mb4)                     │
│  基础管理模块 │ 稿费业务模块 │ 权限与日志模块              │
└─────────────────────────────────────────────────────────┘
```

### 1.2 技术栈总览

| 层级 | 技术栈 | 职责说明 |
|:----:|:------:|:---------|
| **前端** | Vite 5.x + Vue 3 + TypeScript + Element Plus | 页面渲染、动态路由权限控制、纯前端工具（素拓导出）、接口请求 |
| **后端** | Spring Boot 2.7.x / 3.x + MyBatis-Plus + Spring Security + JWT | 无状态 RESTful 接口、核心业务逻辑、权限校验、数据持久化 |
| **数据库** | MySQL 8.x | 存储用户、角色、权限、稿费明细等核心业务数据 |

---

## 二、前端设计方案（Vite + Vue3）

### 2.1 核心技术选型

| 类别 | 选型 | 版本 | 说明 |
|:----:|:----:|:----:|:-----|
| 构建工具 | **Vite** | 5.x | 极速冷启动与热更新，原生 ESM 支持 |
| 核心框架 | **Vue 3** + TypeScript | 3.x | 组合式 API（Composition API），类型安全 |
| UI 组件库 | **Element Plus** | Latest | 高度契合后台管理系统，丰富的表单/表格组件 |
| 状态管理 | **Pinia** | Latest | 轻量级，管理用户信息、Token 与权限菜单树 |
| 网络请求 | **Axios** | Latest | 封装拦截器，自动携带 JWT Token，统一错误处理 |
| 前端工具库 | **xlsx** + **file-saver** | Latest | 用于素拓名单纯前端导出（无需后端参与） |

### 2.2 前端工程目录结构

```
sja-system-frontend/
├── public/                     # 静态资源
├── src/
│   ├── api/                    # 接口请求模块（按业务划分）
│   │   ├── user.ts             # 用户相关接口
│   │   ├── fee.ts              # 稿费相关接口
│   │   └── system.ts           # 系统管理接口
│   ├── assets/                 # 图片、样式等静态资源
│   ├── components/             # 全局公共组件
│   ├── layout/                 # 布局组件（侧边栏、顶栏、面包屑）
│   ├── router/                 # 路由配置（静态路由 + 动态路由逻辑）
│   ├── store/                  # Pinia 状态仓库
│   │   ├── user.ts             # 用户信息 & Token
│   │   └── permission.ts       # 菜单权限树
│   ├── utils/                  # 工具函数（Axios 封装、素拓导出等）
│   ├── views/                  # 页面组件
│   │   ├── login/              # 登录页
│   │   ├── user/profile/       # 个人管理页
│   │   ├── fee/submit/         # 部长稿费提交页
│   │   ├── fee/audit/          # 秘书长稿费审核页
│   │   ├── fee/export/         # 稿费表导出页
│   │   ├── system/users/       # 全部人员信息页
│   │   └── tools/sujia/        # 素拓管理页
│   ├── App.vue                 # 根组件
│   └── main.ts                 # 入口文件
├── index.html
├── vite.config.ts
├── tsconfig.json
└── package.json
```

### 2.3 核心页面与功能拆解

#### 页面一览

| 页面 | 路由路径 | 访问角色 | 核心功能 |
|:-----|:---------|:---------|:---------|
| 登录页 | `/login` | 所有人 | 账号密码登录，获取 JWT Token |
| 个人管理页 | `/user/profile` | 所有登录用户 | 查看/修改个人信息、修改密码 |
| 部长稿费提交页 | `/fee/submit` | 部长 | 按部门切换动态表单，录入稿费明细 |
| 秘书长稿费审核页 | `/fee/audit` | 秘书长 | 审核稿费明细，通过/驳回操作 |
| 全部人员信息页 | `/system/users` | 老师、主席、秘书长 | 全局多条件搜索、信息查看与编辑 |
| 素拓管理页 | `/tools/sujia` | 所有登录用户 | 纯前端解析姓名，生成 Excel 下载 |
| 稿费表导出页 | `/fee/export` | 秘书长、主席 | 按月/部门筛选，导出财务对账单 |

#### 2.3.1 个人管理页（`/user/profile`）

**功能描述**：展示当前登录用户的基本信息，支持自助修改部分字段。

| 功能项 | 说明 |
|:-------|:-----|
| 信息展示 | 学号、真实姓名、性别、学院、班级、宿舍地址、手机号、银行卡号、所属部门、角色 |
| 可编辑字段 | 手机号、宿舍地址、银行卡号 |
| 修改密码 | 输入旧密码 + 新密码（二次确认），调用后端接口完成修改 |

#### 2.3.2 部长稿费提交页（`/fee/submit`）

**功能描述**：部长专属页面，用于按月提交本部门的稿费明细数据。

| 功能项 | 说明 |
|:-------|:-----|
| 部门切换 | Tab 切换采编部、摄影部、视频部、视觉部四个子表单 |
| 稿件/作品录入 | 根据部门类型展示不同的动态表单字段（标题、平台、类别、字数/时长等） |
| 参与人员管理 | 每篇稿件/作品支持动态添加/删除参与人员行，选择人员及角色 |
| 自动计算 | 根据稿费标准配置表（`biz_fee_standard`）实时计算每人应得稿费 |
| 批量提交 | 支持一次提交多稿件/作品的稿费明细 |

**各部门表单字段差异**：

| 部门 | 核心字段 |
|:-----|:---------|
| 采编部 | 文章标题、平台（微信/院网/校报）、类别、字数、阅读量、发布日期、文章链接 |
| 摄影部 | 作品标题、平台、选中图片数、阅读量、发布日期、作品链接 |
| 视频部 | 视频标题、类别（风景/活动新闻/原创短/原创长）、时长、点赞数、转载情况、发布日期 |
| 视觉部 | 文章标题、类别、参与人数、作者稿费均额、发布日期、文章链接 |

#### 2.3.3 秘书长稿费审核页（`/fee/audit`）

**功能描述**：秘书长对所有部长提交的稿费明细进行审核。

| 功能项 | 说明 |
|:-------|:-----|
| 列表展示 | 按时间/部门/状态筛选待审核的稿费明细列表 |
| 详情查看 | 点击展开稿件详情，查看参与人员及稿费分配明细 |
| 审核操作 | 填写审核意见，执行"通过"或"驳回"操作 |
| 状态流转 | 待审核 → 已通过 / 已驳回（驳回后可由部长重新提交） |

#### 2.3.4 全部人员信息页（`/system/users`）

**功能描述**：面向管理层的全局人员信息管理页面。

| 功能项 | 说明 |
|:-------|:-----|
| 多条件搜索 | 支持按姓名、学号、学院、部门、角色等条件组合筛选 |
| 信息查看 | 展示用户完整信息（基本信息 + 部门 + 角色） |
| 信息编辑 | 支持修改用户状态、部门归属、角色分配等 |
| 操作日志 | 所有修改操作自动触发后端日志记录（写入 `sys_operation_log`） |

#### 2.3.5 素拓管理页（`/tools/sujia`）

**功能描述**：纯前端小工具，无需后端参与，用于快速生成素拓名单 Excel。

| 功能项 | 说明 |
|:-------|:-----|
| 姓名输入 | 提供大文本框，支持粘贴姓名（逗号、空格、换行分隔均可识别） |
| 智能解析 | 前端 JS 自动解析去重姓名列表 |
| Excel 生成 | 使用 `xlsx` 库在浏览器端生成包含"学号、姓名、时长"等空白列的标准模板 |
| 一键下载 | 点击按钮直接触发浏览器下载 `.xlsx` 文件 |

#### 2.3.6 稿费表导出页（`/fee/export`）

**功能描述**：导出已审核通过的稿费数据为财务对账 Excel。

| 功能项 | 说明 |
|:-------|:-----|
| 筛选条件 | 按月份、部门筛选已审核通过的稿费数据 |
| 文件下载 | 调用后端接口获取文件流（Blob），一键导出为 Excel 对账单 |
| 数据格式 | 后端使用 EasyExcel 生成格式化的财务报表 |

### 2.4 前端核心机制

#### 2.4.1 动态路由与菜单权限

```
用户登录
  │
  ▼
后端返回角色菜单树 (JSON)
  │
  ▼
Pinia 存储权限数据
  │
  ▼
router.addRoute() 动态注册路由
  │
  ▼
侧边栏菜单自动渲染
```

- 用户登录后，后端根据用户角色查询关联的菜单权限树（`sys_role_menu` → `sys_menu`）并返回。
- 前端通过 `router.addRoute()` 动态注册路由，实现 **页面级权限控制**。
- 未授权的页面路由不会被注册，直接访问 URL 将返回 404。

#### 2.4.2 Axios 请求封装

| 拦截器 | 功能 |
|:-------|:-----|
| **请求拦截器** | 自动从 Pinia 中获取 JWT Token，注入 `Authorization: Bearer <token>` 请求头 |
| **响应拦截器** | 统一处理 HTTP 状态码：`401` 跳转登录页、`403` 提示无权限、`500` 弹出全局错误提示 |
| **Token 刷新** | Token 过期时自动尝试刷新，失败后清除本地状态并跳转登录页 |

#### 2.4.3 按钮级权限控制

- 前端通过自定义指令 `v-permission="'fee:submit'"` 控制按钮的显示/隐藏。
- 按钮权限标识与 `sys_menu.permission_code` 一一对应。

---

## 三、后端设计方案（Spring Boot + JavaEE）

### 3.1 核心技术选型

| 类别 | 选型 | 版本 | 说明 |
|:----:|:----:|:----:|:-----|
| 核心框架 | **Spring Boot** | 2.7.x / 3.x | 自动配置、内嵌 Tomcat、快速开发 |
| ORM 框架 | **MyBatis-Plus** | Latest | 极大简化单表 CRUD，代码生成器、分页插件 |
| 权限认证 | **Spring Security + JWT** | — | 无状态 Token 认证，细粒度权限控制 |
| 接口文档 | **Knife4j** | Swagger 增强版 | 自动生成 API 文档，方便前后端联调 |
| 工具库 | **Hutool / Lombok / EasyExcel** | Latest | Hutool 工具集、Lombok 简化 POJO、EasyExcel 导出 |

### 3.2 后端工程目录结构

```
com.sja.system
├── config/                     # 配置类
│   ├── SecurityConfig.java     #   Spring Security 配置
│   ├── CorsConfig.java         #   跨域配置
│   ├── MybatisPlusConfig.java  #   MyBatis-Plus 分页插件等配置
│   └── Knife4jConfig.java      #   Swagger 文档配置
│
├── controller/                 # 控制层（接收请求，返回 JSON）
│   ├── AuthController.java     #   登录/登出/刷新Token
│   ├── UserController.java     #   用户管理（个人信息 + 全局管理）
│   ├── FeeController.java      #   稿费业务（提交/审核/导出）
│   ├── SystemController.java   #   系统管理（角色/部门/菜单/日志）
│   └── SujiaController.java    #   素拓相关（可选，纯前端则不需要）
│
├── service/                    # 业务逻辑层（接口定义）
│   ├── UserService.java
│   ├── FeeService.java
│   ├── OperationLogService.java
│   └── impl/                   # 业务逻辑层（接口实现）
│       ├── UserServiceImpl.java
│       ├── FeeServiceImpl.java
│       └── OperationLogServiceImpl.java
│
├── mapper/                     # 数据访问层（DAO，MyBatis-Plus BaseMapper）
│   ├── SysUserMapper.java
│   ├── SysRoleMapper.java
│   ├── BizEditorialArticleMapper.java
│   └── ...
│
├── entity/                     # 数据库实体类（一一对应表结构）
│   ├── SysUser.java
│   ├── SysRole.java
│   ├── BizEditorialArticle.java
│   └── ...
│
├── dto/                        # 数据传输对象（接收前端复杂参数）
│   ├── FeeSubmitDTO.java       #   稿费提交 DTO（稿件信息 + 作者列表）
│   ├── FeeAuditDTO.java        #   稿费审核 DTO
│   ├── UserQueryDTO.java       #   用户查询条件 DTO
│   └── LoginDTO.java           #   登录请求 DTO
│
├── vo/                         # 视图对象（返回给前端的脱敏数据）
│   ├── UserVO.java             #   用户信息 VO（脱敏手机号等）
│   ├── FeeDetailVO.java        #   稿费详情 VO
│   ├── MenuTreeVO.java         #   菜单树 VO（递归结构）
│   └── LoginVO.java            #   登录响应 VO（Token + 用户信息）
│
├── security/                   # 安全模块
│   ├── JwtUtils.java           #   JWT 工具类（生成/解析/验证 Token）
│   ├── JwtAuthFilter.java      #   JWT 认证过滤器（请求头解析）
│   └── UserDetailsServiceImpl.java # UserDetailsService 实现
│
└── common/                     # 公共模块
    ├── Result.java             #   统一响应结果封装 {code, message, data}
    ├── GlobalExceptionHandler.java # 全局异常处理 @RestControllerAdvice
    ├── Constants.java          #   常量定义
    └── PageResult.java         #   分页结果封装
```

### 3.3 核心接口（API）设计

#### 3.3.1 认证模块（`AuthController`）

| 方法 | 路径 | 权限 | 说明 |
|:----:|:-----|:----:|:-----|
| `POST` | `/api/v1/auth/login` | 公开 | 账号密码登录，返回 JWT Token + 用户信息 + 菜单树 |
| `POST` | `/api/v1/auth/logout` | 登录 | 登出（前端清除 Token，后端可选黑名单） |
| `POST` | `/api/v1/auth/refresh` | 登录 | 刷新 Token |

#### 3.3.2 用户与权限模块（`UserController`）

| 方法 | 路径 | 权限标识 | 说明 |
|:----:|:-----|:---------|:-----|
| `GET` | `/api/v1/user/profile` | 登录即可 | 获取当前登录人完整信息 |
| `PUT` | `/api/v1/user/profile` | 登录即可 | 更新个人信息（手机号、地址、密码等） |
| `GET` | `/api/v1/users` | `sys:users:view` | 分页查询所有用户（支持多条件组合搜索） |
| `PUT` | `/api/v1/users/{id}` | `sys:users:edit` | 编辑用户信息（自动记录操作日志） |
| `PUT` | `/api/v1/users/{id}/status` | `sys:users:edit` | 启用/禁用用户 |

#### 3.3.3 稿费业务模块（`FeeController`）

| 方法 | 路径 | 权限标识 | 说明 |
|:----:|:-----|:---------|:-----|
| `POST` | `/api/v1/fee/submit` | `fee:submit` | 部长提交稿费明细（事务处理：先插稿件表 → 再插贡献者表 → 根据标准表计算金额） |
| `GET` | `/api/v1/fee/list` | `fee:view` | 查询稿费明细列表（按部门/月份/状态筛选） |
| `GET` | `/api/v1/fee/detail/{id}` | `fee:view` | 查看稿费明细详情（含参与人员分配） |
| `PUT` | `/api/v1/fee/audit/{id}` | `fee:audit` | 秘书长审核操作（通过/驳回 + 审核意见） |
| `GET` | `/api/v1/fee/export` | `fee:export` | 导出已审核通过的稿费数据为 Excel 文件流 |

#### 3.3.4 系统管理模块（`SystemController`）

| 方法 | 路径 | 权限标识 | 说明 |
|:----:|:-----|:---------|:-----|
| `GET` | `/api/v1/system/roles` | `sys:role:view` | 获取角色列表 |
| `GET` | `/api/v1/system/depts` | `sys:dept:view` | 获取部门列表 |
| `GET` | `/api/v1/system/menus` | `sys:menu:view` | 获取菜单树 |
| `GET` | `/api/v1/system/logs` | `sys:log:view` | 分页查询操作日志 |

#### 3.3.5 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

| code | 含义 |
|:----:|:-----|
| `200` | 操作成功 |
| `400` | 请求参数错误 |
| `401` | 未认证（Token 无效或过期） |
| `403` | 无权限 |
| `500` | 服务器内部错误 |

### 3.4 安全与权限控制

#### 3.4.1 认证流程

```
客户端请求 LoginDTO (username + password)
  │
  ▼
AuthController.login()
  │
  ▼
AuthenticationManager.authenticate() → UserDetailsService 查询数据库
  │
  ▼
认证成功 → JwtUtils.generateToken(userId, roleCode)
  │
  ▼
返回 Token + 用户信息 + 菜单权限树
```

#### 3.4.2 三层权限体系

| 层级 | 机制 | 说明 |
|:-----|:-----|:-----|
| **认证层** | JWT 无状态认证 | 登录成功后生成包含 `userId` 和 `roleCode` 的 Token，有效期可配置 |
| **接口级权限** | `@PreAuthorize` 注解 | 使用 `@PreAuthorize("hasAuthority('fee:submit')")` 拦截无权限请求 |
| **数据级隔离** | Service 层强制绑定 | 部长提交稿费时，后端从 Token 解析 `dept_code` 并强制绑定到稿件记录，防止越权操作 |

#### 3.4.3 操作日志自动记录

- 通过 **AOP 切面**（`@Aspect`）或自定义注解 `@OperationLog` 拦截关键操作。
- 自动记录操作人、操作类型、目标模块、操作前后数据对比（JSON 格式）、IP 地址等信息。
- 日志写入 `sys_operation_log` 表，仅供秘书长和主席查看。

---

## 四、数据库设计

> **数据库名**：`sja_system_db`
> **字符集**：`utf8mb4` &nbsp;&nbsp;|&nbsp;&nbsp; **排序规则**：`utf8mb4_general_ci`
> **存储引擎**：InnoDB（全部表）

### 数据库表总览

| 模块 | 表名 | 说明 |
|:----:|:-----|:-----|
| **基础管理** | `sys_role` | 角色表 |
| | `sys_dept` | 部门表 |
| | `sys_user` | 用户表 |
| | `sys_user_role` | 用户-角色关联表 |
| | `sys_user_dept` | 用户-部门关联表 |
| **稿费业务** | `biz_editorial_article` | 采编部稿件表 |
| | `biz_photography_work` | 摄影部作品表 |
| | `biz_video_work` | 视频部作品表 |
| | `biz_visual_work` | 视觉部作品表 |
| | `biz_work_contributor` | 统一稿费人员分配表 |
| | `biz_fee_standard` | 稿费标准配置表 |
| **权限与日志** | `sys_menu` | 菜单/权限资源表 |
| | `sys_role_menu` | 角色-菜单关联表 |
| | `sys_operation_log` | 操作日志表 |

### 表关系概览

```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
    │
    └──< sys_user_dept >── sys_dept

biz_editorial_article ──< biz_work_contributor (work_type='EDITORIAL')
biz_photography_work  ──< biz_work_contributor (work_type='PHOTO')
biz_video_work        ──< biz_work_contributor (work_type='VIDEO')
biz_visual_work       ──< biz_work_contributor (work_type='VISUAL')

biz_work_contributor >── sys_user
biz_fee_standard (稿费规则配置，独立表)
sys_operation_log (操作日志，独立表)
```

---

### 4.1 基础管理模块

#### 4.1.1 角色表（`sys_role`）

> 存储系统所有角色定义，通过 `role_code` 作为唯一业务标识。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `role_name` | VARCHAR(50) | NOT NULL | 角色名称（如：管理员、部长、实习部员） |
| `role_code` | INT | NOT NULL, UNIQUE | 角色代码（唯一标识，用于业务逻辑判断） |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 排序号 |
| `is_valid` | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否有效：`1`-有效，`0`-无效 |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**预置角色数据**：

| 角色名称 | role_code | 排序 | 分类 | 权限说明 |
|:---------|:---------:|:----:|:-----|:---------|
| 主席 | 1 | 1 | 核心管理层 | 最高权限，可查看/修改所有人信息 |
| 老师 | 2 | 2 | 核心管理层 | 指导老师，可查看所有人信息及已审核的稿费、操作日志 |
| 秘书长 | 3 | 3 | 核心管理层 | 负责审核明细表、导出月度稿费发放表、管理所有用户信息及查看日志 |
| 部长 | 4 | 10 | 部门管理层 | 各部门负责人，负责提交每月的稿费明细表 |
| 微信统筹编辑 | 5 | 20 | 采编部细分角色 | 采编部微信统筹负责人 |
| 排版编辑 | 6 | 21 | 采编部细分角色 | 负责微信公众号文章排版 |
| 校对 | 7 | 22 | 采编部细分角色 | 负责文章错别字及内容校对 |
| 实习部员（一轮） | 8 | 30 | 实习与小队角色 | 第一轮招新实习部员 |
| 实习部员（二轮） | 9 | 31 | 实习与小队角色 | 第二轮招新实习部员 |
| 留任部员 | 10 | 32 | 实习与小队角色 | 实习期满留任的正式部员 |
| 校园小队队长 | 11 | 33 | 实习与小队角色 | 校园小队负责人 |
| 校园小队队员 | 12 | 34 | 实习与小队角色 | 校园小队成员 |
| 游客 | 13 | 99 | 其他角色 | 未登录或无具体职务的访客，仅拥有基础查看权限 |

---

#### 4.1.2 部门表（`sys_dept`）

> 存储组织架构中的部门信息。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `dept_name` | VARCHAR(50) | NOT NULL | 部门名称 |
| `dept_code` | VARCHAR(30) | NOT NULL, UNIQUE | 部门代码（业务标识，如 `EDITORIAL`） |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 排序号 |
| `is_valid` | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否有效 |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**预置部门数据**：

| 部门名称 | dept_code | 排序 | 职能说明 |
|:---------|:----------|:----:|:---------|
| 主席团 | `PRESIDIUM` | 1 | 组织最高决策与管理团队 |
| 采编部 | `EDITORIAL` | 2 | 负责文章撰写、编辑、排版与校对 |
| 摄影部 | `PHOTOGRAPHY` | 3 | 负责活动现场拍摄与后期修图 |
| 视觉部 | `VISUAL` | 4 | 负责海报设计、视觉排版与美工 |
| 视频部 | `VIDEO` | 5 | 负责视频策划、拍摄与剪辑 |

---

#### 4.1.3 用户表（`sys_user`）

> 存储系统所有用户的基本信息，`username` 和 `student_no` 均为唯一键。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `username` | VARCHAR(50) | NOT NULL, UNIQUE | 用户名（登录账号） |
| `password` | VARCHAR(255) | NOT NULL | 密码（BCrypt 加密存储） |
| `real_name` | VARCHAR(50) | NOT NULL | 真实姓名 |
| `gender` | TINYINT(1) | NOT NULL, DEFAULT 0 | 性别：`0`-未知，`1`-男，`2`-女 |
| `photo` | VARCHAR(255) | NULLABLE | 照片URL |
| `id_card` | VARCHAR(18) | NULLABLE | 身份证号 |
| `student_no` | VARCHAR(20) | NOT NULL, UNIQUE | 学号 |
| `college` | VARCHAR(100) | NULLABLE | 二级学院 |
| `class_name` | VARCHAR(50) | NULLABLE | 班级 |
| `dorm_address` | VARCHAR(100) | NULLABLE | 宿舍地址 |
| `phone` | VARCHAR(20) | NULLABLE | 手机号 |
| `bank_card` | VARCHAR(30) | NULLABLE | 银行卡号 |
| `is_valid` | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否有效 |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

#### 4.1.4 用户-角色关联表（`sys_user_role`）

> 多对多关联表，一个用户可拥有多个角色。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `user_id` | BIGINT UNSIGNED | NOT NULL, UK | 用户ID（FK → `sys_user.id`） |
| `role_id` | BIGINT UNSIGNED | NOT NULL, UK, IDX | 角色ID（FK → `sys_role.id`） |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

- **唯一键**：`(user_id, role_id)` — 防止重复分配相同角色

---

#### 4.1.5 用户-部门关联表（`sys_user_dept`）

> 多对多关联表，一个用户可归属多个部门。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `user_id` | BIGINT UNSIGNED | NOT NULL, UK | 用户ID（FK → `sys_user.id`） |
| `dept_id` | BIGINT UNSIGNED | NOT NULL, UK, IDX | 部门ID（FK → `sys_dept.id`） |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

- **唯一键**：`(user_id, dept_id)` — 防止重复分配相同部门

---

### 4.2 稿费业务模块

#### 4.2.1 采编部稿件表（`biz_editorial_article`）

> 记录采编部发布的文章信息及稿费数据。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `title` | VARCHAR(255) | NOT NULL | 文章标题 |
| `platform` | VARCHAR(20) | NOT NULL | 发布平台：`微信` / `院网` / `校报` |
| `category` | VARCHAR(50) | NULLABLE | 类别 |
| `word_count` | INT | DEFAULT 0 | 字数 |
| `read_count` | INT | DEFAULT 0 | 阅读量 |
| `publish_date` | DATE | NOT NULL | 发布日期 |
| `article_link` | VARCHAR(500) | NULLABLE | 文章链接 |
| `is_published` | TINYINT(1) | DEFAULT 0 | 是否已发布 |
| `total_amount` | DECIMAL(10,2) | DEFAULT 0.00 | 文章总稿费（元） |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

#### 4.2.2 摄影部作品表（`biz_photography_work`）

> 记录摄影部发布的作品信息及稿费数据。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `title` | VARCHAR(255) | NOT NULL | 作品标题 |
| `platform` | VARCHAR(20) | NOT NULL | 发布平台：`微信` / `院网` / `校报` |
| `photo_count` | INT | DEFAULT 0 | 选中图片数 |
| `read_count` | INT | DEFAULT 0 | 阅读量（微信用） |
| `publish_date` | DATE | NOT NULL | 发布日期 |
| `article_link` | VARCHAR(500) | NULLABLE | 作品链接 |
| `is_published` | TINYINT(1) | DEFAULT 0 | 是否已发布 |
| `total_amount` | DECIMAL(10,2) | DEFAULT 0.00 | 作品总稿费（元） |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

#### 4.2.3 视频部作品表（`biz_video_work`）

> 记录视频部发布的视频作品信息及稿费数据。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `video_title` | VARCHAR(255) | NOT NULL | 视频标题 |
| `category` | VARCHAR(50) | NOT NULL | 类别：`风景` / `活动新闻` / `原创短` / `原创长` |
| `video_duration_sec` | INT | DEFAULT 0 | 视频时长（秒） |
| `like_count` | INT | DEFAULT 0 | 点赞数 |
| `reprint_status` | VARCHAR(50) | DEFAULT '无' | 转载情况 |
| `publish_date` | DATE | NOT NULL | 发布日期 |
| `video_link` | VARCHAR(500) | NULLABLE | 视频链接 |
| `is_published` | TINYINT(1) | DEFAULT 0 | 是否已发布 |
| `total_amount` | DECIMAL(10,2) | DEFAULT 0.00 | 视频总稿费（元） |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

#### 4.2.4 视觉部作品表（`biz_visual_work`）

> 记录视觉部发布的设计作品信息及稿费数据。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `title` | VARCHAR(255) | NOT NULL | 文章标题 |
| `category` | VARCHAR(50) | NULLABLE | 类别 |
| `author_count` | INT | DEFAULT 0 | 参与人数 |
| `author_avg_fee` | DECIMAL(10,2) | DEFAULT 0.00 | 作者稿费均额（元） |
| `publish_date` | DATE | NOT NULL | 发布日期 |
| `article_link` | VARCHAR(500) | NULLABLE | 文章链接 |
| `is_published` | TINYINT(1) | DEFAULT 0 | 是否已发布 |
| `total_amount` | DECIMAL(10,2) | DEFAULT 0.00 | 总金额（元） |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

---

#### 4.2.5 统一稿费人员分配表（`biz_work_contributor`）

> **核心关联表**：统一记录四类作品（采编/摄影/视频/视觉）的参与人员及稿费分配。通过 `work_type` 字段区分作品类型。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `work_type` | VARCHAR(20) | NOT NULL, UK | 作品类型枚举 |
| `work_id` | BIGINT UNSIGNED | NOT NULL, UK | 对应作品表的 ID |
| `user_id` | BIGINT UNSIGNED | NOT NULL, UK, IDX | 关联用户（FK → `sys_user.id`） |
| `role_in_work` | VARCHAR(20) | NOT NULL, UK | 在作品中的角色 |
| `fee_amount` | DECIMAL(10,2) | NOT NULL, DEFAULT 0.00 | 该成员最终应得稿费（元） |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**`work_type` 枚举值**：

| 枚举值 | 含义 | 关联表 |
|:-------|:-----|:-------|
| `EDITORIAL` | 采编部稿件 | `biz_editorial_article` |
| `PHOTO` | 摄影部作品 | `biz_photography_work` |
| `VIDEO` | 视频部作品 | `biz_video_work` |
| `VISUAL` | 视觉部作品 | `biz_visual_work` |

**`role_in_work` 枚举值**：

| 枚举值 | 含义 | 适用部门 |
|:-------|:-----|:---------|
| `AUTHOR` | 作者 | 采编部 |
| `SHOOTER` | 拍摄 | 摄影部、视频部 |
| `EDITOR` | 剪辑/编辑 | 视频部、采编部 |
| `DESIGNER` | 设计 | 视觉部 |

- **唯一键**：`(work_type, work_id, user_id, role_in_work)` — 同一作品中同一用户同一角色不可重复

---

#### 4.2.6 稿费标准配置表（`biz_fee_standard`）

> 可配置的稿费计算规则表，支持按部门、平台、类别灵活设置稿费标准。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `dept_code` | VARCHAR(30) | NOT NULL, UK | 部门代码（FK → `sys_dept.dept_code`） |
| `platform` | VARCHAR(20) | NOT NULL, UK | 发布平台 |
| `category` | VARCHAR(50) | UK | 类别（可为空表示通用） |
| `rule_key` | VARCHAR(50) | NOT NULL, UK | 规则键（如 `base_fee`, `per_word_fee`, `read_bonus`） |
| `rule_value` | DECIMAL(10,4) | NOT NULL | 规则值 |
| `description` | VARCHAR(255) | NULLABLE | 规则描述 |
| `is_active` | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否启用：`1`-是，`0`-否 |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

- **唯一键**：`(dept_code, platform, category, rule_key)` — 同一维度下规则键不可重复

**`rule_key` 示例**：

| rule_key | 含义 | rule_value 示例 |
|:---------|:-----|:---------------|
| `base_fee` | 基础稿费 | `10.0000`（元/篇） |
| `per_word_fee` | 按字数计费 | `0.0500`（元/字） |
| `read_bonus_threshold` | 阅读量奖励阈值 | `1000.0000` |
| `read_bonus_amount` | 阅读量达标奖励 | `5.0000`（元） |

---

### 4.3 权限与日志模块

#### 4.3.1 菜单/权限资源表（`sys_menu`）

> 存储系统菜单、页面和按钮权限，支持树形结构。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `menu_name` | VARCHAR(50) | NOT NULL | 菜单/按钮名称 |
| `permission_code` | VARCHAR(100) | NOT NULL, UNIQUE | 权限标识（如 `fee:export`、`sys:users:view`） |
| `menu_type` | TINYINT(1) | NOT NULL | 类型：`1`-目录，`2`-菜单，`3`-按钮 |
| `parent_id` | BIGINT UNSIGNED | DEFAULT 0 | 父级ID（`0` 表示顶级） |
| `path` | VARCHAR(200) | NULLABLE | 前端路由路径 |
| `sort_order` | INT | NOT NULL, DEFAULT 0 | 排序号 |
| `is_valid` | TINYINT(1) | NOT NULL, DEFAULT 1 | 是否有效 |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| `update_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**`menu_type` 层级说明**：

| 类型值 | 含义 | 示例 |
|:------:|:-----|:-----|
| `1` | 目录 | 稿费管理、系统管理 |
| `2` | 菜单 | 稿费提交页、稿费审核页 |
| `3` | 按钮 | 审核通过按钮、导出按钮 |

---

#### 4.3.2 角色-菜单关联表（`sys_role_menu`）

> 多对多关联表，定义每个角色拥有的菜单/按钮权限。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `role_id` | BIGINT UNSIGNED | NOT NULL, UK | 角色ID（FK → `sys_role.id`） |
| `menu_id` | BIGINT UNSIGNED | NOT NULL, UK | 菜单ID（FK → `sys_menu.id`） |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

- **唯一键**：`(role_id, menu_id)` — 防止重复授权

---

#### 4.3.3 操作日志表（`sys_operation_log`）

> 记录系统中所有关键操作的审计日志，仅追加不修改。

| 字段 | 类型 | 约束 | 说明 |
|:-----|:-----|:-----|:-----|
| `id` | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 主键ID |
| `user_id` | BIGINT UNSIGNED | NOT NULL, IDX | 操作人ID |
| `real_name` | VARCHAR(50) | NOT NULL | 操作人姓名（冗余存储，防止用户改名后日志不可读） |
| `role_name` | VARCHAR(50) | NULLABLE | 操作人当时的角色（快照） |
| `operation_type` | VARCHAR(50) | NOT NULL | 操作类型（如：审核稿费、修改个人信息、导出稿费表） |
| `target_module` | VARCHAR(50) | NOT NULL | 操作模块（如：视频部稿费、用户管理） |
| `target_id` | BIGINT UNSIGNED | NULLABLE | 操作对象的ID |
| `detail` | TEXT | NULLABLE | 操作详情/修改前后的数据对比（JSON 格式） |
| `ip_address` | VARCHAR(50) | NULLABLE | 操作IP地址 |
| `remark` | VARCHAR(500) | NULLABLE | 备注 |
| `create_time` | DATETIME | DEFAULT CURRENT_TIMESTAMP, IDX | 操作时间 |

**`detail` 字段 JSON 格式示例**：

```json
{
  "before": { "phone": "13800000001", "dorm_address": "A栋301" },
  "after":  { "phone": "13800000002", "dorm_address": "A栋301" },
  "changed_fields": ["phone"]
}
```
