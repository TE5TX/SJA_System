# SJA 系统安全测试方案

> 适用范围：仅限系统所有者明确授权的 SJA 测试环境。本文以 OWASP ASVS 5.0 和 OWASP WSTG 为基线，目标是防止身份证、手机号、银行卡、宿舍、学号、稿费和账户数据泄露。

参考资料：

- [OWASP Web Security Testing Guide](https://owasp.org/www-project-web-security-testing-guide/latest/)
- [OWASP ASVS 5.0](https://owasp.org/www-project-application-security-verification-standard/)
- [OWASP ZAP Baseline Scan](https://www.zaproxy.org/docs/docker/baseline-scan/)
- [Nmap Reference Guide](https://nmap.org/book/man.html)
- [Cloudflare Rate Limiting](https://developers.cloudflare.com/waf/rate-limiting-rules/)

## 1. 测试原则与边界

### 1.1 禁止直接破坏正式环境

1. 使用与正式环境相同版本的独立测试环境，例如 `https://staging.sja.org.cn`。
2. 测试数据库从备份恢复，但在恢复后立即把身份证、手机号、银行卡、宿舍和姓名替换为合成数据。
3. 测试环境使用独立的 MySQL、Redis、JWT 密钥、Cloudflare Tunnel Token 和测试域名。
4. 不对正式数据库执行 sqlmap、ZAP Active Scan、批量登录、文件模糊测试或并发安全测试。
5. 不使用 `sqlmap --dump`、`--dbs`、`--tables`、`--os-shell`、`--sql-shell`、`--file-read`、`--file-write`。
6. 不执行拒绝服务测试、上传压缩炸弹、恶意软件、反向 Shell、持久化后门或社会工程。
7. 一旦确认能够读取一条不应访问的数据，立即停止，不继续扩大读取范围。

### 1.2 测试授权单

开始前记录以下信息：

| 项目 | 内容 |
|---|---|
| 测试域名 | `https://staging.sja.org.cn` |
| 源站测试 IP | 仅填写隔离测试服务器 IP |
| Kali 源 IP | 填写 Kali 的固定 IP |
| 测试时间 | 开始和结束时间 |
| 授权人 | 系统负责人 |
| 允许工具 | Nmap、ZAP、testssl.sh、Nuclei 安全模板、sqlmap 低风险模式 |
| 禁止行为 | 数据导出、破坏性 SQL、DoS、真实账号撞库 |
| 紧急停止人 | 姓名和联系方式 |

### 1.3 立即停止条件

- 后端或 MySQL 容器重启。
- CPU 持续超过 85%，内存超过 85%。
- 数据库连接池连续超时或出现死锁。
- 正式域名、正式数据库或真实用户受到影响。
- 确认存在任意未授权敏感数据读取。

## 2. 测试环境准备

### 2.1 建立角色测试账号

至少准备以下独立测试账号，所有资料必须为合成数据：

| 编号 | 角色 | 部门 | 用途 |
|---|---|---|---|
| A0 | 管理员 | 主席团 | 全权限基线 |
| A1 | 主席 | 主席团 | 管理权限验证 |
| A2 | 老师 | 主席团 | 人员管理和受限稿费验证 |
| A3 | 秘书长 | 主席团 | 审核、金额、导出验证 |
| M1 | 部长 | 采编部 | 本部门提交和全局元数据验证 |
| M2 | 部长 | 摄影部 | 跨部门提交验证 |
| M3 | 部长 | 视觉部 | 跨部门提交验证 |
| M4 | 部长 | 视频部 | 跨部门提交验证 |
| U1 | 正式部员 | 采编部 | 普通用户基线 |
| U2 | 正式部员 | 摄影部 | IDOR 验证 |
| D1 | 禁用用户 | 任意 | 禁用令牌验证 |
| F1 | 首次登录用户 | 任意 | 强制改密验证 |

每个账号使用不同密码。禁止使用正式用户密码。

### 2.2 备份与观测

测试前：

```bash
docker compose ps
docker compose logs --since 10m backend > security-before-backend.log
docker compose exec -T mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES;"'
```

测试过程中保持三个终端：

```bash
watch -n 1 'docker stats --no-stream'
```

```bash
docker compose logs -f --tail=200 backend
```

```bash
sudo journalctl -u cloudflared -f
```

所有报告放在 `security-test/results/`，所有请求抓包放在 `security-test/captures/`。这两个目录已被 Git 忽略。

## 3. Kali 工具准备

```bash
sudo apt update
sudo apt install -y nmap zaproxy testssl.sh nikto sqlmap jq curl git python3-pip ripgrep
mkdir -p ~/sja-security/{nmap,zap,tls,http,nuclei,sqlmap,evidence}
export TARGET='https://staging.sja.org.cn'
```

建议额外安装：

- Trivy：扫描源码、镜像和配置。
- Gitleaks：扫描 Git 历史中的密钥。
- Nuclei：只运行 `exposure`、`misconfig`、`ssl` 等非侵入模板。
- Burp Suite Community：用于手工重放和修改请求。

验证目标，避免输错正式域名：

```bash
printf '%s\n' "$TARGET"
read -r -p '确认这是隔离测试域名后输入 YES: ' CONFIRM
test "$CONFIRM" = 'YES' || exit 1
```

## 4. 代码、依赖和密钥扫描

### 4.1 Git 历史密钥扫描

在代码仓库根目录执行：

```bash
gitleaks git . --redact \
  --report-format json \
  --report-path security-test/results/gitleaks-git.json

gitleaks dir . --redact \
  --report-format json \
  --report-path security-test/results/gitleaks-files.json
```

验收要求：

- `.env`、SQL 数据快照、JWT 密钥、Cloudflare Token、数据库密码不得出现在当前文件或 Git 历史。
- 发现历史密钥后，即使删除文件也必须轮换密钥；仅重写 Git 历史不能使密钥失效。
- SQL 快照包含大量个人信息，必须加密保存，不得放入 Git、网盘公开链接或聊天记录。

### 4.2 源码规则扫描

```bash
python3 -m pip install --user semgrep
semgrep scan --config p/owasp-top-ten \
  --json --output security-test/results/semgrep.json .
```

人工重点搜索：

```bash
rg -n 'password|secret|token|jdbc:mysql|BEGIN.*PRIVATE KEY' .
rg -n '\$\{|Runtime\.getRuntime|ProcessBuilder|ObjectInputStream|XMLDecoder' SJA_Sys_Java
rg -n 'v-html|innerHTML|eval\(|new Function|document\.write' SJA_Sys_Vite/src
```

### 4.3 依赖和镜像扫描

```bash
trivy fs --scanners vuln,secret,misconfig \
  --format json -o security-test/results/trivy-fs.json .

trivy image --severity HIGH,CRITICAL \
  --format json -o security-test/results/trivy-backend.json sja-system-backend

trivy image --severity HIGH,CRITICAL \
  --format json -o security-test/results/trivy-web.json sja-system-web
```

前端依赖：

```bash
cd SJA_Sys_Vite
npm ci
npm audit --omit=dev --json > ../security-test/results/npm-audit.json
```

后端依赖：

```bash
cd SJA_Sys_Java
./mvnw dependency:tree > ../security-test/results/maven-dependencies.txt
```

验收要求：生产依赖不存在未处理的 Critical/High 漏洞；无法升级的漏洞必须记录是否可达、补偿措施和复查日期。

## 5. 网络暴露面测试

### 5.1 公网域名

只扫描常用 Web 端口，避免扫描 Cloudflare 共享边缘的全部端口：

```bash
nmap -Pn -sT -sV --version-light -T3 -p 80,443 \
  -oA ~/sja-security/nmap/public-web staging.sja.org.cn
```

### 5.2 隔离测试服务器

仅对自己控制的测试服务器 IP 执行：

```bash
nmap -Pn -sT -sV -T3 -p- \
  -oA ~/sja-security/nmap/test-origin TEST_SERVER_IP
```

预期结果：

- 公网只允许 80/443，经 Cloudflare 到达应用。
- MySQL 3306、Redis 6379、后端 8080 不得公网开放。
- SSH 22 只允许可信管理 IP，禁止密码登录和 root 远程登录。
- 若使用 Cloudflare Tunnel，公网 DNS 不应暴露家庭或虚拟机源站 IP。

服务器本地核对：

```bash
sudo ss -lntup
sudo ufw status verbose
docker compose ps
docker network inspect sja-system_internal
```

## 6. TLS、DNS 和 HTTP 安全头

```bash
testssl.sh --severity HIGH --htmlfile ~/sja-security/tls/testssl.html "$TARGET"
curl -sSIk "$TARGET" | tee ~/sja-security/http/headers.txt
dig +short staging.sja.org.cn A
dig +short staging.sja.org.cn AAAA
dig +short staging.sja.org.cn CNAME
```

逐项确认：

- HTTP 自动跳转 HTTPS。
- TLS 1.0/1.1 禁用，只启用安全的 TLS 1.2/1.3 套件。
- 证书域名、有效期和证书链正确。
- 包含 `Strict-Transport-Security`、`X-Content-Type-Options: nosniff`、`X-Frame-Options: DENY`。
- 包含合理的 `Referrer-Policy` 和 `Permissions-Policy`。
- 建议增加并验证 `Content-Security-Policy`；当前 Nginx 配置未显式设置 CSP，应作为重点检查项。
- API 敏感响应建议返回 `Cache-Control: no-store`，避免共享设备缓存个人信息。

## 7. 匿名访问和接口暴露

确认以下地址在生产配置中为 401、403 或 404，不能返回文档和数据：

```bash
for path in \
  /api/v1/user/profile \
  /api/v1/users \
  /api/v1/fee/list \
  /api/v1/system/logs \
  /api/v3/api-docs \
  /api/swagger-ui/index.html \
  /actuator \
  /actuator/env; do
  code=$(curl -sk -o /dev/null -w '%{http_code}' "$TARGET$path")
  printf '%-40s %s\n' "$path" "$code"
done
```

允许匿名访问的范围只能是登录、刷新和验证码相关接口。错误响应不得包含：

- Java 堆栈、SQL、表名、磁盘路径。
- JWT、数据库地址或内部容器主机名。
- 用户是否存在的明确差异。

## 8. CORS 与 CSRF

恶意来源预检：

```bash
curl -isk -X OPTIONS "$TARGET/api/v1/users" \
  -H 'Origin: https://evil.example' \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: Authorization,Content-Type'
```

预期：不得返回 `Access-Control-Allow-Origin: https://evil.example`，也不得返回通配符来源。

合法来源预检：

```bash
curl -isk -X OPTIONS "$TARGET/api/v1/users" \
  -H 'Origin: https://staging.sja.org.cn' \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: Authorization,Content-Type'
```

预期：只回显配置的完整来源，并允许凭证。

CSRF 验证：

1. 登录后确认访问令牌存储在 `sessionStorage`，普通 API 必须使用 `Authorization: Bearer`。
2. 刷新令牌 Cookie 必须具有 `HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth`。
3. 从其他来源提交刷新请求，即使浏览器携带 Cookie，也应被 CORS、SameSite 和 `X-SJA-Refresh: 1` 三层限制阻止。
4. 删除 `X-SJA-Refresh` 后调用刷新接口，必须失败。

## 9. 认证、验证码和账户安全

### 9.1 验证码

使用测试账号验证：

1. 未完成滑块直接登录，应失败。
2. 完成滑块获得令牌，但不使用，等待超过 120 秒后登录，应失败。
3. 同一个验证码令牌第一次登录成功，第二次重放必须失败。
4. 把账号 A 的验证码令牌用于账号 B，不应绕过其他认证控制。
5. 高频生成验证码时应受到 Cloudflare 或应用层速率限制，不能造成 CPU/Redis 资源耗尽。

### 9.2 用户枚举和锁定

只使用专门的测试账号：

1. 分别输入不存在账号、存在账号错误密码、禁用账号。
2. 比较 HTTP 状态、消息长度和平均响应时间，不能明显暴露账号是否存在。
3. 连续输入5次错误密码，再输入正确密码，测试账号应锁定约15分钟。
4. 同一测试 IP 对多个账号尝试错误密码，达到配置阈值后 IP 应受限。
5. 添加伪造的 `X-Forwarded-For` 后重复，不能绕过限流。
6. 在 Cloudflare 安全事件中应能看到来源 IP 和限流动作。

### 9.3 密码策略

验证新增用户、重置密码、首次改密和主动改密：

- 空密码、常见弱密码、与账号相同的密码应被拒绝。
- 新密码不得与旧密码相同。
- 管理员重置后必须强制首次改密。
- 密码不能出现在响应、日志、操作日志或浏览器存储中。
- 修改密码后所有旧访问令牌和刷新令牌必须失效。

## 10. JWT 与刷新令牌

在 ZAP Request Editor 或 Burp Repeater 中执行：

1. 删除 `Authorization`，访问受保护接口，预期 401。
2. 修改 JWT payload 任意一个字符但不重新签名，预期 401。
3. 把 JWT header 改成 `alg=none`，预期 401。
4. 使用错误密钥签名，预期 401。
5. 使用过期访问令牌，预期 401。
6. 把刷新令牌作为 Bearer 访问业务接口，预期 401。
7. 把访问令牌放入刷新 Cookie，预期刷新失败。
8. 正常刷新一次后，重放旧刷新令牌，必须失败。
9. 注销后重放刷新令牌，必须失败。
10. 修改密码、角色、部门或禁用账号后，旧令牌必须立即失效。
11. 用户 A 的有效 JWT 修改为用户 B 的 `userId` 或 `username`，签名无效且必须被拒绝。

检查响应和日志中不得输出完整 JWT。证据只保留令牌前后各4个字符，其余用 `***` 替换。

## 11. 权限与 IDOR 测试

### 11.1 人员信息

对每个角色重复以下操作：

| 操作 | 管理员/主席/老师/秘书长 | 部长/普通部员 |
|---|---|---|
| 查看自己的完整资料 | 允许 | 允许 |
| 修改自己的资料 | 允许 | 允许 |
| `GET /api/v1/users` | 按设计允许 | 应拒绝 |
| `GET /api/v1/users/{其他人ID}` | 按设计允许 | 应拒绝 |
| `PUT /api/v1/users/{其他人ID}` | 按设计允许 | 应拒绝 |
| 修改角色、部门、有效状态 | 按设计允许 | 应拒绝 |
| 导出人员信息 | 仅授权管理角色 | 应拒绝 |

测试方法：

1. 用低权限账号抓取自己的合法请求。
2. 只把路径中的 ID 改成另一个测试用户 ID。
3. 分别测试相邻 ID、不存在 ID、禁用用户 ID、负数和超大数。
4. 不因返回404就认定安全，同时检查数据库和操作日志是否被修改。
5. 列表、详情和导出响应不得出现未选择或无权限查看的身份证、银行卡、手机号、宿舍字段。

### 11.2 稿费权限

重点验证既定业务规则：

- 部长只能向自己的部门提交任务。
- 部长可以查看任务元数据和按规则编辑未审核/已驳回任务，但不能看到任何金额。
- 部长不能审核、重置审核状态或导出含金额文件。
- 秘书长和管理员可以审核、调整金额、重置审核状态和导出。
- 特殊节点只能由秘书长和管理员决定；部长提交 `isSpecialNode=1` 时服务端必须忽略或拒绝。
- 被软删除的驳回任务不得再出现在列表、部门任务和导出中。

对部长账号逐一检查以下响应是否泄露金额：

- `/api/v1/fee/list`
- `/api/v1/fee/detail/{workType}/{id}`
- `/api/v1/fee/calculate`
- 编辑任务响应
- 错误消息和操作日志

金额字段应不存在或为空，不能仅靠前端隐藏。

### 11.3 素拓和日志

- 部长素拓只能返回本部门有效人员；管理角色按设计返回全部有效人员。
- 普通用户不得访问操作日志。
- 日志查询参数不能绕过权限。
- 操作日志不得记录密码、完整 JWT、刷新令牌、银行卡或身份证全文。
- 每次资料修改、权限变更、稿费提交、编辑、审核、删除和导出都应有操作者、请求ID、结果和时间。

## 12. 输入验证、注入和业务字段篡改

### 12.1 手工输入集

对姓名、学院、班级、部门、标题、备注、查询条件分别测试：

```text
'
"
<script>alert(document.domain)</script>
<img src=x onerror=alert(document.domain)>
../../../../etc/passwd
%00
%27 OR %271%27=%271
😀超长文本（超过字段最大长度）
```

预期：

- SQL 特殊字符作为普通文本处理或被格式校验拒绝。
- 页面展示时编码，不能执行 JavaScript。
- 响应不出现 SQL 语句、堆栈和路径。
- 超长、空值、负数、超大数、错误枚举值返回明确的 400。

### 12.2 Mass Assignment

在个人资料请求中额外加入：

```json
{
  "roleCode": 0,
  "roleIds": [14],
  "deptCodes": ["PRESIDIUM"],
  "isValid": 1,
  "mustChangePassword": 0,
  "tokenVersion": 0
}
```

预期：字段被忽略或请求被拒绝，数据库角色、部门和状态不发生改变。

部长提交稿费时额外加入：

```json
{
  "feeAmount": 999999,
  "totalAmount": 999999,
  "isSpecialNode": 1,
  "specialMultiplier": 3,
  "auditStatus": "APPROVED"
}
```

预期：金额由服务端计算，审核状态不能被客户端指定，部长不能设置特殊节点。

### 12.3 sqlmap 低风险检测

先用 ZAP 保存一条测试环境的合法请求为 `captures/request.txt`，然后仅做检测：

```bash
sqlmap -r captures/request.txt \
  --batch --risk=1 --level=2 --threads=1 \
  --technique=BEU --timeout=10 --retries=1 \
  --output-dir=~/sja-security/sqlmap
```

限制：

- 只扫描测试环境和合成数据。
- 不使用数据枚举、数据导出、文件读写、命令执行和堆叠查询功能。
- 一旦确认参数可注入立即停止，保存最小证据并修复。

## 13. 文件上传安全

使用普通测试用户和以下测试集：

| 文件 | 预期 |
|---|---|
| 正常 JPG/PNG | 上传后重新编码，元数据被移除 |
| 文本文件改名 `.jpg` | 拒绝 |
| HTML/SVG 改名 `.png` | 拒绝 |
| GIF | 解码后重新编码为安全 JPG/PNG |
| 超过5MB | 拒绝 |
| 任一边超过6000像素 | 拒绝 |
| 总像素超过2000万 | 拒绝 |
| 双扩展名 `a.jpg.php` | 拒绝 |
| 空文件 | 拒绝 |

命令示例：

```bash
curl -isk -X POST "$TARGET/api/v1/upload/avatar" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F 'file=@not-an-image.jpg;type=image/jpeg'
```

继续验证：

1. 上传文件名由服务器生成 UUID，不使用原始文件名。
2. GET 请求尝试 `../`、URL 编码路径穿越和任意文件名，必须404/400。
3. 未绑定到用户的头像不能通过猜测 URL 读取。
4. 用户 A 不能把用户 B 的头像路径绑定给自己。
5. 更换或删除头像后，旧文件在事务提交后从磁盘清理。
6. 上传和读取响应具有正确 `Content-Type` 和 `X-Content-Type-Options: nosniff`。

不要创建或上传图片炸弹；尺寸限制只需使用可控的小型测试图片验证边界。

## 14. Excel 导出安全

在测试用户资料或稿费标题中分别保存以下前缀：

```text
=1+1
+1+1
-1+1
@SUM(1,1)
```

由授权账号导出 XLSX，在离线虚拟机中打开：

- 单元格必须作为普通文本显示，不能作为公式执行。
- 导出文件只能由拥有对应权限的账号获取。
- 部长导出接口必须403，不能通过修改字段列表获取金额。
- 文件名不得包含路径字符或响应拆分字符。
- 导出响应应设置正确 MIME、下载头和 `Cache-Control: no-store`。

## 15. ZAP 动态扫描

由于系统有滑块验证码，采用浏览器手工登录并代理抓包：

1. Kali 启动 `zaproxy`。
2. 使用 ZAP 内置浏览器访问测试域名，不要使用日常浏览器配置。
3. 把测试域名加入 Context 和 Scope，明确排除正式域名。
4. 手工完成滑块并登录测试账号。
5. 正常访问个人资料、人员、稿费、日志、素拓、上传和导出页面，让 ZAP 建立站点树。
6. 先运行 Passive Scan，修复高危告警后再进行 Active Scan。
7. Active Scan 线程设置为2，单请求延迟至少200ms。
8. 第一轮排除登录、登出、刷新、删除、批量修改、导出和上传端点。
9. 第二轮对这些端点逐个手工测试，不进行全自动模糊扫描。
10. 分别用管理员、秘书长、部长和普通用户建立独立 Session，不能混用令牌。
11. 导出 HTML/JSON 报告到 `security-test/results/`。

可先进行低影响基线扫描：

```bash
docker run --rm -v "$PWD:/zap/wrk/:rw" \
  -t ghcr.io/zaproxy/zaproxy:stable zap-baseline.py \
  -t "$TARGET" -r zap-baseline.html -J zap-baseline.json
```

## 16. Nuclei 与 Nikto 辅助扫描

仅运行非侵入模板并限制速率：

```bash
nuclei -u "$TARGET" \
  -tags exposure,misconfig,ssl \
  -exclude-tags dos,fuzz,intrusive \
  -rl 5 -c 2 \
  -jsonl-export ~/sja-security/nuclei/findings.jsonl
```

```bash
nikto -h "$TARGET" -maxtime 20m \
  -Format htm -output ~/sja-security/http/nikto.html
```

所有自动化结果必须手工复现。Cloudflare 返回的边缘服务特征不一定是源站漏洞。

## 17. Cloudflare 和源站保护

### 17.1 Tunnel

- Tunnel Token 不得写入 Git、截图、聊天记录或命令历史。
- `/etc/systemd/system/cloudflared.service` 或环境文件只允许 root 读取。
- 不在路由器开放80/443到虚拟机；Tunnel 使用出站连接。
- 若 Token 曾泄露，立即轮换并删除旧连接器。
- Cloudflare DNS 记录不得同时保留指向真实公网 IP 的灰云 A/AAAA 记录。

### 17.2 WAF 与限流建议测试

在测试域名先以 Log/Managed Challenge 模式验证：

- `/api/v1/auth/login`：按 IP 限制短时间高频尝试。
- `/api/v1/auth/captcha/generate`：限制自动化生成。
- `/api/v1/auth/refresh`：限制异常刷新频率。
- `/api/v1/users/export`、`/api/v1/fee/export`：每账号和每 IP 限制频率。
- `/api/v1/upload/avatar`：限制上传频率和总请求体。
- 对明显扫描器和异常国家/ASN先记录，再决定挑战或阻断。

验证 Cloudflare 安全事件中能看到规则命中、来源IP、URI和动作，但不得记录 Authorization 和 Cookie 明文。

## 18. Docker、主机和数据库

### 18.1 容器隔离

```bash
docker compose config
docker inspect sja-system-backend-1 --format '{{json .HostConfig}}' | jq
docker inspect sja-system-mysql-1 --format '{{json .HostConfig}}' | jq
```

确认：

- 未使用 `privileged: true`。
- 未挂载 `/var/run/docker.sock`、宿主机根目录或不必要设备。
- MySQL、Redis、backend 只在内部 Docker 网络。
- Web 仅绑定 `127.0.0.1:80` 时由本机 Tunnel 访问；不要误绑 `0.0.0.0`。
- 容器镜像固定版本并定期重建，不长期依赖不可追踪的 `latest`。
- 上传卷和数据库卷权限最小化。

### 18.2 MySQL 最小权限

```bash
docker compose exec mysql sh -lc \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW GRANTS FOR '\''sja'\''@'\''%'\'';"'
```

应用账号不应具有全局 `SUPER`、`FILE`、`CREATE USER`、`GRANT OPTION`、`SHUTDOWN` 权限。生产运行通常只需业务库的 SELECT、INSERT、UPDATE、DELETE；数据库迁移使用单独账号。

### 18.3 Redis

- 6379 不对宿主机和公网映射。
- 使用独立随机密码。
- 验证未认证连接被拒绝。
- Redis 日志和备份不得包含刷新令牌明文或验证码答案。

### 18.4 Ubuntu 与 SSH

```bash
sudo sshd -T | grep -E 'permitrootlogin|passwordauthentication|pubkeyauthentication'
sudo ufw status verbose
sudo apt update
apt list --upgradable
```

目标状态：`PermitRootLogin no`、`PasswordAuthentication no`、仅密钥登录、UFW默认拒绝入站、定期安装安全更新。

## 19. 数据库泄露专项验证

按以下顺序检查：

1. 匿名请求不能读取任何人员、稿费、日志、头像内容。
2. 普通用户响应只包含完成当前功能所需字段。
3. 登录响应不包含身份证、银行卡、宿舍和手机号。
4. 浏览器 `localStorage`、`sessionStorage`、IndexedDB 和 Cache Storage 不保存完整人员对象。
5. 部长不能从任何接口、错误消息、导出文件和日志读取金额。
6. 列表接口不能通过 `pageSize`、字段名、排序字段或筛选条件绕过数据范围。
7. 备份文件、旧 SQL、`.env`、日志和上传目录不能从 Web 根目录访问。
8. GitHub 仓库和 Git 历史不存在 SQL 数据、密钥和真实 `.env`。
9. MySQL 和 Redis 不能从公网连接。
10. Cloudflare 源站 IP 不暴露，Tunnel Token 已安全保存。
11. 操作日志记录所有敏感导出、权限变更、禁用启用和审核行为。
12. 数据库备份加密，并实际完成一次恢复演练。

## 20. 受控资源消耗测试

这不是 DoS 测试，只验证限制是否生效：

- 连续5次上传接近5MB的合法图片，观察CPU、堆内存和临时文件清理。
- 连续5次执行人员或稿费导出，确认存在频率限制和稳定内存上限。
- 用 k6 逐级执行 5、10、20 个并发安全相关请求，不直接跳到高并发。
- 当 CPU/内存超过停止阈值立即终止。

禁止对公网正式域名执行无限循环、无速率 Nuclei、ZAP 高线程扫描或大文件并发上传。

## 21. 日志、告警与事件响应

验证以下事件可以通过请求 ID 追踪：

- 登录失败、账号锁定、验证码重放。
- 连续401/403、权限越权尝试。
- 人员资料、角色、部门、状态和密码变化。
- 稿费提交、编辑、审核、重置、软删除和导出。
- 文件上传、替换和删除。
- Cloudflare WAF、限流和异常来源。

日志要求：

- 记录时间、操作者、模块、动作、目标ID、结果、客户端IP、请求ID。
- 不记录密码、JWT、Cookie、完整身份证和银行卡。
- 失败日志写入失败时应告警，不能悄悄吞掉。
- 日志定期归档并限制管理员之外的访问。

事件响应预案：

1. 立即暂停 Tunnel 或阻断异常来源。
2. 轮换 Cloudflare Token、JWT、数据库和 Redis 密钥。
3. 撤销全部刷新令牌并递增用户 tokenVersion。
4. 保存 Cloudflare、Nginx、后端、MySQL 和操作日志证据。
5. 核对人员导出、稿费导出、角色变更和异常查询记录。
6. 修复后在隔离环境复测，再恢复公网服务。

## 22. 漏洞记录和优先级

每个发现使用以下格式：

```markdown
## [SJA-SEC-001] 漏洞标题

- 严重程度：Critical / High / Medium / Low / Info
- 影响资产：接口、角色、数据字段
- 前置条件：需要的账号和权限
- 最小复现：不包含真实隐私和完整令牌
- 实际结果：
- 预期结果：
- 影响：机密性 / 完整性 / 可用性
- 证据：脱敏请求、响应、截图、请求ID
- 修复建议：
- 修复提交：
- 复测结果：
```

分级原则：

- **Critical**：无需高权限即可批量读取数据库、执行命令、接管管理员或写入任意文件。
- **High**：读取他人身份证/银行卡、部长获取金额、普通用户提升权限、刷新令牌可重放。
- **Medium**：有限 IDOR、存储型 XSS、弱限流、敏感响应可缓存、上传校验绕过但无执行。
- **Low**：缺少安全头、版本泄露、低影响配置错误。

## 23. 推荐执行顺序

1. 建立隔离测试环境并脱敏数据库。
2. 轮换之前可能泄露的全部密钥。
3. Gitleaks、Semgrep、Trivy、npm audit。
4. Nmap、TLS、HTTP头、匿名接口和源站暴露。
5. 验证码、登录限流、JWT、刷新和注销。
6. 按角色矩阵手工验证人员、稿费、素拓、日志和导出权限。
7. 输入验证、Mass Assignment、文件上传和 Excel 公式注入。
8. ZAP Passive Scan。
9. ZAP 低线程 Active Scan、Nuclei 非侵入模板、sqlmap 低风险检测。
10. Cloudflare WAF、限流、Docker、主机、MySQL 和 Redis 检查。
11. 修复 Critical/High 后立即复测。
12. 发布前重新执行全套检查并归档报告。

## 24. 上线门槛

满足以下条件后才建议正式开放公网：

- Critical 和 High 数量为0。
- Medium 均有修复计划、负责人和截止日期。
- 所有角色权限矩阵通过，服务端无金额和人员隐私越权。
- 旧 JWT、Cloudflare、数据库和 Redis 密钥完成轮换。
- MySQL/Redis/8080 未暴露公网，源站 IP 不可直接访问。
- Swagger、Actuator 和调试错误在生产关闭。
- 上传、导出、登录和验证码具备限流。
- 备份已加密且恢复演练成功。
- 操作日志和 Cloudflare 告警可以追踪敏感操作。
- 安全扫描报告、修复记录和复测证据已归档。

## 25. 周期安排

- 每次提交：Gitleaks、Semgrep、前后端单元测试。
- 每周：依赖和镜像漏洞扫描。
- 每月：权限矩阵、Cloudflare 规则、日志和备份抽查。
- 每次上线前：ZAP Baseline、TLS、Nmap 和关键业务手工测试。
- 每季度：完整隔离环境安全测试和恢复演练。
- 发生密钥泄露、权限模型变化、文件上传变化后：立即专项复测。
