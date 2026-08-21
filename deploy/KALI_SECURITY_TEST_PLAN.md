# SJA System Kali Linux 完整安全测试方案

> 适用目标：SJA System 的隔离测试环境  
> 当前测试地址：`http://192.168.255.128/`  
> 技术栈：Vue/Nginx、Spring Boot/JWT、MySQL 8.4、Redis 7.4、Docker Compose  
> 基准：OWASP ASVS 5.0 Level 2、OWASP WSTG、OWASP API Security Top 10  
> 文档日期：2026-08-20

## 1. 目的和边界

本方案用于验证系统的机密性、完整性、可用性，以及身份认证、授权、输入校验、文件上传、敏感数据、日志和部署配置是否符合上线要求。

安全测试只能证明“已测试范围内未发现或已修复已知问题”，不能证明系统永远不会被攻击。自动化扫描结果必须人工复核，尤其是本系统的角色、部门、稿费状态流转等业务授权逻辑。

### 1.1 明确授权范围

本方案只允许测试：

- Kali 测试机；
- Ubuntu/Docker 测试虚拟机 `192.168.255.128`；
- SJA System 测试站点 `http://192.168.255.128/`；
- 为测试专门创建的虚构账号、虚构用户、虚构稿费任务和虚构头像；
- 经明确授权的项目源代码副本。

禁止：

- 扫描同网段的其他主机；
- 对学校、云服务器或任何正式环境直接运行主动扫描；
- 使用真实身份证、银行卡、手机号或学生照片；
- 使用密码字典批量撞库；
- 使用 `sqlmap --os-shell`、文件写入、提权、持久化、清日志等攻击选项；
- 破坏数据库、删除容器卷、上传压缩炸弹或无限制进行拒绝服务测试；
- 将访问令牌、Cookie、`.env`、数据库备份和扫描原始流量提交到 Git。

### 1.2 停止条件

满足任一条件立即停止主动测试，保留日志并恢复环境：

- Ubuntu 主机失去响应或 VMware 明显卡顿；
- 后端 CPU 在 2 核虚拟机上持续超过 `190%` 达 60 秒；
- 任一容器内存超过限制的 90%；
- 磁盘可用空间低于 20%；
- 5xx 错误率超过 1%，或正常测试账号无法登录；
- MySQL/Redis 重启、数据异常，或响应时间连续超过 10 秒；
- 发现可能影响范围超出 `192.168.255.128` 的配置错误。

## 2. 当前系统安全基线

对当前代码和 Compose 配置的只读核对显示：

- 仅 Nginx 的 `80/tcp` 映射到虚拟机地址；Spring Boot、MySQL 和 Redis 位于 Compose 内部网络；
- JWT Access Token 默认 15 分钟，Refresh Token 默认 7 天；
- Refresh Token 使用 `HttpOnly`、`SameSite=Strict` Cookie，HTTPS 模式应启用 `Secure`；
- 已实现账号/IP登录节流和15分钟短期锁定；
- 已实现验证码一次性登录凭证；
- 头像读取已要求认证，并返回 `no-store` 和 `nosniff`；
- CORS 使用显式 Origin 白名单；
- API 文档在生产配置中关闭；
- 批量稿费审核和稿费导出均限制为最多200条；
- 用户列表、日志列表的分页大小限制为最多200；
- `SujiaResolveDTO.names` 当前未发现集合数量上限，仍需重点验证；
- 当前测试站点使用 HTTP，不能满足公网生产环境的传输安全要求；
- Nginx 已设置 `nosniff`、`X-Frame-Options`、Referrer Policy 和 Permissions Policy，但尚需验证 CSP 和 HTTPS/HSTS。

## 3. 测试角色和测试数据

至少准备以下虚构账号。每个账号使用不同密码，不得使用真实人员账号。

| 编号 | 角色 | 部门 | 用途 |
|---|---|---|---|
| A0 | 管理员 | 全部 | 最高权限、用户管理、日志验证 |
| A1 | 主席/老师 | 全部或管理范围 | 敏感金额隐藏、业务范围验证 |
| A2 | 秘书长 | 管理范围 | 用户管理、导出、审核验证 |
| A3 | 采编部长 | EDITORIAL | 同部门授权与跨部门越权 |
| A4 | 摄影部长 | PHOTOGRAPHY | 同部门授权与跨部门越权 |
| A5 | 普通部员 | EDITORIAL | 最小权限、个人资料和稿费提交 |
| A6 | 普通部员 | PHOTOGRAPHY | 跨用户、跨部门对象访问 |
| A7 | 首次登录用户 | 任意 | 强制改密绕过测试 |
| A8 | 禁用用户 | 任意 | 旧令牌撤销测试 |

另外创建：

- 两个属于不同用户的虚构稿费任务；
- 一条已提交、一条已审核、一条已驳回的虚构任务；
- 两张无隐私内容的测试头像；
- 可随时恢复的测试数据库快照。

## 4. 测试环境准备

### 4.1 Kali 网络确认

在 Kali 打开终端：

```bash
export TARGET_IP='192.168.255.128'
export BASE_URL="http://${TARGET_IP}"

ip -br addr
ip route
ping -c 3 "$TARGET_IP"
curl -sS -o /dev/null -w 'HTTP %{http_code}  time=%{time_total}s\n' "$BASE_URL/"
```

预期：能访问目标，首页返回 `200`。不要因为网关不响应 Ping 就判断网络失败，以目标站点实际 HTTP 连通性为准。

### 4.2 安装 Kali 工具

```bash
sudo apt update
sudo apt install -y \
  curl jq nmap whatweb nikto zaproxy sqlmap ffuf \
  testssl.sh seclists docker.io unzip imagemagick

sudo systemctl enable --now docker
```

可选安装 Nuclei。先检查 Kali 仓库是否提供：

```bash
sudo apt install -y nuclei
nuclei -version
nuclei -update-templates
```

如果某个包在当前 Kali 版本中改名，使用 `apt search 工具名` 确认，不要从不明脚本直接下载安装。

### 4.3 建立证据目录

```bash
export TEST_ID="sja-$(date +%Y%m%d-%H%M%S)"
export EVIDENCE="$HOME/security-tests/$TEST_ID"

mkdir -p "$EVIDENCE"/{00-scope,01-network,02-http,03-zap,04-auth,05-authz,06-input,07-upload,08-data,09-load,10-source,11-report}

date -Is | tee "$EVIDENCE/00-scope/start-time.txt"
printf 'target=%s\nbase_url=%s\ntester_ip=%s\n' \
  "$TARGET_IP" "$BASE_URL" "$(hostname -I | awk '{print $1}')" \
  | tee "$EVIDENCE/00-scope/scope.txt"
```

证据文件中如果包含 Token、Cookie、身份证、银行卡或密码，应脱敏后再分享。完成测试后安全删除未脱敏的临时请求文件。

### 4.4 Ubuntu端备份和监控

在 Ubuntu 目标机执行备份；以下命令只用于测试环境：

```bash
cd ~/SJA_System
mkdir -p ~/sja-security-backup

docker compose exec -T mysql sh -lc \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers sja_system_db' \
  | gzip > ~/sja-security-backup/sja-before-security-test.sql.gz

docker compose ps
df -h
free -h
```

主动测试期间另外打开两个 Ubuntu 终端。

终端一：

```bash
docker stats
```

终端二：

```bash
cd ~/SJA_System
docker compose logs -f --tail=100 backend web mysql redis
```

## 5. 第一阶段：网络和攻击面

### 5.1 端口发现

先执行低速、明确范围的扫描：

```bash
sudo nmap -Pn -sS -sV -T3 \
  -p 22,80,443,8080,3306,33060,6379 \
  -oA "$EVIDENCE/01-network/selected-ports" \
  "$TARGET_IP"
```

再执行全TCP端口确认，限制发送速率：

```bash
sudo nmap -Pn -sS -T3 --max-rate 300 -p- \
  -oA "$EVIDENCE/01-network/all-tcp" \
  "$TARGET_IP"
```

验收：

- `80/tcp` 可以开放；如果已启用 HTTPS，`443/tcp` 可以开放；
- `8080`、`3306`、`33060`、`6379` 不得从 Kali 直接访问；
- `22/tcp` 只应在受控管理网络开放，并使用密钥认证；
- 不应出现未知服务。

不要使用 `-A`、UDP全端口或高 `--min-rate` 开始扫描。

### 5.2 服务和页面指纹

```bash
whatweb -a 3 --log-verbose="$EVIDENCE/01-network/whatweb.txt" "$BASE_URL/"

curl -sS -D "$EVIDENCE/02-http/home-headers.txt" \
  -o "$EVIDENCE/02-http/home.html" \
  "$BASE_URL/"
```

检查响应中是否泄露精确框架版本、内部主机名、路径、调试信息或源码映射文件。

### 5.3 HTTP方法

```bash
for method in GET HEAD OPTIONS TRACE PUT DELETE PATCH; do
  printf '\n===== %s =====\n' "$method"
  curl -sS -i -X "$method" "$BASE_URL/" | sed -n '1,20p'
done | tee "$EVIDENCE/02-http/methods.txt"
```

验收：

- `TRACE` 不得启用；
- 未定义的 `PUT/DELETE/PATCH` 不应返回成功；
- 不能通过方法覆盖头绕过授权。

