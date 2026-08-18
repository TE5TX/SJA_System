# 基于 form_frontend 的 SJA 前端重构方案

## 1. 方案结论

建议以 `C:/Users/Administrator/Desktop/form_yjhuang/form_frontend/src` 的后台管理端为基础，新建一个独立的 SJA 前端工程或独立分支，复用其布局、响应式检测、通用组件、TypeScript、UnoCSS 和工程化能力，再迁移 SJA 的业务页面。

不建议直接把 SJA 页面复制进 `form_frontend` 后立即替换现有系统，原因是两个项目的登录、令牌、动态菜单、权限和接口返回协议不同。正确做法是增加一层 SJA 兼容适配，并按模块逐步迁移。

推荐目标工程目录：

```text
C:/Users/Administrator/Desktop/SJA_System/SJA_Sys_Form_Vite
```

原 `form_frontend` 作为参考源保留，避免后续更新或回退困难。

## 2. form_frontend 可复用能力

### 2.1 可以直接借用

- Vue 3 + TypeScript + Vite 工程结构；
- Element Plus 统一组件体系；
- Pinia 模块化状态管理；
- `ConfigGlobal.vue` 中基于 `useWindowSize` 的移动端检测；
- 移动端侧栏遮罩和自动折叠逻辑；
- `Layout.vue`、`useRenderLayout.tsx`、`AppView.vue` 的布局框架；
- `ContentWrap`、`Dialog`、`Pagination`、`Table`、`Icon` 等通用组件；
- UnoCSS 响应式工具类；
- 统一 Axios 请求、上传和下载能力；
- ESLint、Stylelint、Prettier 和 TypeScript 检查。

### 2.2 不能直接照搬

- 原项目的登录接口为 `/system/auth/login`，SJA 使用 `/v1/auth/login`；
- 原项目期望 `accessToken`、`refreshToken`，SJA 当前返回 `token`、`userInfo`、`menus`、`mustChangePassword`；
- 原项目使用租户、后端动态菜单和权限标识，SJA 当前主要根据角色代码控制权限；
- 原项目包含大量 ERP、WMS、商城、工作流等无关模块；
- 原项目部分业务表格仍使用固定宽度操作列，本身也不能自动解决 SJA 手机端表格问题；
- `uniapp-user` 是独立移动端表单项目，不是后台管理端的直接替代品。

因此，该方案是“复用基础设施并重建 SJA 模块”，不是把两个项目机械合并。

## 3. 推荐架构

```text
SJA_Sys_Form_Vite/
├─ src/
│  ├─ api/
│  │  └─ sja/
│  │     ├─ auth.ts
│  │     ├─ user.ts
│  │     ├─ fee.ts
│  │     ├─ system.ts
│  │     ├─ sujia.ts
│  │     └─ upload.ts
│  ├─ components/
│  │  └─ Sja/
│  │     ├─ ResponsiveDataView.vue
│  │     ├─ ResponsiveDialog.vue
│  │     ├─ MobileActionBar.vue
│  │     ├─ WorkInfoForm.vue
│  │     ├─ ContributorEditor.vue
│  │     └─ AuthorInput.vue
│  ├─ router/
│  │  └─ modules/
│  │     └─ sja.ts
│  ├─ store/modules/
│  │  └─ sjaUser.ts
│  ├─ views/sja/
│  │  ├─ profile/
│  │  ├─ users/
│  │  ├─ fee-submit/
│  │  ├─ fee-audit/
│  │  ├─ fee-standards/
│  │  ├─ operation-logs/
│  │  └─ sujia/
│  └─ styles/
│     └─ sja-responsive.scss
└─ package.json
```

## 4. 工程裁剪策略

推荐采用“精简副本”方式：

1. 复制 `form_frontend` 为新的 SJA 前端工程。
2. 保留布局、登录框架、通用组件、样式、请求层和构建配置。
3. 删除或停止注册 ERP、WMS、商城、支付、物联网、工作流等无关页面和路由。
4. 保留必要依赖，等 SJA 功能稳定后再清理未使用依赖。
5. 将所有 SJA 页面放入 `views/sja`，避免和模板原业务目录混杂。

第一阶段不要急于大规模删除依赖。先完成可运行迁移，再通过构建分析和代码搜索分批清理，风险更小。

