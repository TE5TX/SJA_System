# SJA System 测试完成后修复清单

> 本清单用于安全测试期间持续登记问题。为避免改变测试基线，除非出现 Critical/High、数据泄露、越权或系统不可用等必须立即处理的情况，其余项目统一在全部测试完成后修复并回归验证。

## 状态说明

- `待修复`：已有证据支持，测试完成后实施修复。
- `待复核`：扫描器命中，但必须人工确认后才能定性。
- `规划项`：当前部署能力缺失，需要在上线方案中完成。
- `暂不处理`：信息项、不适用项或可能破坏业务兼容性的可选加固。
- `已排除`：已人工确认不是安全漏洞。

## 待修复与规划项

| 编号 | 状态 | 初步等级 | 问题 | 证据与判断 | 测试完成后的处理 | 回归验证 |
|---|---|---|---|---|---|---|
| SJA-REM-001 | 规划项 | Medium | 当前仅提供HTTP，未部署HTTPS/HSTS | Nmap仅发现80开放、443为filtered；Nikto和Nuclei均提示缺少HSTS。HSTS必须在HTTPS正确部署后启用 | 为正式环境配置可信HTTPS证书和HTTP到HTTPS跳转；确认所有资源均可通过HTTPS加载后，再添加HSTS | `curl -sSI https://目标/`；确认有效证书、跳转正确、无混合内容且存在预期的`Strict-Transport-Security` |
| SJA-REM-002 | 待修复 | Medium | Nginx 1.27.5版本过旧 | WhatWeb、Nmap、Nikto、Nuclei和ZAP均识别到`nginx/1.27.5`；Nuclei命中`nginx-eol:version` | 将前端运行镜像升级到受维护的稳定版本；阅读跨版本变更和安全公告后重新构建，不直接复用旧容器 | `docker compose build --pull frontend`；检查镜像版本、健康状态、前端/API功能，并重新运行Nuclei/ZAP基线 |
| SJA-REM-003 | 待修复 | Low | HTTP响应暴露精确Nginx版本 | ZAP告警10036；响应头为`Server: nginx/1.27.5` | 在Nginx配置中设置`server_tokens off;`；注意这只能降低信息披露，不能代替版本升级 | `curl -sSI "$BASE_URL/"`，确认不再返回精确版本号 |
| SJA-REM-004 | 待修复 | Low/Medium | 未设置Content-Security-Policy | Nikto、Nuclei与ZAP告警10038一致；首页当前无CSP | 根据Vite模块、验证码Base64/Blob资源、API连接和字体来源设计策略；先以`Content-Security-Policy-Report-Only`验证，再逐步强制执行 | 检查浏览器控制台和CSP报告；验证登录、验证码、头像、导出等功能；重新运行ZAP/Nuclei |
| SJA-REM-005 | 待修复 | Low | 静态资源未继承公共安全响应头 | ZAP告警10021、10063。`SJA_Sys_Vite/nginx.conf`的静态资源`location`单独添加`Cache-Control`，导致服务器级`add_header`未继承；CSS/JS/SVG缺少`X-Content-Type-Options`，部分JS缺少`Permissions-Policy` | 抽取公共安全头配置或在静态资源`location`中显式重复；保留静态资源缓存策略 | 分别对首页、JS、CSS、SVG执行`curl -sSI`；确认`nosniff`等预期响应头一致，随后重跑ZAP基线 |
| SJA-REM-006 | 待修复 | Low | 页面依赖外部Google Fonts且未提供SRI | Nuclei命中`missing-sri`和`google-font-api`；ZAP告警90003；`SJA_Sys_Vite/index.html`直接加载`fonts.googleapis.com` | 优先将所需字体本地化并删除外部依赖；若继续使用外部资源，评估SRI、CORS、隐私、可用性和CSP白名单 | 断开外网后验证页面字体与布局；检查HTML无非预期第三方资源；重新运行Nuclei/ZAP |
| SJA-REM-007 | 待复核 | 待定 | 前端生产JS被ZAP识别为包含可疑注释 | ZAP告警10027，命中`/assets/index-CRhntCok.js`；当前仅有URL，尚未取得告警`evidence`，不能直接定性为泄密 | 从ZAP JSON提取`evidence`；若只是许可证、第三方库或验证码HTML注释则排除；若包含密码、密钥、内部地址或调试信息则清除并轮换相应凭据 | 记录人工复核内容；重新构建并检查生产JS；重跑ZAP规则10027 |
| SJA-REM-008 | 待复核 | Info/Low | Nginx接受任意Host头 | 人工请求使用`Host: evil.example`仍返回200及验证码响应；当前未发现Host反射、密码重置链接污染或缓存投毒，尚未证明可利用 | 结合代理拓扑验证Host是否参与绝对链接、重置链接、缓存键或审计；若无业务需要，将`server_name`设为允许值并为未知Host配置拒绝的default server | 使用合法Host和恶意Host分别测试首页、登录、验证码、重置链接及API；未知Host应按设计拒绝 |