### 5.4 安全响应头

```bash
curl -sSI "$BASE_URL/" | tee "$EVIDENCE/02-http/security-headers-home.txt"
curl -sSI "$BASE_URL/api/v1/user/profile" | tee "$EVIDENCE/02-http/security-headers-api.txt"
```

检查：

- `X-Content-Type-Options: nosniff`；
- `X-Frame-Options: DENY` 或等效 CSP；
- `Referrer-Policy`；
- `Permissions-Policy`；
- `Content-Security-Policy`；
- HTTPS环境中的 `Strict-Transport-Security`。

当前HTTP测试环境没有HSTS是正常现象，但公网验收必须使用HTTPS。

### 5.5 CORS

恶意来源预检：

```bash
curl -sS -i -X OPTIONS "$BASE_URL/api/v1/user/profile" \
  -H 'Origin: https://evil.example' \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: Authorization,Content-Type' \
  | tee "$EVIDENCE/02-http/cors-evil.txt"
```

合法来源预检：

```bash
curl -sS -i -X OPTIONS "$BASE_URL/api/v1/user/profile" \
  -H "Origin: $BASE_URL" \
  -H 'Access-Control-Request-Method: GET' \
  -H 'Access-Control-Request-Headers: Authorization,Content-Type' \
  | tee "$EVIDENCE/02-http/cors-valid.txt"
```

验收：恶意来源不得获得 `Access-Control-Allow-Origin`；合法来源可以返回精确来源，但不能返回 `*` 与凭据组合。

### 5.6 Host和代理头

```bash
curl -sS -i "$BASE_URL/api/v1/auth/captcha/generate" \
  -X POST -H 'Host: evil.example' \
  | tee "$EVIDENCE/02-http/host-header.txt"

curl -sS -i "$BASE_URL/api/v1/auth/captcha/generate" \
  -X POST -H 'X-Forwarded-For: 127.0.0.1' \
  | tee "$EVIDENCE/02-http/x-forwarded-for.txt"
```

验收：响应中不能生成或反射恶意Host链接；外部客户端不能通过伪造 `X-Forwarded-For` 绕过登录限流。

### 5.7 TLS（启用HTTPS后执行）

```bash
testssl --warnings batch --color 0 \
  --logfile "$EVIDENCE/02-http/testssl.log" \
  "https://$TARGET_IP/"
```

验收：

- TLS 1.0/1.1 禁用；
- 证书域名、链和有效期正确；
- 不支持明显弱密码套件；
- HTTP跳转HTTPS；
- HSTS只在HTTPS验证无误后开启。

## 6. 第二阶段：低风险自动化扫描

### 6.1 Nikto

```bash
nikto -h "$BASE_URL" -maxtime 10m -Pause 0.2 \
  -output "$EVIDENCE/02-http/nikto.txt" -Format txt
```

Nikto结果只作为线索。对每个告警使用 `curl` 重现后才能登记漏洞。

### 6.2 Nuclei

先确认公共模板可用。若 `nuclei -update-templates` 没有下载模板，可从ProjectDiscovery官方仓库克隆：

```bash
git clone --depth 1 \
  https://github.com/projectdiscovery/nuclei-templates.git \
  "$HOME/nuclei-templates-official"

git -C "$HOME/nuclei-templates-official" remote get-url origin
git -C "$HOME/nuclei-templates-official" status --porcelain

find "$HOME/nuclei-templates-official/http" \
  -type f \( -name '*.yaml' -o -name '*.yml' \) | wc -l
```

`remote get-url` 必须指向ProjectDiscovery官方仓库，`status --porcelain` 应无输出。通过Git克隆的模板可能不带Nuclei发布包签名，因此不要使用 `-disable-unsigned-templates`，否则所有模板可能被跳过。未显式提供 `-code`、`-headless` 或 `-fuzz` 时，相应类型不会执行；本方案进一步只加载 `http` 目录并排除DoS、Fuzz和爆破标签。

```bash
nuclei \
  -u "$BASE_URL" \
  -t "$HOME/nuclei-templates-official/http" \
  -pt http \
  -severity info,low,medium,high,critical \
  -exclude-tags dos,fuzz,bruteforce \
  -rl 10 -c 2 -timeout 10 -retries 1 -stats \
  -jsonl-export "$EVIDENCE/02-http/nuclei.jsonl"
```

正确运行时应显示已加载非零数量的模板，扫描不应在毫秒级结束。若出现 `Skipping ... unsigned templates` 后又提示 `no templates provided`，检查并移除 `-disable-unsigned-templates`。禁止在本阶段启用 code、fuzz、headless、大规模目录或拒绝服务类模板。

### 6.3 ZAP Baseline被动扫描

```bash
mkdir -p "$EVIDENCE/03-zap/baseline"

docker run --rm --network host \
  -v "$EVIDENCE/03-zap/baseline:/zap/wrk:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-baseline.py -t "$BASE_URL" \
  -m 3 \
  -r zap-baseline.html \
  -J zap-baseline.json \
  -w zap-baseline.md
```

Baseline主要进行爬取和被动分析，不等于完整渗透测试。保存HTML、JSON和Markdown报告。

## 7. 第三阶段：通过ZAP获取测试会话

由于登录包含滑块验证码，不应编写绕过验证码的脚本。使用ZAP代理和浏览器人工完成验证码。

### 7.1 启动ZAP和配置浏览器

```bash
zaproxy
```

在 Kali Firefox 中：

1. 设置 HTTP/HTTPS 代理为 `127.0.0.1:8080`；
2. 打开 `http://192.168.255.128/`；
3. 使用测试账号登录并人工完成滑块；
4. 在 ZAP History 中找到 `/api/v1/auth/login`；
5. 检查响应JSON中的 Access Token 和 `Set-Cookie`；
6. 分别用管理员、部长、普通用户重复，给会话加清晰标签；
7. 不要把含令牌的ZAP会话上传到公共位置。

### 7.2 在终端安全录入Token

```bash
read -rsp 'Admin Access Token: ' TOKEN_ADMIN; echo
read -rsp 'Member A Access Token: ' TOKEN_MEMBER_A; echo
read -rsp 'Member B Access Token: ' TOKEN_MEMBER_B; echo
export TOKEN_ADMIN TOKEN_MEMBER_A TOKEN_MEMBER_B
```

测试完成后清理：

```bash
unset TOKEN_ADMIN TOKEN_MEMBER_A TOKEN_MEMBER_B
```

不要把Token直接写进Markdown、Shell脚本或命令历史。

## 8. 第四阶段：未认证访问和身份认证

### 8.1 未认证访问矩阵

```bash
for path in \
  '/api/v1/user/profile' \
  '/api/v1/users' \
  '/api/v1/fee/list' \
  '/api/v1/fee/standards' \
  '/api/v1/system/logs' \
  '/api/v1/upload/avatar/00000000-0000-0000-0000-000000000000.jpg'
do
  printf '%-75s' "$path"
  curl -sS -o /dev/null -w '%{http_code}\n' "$BASE_URL$path"
done | tee "$EVIDENCE/04-auth/anonymous-matrix.txt"
```

验收：以上接口均返回 `401`。允许匿名的范围只应包括登录、刷新、验证码和站点静态资源。

### 8.2 账号枚举

人工获取两个新的验证码Token，分别提交：

- 不存在的用户名 + 错误密码；
- 存在的用户名 + 错误密码。

对比HTTP状态、响应正文和响应时间。验收：错误信息均为“用户名或密码错误”一类的统一内容，不能明确区分账号是否存在；耗时不应有稳定、明显差异。

由于每次登录必须消耗新的验证码Token，不要重复使用同一Token进行比较。

### 8.3 验证码一次性和过期

1. 在ZAP中完成一次验证码验证并得到 `captchaToken`；
2. 使用该Token完成一次登录；
3. 重放完全相同的登录请求；
4. 等待超过120秒后再使用另一个未消费Token。

验收：已消费Token和过期Token均被拒绝，且不能跨账号复用。

### 8.4 登录限流和锁定

只对专用测试账号执行，不使用密码字典。

1. 连续进行5次错误密码登录，每次人工完成新验证码；
2. 记录第1至第6次状态码、消息、时间和 `X-Request-ID`；
3. 第5次失败后尝试一次正确密码；
4. 检查是否进入短期锁定；
5. 检查Redis限流键和后端安全日志；
6. 测试结束后等待锁定过期或由管理员恢复测试账号。

验收：当前默认配置下，账号失败5次应触发约15分钟锁定；大量IP失败也应受限。不要为了测试IP上限而影响其他测试人员。

### 8.5 JWT篡改

先确认正常Token：

```bash
curl -sS -i "$BASE_URL/api/v1/user/profile" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  | tee "$EVIDENCE/04-auth/jwt-valid.txt"
```

篡改签名最后一个字符：

```bash
case "${TOKEN_MEMBER_A: -1}" in
  A) BAD_TOKEN="${TOKEN_MEMBER_A::-1}B" ;;
  *) BAD_TOKEN="${TOKEN_MEMBER_A::-1}A" ;;
esac

curl -sS -i "$BASE_URL/api/v1/user/profile" \
  -H "Authorization: Bearer $BAD_TOKEN" \
  | tee "$EVIDENCE/04-auth/jwt-tampered.txt"

unset BAD_TOKEN
```