## 5. SJA 接口适配

### 5.1 独立请求实例

建议新增 `src/config/axios/sjaService.ts`，专门适配 SJA 后端。这样不会破坏 `form_frontend` 原有请求协议，也方便迁移期间同时运行两类接口。

请求实例需要满足：

- `baseURL` 指向 `/api`；
- 从 SJA 用户状态或本地缓存读取 `token`；
- 请求头使用 `Authorization: Bearer <token>`；
- 成功码为 `200`；
- `blob` 响应不进行普通 JSON 解包；
- `401` 清理 SJA 登录信息并跳转登录页；
- “首次登录请先修改密码”跳转到强制修改密码页；
- 错误消息只弹出一次。

建议保持现有 SJA 后端接口不变，例如：

```ts
export const getUserList = (params: UserQuery) =>
  sjaRequest.get<PageResult<UserVO>>({ url: '/v1/users', params })

export const exportFee = (data: FeeExportRequest) =>
  sjaRequest.download({ url: '/v1/fee/export', method: 'POST', data })
```

### 5.2 类型定义

为后端返回数据补充 TypeScript 类型：

- `SjaLoginResult`；
- `SjaUserInfo`；
- `UserVO`、`UserDetailVO`；
- `FeeListVO`、`FeeDetailVO`、`FeeContributorVO`；
- `DeptVO`、`RoleVO`、`OperationLogVO`；
- 分页和统一响应类型。

类型应按现有后端实际 JSON 定义，不修改字段名称来迎合模板。

## 6. 登录与权限适配

### 6.1 登录状态

新增 `sjaUser.ts`，保存：

```ts
interface SjaUserState {
  token: string
  userInfo?: SjaUserInfo
  menus: SjaMenu[]
  mustChangePassword: boolean
}
```

登录流程继续使用 SJA 规则：

1. 调用 `/v1/auth/login`；
2. 保存 `token` 和 `userInfo`；
3. 如果 `mustChangePassword` 为 `true`，跳转修改密码；
4. 否则进入 SJA 工作区；
5. 退出时调用 `/v1/auth/logout` 并清理状态。

### 6.2 权限模型

保留当前角色语义：

| 角色 | 前端能力 |
| --- | --- |
| 管理员 | 所有页面和所有操作，便于测试 |
| 老师、主席、秘书长 | 查看和修改所有人员、使用素拓；具体稿费权限按现有规则 |
| 秘书长 | 查看金额、审核、编辑金额、撤回审核、导出稿费 |
| 部长 | 提交任务、查看所有稿费任务、编辑未审核任务，不显示金额，不可审核 |
| 普通成员 | 个人资料等已有授权功能 |

建议创建统一权限组合函数，而不是在每个页面重复判断数字角色：

```ts
export function useSjaPermission() {
  return {
    canManageUsers,
    canSubmitFee,
    canViewFeeTasks,
    canAuditFee,
    canManageFeeAmount,
    canUseSujia
  }
}
```

路由、菜单、按钮和字段显示都调用同一套权限结果，避免出现“菜单能看到但接口无权访问”或“页面能进入但按钮错误显示”的情况。

### 6.3 菜单方案

SJA 页面数量较少，建议使用静态路由表加角色过滤，不接入 `form_frontend` 原有复杂的租户动态菜单。

路由结构建议：

```text
/sja/profile
/sja/fee/submit
/sja/fee/tasks
/sja/fee/standards
/sja/system/users
/sja/system/logs
/sja/sujia
```

登录后根据 `userInfo.roleCodes` 生成可见菜单，同时保留路由守卫，防止直接输入 URL 绕过前端限制。真正的数据权限继续以后端为准。

## 7. 响应式布局设计

`form_frontend` 已在 `ConfigGlobal.vue` 中通过 `useWindowSize` 将 `< 768px` 标记为移动端，并在布局层支持侧栏遮罩和自动折叠。SJA 可以直接利用该能力，但仍需补充以下规则。

### 7.1 断点

| 状态 | 范围 | 行为 |
| --- | ---: | --- |
| Mobile | `< 768px` | 抽屉导航、卡片列表、全屏弹窗、单列表单 |
| Tablet | `768px - 1199px` | 折叠菜单、紧凑筛选、横向表格或卡片 |
| Desktop | `>= 1200px` | 展开菜单、多列表单、完整表格 |

