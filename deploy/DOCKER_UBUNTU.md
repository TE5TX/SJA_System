# Ubuntu 24.04 Docker 部署

本项目的 Compose 会启动四个容器：Nginx/Vue、Spring Boot、MySQL 8.4 和 Redis。

## 0. 确认虚拟机网络和 SSH

先在虚拟机控制台执行：

```bash
ip -br addr
ip route
ping -c 3 "$(ip route | awk '/default/ {print $3; exit}')"
sudo apt update
sudo apt install -y openssh-server openssl
sudo systemctl enable --now ssh
```

确认输出中存在 `192.168.255.128`；当前 VMware NAT 网关应显示为 `192.168.255.2`。再在 Windows PowerShell 测试：

```powershell
ping 192.168.255.128
ssh yjhuang@192.168.255.128
```

本文按当前用户名 `yjhuang` 编写；其他环境请替换为实际用户名。若 IP 不存在或宿主机无法 ping 通，先检查虚拟机网卡是否为 NAT 或桥接模式以及 IP 是否变化。

## 1. 安装 Docker

在 Ubuntu 虚拟机中执行：

```bash
sudo apt update
sudo apt install -y ca-certificates curl
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources >/dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "${UBUNTU_CODENAME:-$VERSION_CODENAME}")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
```

执行 `newgrp docker`，或者退出 SSH 后重新登录，然后验证：

```bash
newgrp docker
groups
docker --version
docker compose version
docker run --rm hello-world
```

`groups` 应包含 `docker`。不要使用 `chmod 777 /var/run/docker.sock` 绕过权限检查。

## 2. 上传项目

为避免上传 `.git`、`node_modules` 和本机编译产物，在 Windows PowerShell 中执行：

```powershell
cd C:\Users\Administrator\Desktop
tar --exclude=SJA_System/.git --exclude=SJA_System/.codex-tmp --exclude=SJA_System/SJA_Sys_Vite/node_modules --exclude=SJA_System/SJA_Sys_Vite/dist --exclude=SJA_System/SJA_Sys_Java/target -czf SJA_System_deploy.tar.gz SJA_System
scp .\SJA_System_deploy.tar.gz yjhuang@192.168.255.128:~/
ssh yjhuang@192.168.255.128 "tar -xzf ~/SJA_System_deploy.tar.gz -C ~/"
```

也可以使用 Xftp，直接把本机 `C:\Users\Administrator\Desktop\SJA_System` 上传为 `/home/yjhuang/SJA_System`。不需要上传 `.git`、`.codex-tmp`、`node_modules`、`dist` 和 `target`；必须上传隐藏文件 `.env.example`、两个 Dockerfile、`docker-compose.yml`、`nginx.conf` 和 `sql` 目录。

上传后在虚拟机中执行：

```bash
cd ~/SJA_System
ls sql/sja_system_db_202608131609.sql
```

必须能看到该 SQL 文件；它包含首次启动所需的数据库结构和现有数据。

## 3. 配置密钥

```bash
cd ~/SJA_System
cp .env.example .env

MYSQL_ROOT_PASSWORD=$(openssl rand -base64 36 | tr -d '\n')
MYSQL_PASSWORD=$(openssl rand -base64 36 | tr -d '\n')
REDIS_PASSWORD=$(openssl rand -base64 36 | tr -d '\n')
SJA_JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')

sed -i "s|^MYSQL_ROOT_PASSWORD=.*|MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD|" .env
sed -i "s|^MYSQL_PASSWORD=.*|MYSQL_PASSWORD=$MYSQL_PASSWORD|" .env
sed -i "s|^REDIS_PASSWORD=.*|REDIS_PASSWORD=$REDIS_PASSWORD|" .env
sed -i "s|^SJA_JWT_SECRET=.*|SJA_JWT_SECRET=$SJA_JWT_SECRET|" .env
chmod 600 .env
```

确认 `.env` 中的 `SJA_HOST=192.168.255.128`。这里不要把 `.env` 内容粘贴到聊天或日志中。

<!-- 202608210907 - 补充了人员批量注册功能的Redis令牌和XLSX限制说明。 -->
人员管理页的批量注册预览使用Redis保存一次性确认令牌，默认10分钟过期；单个xlsx默认不超过2MB且最多500人。可在`.env`中通过`SJA_USER_IMPORT_TOKEN_TTL_SECONDS`、`SJA_USER_IMPORT_MAX_FILE_BYTES`和`SJA_USER_IMPORT_MAX_ROWS`调整，修改后需要重建或重启后端容器。

也可以通过 `nano .env` 手动填写密码。`MYSQL_ROOT_PASSWORD` 是数据库管理员密码，`MYSQL_PASSWORD` 是 Java 后端使用的 `sja` 数据库用户密码，两者应不同。JWT 密钥可以使用 `openssl rand -base64 48` 生成。

## 4. 启动

```bash
docker compose config --quiet

# 4GB 虚拟机按顺序拉取和构建，避免前后端并行构建造成内存压力。
docker compose pull mysql redis
docker compose build backend
docker compose build web
docker compose up -d
docker compose ps
```

`config --quiet` 没有输出表示配置解析成功。首次构建和导入数据库通常需要几分钟。查看启动过程：

```bash
docker compose logs -f --tail=100
```

看到后端启动完成后按 `Ctrl+C` 退出日志（不会停止容器）。

## 5. 开放防火墙并访问

Compose 只把 Web 端口绑定到 `192.168.255.128`，MySQL、Redis 和后端端口不会暴露到宿主机。如果启用了 UFW：