验收：返回 `401`，响应不得包含JWT密钥、Java堆栈或解析库内部错误。

### 8.6 Token撤销

分别验证以下操作后旧Access Token立即失效：

- 注销；
- 修改密码；
- 管理员重置密码；
- 修改角色；
- 禁用账号。

操作前保存旧Token，操作后再次请求 `/api/v1/user/profile`。预期统一返回 `401`，不能等到15分钟自然过期才失效。

### 8.7 Refresh Cookie

在ZAP登录响应中检查：

- `HttpOnly`；
- `SameSite=Strict`；
- `Path=/api/v1/auth`；
- HTTPS生产环境必须包含 `Secure`；
- Refresh Token不出现在登录JSON、前端Local Storage、URL或应用日志中。

测试刷新：

1. 带Cookie但不带 `X-SJA-Refresh: 1`，应拒绝；
2. 带 `X-SJA-Refresh: 1` 和Cookie，应成功轮换；
3. 再次使用旧Refresh Token，应拒绝并触发令牌族保护；
4. Access Token不能当Refresh Token使用。

## 9. 第五阶段：角色、功能和对象级授权

### 9.1 权限矩阵

对每个角色至少执行下表。保存请求、响应状态、业务码和 `X-Request-ID`。

| 接口/操作 | 管理员 | 部长 | 普通部员 | 未登录 |
|---|---:|---:|---:|---:|
| `GET /api/v1/user/profile` | 200 | 200 | 200 | 401 |
| `GET /api/v1/users` | 按权限200 | 按权限 | 403 | 401 |
| `GET /api/v1/system/logs` | 按权限200 | 403 | 403 | 401 |
| `POST /api/v1/fee/submit` | 按权限 | 本部门 | 本部门且有权限 | 401 |
| `PUT /api/v1/fee/audit/...` | 按权限 | 按权限 | 403 | 401 |
| `POST /api/v1/fee/export` | 按权限 | 按权限 | 403 | 401 |
| `PUT /api/v1/fee/standards/{id}` | 按权限 | 403或按设计 | 403 | 401 |
| `POST /api/v1/tools/sujia/resolve` | 按权限 | 按权限 | 按菜单权限 | 401 |

“按权限”必须以实际角色菜单设计为准，不允许为了让测试通过而扩大权限。

### 9.2 功能级越权示例

普通用户访问系统日志：

```bash
curl -sS -i "$BASE_URL/api/v1/system/logs?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  | tee "$EVIDENCE/05-authz/member-system-logs.txt"
```

预期：`403`。

普通用户访问用户管理：

```bash
curl -sS -i "$BASE_URL/api/v1/users?pageNum=1&pageSize=10" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  | tee "$EVIDENCE/05-authz/member-users.txt"
```

预期：`403`。

### 9.3 用户对象IDOR

用专门创建的测试用户A、B的ID替换下面变量：

```bash
export TEST_USER_A_ID='替换为用户A_ID'
export TEST_USER_B_ID='替换为用户B_ID'
```

使用A的Token直接请求B的管理接口：

```bash
curl -sS -i "$BASE_URL/api/v1/users/$TEST_USER_B_ID" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  | tee "$EVIDENCE/05-authz/idor-user-a-reads-b.txt"
```

预期：普通用户返回 `403`，不能泄露B的身份证、银行卡、手机号等字段。

### 9.4 稿费对象IDOR和跨部门访问

只使用测试任务ID：

```bash
export TEST_FEE_TYPE='EDITORIAL'
export TEST_FEE_ID='替换为测试任务ID'

curl -sS -i "$BASE_URL/api/v1/fee/detail/$TEST_FEE_TYPE/$TEST_FEE_ID" \
  -H "Authorization: Bearer $TOKEN_MEMBER_B" \
  | tee "$EVIDENCE/05-authz/idor-fee-cross-dept.txt"
```

验证：

- 无权部门不能读取、编辑或审核；
- 主席/老师等受限角色的响应中不出现总额或个人金额；
- 修改路径中的 `workType` 和 `id` 不能绕过授权；
- 已审核、已驳回任务不能非法跳转状态；
- 重复审核必须被拒绝或保持幂等，不能重复计费。

### 9.5 Mass Assignment

用普通用户自己的资料更新接口附加越权字段：

```bash
cat > /tmp/sja-profile-extra-fields.json <<'JSON'
{
  "realName": "安全测试用户",
  "roleIds": [1],
  "deptIds": [1],
  "isValid": 1,
  "tokenVersion": 0,
  "mustChangePassword": 0
}
JSON

curl -sS -i -X PUT "$BASE_URL/api/v1/user/profile" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  -H 'Content-Type: application/json' \
  --data-binary @/tmp/sja-profile-extra-fields.json \
  | tee "$EVIDENCE/05-authz/mass-assignment-profile.txt"

rm -f /tmp/sja-profile-extra-fields.json
```

验收：额外字段被拒绝或完全忽略；重新读取账号后角色、部门、状态、Token版本和首次改密状态均未改变。

## 10. 第六阶段：输入校验、注入和业务逻辑

### 10.1 错误类型和边界值

对测试环境中的虚构数据逐项提交：

- 空字符串、`null`、数组代替字符串、字符串代替数字；
- 超过DTO上限的字符串；
- 负阅读量、负点赞量、负金额；
- 极大金额、科学计数法、`NaN`、`Infinity`；
- 不存在月份、未来日期和非法日期；
- 重复JSON字段；
- 非法作品类型，例如 `PHOTOGRAPHY`（业务作品类型应为 `PHOTO`）；
- 额外的 `totalAmount`、`feeAmount`、`auditStatus` 字段。

验收：返回明确的4xx业务错误，不产生500，不暴露SQL、表名、Java类名、本地路径或堆栈。

### 10.2 SQL注入人工探测

只在只读列表查询参数中使用无破坏性的探测字符串：

```bash
curl -sS -G "$BASE_URL/api/v1/users" \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  --data-urlencode "realName=SJA' OR '1'='1" \
  --data-urlencode 'pageNum=1' \
  --data-urlencode 'pageSize=10' \
  -D "$EVIDENCE/06-input/sqli-manual-headers.txt" \
  -o "$EVIDENCE/06-input/sqli-manual-body.json"
```

验收：不会返回全部用户，不出现数据库错误。输入被当作普通搜索字符串处理。

### 10.3 SQLMap受控验证

在ZAP中导出一条管理员访问用户列表的原始HTTP请求，保存为：

```text
$EVIDENCE/06-input/users-list-request.txt
```

确认该文件只指向 `192.168.255.128` 后执行：

```bash
sqlmap -r "$EVIDENCE/06-input/users-list-request.txt" \
  -p realName \
  --batch --level=2 --risk=1 \
  --threads=1 --delay=0.2 --timeout=10 --retries=1 \
  --output-dir="$EVIDENCE/06-input/sqlmap"
```

禁止增加：

```text
--risk=3 --os-shell --sql-shell --file-read --file-write --file-dest --priv-esc
```

如果发现疑似注入，只用布尔差异或时间差异复核，不导出数据库内容。

### 10.4 XSS

仅在虚构标题、备注和测试用户姓名中使用可识别但不外传数据的Canary：

```html
<img src=x onerror="console.log('SJA_XSS_CANARY')">
```

以及：

```html
<svg onload="console.log('SJA_XSS_CANARY')"></svg>
```

步骤：

1. 通过ZAP修改测试对象的标题或备注；
2. 分别打开列表、详情、审核页和操作日志页；
3. 查看DOM和浏览器控制台；
4. 导出包含该字段的Excel并检查单元格；
5. 清除测试数据。

验收：页面将内容作为文本展示，不创建可执行事件属性；响应不应直接拼接未编码HTML。

### 10.5 Excel公式注入

分别使用以下虚构标题或姓名：

```text
=1+1
+SUM(1,1)
-1+2
@SUM(1,1)
```

导出XLSX后不要直接允许Excel执行外部内容。先离线检查：

```bash
unzip -l exported-test.xlsx | tee "$EVIDENCE/06-input/xlsx-file-list.txt"
unzip -p exported-test.xlsx xl/sharedStrings.xml \
  | tee "$EVIDENCE/06-input/xlsx-shared-strings.xml"
```

验收：危险首字符应被安全转义或作为明确的纯文本单元格存储，不能生成公式。

### 10.6 Sujia名单数量边界

该DTO当前未发现 `@Size` 数量上限，因此必须逐级测试。不要直接从一万个元素开始。

```bash
for count in 10 100 500 1000; do
  jq -n --argjson n "$count" \
    '{names: [range(0;$n) | "安全测试姓名-\(.)"]}' \
    > "/tmp/sujia-$count.json"

  curl -sS -o "$EVIDENCE/06-input/sujia-$count.json" \
    -w "count=$count http=%{http_code} time=%{time_total}s size=%{size_download}\n" \
    -X POST "$BASE_URL/api/v1/tools/sujia/resolve" \
    -H "Authorization: Bearer $TOKEN_ADMIN" \
    -H 'Content-Type: application/json' \
    --data-binary "@/tmp/sujia-$count.json"

  sleep 3
done | tee "$EVIDENCE/06-input/sujia-timing.txt"

rm -f /tmp/sujia-*.json
```

