# 异步写入队列维护说明

## 1. 解决的问题

`202608192148` 改造将以下两个高峰写入接口接入 Redis Stream：

- `PUT /api/v1/user/profile`
- `POST /api/v1/fee/submit`

接口完成参数校验、权限校验和加密入队后返回 HTTP `202 Accepted`。固定大小的后台线程池再执行原有 `UserService`、`FeeService`，避免约 200 个请求同时占用 MySQL 连接。

## 2. 请求流程

1. 前端为每次提交生成 `X-Idempotency-Key`。
2. 后端把 DTO 序列化后使用 AES-GCM 加密，仅将密文写入 Redis Stream。
3. 后端返回 `taskId`、`statusToken` 和 `statusUrl`。
4. 前端使用 `X-Task-Token` 低频轮询任务状态，不携带 JWT，不查询用户权限表。
5. 消费者执行前重新从数据库读取账号状态和最新角色权限。
6. 业务写入、操作日志和 `sys_async_task_execution` 标记在同一数据库事务中提交。
7. 前端只有收到 `SUCCESS` 后才显示成功并清空表单。

任务状态为：`PENDING`、`PROCESSING`、`SUCCESS`、`FAILED`。

## 3. 关键配置

2 核、2GB 单机的建议值：

```dotenv
SJA_ASYNC_TASK_ENCRYPTION_KEY=使用openssl生成的独立随机密钥
SJA_ASYNC_TASK_WORKER_COUNT=4
```

Compose 默认还会把 MySQL、后端、Redis、Nginx 和 Cloudflared 的内存上限分别控制为 `640m`、`640m`、`128m`、`64m`、`128m`。这些是上限而非预留值，总和约 1.6GB，给 Ubuntu 和 Docker 保留约 400MB。MySQL 默认最大连接数为 40，Java Hikari 池为 10，Tomcat 最大工作线程为 80。

4GB 内存环境可在压测确认后改为：

```dotenv
MYSQL_MEMORY_LIMIT=1g
MYSQL_INNODB_BUFFER_POOL_SIZE=384M
BACKEND_MEMORY_LIMIT=1g
REDIS_MEMORY_LIMIT=192m
REDIS_MAXMEMORY=128mb
SJA_DB_POOL_MAX_SIZE=16
SJA_TOMCAT_MAX_THREADS=120
```

可选环境变量及默认值：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `SJA_ASYNC_TASK_WORKER_COUNT` | `4` | 同时执行数据库写入的线程数 |
| `SJA_ASYNC_TASK_CONSUMER_NAME` | 自动生成 | Redis Stream消费者名称；多实例显式配置时每个实例必须唯一 |
| `SJA_ASYNC_TASK_CONSUMER_LEASE_TTL` | `90s` | 同名消费者实例冲突检测租约有效期 |
| `SJA_ASYNC_TASK_CONSUMER_LEASE_RENEW_INTERVAL` | `30s` | 消费者租约续期间隔，必须小于租约有效期 |
| `SJA_ASYNC_TASK_WORKER_QUEUE_CAPACITY` | `500` | JVM 内等待工作线程的任务数 |
| `SJA_ASYNC_TASK_QUEUE_CAPACITY` | `5000` | Redis Stream 最大受理数量 |
| `SJA_ASYNC_TASK_MAX_RETRIES` | `2` | 基础设施故障自动重试次数 |
| `SJA_ASYNC_TASK_VISIBILITY_TIMEOUT` | `5m` | pending 消息被其他消费者恢复前的空闲时间 |
| `SJA_ASYNC_TASK_STATUS_TTL` | `24h` | 前端可查询任务状态的时间 |

不要随意修改 `SJA_ASYNC_TASK_ENCRYPTION_KEY`。旧密钥仍有任务排队时更换密钥，会导致旧任务无法解密。正确做法是先确认队列为空，再轮换并重建后端容器。

### 多实例消费者名称

`202609201556` 起，未配置 `SJA_ASYNC_TASK_CONSUMER_NAME` 时，系统会根据主机名、进程ID和随机后缀生成唯一名称。单实例及 Docker Compose 动态扩容建议保持为空，让每个容器使用自己的 `HOSTNAME`。

需要显式配置时，每个实例必须使用不同值，例如：

```bash
# 实例一
SJA_ASYNC_TASK_CONSUMER_NAME=sja-backend-node-01

# 实例二
SJA_ASYNC_TASK_CONSUMER_NAME=sja-backend-node-02
```

Kubernetes 应通过 Downward API 注入 Pod 名称：

```yaml
env:
  - name: SJA_ASYNC_TASK_CONSUMER_NAME
    valueFrom:
      fieldRef:
        fieldPath: metadata.name
```

系统会为消费者名称申请 Redis 租约。如果两个实例误用相同名称，后启动的实例会拒绝启动消费者，避免 pending 消息归属和监控数据混乱。

## 4. 数据库升级

首次部署会由 Compose 自动挂载升级脚本。已有 MySQL 数据卷必须手动执行：

```bash
set -a
. ./.env
set +a
docker compose exec -T mysql mysql \
  -uroot -p"${MYSQL_ROOT_PASSWORD}" sja_system_db \
  < sql/20260819_异步任务队列升级.sql
```

必须先完成数据库升级，再启动包含异步消费者的新后端。

## 5. 日常监控

加载 `.env` 后查看队列长度、消费者组和 pending 消息：

```bash
set -a
. ./.env
set +a

docker compose exec redis redis-cli -a "${REDIS_PASSWORD}" \
  XLEN sja:async:write-tasks
docker compose exec redis redis-cli -a "${REDIS_PASSWORD}" \
  XINFO GROUPS sja:async:write-tasks
docker compose exec redis redis-cli -a "${REDIS_PASSWORD}" \
  XINFO CONSUMERS sja:async:write-tasks sja-write-workers
docker compose exec redis redis-cli -a "${REDIS_PASSWORD}" \
  XPENDING sja:async:write-tasks sja-write-workers
docker compose logs --tail=200 backend
```

正常情况下，短时高峰后 `XLEN` 和 pending 数量应逐步回到 `0`。不要直接执行 `DEL sja:async:write-tasks`，否则尚未处理的个人资料和稿费任务会丢失。

## 6. 故障处理

- Redis 短时不可用：新请求返回 `503`，未确认消息保留，Redis 恢复后继续处理。
- MySQL 短时不可用：任务最多自动重试 2 次；仍失败则前端收到 `FAILED`。
- 后端处理中退出：消息保留在 pending，超过 5 分钟后由恢复任务重新领取。
- 版本升级后，旧消费者 `sja-write-worker-1` 的 pending 会在可见性超时后被新实例认领。只有确认其 pending 数量为 `0` 后，才可以使用 `XGROUP DELCONSUMER` 清理旧消费者。
- 数据库已提交、Redis 状态未更新：消费者重放时通过 `sys_async_task_execution.task_id` 跳过业务写入，只补齐成功状态。
- 账号排队期间被禁用或撤销权限：任务失败，不沿用入队时的旧权限。

## 7. 压力测试验收

使用 [load-test/README.md](../load-test/README.md) 生成数据并运行 k6。验收必须同时满足：

- 所有写入请求返回 HTTP `202` 和业务码 `200`。
- 所有任务在 120 秒内进入 `SUCCESS`。
- 数据库新增稿费数量与成功任务数一致，不存在重复任务。
- `sys_async_task_execution` 每个 `task_id` 只有一条记录。
- 高峰期间 MySQL 连接数不会随前端并发人数等比例增长。