## 暂不处理或需要按业务决定的项目

| 编号 | 状态 | 项目 | 当前判断 | 后续决策条件 |
|---|---|---|---|---|
| SJA-OBS-001 | 暂不处理 | 缺少COEP/COOP/CORP及`X-Permitted-Cross-Domain-Policies` | Nuclei/ZAP信息级提示。普通管理系统通常不依赖跨源隔离；盲目添加可能阻断字体、图片或其他跨源资源 | 只有在需要`SharedArrayBuffer`、严格跨源隔离或组织基线明确要求时再设计并测试 |
| SJA-OBS-002 | 已排除 | `/css/`和`/js`被Nikto标记为interesting | 人工检查均返回与首页完全相同的SPA入口HTML，`cmp`结果为0，且无目录索引 | 无需修复；保留证据即可 |
| SJA-OBS-003 | 已排除 | ZAP提示公开静态内容可缓存 | 告警10049涉及首页、图标、CSS等公开资源；带哈希静态资源设置长期缓存属于预期设计 | 若后续发现用户数据、令牌或认证API可被共享缓存，再升级为真实问题 |
| SJA-OBS-004 | 已排除 | `robots.txt`和`sitemap.xml`上的重复告警 | 项目中不存在这两个文件；Nginx SPA回退将请求返回为`index.html`，因此CSP/SRI/缓存告警是首页的重复结果 | 可按SEO或语义需要新增真实文件，但不作为安全漏洞修复 |
| SJA-OBS-005 | 已排除 | ZAP `Modern Web Application`与Nuclei技术识别/WAF识别 | 仅说明网站是现代SPA、使用Nginx或表现出通用Nginx拦截特征，不代表存在漏洞或已部署专业WAF | 无需修复 |

## 本轮证据位置

- Kali Nuclei结果：`$EVIDENCE/02-http/nuclei-http.jsonl`
- Kali ZAP Baseline报告目录：`$EVIDENCE/03-zap/baseline/`
- ZAP报告文件：`zap-baseline.html`、`zap-baseline.json`、`zap-baseline.md`
- Nginx配置：`SJA_Sys_Vite/nginx.conf`
- 前端入口：`SJA_Sys_Vite/index.html`

## 测试完成后的统一处理流程

1. 冻结测试结果并备份全部原始证据。
2. 对所有`待复核`项目完成单请求人工验证并确定等级。
3. 按Critical、High、Medium、Low、Info顺序安排修复。
4. 修改配置或代码并重新构建镜像，不直接修改运行中容器。
5. 执行功能回归、权限矩阵回归和对应安全工具复扫。
6. 在本清单更新状态、修复Commit、验证日期和复测证据。
7. 将最终确认的问题同步到安全测试报告，保留误报和接受风险的理由。

## 变更记录

| 日期 | 内容 |
|---|---|
| 2026-08-20 | 建立清单；登记Nikto、Nuclei、ZAP Baseline及人工复核阶段已出现的问题；当前仅登记，未实施修复 |