测试时监控后端。验收建议：服务端设置明确且合理的集合上限，超限返回 `400/413`，不能造成长时间CPU或内存增长。

### 10.7 业务金额篡改

对于自动计算的稿费类型：

1. 在ZAP中截获提交请求；
2. 将贡献者 `feeAmount`、总金额或工作数量改为负数、零和极大值；
3. 增加未授权的 `totalAmount` 字段；
4. 提交后重新读取详情；
5. 检查数据库中的最终金额和操作日志。

验收：自动金额必须由后端根据规则计算，客户端金额不能覆盖；非法数值被拒绝；金额计算使用确定的小数精度。

## 11. 第七阶段：头像上传

### 11.1 生成安全测试文件

```bash
mkdir -p /tmp/sja-upload-tests

printf '<html><script>console.log("SJA_UPLOAD_CANARY")</script></html>' \
  > /tmp/sja-upload-tests/not-image.jpg

convert -size 32x32 xc:blue /tmp/sja-upload-tests/valid.png
convert -size 6001x1 xc:red /tmp/sja-upload-tests/too-wide.png
convert -size 4500x4500 xc:white /tmp/sja-upload-tests/too-many-pixels.png

dd if=/dev/zero of=/tmp/sja-upload-tests/over-5m.jpg bs=1M count=6 status=none
printf 'broken-jpeg' > /tmp/sja-upload-tests/broken.jpg
```

逐个上传，一次只发一个请求：

```bash
for file in /tmp/sja-upload-tests/*; do
  name=$(basename "$file")
  curl -sS -i -X POST "$BASE_URL/api/v1/upload/avatar" \
    -H "Authorization: Bearer $TOKEN_MEMBER_A" \
    -F "file=@$file" \
    > "$EVIDENCE/07-upload/$name.response.txt"
done
```

预期：

- `valid.png` 成功，并由服务端重新编码、随机命名；
- HTML伪装、损坏图片、超5MB、超尺寸和超像素图片均被拒绝；
- 错误响应不暴露服务器文件路径。

### 11.2 文件名和路径穿越

```bash
curl -sS -i -X POST "$BASE_URL/api/v1/upload/avatar" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  -F 'file=@/tmp/sja-upload-tests/valid.png;filename=../../outside.png' \
  | tee "$EVIDENCE/07-upload/traversal-filename.txt"

curl -sS -i "$BASE_URL/api/v1/upload/avatar/..%2f..%2fetc%2fpasswd" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  | tee "$EVIDENCE/07-upload/traversal-read.txt"
```

验收：服务端只使用UUID文件名；路径穿越返回4xx，不能读取任意文件。

### 11.3 头像访问控制

对成功上传并绑定到测试账号的头像：

```bash
curl -sS -I "$BASE_URL/api/v1/upload/avatar/替换为UUID.png" \
  | tee "$EVIDENCE/07-upload/avatar-anonymous.txt"

curl -sS -I "$BASE_URL/api/v1/upload/avatar/替换为UUID.png" \
  -H "Authorization: Bearer $TOKEN_MEMBER_A" \
  | tee "$EVIDENCE/07-upload/avatar-authenticated.txt"
```

验收：匿名返回401；认证后只允许读取已被用户资料引用的UUID；响应为固定图片MIME、`nosniff`、`no-store`。

### 11.4 孤儿文件

1. 上传合法头像但不调用资料保存接口；
2. 记录返回UUID；
3. 尝试读取；
4. 检查服务端是否定期清理未绑定文件；
5. 验证重复上传不会无限消耗磁盘。

如果系统允许上传后立即产生文件，应至少具备未绑定文件的过期清理机制。

## 12. 第八阶段：ZAP主动扫描

只在数据库已备份、监控已开启、无人进行其他测试时执行。

### 12.1 匿名攻击面

```bash
mkdir -p "$EVIDENCE/03-zap/full-anonymous"

docker run --rm --network host \
  -v "$EVIDENCE/03-zap/full-anonymous:/zap/wrk:rw" \
  ghcr.io/zaproxy/zaproxy:stable \
  zap-full-scan.py -t "$BASE_URL" \
  -m 5 \
  -r zap-full-anonymous.html \
  -J zap-full-anonymous.json \
  -w zap-full-anonymous.md
```

### 12.2 认证攻击面

推荐使用ZAP GUI，避免把Token暴露在进程参数：

1. 使用测试角色登录；
2. 在Sites树中只选择 `192.168.255.128`；
3. 将登出、密码修改、批量审核、导出、用户状态修改等有副作用接口排除；
4. 先Spider，再Ajax Spider；
5. 只对所选节点运行Active Scan；
6. 并发线程设为2，单主机请求速率保持低速；
7. 管理员、部长、普通用户分别扫描并保存不同会话；
8. 每轮后检查数据库是否产生测试记录；
9. 导出HTML和JSON报告。

所有Medium/High/Critical告警必须用单个HTTP请求人工复现。无法复现的标记为误报并说明依据。

## 13. 第九阶段：Docker、主机、数据库和Redis

以下命令在Ubuntu目标机执行。

### 13.1 暴露端口

```bash
sudo ss -lntup
docker compose ps
```

验收：MySQL、Redis、Spring Boot没有绑定到宿主机公网或局域网端口；Nginx只绑定预期地址。

### 13.2 容器身份和权限

```bash
docker compose exec backend id
docker compose exec web id
docker inspect sja-system-backend-1 --format '{{json .Config.User}}'
docker inspect sja-system-backend-1 --format '{{json .HostConfig.Privileged}}'
```

验收：后端使用非root用户，容器不是 privileged，不挂载 Docker Socket，不使用不必要的宿主目录。

容器实际名称可能不同，先用 `docker compose ps` 获取名称。

### 13.3 数据库最小权限

```bash
docker compose exec -T mysql sh -lc \
  'mysql -usja -p"$MYSQL_PASSWORD" -e "SHOW GRANTS FOR CURRENT_USER;"' \
  | sed -E 's/IDENTIFIED BY PASSWORD .*/IDENTIFIED BY PASSWORD [REDACTED]/'
```

记录应用账号是否拥有 `DROP`、`ALTER`、`CREATE USER`、`GRANT OPTION`。理想情况下，运行账号只拥有业务运行所需的 `SELECT/INSERT/UPDATE/DELETE`；数据库迁移应使用独立账号。

不要为了验证权限而真的删除表。使用 `SHOW GRANTS` 即可。

### 13.4 Redis认证和网络

未认证测试：

```bash
docker compose exec -T redis redis-cli PING
```

预期：`NOAUTH Authentication required`。

认证健康检查：

```bash
docker compose exec -T redis sh -lc \
  'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli PING'
```

预期：`PONG`。不要运行 `KEYS *`；使用 `SCAN` 检查少量键名，并确认不保存明文密码或完整Token。

### 13.5 Docker镜像扫描

在装有Trivy的测试机执行：

```bash
trivy image --severity HIGH,CRITICAL --ignore-unfixed \
  --format json --output "$EVIDENCE/10-source/backend-image-trivy.json" \
  sja-system-backend

trivy image --severity HIGH,CRITICAL --ignore-unfixed \
  --format json --output "$EVIDENCE/10-source/web-image-trivy.json" \
  sja-system-web
```

如果Kali无法访问Ubuntu本地镜像，可在Ubuntu执行并把脱敏后的报告复制到证据目录。

## 14. 第十阶段：敏感数据、日志和告警

### 14.1 API最小披露

分别使用普通用户、部长、主席/老师和管理员读取同一测试对象，比较响应字段：

- 身份证和银行卡是否只在确有业务需要时返回；
- 手机号是否按角色限制；
- 主席/老师是否看不到受限稿费金额；
- 列表接口是否比详情接口返回更少字段；
- 操作日志是否对普通用户完全禁止；
- 错误响应是否包含请求标识但不包含敏感输入。

### 14.2 日志泄露检查

在Ubuntu执行：

```bash
cd ~/SJA_System
docker compose logs --no-color backend web \
  | rg -i 'authorization:|bearer |sja_refresh_token|password|jwt|id_card|bank_card|身份证|银行卡' \
  | head -n 200
```

命中项必须人工判断。验收：不得记录密码、Access Token、Refresh Token、完整身份证或银行卡。

### 14.3 安全事件审计

验证以下事件都有时间、用户/账号、来源IP、请求ID、动作、对象和结果：

- 连续登录失败和锁定；
- 验证码异常请求；
- 401/403；
- 管理员修改角色、状态、密码和稿费标准；
- 导出、批量审核和密码重置；
- 非法文件上传；
- Refresh Token重放；
- MySQL/Redis认证失败。

日志应能调查事件，但不能因记录过多敏感数据造成二次泄露。

## 15. 第十一阶段：受控资源耗尽测试

本阶段不是无限制拒绝服务攻击。只逐级增加负载，并严格遵守停止条件。

### 15.1 已知业务写入基线

现有k6结果显示：25个瞬时并发写请求时，P95约0.59秒、失败率0%；193个瞬时并发写请求时会出现约5秒排队。安全测试以25并发作为已知安全起点。

逐级测试：