### 7.2 页面容器

复用 `ContentWrap`，并新增 SJA 页面统一类：

```scss
.sja-page {
  min-width: 0;
}

.sja-page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

@media (max-width: 767px) {
  :root {
    --app-content-padding: 12px;
  }

  .sja-page-header {
    align-items: stretch;
    flex-direction: column;
  }
}
```

### 7.3 弹窗

复用模板的 `Dialog` 组件，但设置清晰规则：

- 手机端自动全屏；
- 桌面宽度使用 `min(目标宽度, calc(100vw - 32px))`；
- 表单内容区滚动，底部按钮固定；
- 抽屉手机端宽度为 `100%`；
- 详情弹层关闭按钮、返回按钮和主要操作必须可见。

### 7.4 表格

模板已有的 `Table` 和 `Pagination` 可继续用于桌面端，但不能依赖它们自动生成可用的手机界面。为 SJA 新增 `ResponsiveDataView`：

- `>= 768px` 渲染表格；
- `< 768px` 渲染业务卡片；
- 加载、空状态、分页和选择状态共用；
- 表格操作列和卡片菜单调用相同的业务方法。

## 8. 页面迁移设计

### 8.1 个人管理

- 使用 `ContentWrap` 展示个人资料；
- 桌面两栏、手机单栏；
- 描述列表手机端改为一列；
- 编辑信息和修改密码可以使用选项卡；
- 照片上传继续使用文件上传，照片非必填，可删除；
- 左侧头像继续显示蓝色圆形底和白色姓氏，不渲染照片。

### 8.2 人员管理

- 查询区使用可折叠 `el-form`；
- 手机端只显示关键词输入和“筛选”按钮；
- 桌面表格保留完整字段；
- 手机端使用人员卡片；
- 新增/编辑使用共享 `UserFormDialog.vue`；
- 批量操作使用移动底部选择栏；
- 导出字段弹窗手机端全屏；
- 保留一页显示全部人员选项，但默认仍建议分页，避免低性能手机一次渲染过多 DOM。

### 8.3 稿费提交

- 使用 `WorkInfoForm.vue` 根据作品类型和平台渲染字段；
- 使用 `AuthorInput.vue` 继续支持空格、逗号和换行录入姓名；
- 作者匹配数据库中全部 `is_valid=1` 的人员；
- 桌面双栏，手机单栏；
- 特殊节点仅秘书长和管理员显示；
- 移动端底部固定提交按钮；
- 保留阅读量、字数步长 `1000` 和直接输入能力。

### 8.4 稿费任务、审核与导出

- 桌面端使用 `Table`；
- 手机端使用稿费任务卡片；
- 详情使用响应式 `Dialog` 或全屏抽屉；
- 编辑和提交共用 `WorkInfoForm.vue`；
- 部长不渲染金额和审核操作；
- 秘书长、管理员可以审核、撤回到待审核和选择字段导出；
- 被驳回任务支持软删除，删除后列表和导出均不显示；
- 筛选条件支持部门、平台、月份和状态。

### 8.5 稿费标准、日志、素拓

- 稿费标准：桌面表格、手机规则卡片；
- 操作日志：桌面表格、手机日志时间线或卡片；
- 素拓：桌面左右分栏、手机上下排列，结果支持 Excel 导出。

## 9. uniapp-user 的使用边界

`form_frontend/uniapp-user` 可以作为后续独立移动端，但不建议在本次重构中同时承担全部管理功能。

适合放入 uni-app 的功能：

- 登录；
- 个人资料查看；
- 部长稿费提交；
- 部门任务查看；
- 简化版素拓查询。

不建议第一阶段放入 uni-app 的功能：

- 人员批量管理；
- 复杂稿费审核和字段导出；
- 稿费标准配置；
- 大量日志查看。

这些功能在响应式后台网页中更容易维护，也更适合管理员和秘书长使用。

## 10. 迁移步骤

### 阶段一：建立 SJA 外壳

1. 创建 `SJA_Sys_Form_Vite`。
2. 保留 `form_frontend` 的布局、基础组件和构建系统。
3. 配置 SJA 品牌、颜色、标题和路由前缀。
4. 接入 SJA 登录、令牌、首次改密和退出逻辑。
5. 建立统一角色权限组合函数。

