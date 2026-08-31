# SJA 并发压力测试

本目录按数据库快照中的有效人数执行真实写入压测。只能对从快照恢复的隔离测试数据库运行，禁止直接压正式数据库。

## 当前快照统计

`sja_system_db_202608131609.sql` 包含 181 个用户，其中 179 个有效用户、2 个无效用户。按角色菜单关系计算，有 14 个有效用户具备 `fee:submit` 权限。脚本因此同时执行 179 次个人资料修改和 14 次稿费提交。

## Windows 安装 k6

在实体 Windows 的 PowerShell 中执行：

```powershell
winget install --id GrafanaLabs.k6 --exact
k6 version
```

压测程序应运行在实体机，SJA、MySQL 和 Redis 继续运行在 Ubuntu 虚拟机。

## 生成测试数据

生成器不会复制 SQL 中的身份证、手机号或银行卡，而是根据有效用户 ID 创建互不冲突的合成数据。JWT 只用于测试，默认有效 120 分钟。

```powershell
cd C:\Users\Administrator\Desktop\SJA_System\load-test

$secure = Read-Host "输入测试环境 SJA_JWT_SECRET" -AsSecureString
$env:SJA_JWT_SECRET = [Net.NetworkCredential]::new('', $secure).Password

python .\prepare_data.py `
  --sql C:\Users\Administrator\Desktop\sja_system_db_202608131609.sql

Remove-Item Env:SJA_JWT_SECRET
```

输出为 `generated/users.generated.json`。该文件包含短期令牌，已被 Git 忽略。

测试数据库必须由同一份 SQL 恢复，而且后端的 `SJA_JWT_SECRET` 必须与生成数据时输入的值相同，否则令牌校验会失败。

## 执行

先用隔离测试环境地址执行，不要先压公网正式域名：

```powershell
k6 run `
  -e BASE_URL=https://你的测试域名 `
  --summary-export=results\mixed-summary.json `
  .\mixed-load.js
```

如果需要先小流量验证，可复制脚本并把 `vus` 临时改为固定小数字。`per-vu-iterations` 会让所有虚拟用户各执行一次，而不是持续重复写入。

## 服务器监控

在 Ubuntu 的三个终端分别运行：

```bash
watch -n 1 'docker stats --no-stream'
```

```bash
docker compose logs -f --tail=200 backend
```

```bash
watch -n 1 'docker compose exec -T mysql sh -lc '\''mysql -usja -p"$MYSQL_PASSWORD" -e "SHOW GLOBAL STATUS WHERE Variable_name IN (\"Threads_connected\",\"Threads_running\",\"Connections\",\"Aborted_connects\");"'\'''
```

## 验收

- 179 次个人资料修改和 14 次稿费提交均返回 HTTP 202、业务码 200，并在 120 秒内轮询到 `SUCCESS`。
- 失败率小于 1%，P95 小于 2 秒，P99 小于 5 秒。
- 后端没有数据库连接池超时、死锁、HTTP 500 或进程重启。
- 数据库新增 14 条有效稿费任务，操作日志数量与成功操作数一致。
- 测试完成后销毁测试数据库，不要把生成的合成资料同步回正式库。