```powershell
# Windows压测机示例，不在Kali上重复压测也可以
k6 run `
  -e BASE_URL=http://192.168.255.128/ `
  -e PROFILE_VUS=20 `
  -e FEE_VUS=5 `
  .\mixed-load.js
```

后续只在资源稳定时增加到60、110并发。每一档之间等待至少60秒，并记录CPU、内存、连接池、MySQL和错误率。

### 15.2 安全相关资源用例

按 `1 → 5 → 10 → 20` 并发逐级测试：

- 验证码生成；
- 大页码用户/稿费查询；
- 合法头像上传；
- 不存在头像UUID读取；
- Sujia名单解析；
- Excel导出。

停止阈值：失败率超过1%、P95超过5秒、CPU/内存达到本文停止条件时不再增加。

验收：系统应限流或返回可预期的4xx/429/503，不能崩溃、OOM、重启或造成永久数据损坏。

## 16. 第十二阶段：源代码、依赖和密钥

本阶段在仓库副本执行，避免污染正式工作目录。

### 16.1 后端测试和依赖树

```bash
cd /path/to/SJA_System/SJA_Sys_Java
./mvnw clean test
./mvnw dependency:tree > "$EVIDENCE/10-source/maven-dependency-tree.txt"
```

OWASP Dependency-Check需要NVD数据，建议配置NVD API Key后运行：

```bash
export NVD_API_KEY='从安全渠道读取，不写入脚本'

./mvnw org.owasp:dependency-check-maven:check \
  -DnvdApiKey="$NVD_API_KEY" \
  -DfailBuildOnCVSS=7

unset NVD_API_KEY
```

扫描结果要确认漏洞是否影响实际使用路径，不能只看CVE数量。

### 16.2 前端依赖

```bash
cd /path/to/SJA_System/SJA_Sys_Vite
npm ci
npm audit --audit-level=high | tee "$EVIDENCE/10-source/npm-audit.txt"
npm audit signatures | tee "$EVIDENCE/10-source/npm-signatures.txt"
npm run build
```

不要直接运行 `npm audit fix --force`。

### 16.3 密钥扫描

使用Gitleaks扫描当前仓库及历史：

```bash
gitleaks git /path/to/SJA_System \
  --redact \
  --report-format json \
  --report-path "$EVIDENCE/10-source/gitleaks.json"
```

重点检查：

- `.env`、数据库密码、Redis密码、JWT密钥；
- SQL备份中的默认密码哈希和真实敏感数据；
- Git历史中已删除但仍可恢复的密钥；
- 测试报告中的Access Token和Cookie。

密钥一旦进入Git历史，应按已泄露处理并轮换，而不是只删除当前文件。

### 16.4 静态代码扫描

```bash
semgrep scan --config auto /path/to/SJA_System \
  --json --output "$EVIDENCE/10-source/semgrep.json"
```

人工复核：

- SQL拼接、命令执行、路径穿越和不安全反序列化；
- Vue中的 `v-html`、`innerHTML`、`eval`；
- 未授权接口和仅依赖前端隐藏的权限；
- 日志中的密码、Token、身份证和银行卡；
- 可控URL请求造成的SSRF；
- Excel公式注入；
- DTO集合和字符串缺少上限。

### 16.5 SBOM

为前后端和容器生成CycloneDX或SPDX SBOM，并与版本发布归档。SBOM不是漏洞报告，但能帮助后续快速判断新CVE是否影响已部署版本。

## 17. 漏洞记录规范

每个问题建立独立记录：

```text
编号：SJA-SEC-YYYY-NNN
标题：
发现日期：
测试环境：
影响资产：
OWASP ASVS/WSTG/API分类：
严重性：Critical / High / Medium / Low / Info
前置条件：
复现步骤：
实际结果：
预期结果：
证据文件：
影响分析：
修复建议：
负责人：
修复版本：
复测日期：
复测结果：
```

风险评级同时考虑：

- 是否无需登录；
- 是否可跨用户、跨部门或提升到管理员；
- 是否泄露身份证、银行卡、手机号、头像或Token；
- 是否能修改稿费金额、审核状态或用户权限；
- 是否能远程执行代码、读取服务器文件或破坏数据；
- 是否容易批量利用；
- 是否已有日志和告警能够检测。

## 18. 上线阻断标准

只有同时满足以下条件才建议公网部署：

- Critical和High漏洞为0；
- Medium漏洞有修复计划、负责人和明确期限；
- 所有角色、部门、对象级授权矩阵通过；
- 不存在默认密码、共享密码和已泄露密钥；
- 登录限流、锁定、验证码和安全审计有效；
- 注销、改密、重置、改角色、禁用后旧Token立即失效；
- Refresh Token轮换和重放检测通过；
- ZAP高危结果为0，其他结果完成复核；
- SCA和镜像扫描无可利用的高危依赖；
- 头像上传和读取测试通过；
- Sujia集合、批量接口、分页和导出均有服务端上限；
- HTTPS、Cookie、CORS、CSP和安全头通过；
- 公网只开放必要的80/443，MySQL、Redis、Spring Boot不公开；
- 数据库运行账号符合最小权限；
- 日志不包含密码、Token、完整身份证或银行卡；
- 数据库、Redis和头像完成一次真实恢复演练；
- 修复后按原复现步骤独立复测通过。

## 19. 推荐执行顺序

按以下顺序逐步执行，每完成一阶段再进入下一阶段：

1. 确认授权范围、测试账号和虚构数据；
2. 备份MySQL、Redis和头像卷；
3. 开启Ubuntu监控；
4. 端口、HTTP头、CORS和TLS检查；
5. Nikto、Nuclei、ZAP Baseline；
6. ZAP人工登录并建立各角色会话；
7. 未认证、验证码、限流、JWT和Cookie；
8. 角色权限矩阵、IDOR和Mass Assignment；
9. 输入校验、SQL注入、XSS、公式注入和金额篡改；
10. 头像上传和路径测试；
11. ZAP低并发主动扫描；
12. Docker、MySQL、Redis和敏感数据检查；
13. 受控资源耗尽测试；
14. 依赖、镜像、密钥、静态代码和SBOM；
15. 汇总报告、修复、恢复测试数据并复测。

## 20. 参考标准和工具文档

- [OWASP ASVS](https://owasp.org/www-project-application-security-verification-standard/)
- [OWASP Web Security Testing Guide](https://owasp.org/www-project-web-security-testing-guide/)
- [OWASP API Security Top 10](https://owasp.org/www-project-api-security/)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
- [OWASP Logging Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Logging_Cheat_Sheet.html)
- [ZAP Baseline Scan](https://www.zaproxy.org/docs/docker/baseline-scan/)
- [ZAP Full Scan](https://www.zaproxy.org/docs/docker/full-scan/)
- [Nmap Reference Guide](https://nmap.org/book/man.html)
- [sqlmap Usage](https://github.com/sqlmapproject/sqlmap/wiki/Usage)

## 21. 实验手册：从开机到第一份报告

本节是实际操作顺序。不要跳着执行。三个终端分别标记为：

- `[KALI]`：Kali测试机；
- `[UBUNTU]`：运行Docker Compose的目标机；
- `[BROWSER]`：Kali中的Firefox，经ZAP代理访问系统。

### 21.1 第0天：只准备，不扫描

#### 步骤1：创建虚拟机快照

1. 正常关闭Ubuntu测试机；
2. 在VMware中选择该虚拟机；
3. 打开“虚拟机 → 快照 → 拍摄快照”；
4. 名称填写 `before-sja-security-test-20260820`；
5. 描述记录代码版本、数据库快照时间和测试IP；
6. 启动Ubuntu；
7. 确认系统和容器正常后再继续。

#### 步骤2：记录版本

`[UBUNTU]`：

```bash
cd ~/SJA_System

date -Is
uname -a
docker version
docker compose version
docker compose images
docker compose ps
git rev-parse HEAD 2>/dev/null || true
```

将输出保存到测试记录。不要执行 `docker compose config` 后把完整输出放进报告，因为展开后的配置可能包含密码。

#### 步骤3：确认测试数据不是正式数据

`[UBUNTU]`：

```bash
cd ~/SJA_System
docker compose exec -T mysql sh -lc \
  'mysql -usja -p"$MYSQL_PASSWORD" sja_system_db -e "
   SELECT COUNT(*) AS users FROM sys_user;
   SELECT id,username,real_name,is_valid,must_change_password
   FROM sys_user ORDER BY id LIMIT 10;
  "'
```

人工确认显示的是测试账号。报告中只记录账号编号和测试角色，不保存身份证、手机号或银行卡。

#### 步骤4：验证备份可读取

```bash
gzip -t ~/sja-security-backup/sja-before-security-test.sql.gz
echo $?
```

预期退出码为 `0`。仅“生成了备份文件”不等于备份可恢复。

### 21.2 第1天上午：只读基线

#### 步骤1：初始化Kali变量

`[KALI]`：

```bash
cp /path/to/SJA_System/deploy/security-test/kali/kali.env.example \
  "$HOME/sja-kali.env"