### 阶段二：迁移基础功能

1. 个人管理；
2. 人员管理；
3. 操作日志；
4. 素拓名单。

这一步用于验证请求适配、上传、下载、分页和权限是否正确。

### 阶段三：迁移稿费功能

1. 建立共享作品信息表单；
2. 迁移稿费提交和作者录入；
3. 迁移稿费任务、审核、编辑、详情；
4. 迁移稿费标准和字段选择导出；
5. 验证所有计算仍由后端完成。

### 阶段四：响应式验收和切换

1. 对六种视口执行自动化测试；
2. 按角色执行完整业务回归；
3. 与原前端并行部署到不同路径；
4. 通过验收后再切换默认入口；
5. 原前端保留一段时间用于回退。

## 11. 测试策略

### 11.1 视口测试

至少覆盖：

- `360 × 800`；
- `390 × 844`；
- `430 × 932`；
- `768 × 1024`；
- `1024 × 768`；
- `1440 × 900`。

### 11.2 角色测试

分别使用管理员、秘书长、部长和普通成员账号验证：

- 菜单是否正确；
- 页面是否可进入；
- 金额和特殊节点是否按角色显示；
- 编辑、审核、撤回、软删除和导出操作是否正确；
- 直接输入 URL 时是否仍受权限限制。

### 11.3 自动化工具

建议增加：

- Playwright：页面流程、视口和截图测试；
- `vue-tsc`：类型检查；
- ESLint、Stylelint、Prettier：代码质量；
- 构建体积分析：确认裁剪无关模块后的产物大小。

## 12. 风险与控制

| 风险 | 控制方式 |
| --- | --- |
| 两套登录协议冲突 | 使用独立 `sjaService` 和 `sjaUser`，不复用原租户登录状态 |
| 动态菜单协议不一致 | SJA 使用静态路由加角色过滤 |
| 模板依赖过多、构建较慢 | 先迁移后裁剪，按引用关系删除模块 |
| 业务页面复制后出现逻辑差异 | 抽取共享表单，并为旧版和新版建立相同接口用例 |
| 手机端仍依赖桌面表格 | 明确实现卡片视图，不把横向滚动作为手机主方案 |
| 一次性替换风险过大 | 新旧前端并行运行，分角色验收后切换 |

## 13. 工作量评估

| 模块 | 预计工作量 |
| --- | ---: |
| 工程裁剪、主题和布局 | 2 - 3 天 |
| SJA 请求、登录和权限适配 | 2 - 3 天 |
| 人员、个人资料、日志、素拓迁移 | 3 - 4 天 |
| 稿费提交和共享表单 | 2 - 3 天 |
| 稿费审核、详情、导出、标准 | 3 - 4 天 |
| 多视口和多角色测试 | 2 - 3 天 |
| 合计 | 14 - 20 天 |

如果同时开发独立 uni-app 移动端，需根据实际功能范围额外增加约 5 - 10 天。

## 14. 与现有项目改造方案的比较

| 对比项 | 改造现有 `SJA_Sys_Vite` | 基于 `form_frontend` 重构 |
| --- | --- | --- |
| 上线速度 | 快 | 较慢 |
| 对现有功能影响 | 小 | 中到大 |
| 工程规范 | 需要补充 | 较完整 |
| TypeScript | 无 | 有 |
| 响应式布局基础 | 需要自行实现 | 已有移动端检测和布局基础 |
| 登录权限适配 | 无需迁移 | 必须适配 |
| 长期扩展能力 | 中 | 高 |
| 推荐场景 | 尽快解决当前可用性问题 | 计划长期维护并持续扩展系统 |

## 15. 推荐落地方式

短期先完成现有 `SJA_Sys_Vite` 的导航、弹窗、表单和核心页面响应式改造，立即解决手机和小窗口不可用问题。

如果系统后续还会增加统计、工作流、消息、更多导出和复杂管理功能，再并行启动 `SJA_Sys_Form_Vite`。两者共用现有 Java 后端接口，新版按页面逐步替换旧版。这样可以获得 `form_frontend` 的长期工程能力，同时避免一次性重写导致现有业务中断。