```bash
sudo ufw allow OpenSSH
sudo ufw allow 80/tcp
sudo ufw enable
sudo ufw status
```

在宿主机浏览器访问：`http://192.168.255.128/`。

数据库仅执行 `01-schema-data.sql`。初始账号和密码以该脚本中的数据为准；部署成功后应立即修改管理员密码。

## 6. 使用 Cloudflare Tunnel 发布到公网

推荐使用 Cloudflare Tunnel 发布本系统。虚拟机只建立到 Cloudflare 的出站连接，不要求公网 IP或路由器端口转发，也不要将 MySQL `3306`、Redis `6379` 或后端 `8080` 暴露到公网。

前置条件：准备一个已托管到 Cloudflare 的域名。在 Cloudflare 控制台进入 `Networking -> Tunnels`，创建 remotely-managed tunnel，然后添加 Published application：

```text
Hostname: sja.example.com（替换为实际域名）
Service type: HTTP
Service URL: http://web:80
```

从 Tunnel 的安装命令中取得以 `eyJ` 开头的 token。不要把 token 提交到 Git、粘贴到聊天或写入本文。编辑 Ubuntu 项目中的 `.env`：

```bash
cd ~/SJA_System
nano .env
```

添加或修改：

```dotenv
SJA_PUBLIC_ORIGIN=https://sja.example.com
CLOUDFLARE_TUNNEL_TOKEN=替换为真实TunnelToken
```

确认 `.env` 只有当前用户可读，然后检查合并后的 Compose 配置：

```bash
chmod 600 .env
docker compose -f docker-compose.yml -f deploy/docker-compose.public.yml config --quiet
```

首次启动或更新公网部署：

```bash
docker compose -f docker-compose.yml -f deploy/docker-compose.public.yml pull cloudflared
docker compose -f docker-compose.yml -f deploy/docker-compose.public.yml up -d --force-recreate backend web cloudflared
docker compose -f docker-compose.yml -f deploy/docker-compose.public.yml ps
docker compose -f docker-compose.yml -f deploy/docker-compose.public.yml logs --tail=100 cloudflared
```

日志中出现已建立连接后，通过 `https://sja.example.com` 访问。Cloudflare 负责浏览器侧 HTTPS，覆盖文件同时将刷新令牌 Cookie 设置为 Secure，并把后端 CORS 限制为该公网来源。

使用 Tunnel 时无需为公网执行 `ufw allow 80/tcp`。若仍需局域网直接访问，可以仅允许局域网网段访问 80 端口；例如：

```bash
sudo ufw delete allow 80/tcp
sudo ufw allow from 192.168.1.0/24 to any port 80 proto tcp
sudo ufw allow OpenSSH
sudo ufw enable
sudo ufw status
```

不要开放以下端口：

```text
3306 MySQL
6379 Redis
8080 Spring Boot
```

如果 Tunnel 无法连接，检查虚拟机是否能主动访问 Cloudflare，以及网络是否限制 Tunnel 使用的出站 `7844` 端口：

```bash
docker compose -f docker-compose.yml -f deploy/docker-compose.public.yml logs --tail=200 cloudflared
curl -I https://www.cloudflare.com
```

## 7. 常用维护命令

```bash
# 查看状态
docker compose ps

# 查看后端日志
docker compose logs -f --tail=200 backend

# 重启服务
docker compose restart

# 更新代码后重新构建
docker compose build backend
docker compose build web
docker compose up -d

# 停止但保留数据库和上传文件
docker compose down
```

不要轻易执行 `docker compose down -v`，其中的 `-v` 会删除 MySQL、Redis 和头像数据卷。

首次初始化 SQL 只会在 MySQL 数据卷为空时执行。若修改了 SQL，已有数据库不会自动重导；应先备份数据库，再决定是否重建数据卷。

## 8. 构建故障排查

### npm 出现 ECONNRESET

如果前端构建出现以下错误：

```text
npm error code ECONNRESET
npm error network aborted
```

这是容器访问 npm 仓库时连接被重置，不是项目代码、MySQL 密码或 Redis 密码错误。先单独重试：

```bash
docker compose build web
```

仍然失败时测试容器内的 npm 网络：

```bash
docker run --rm node:22-alpine npm ping
```

正常输出应包含 `PING https://registry.npmjs.org/` 和 `PONG`。测试失败时保留完整输出用于进一步排查代理、DNS或镜像源；不要删除数据卷。

### Maven Central 返回 403

如果后端构建出现：

```text
Could not transfer artifact ... from/to central
status code: 403, reason phrase: Forbidden
```

这表示容器访问 Maven Central 被当前网络或代理拒绝，不是 `pom.xml` 依赖版本不存在。项目的 `SJA_Sys_Java/settings-docker.xml` 已为 Docker 构建配置阿里云 HTTPS 公共 Maven 镜像，并在 Dockerfile 中加入下载重试。确认服务器已经上传最新版的以下两个文件：

```text
SJA_Sys_Java/Dockerfile
SJA_Sys_Java/settings-docker.xml
```

然后重新构建；已经下载的基础镜像会从缓存复用：

```bash
docker compose build backend
```

### 查看启动失败的服务

```bash
docker compose ps -a
docker compose logs --tail=100 mysql
docker compose logs --tail=100 redis
docker compose logs --tail=100 backend
docker compose logs --tail=100 web
```

状态为 `Exited`、`Restarting` 或 `unhealthy` 时，优先查看对应服务日志。