nano "$HOME/sja-kali.env"
```

确认 `TARGET_IP=192.168.255.128`，然后：

```bash
chmod 600 "$HOME/sja-kali.env"
source "$HOME/sja-kali.env"
```

#### 步骤2：运行只读基线脚本

```bash
chmod +x /path/to/SJA_System/deploy/security-test/kali/01-safe-baseline.sh
/path/to/SJA_System/deploy/security-test/kali/01-safe-baseline.sh
```

脚本结束时会显示证据目录。逐个查看：

```bash
find "$EVIDENCE" -maxdepth 2 -type f -printf '%p %s bytes\n' | sort
```

#### 步骤3：人工判定端口

```bash
cat "$EVIDENCE/01-network/selected-ports.nmap"
```

逐项填写：

| 端口 | 预期 | 实际 | 判定 |
|---:|---|---|---|
| 22 | 仅管理网络允许 |  |  |
| 80 | 当前测试环境开放 |  |  |
| 443 | 启用HTTPS后开放 |  |  |
| 8080 | 不可从Kali访问 |  |  |
| 3306 | 不可从Kali访问 |  |  |
| 33060 | 不可从Kali访问 |  |  |
| 6379 | 不可从Kali访问 |  |  |

#### 步骤4：人工判定响应头

```bash
sed -n '1,40p' "$EVIDENCE/02-http/home.headers.txt"
```

用下面命令逐项检查：

```bash
for header in \
  x-content-type-options \
  x-frame-options \
  referrer-policy \
  permissions-policy \
  content-security-policy \
  strict-transport-security
do
  printf '%-32s' "$header"
  grep -i "^$header:" "$EVIDENCE/02-http/home.headers.txt" || echo 'MISSING'
done
```

当前HTTP环境的HSTS缺失先记录为“生产前待验证”；CSP缺失应登记为安全加固项。

### 21.3 第1天下午：ZAP和测试账号

#### 步骤1：创建ZAP会话

`[KALI]`：

```bash
zaproxy
```

ZAP弹出会话选择时：

1. 选择“是，我要保存此会话”；
2. 保存为 `$HOME/security-tests/当前TEST_ID/03-zap/sja-security.session`；
3. 打开 `Tools → Options → Network → Local Servers/Proxies`；
4. 确认地址 `127.0.0.1`、端口 `8080`；
5. 打开 `Tools → Options → Display`，确认响应和请求正文可查看；
6. 不启用全局主动扫描。

#### 步骤2：配置Firefox代理

`[BROWSER]`：

1. Firefox → Settings；
2. 搜索 `proxy`；
3. 打开 Network Settings；
4. 选择 Manual proxy configuration；
5. HTTP Proxy填写 `127.0.0.1`，Port填写 `8080`；
6. 勾选“Also use this proxy for HTTPS”；
7. 在“No proxy for”中不要填写目标IP；
8. 保存；
9. 访问 `http://192.168.255.128/`。

#### 步骤3：依次登录角色账号

每次只登录一个账号：

1. 人工完成滑块；
2. 登录；
3. 在ZAP History中过滤 `auth/login`；
4. 双击请求，确认Request Body只有测试用户名、密码和captchaToken；
5. 查看Response Body，找到 `data.token`；
6. 查看Response Headers中的 `Set-Cookie`；
7. 截图时遮挡Token、Cookie和密码；
8. 登出后清除站点Cookie；
9. 再测试下一个角色。

建议会话标记：

```text
A0_ADMIN
A3_EDITORIAL_MANAGER
A4_PHOTO_MANAGER
A5_EDITORIAL_MEMBER
A6_PHOTO_MEMBER
A7_FIRST_LOGIN
A8_DISABLED
```

#### 步骤4：保存Token到当前Shell，不写文件

```bash
read -rsp 'A0 admin token: ' TOKEN_ADMIN; echo
read -rsp 'A3 editorial manager token: ' TOKEN_EDITORIAL_MANAGER; echo
read -rsp 'A4 photo manager token: ' TOKEN_PHOTO_MANAGER; echo
read -rsp 'A5 member A token: ' TOKEN_MEMBER_A; echo
read -rsp 'A6 member B token: ' TOKEN_MEMBER_B; echo

export TOKEN_ADMIN TOKEN_EDITORIAL_MANAGER TOKEN_PHOTO_MANAGER TOKEN_MEMBER_A TOKEN_MEMBER_B
```

确认没有回显Token：

```bash
env | grep '^TOKEN_' | sed 's/=.*$/=[REDACTED]/'
```

### 21.4 第2天：认证和授权

执行顺序：

1. 匿名访问矩阵；
2. 有效Token基线；
3. 无Token、空Token、错误前缀、篡改Token；
4. 首次改密绕过；
5. 普通用户访问管理员功能；
6. 同角色不同用户对象IDOR；
7. 同级不同部门稿费IDOR；
8. 受限角色金额字段检查；
9. 禁用、改密、改角色后的旧Token撤销；
10. Refresh Token轮换与重放。

每完成一个改变账号状态的用例，都要重新获取新Token，避免把“Token已撤销”误判为后续接口授权失败。

### 21.5 第3天：输入、上传和扫描

执行顺序：

1. 先运行单请求边界值；
2. 再运行XSS Canary；
3. 再运行只读SQLMap；
4. 再测试头像格式和尺寸；
5. 清理虚构记录；
6. 执行ZAP匿名Full Scan；
7. 最后才执行低并发认证Active Scan；
8. 每轮扫描后检查容器日志、数据库数量和磁盘空间。

### 21.6 第4天：复核和恢复

1. 将扫描器结果逐条用单请求复现；
2. 删除误报；
3. 对真实问题分级；
4. 导出操作日志中的测试事件；
5. 删除测试头像和虚构业务数据；
6. 检查容器仍健康；
7. 必要时恢复测试快照；
8. 形成初测报告；
9. 修复后使用同一个测试编号和同一请求复测。

## 22. API请求模板：先跑正常请求，再修改一个变量

安全测试不能只发送攻击字符串。每个接口都应先保存一个能够成功的正常请求，然后每次只修改一个变量。否则无法判断失败来自权限、格式、验证码、Token撤销还是测试载荷。

### 22.1 通用请求记录函数

在Kali当前Shell定义：

```bash
sja_request() {
  label="$1"
  method="$2"
  path="$3"
  token="$4"
  data_file="${5:-}"

  output_dir="$EVIDENCE/manual"
  mkdir -p "$output_dir"

  if [ -n "$data_file" ]; then
    curl -sS \
      -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $token" \
      -H 'Content-Type: application/json' \
      -H "X-Request-ID: SEC-$TEST_ID-$label" \
      --data-binary "@$data_file" \
      -D "$output_dir/$label.headers.txt" \
      -o "$output_dir/$label.body.txt" \
      -w 'http=%{http_code} time=%{time_total}s bytes=%{size_download}\n' \
      | tee "$output_dir/$label.metrics.txt"
  else
    curl -sS \
      -X "$method" "$BASE_URL$path" \
      -H "Authorization: Bearer $token" \
      -H "X-Request-ID: SEC-$TEST_ID-$label" \
      -D "$output_dir/$label.headers.txt" \
      -o "$output_dir/$label.body.txt" \
      -w 'http=%{http_code} time=%{time_total}s bytes=%{size_download}\n' \
      | tee "$output_dir/$label.metrics.txt"
  fi
}
```

函数会把请求结果分成Headers、Body和Metrics三个证据文件，但不会保存Token值。

### 22.2 读取个人资料

```bash
sja_request \
  'AUTH-VALID-PROFILE' \
  'GET' \
  '/api/v1/user/profile' \
  "$TOKEN_MEMBER_A"

jq . "$EVIDENCE/manual/AUTH-VALID-PROFILE.body.txt"
```

通过标准：HTTP 200、业务码200，只返回当前用户信息。

### 22.3 更新个人资料正常请求

```bash
cat > /tmp/profile-valid.json <<'JSON'
{
  "realName": "安全测试甲",
  "gender": 1,
  "idCard": "110101200001010037",
  "studentNo": "SEC20260001",
  "college": "安全测试学院",
  "className": "安全测试班",
  "dormAddress": "测试地址A-101",
  "phone": "13900000001",
  "bankCard": "6222020000000000001"
}
JSON

sja_request \
  'PROFILE-UPDATE-VALID' \
  'PUT' \
  '/api/v1/user/profile' \
  "$TOKEN_MEMBER_A" \
  /tmp/profile-valid.json
```

注意：如果修改 `studentNo`，后端会同步修改用户名并增加Token版本，当前Token可能立即失效。测试普通字段时保持 `studentNo` 与当前值一致。

### 22.4 修改密码

不要把真实密码直接保存在请求模板。临时创建后立即删除：

```bash
read -rsp 'Old test password: ' OLD_PASSWORD; echo
read -rsp 'New test password: ' NEW_PASSWORD; echo

jq -n \
  --arg old "$OLD_PASSWORD" \
  --arg new "$NEW_PASSWORD" \
  '{oldPassword:$old,newPassword:$new}' \
  > /tmp/password-change.json

sja_request \
  'PASSWORD-CHANGE' \
  'PUT' \
  '/api/v1/user/profile/password' \
  "$TOKEN_MEMBER_A" \
  /tmp/password-change.json

shred -u /tmp/password-change.json 2>/dev/null || rm -f /tmp/password-change.json
unset OLD_PASSWORD NEW_PASSWORD
```

通过标准：改密成功；旧Token立即失效；旧密码不能登录；新密码可以登录；操作日志不记录密码。

### 22.5 创建虚构测试用户

先在系统中查询测试部门ID和角色ID。不要照抄示例ID：

```bash
sja_request 'SYSTEM-DEPTS' 'GET' '/api/v1/system/depts' "$TOKEN_ADMIN"
sja_request 'SYSTEM-ROLES' 'GET' '/api/v1/system/roles' "$TOKEN_ADMIN"
```

根据返回值替换：

```bash
export TEST_DEPT_ID='替换为测试部门ID'
export TEST_ROLE_ID='替换为普通部员角色ID'

jq -n \
  --argjson dept "$TEST_DEPT_ID" \
  --argjson role "$TEST_ROLE_ID" \
  '{
    realName:"安全测试乙",
    gender:2,
    studentNo:"SEC20260002",
    password:"SecTest2026",
    idCard:"110101200001010045",
    phone:"13900000002",
    bankCard:"6222020000000000002",
    college:"安全测试学院",
    className:"安全测试班",
    dormAddress:"测试地址A-102",
    deptIds:[$dept],
    roleIds:[$role],
    isValid:1,
    remark:"仅用于隔离环境安全测试"
  }' > /tmp/user-create.json

sja_request \
  'USER-CREATE-VALID' \
  'POST' \
  '/api/v1/users' \
  "$TOKEN_ADMIN" \
  /tmp/user-create.json

rm -f /tmp/user-create.json
```

保存响应中的用户ID到 `TEST_USER_B_ID`。测试结束后通过管理界面禁用该用户；当前系统没有硬删除用户接口。

### 22.6 状态修改正常和非法请求

```bash
printf '{"status":0}\n' > /tmp/status-disabled.json
printf '{"status":2}\n' > /tmp/status-invalid.json

sja_request \
  'USER-DISABLE-ADMIN' \
  'PUT' \
  "/api/v1/users/$TEST_USER_B_ID/status" \
  "$TOKEN_ADMIN" \
  /tmp/status-disabled.json

sja_request \
  'USER-STATUS-INVALID' \
  'PUT' \
  "/api/v1/users/$TEST_USER_B_ID/status" \
  "$TOKEN_ADMIN" \
  /tmp/status-invalid.json

rm -f /tmp/status-disabled.json /tmp/status-invalid.json
```

通过标准：`status=0`由有权管理员执行成功并撤销旧Token；`status=2`返回400；普通用户执行相同接口返回403。

### 22.7 稿费提交正常请求

使用测试用户A自己的ID：

```bash
export TEST_USER_A_ID='替换为测试用户A ID'

jq -n --argjson uid "$TEST_USER_A_ID" '{
  workType:"EDITORIAL",
  workInfo:{
    title:"SEC-正常稿费提交",
    platform:"微信",
    category:"原创类",
    publishDate:"2026-08-20",
    isPublished:1,
    isSpecialNode:0,
    specialMultiplier:1,
    wordCount:800,
    readCount:1000,
    articleLink:"https://example.invalid/sja-security-test",
    remark:"隔离环境安全测试"
  },
  contributors:[{
    userId:$uid,
    roleInWork:"AUTHOR",
    feeAmount:null,
    workCount:null,
    remark:"安全测试"
  }]
}' > /tmp/fee-editorial-valid.json

sja_request \
  'FEE-SUBMIT-VALID' \
  'POST' \
  '/api/v1/fee/submit' \
  "$TOKEN_MEMBER_A" \
  /tmp/fee-editorial-valid.json
```

通过标准：有 `fee:submit` 权限且部门匹配时成功；跨部门Token提交时返回403；服务端计算金额而不是信任客户端金额。

### 22.8 稿费金额篡改

```bash
jq '.contributors[0].feeAmount=999999999 |
    .contributors[0].workCount=-1 |
    .workInfo.totalAmount=999999999 |
    .totalAmount=999999999' \
  /tmp/fee-editorial-valid.json \
  > /tmp/fee-editorial-tampered.json

sja_request \
  'FEE-SUBMIT-AMOUNT-TAMPER' \
  'POST' \
  '/api/v1/fee/submit' \
  "$TOKEN_MEMBER_A" \
  /tmp/fee-editorial-tampered.json

rm -f /tmp/fee-editorial-tampered.json
```

判定不能只看HTTP码。若请求成功，必须读取详情并确认最终金额没有采用篡改值。

### 22.9 单条审核

只使用新建的测试稿费ID：

```bash
export TEST_FEE_TYPE='EDITORIAL'
export TEST_FEE_ID='替换为测试稿费ID'

cat > /tmp/fee-audit-approve.json <<'JSON'
{
  "approved": true,
  "opinion": "SEC-授权审核测试"
}
JSON

sja_request \
  'FEE-AUDIT-ADMIN' \
  'PUT' \
  "/api/v1/fee/audit/$TEST_FEE_TYPE/$TEST_FEE_ID" \
  "$TOKEN_ADMIN" \
  /tmp/fee-audit-approve.json
```

然后分别使用普通用户、非管理部门部长重放同一请求，预期403。管理员重复审核同一任务应被拒绝，不能重复产生业务金额。

### 22.10 批量审核边界

批量审核需要当前管理员密码，不应把密码写入长期文件：

```bash
read -rsp 'Admin current password: ' ADMIN_PASSWORD; echo

jq -n \
  --arg password "$ADMIN_PASSWORD" \
  --arg type "$TEST_FEE_TYPE" \
  --argjson id "$TEST_FEE_ID" \
  '{
    tasks:[{workType:$type,id:$id}],
    approved:true,
    opinion:"SEC-批量审核测试",
    currentPassword:$password
  }' > /tmp/fee-batch-audit.json

sja_request \
  'FEE-BATCH-AUDIT' \
  'PUT' \
  '/api/v1/fee/audit/batch' \
  "$TOKEN_ADMIN" \
  /tmp/fee-batch-audit.json

shred -u /tmp/fee-batch-audit.json 2>/dev/null || rm -f /tmp/fee-batch-audit.json
unset ADMIN_PASSWORD
```

边界测试：0条应返回400；201条应返回400；错误管理员密码应返回4xx；普通用户无论密码是否正确都应先因权限不足返回403。

### 22.11 Sujia正常请求

```bash
cat > /tmp/sujia-normal.json <<'JSON'
{
  "names": ["安全测试甲", "安全测试乙", "不存在的测试姓名"]
}
JSON

sja_request \
  'SUJIA-NORMAL' \
  'POST' \
  '/api/v1/tools/sujia/resolve' \
  "$TOKEN_ADMIN" \
  /tmp/sujia-normal.json
```

通过标准：仅返回当前角色有权查看范围内的姓名和学号；不存在姓名明确标记未匹配；不能借此查询其他部门用户。

## 23. 逐项测试用例总表

每个用例必须记录“执行人、时间、构建版本、账号角色、请求ID、实际状态、证据路径”。

### 23.1 网络和部署

| ID | 测试 | 操作 | 通过标准 |
|---|---|---|---|
| NET-001 | 指定端口 | Nmap扫描22/80/443/8080/3306/33060/6379 | 只开放预期端口 |
| NET-002 | 全TCP端口 | 低速 `-p-` | 无未知服务 |
| NET-003 | 后端端口 | Kali连接8080 | 连接失败 |
| NET-004 | MySQL端口 | Kali连接3306/33060 | 连接失败 |
| NET-005 | Redis端口 | Kali连接6379 | 连接失败 |
| NET-006 | HTTP方法 | TRACE和未定义方法 | 不返回成功且不回显请求 |
| NET-007 | Host注入 | 恶意Host请求 | 不生成恶意链接、不缓存污染 |
| NET-008 | 转发IP伪造 | 伪造XFF | 不能绕过IP限流 |
| NET-009 | API文档 | 请求 `/v3/api-docs`、`/swagger-ui/` | 生产配置404/401，不公开 |
| NET-010 | 备份文件 | 请求常见 `.bak/.sql/.zip/.env` | 404，不泄露文件 |

### 23.2 认证和会话

| ID | 测试 | 操作 | 通过标准 |
|---|---|---|---|
| AUTH-001 | 匿名访问 | 无Token访问受保护API | 401 |
| AUTH-002 | 空Token | `Authorization: Bearer ` | 401 |
| AUTH-003 | 错误方案 | `Authorization: Basic ...` | 401 |
| AUTH-004 | 篡改JWT | 改变Payload或签名 | 401 |
| AUTH-005 | 过期JWT | 使用过期测试Token | 401 |
| AUTH-006 | Access/Refresh混用 | 交换两类Token | 401/4xx |
| AUTH-007 | 验证码重放 | 同Token登录两次 | 第二次失败 |
| AUTH-008 | 验证码过期 | 超过120秒 | 失败 |
| AUTH-009 | 账号枚举 | 存在/不存在账号错误密码 | 消息一致、耗时无稳定差异 |
| AUTH-010 | 账号锁定 | 专用账号5次失败 | 触发短期锁定 |
| AUTH-011 | 注销撤销 | 注销后使用旧Token | 401 |
| AUTH-012 | 改密撤销 | 改密后使用旧Token | 401 |
| AUTH-013 | 重置撤销 | 管理员重置后旧Token | 401 |
| AUTH-014 | 改角色撤销 | 修改角色后旧Token | 401 |
| AUTH-015 | 禁用撤销 | 禁用后旧Token | 401 |
| AUTH-016 | Refresh轮换 | 使用新旧Refresh | 新成功、旧失败 |
| AUTH-017 | Cookie属性 | 检查Set-Cookie | HttpOnly/Strict/正确Path，HTTPS含Secure |
| AUTH-018 | 首次改密 | 首次用户调用普通业务API | 403，仅允许改密/登出等必要操作 |

### 23.3 授权和业务对象

| ID | 测试 | 操作 | 通过标准 |
|---|---|---|---|
| AZ-001 | 用户管理 | 普通用户访问 `/users` | 403 |
| AZ-002 | 操作日志 | 普通用户访问 `/system/logs` | 403 |
| AZ-003 | 用户IDOR | A读取B管理详情 | 403 |
| AZ-004 | 用户状态 | A禁用B | 403 |
| AZ-005 | 密码重置 | A重置B密码 | 403 |
| AZ-006 | 角色注入 | 自助资料中加入roleIds | 角色不变 |
| AZ-007 | 部门注入 | 自助资料中加入deptIds | 部门不变 |
| AZ-008 | 跨部门列表 | 部长筛选其他部门 | 不返回越权数据 |
| AZ-009 | 稿费IDOR | A读取B任务 | 403/按明确业务范围限制 |
| AZ-010 | 跨部门稿费 | 采编角色访问摄影任务 | 403 |
| AZ-011 | 金额字段 | 受限角色读取详情 | 不出现个人和总金额 |
| AZ-012 | 稿费计算 | 普通用户调用calculate | 403 |
| AZ-013 | 稿费审核 | 普通用户调用audit | 403 |
| AZ-014 | 批量审核 | 非管理员/秘书长调用 | 403 |
| AZ-015 | 重复审核 | 同一任务审核两次 | 第二次拒绝，不重复计费 |
| AZ-016 | 非法状态跳转 | 已审核任务直接编辑 | 拒绝或按明确流程控制 |
| AZ-017 | 导出权限 | 普通用户直接调用导出 | 403 |
| AZ-018 | Sujia范围 | 部长查询其他部门姓名 | 不返回学号 |
| AZ-019 | 头像匿名读 | 无Token读取UUID | 401 |
| AZ-020 | 未引用头像 | 读取孤儿UUID | 404 |

### 23.4 输入和输出

| ID | 测试 | 操作 | 通过标准 |
|---|---|---|---|
| IN-001 | SQLi人工 | 只读搜索参数Canary | 无注入、无SQL错误 |
| IN-002 | SQLMap | level2/risk1/单线程 | 无可利用注入 |
| IN-003 | 反射XSS | 查询参数插入Canary | 输出编码 |
| IN-004 | 存储XSS | 标题/备注/姓名插入Canary | 所有页面不执行 |
| IN-005 | DOM XSS | 检查URL和前端渲染 | 不执行 |
| IN-006 | JSON类型 | 数组替代字符串等 | 400而非500 |
| IN-007 | 重复字段 | 同名JSON键不同值 | 明确拒绝或确定行为 |
| IN-008 | 额外字段 | 加roleIds/金额/状态 | 拒绝或忽略 |
| IN-009 | 负数 | 阅读量/金额/数量为负 | 400 |
| IN-010 | 极大数 | 超范围数值 | 400，不溢出 |
| IN-011 | 非法日期 | 13月/不存在日期 | 400 |
| IN-012 | 非法作品类型 | `PHOTOGRAPHY` | 400，不出现500 |
| IN-013 | 分页上限 | pageSize=201 | 400或强制限制到200 |
| IN-014 | 批量上限 | 201个审核/导出对象 | 400 |
| IN-015 | Sujia上限 | 10→100→500→1000 | 有明确上限且资源稳定 |
| IN-016 | Excel公式 | `= + - @` 开头数据 | 导出为安全文本 |
| IN-017 | 错误泄露 | 各类非法输入 | 无堆栈、SQL和路径 |

### 23.5 文件上传

| ID | 测试 | 操作 | 通过标准 |
|---|---|---|---|
| UP-001 | 正常PNG | 上传32x32图片 | 成功、UUID命名、重新编码 |
| UP-002 | HTML伪装 | HTML改名jpg | 4xx |
| UP-003 | 损坏JPEG | 无效二进制 | 4xx |
| UP-004 | 超5MB | 6MB文件 | 413/4xx |
| UP-005 | 超尺寸 | 6001像素边长 | 4xx |
| UP-006 | 超像素 | 20M以上像素 | 4xx且无OOM |
| UP-007 | 双扩展 | `a.jpg.html` | 4xx |
| UP-008 | 路径文件名 | `../../a.png` | 随机名或拒绝，不能越界 |
| UP-009 | 读取穿越 | URL编码目录穿越 | 4xx |
| UP-010 | MIME | 检查成功头像响应 | image/png或jpeg、nosniff |
| UP-011 | 缓存 | 检查Cache-Control | no-store |
| UP-012 | 并发上传 | 1→5→10并发 | 资源稳定且有上限 |
| UP-013 | 孤儿清理 | 上传不绑定 | 不能永久无限累积 |

### 23.6 数据、日志和恢复

| ID | 测试 | 操作 | 通过标准 |
|---|---|---|---|
| DATA-001 | MySQL暴露 | Kali连接3306 | 不可达 |
| DATA-002 | Redis暴露 | Kali连接6379 | 不可达 |
| DATA-003 | Redis认证 | 容器内无密码PING | NOAUTH |
| DATA-004 | DB权限 | SHOW GRANTS | 无全局和用户管理权限 |
| DATA-005 | 日志Token | 搜索Bearer/Cookie | 无完整Token |
| DATA-006 | 日志密码 | 错误登录/批量审核 | 无密码 |
| DATA-007 | 日志PII | 修改资料后检查 | 身份证/银行卡脱敏 |
| DATA-008 | 导出控制 | 下载和权限 | 只有授权角色可导出 |
| DATA-009 | 备份加密 | 检查存储和访问 | 加密、限制权限、有清理周期 |
| DATA-010 | 恢复演练 | 恢复到临时库/临时卷 | 数据和头像可恢复 |

## 24. 结果如何判定，不只看HTTP 200

SJA接口通常同时包含HTTP状态和JSON业务码。每次至少检查五项：

1. HTTP状态；
2. JSON中的 `code`；
3. `message` 是否泄露内部信息；
4. 数据库最终状态；
5. 操作日志和 `X-Request-ID`。

### 24.1 正常成功

```text
HTTP 200
JSON code = 200
业务数据只发生一次预期变化
存在可审计日志
无敏感信息泄露
```

### 24.2 认证失败

```text
HTTP 401
消息统一且不泄露Token解析细节
数据库没有业务变化
必要时记录安全失败事件
```

### 24.3 授权失败

```text
HTTP 403
不能只在前端隐藏按钮
数据库没有业务变化
不能通过改变ID、workType、部门参数绕过
```

### 24.4 输入失败

```text
HTTP 400/409/413等明确4xx
不能返回500
不能包含SQL、Java堆栈、容器路径
不能产生半条记录或孤立关联数据
```

### 24.5 疑似漏洞

下列情况必须登记并人工复现：

- 请求返回200且发生越权数据读取/修改；
- 返回403但数据实际上已被修改；
- 返回500并包含数据库或Java内部信息；
- 只改变对象ID即可访问他人数据；
- 客户端金额、角色、状态覆盖服务端值；
- 安全测试触发容器重启、OOM或长期不可用；
- 扫描器报告High/Critical，但尚未单请求复核。

## 25. 每日收尾检查

`[KALI]`：

```bash
unset TOKEN_ADMIN TOKEN_EDITORIAL_MANAGER TOKEN_PHOTO_MANAGER TOKEN_MEMBER_A TOKEN_MEMBER_B
rm -f \
  /tmp/profile-valid.json \
  /tmp/status-disabled.json \
  /tmp/status-invalid.json \
  /tmp/fee-editorial-valid.json \
  /tmp/fee-editorial-tampered.json \
  /tmp/fee-audit-approve.json \
  /tmp/sujia-normal.json

# 确认路径无误后，只删除本方案创建的临时目录。
test -d /tmp/sja-upload-tests && rm -rf -- /tmp/sja-upload-tests
date -Is | tee "$EVIDENCE/00-scope/end-time.txt"
```

`[UBUNTU]`：

```bash
cd ~/SJA_System
docker compose ps
docker stats --no-stream
df -h
docker compose logs --since=30m --no-color backend \
  | tail -n 300 > ~/sja-security-backup/backend-last-30m.log
```

最后确认：

- 所有测试Token已撤销或过期；
- 临时密码已更换；
- 测试账号已禁用；
- 测试稿费记录已标识或清理；
- 测试头像已清理；
- ZAP会话受到文件权限保护；
- 未脱敏证据不进入Git；
- 容器健康、正常账号能登录、数据库数量合理。

## 26. 测试完成后修复清单

测试期间发现但不需要立即改变测试基线的问题，统一登记到：

- [SJA System 测试完成后修复清单](security-test/POST_TEST_REMEDIATION_BACKLOG.md)

测试期间只追加问题、证据和复核状态；除非触发本方案的立即停止或上线阻断条件，否则在全部测试完成后统一修复并回归验证。
