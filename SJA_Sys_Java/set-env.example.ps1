# Copy these commands into your PowerShell profile or set them in the IDE run configuration.
# Do not commit real passwords or secrets.
$env:SJA_DB_URL = "jdbc:mysql://localhost:3306/sja_system_db?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:SJA_DB_USERNAME = "sja_app"
$env:SJA_DB_PASSWORD = "replace-with-a-long-random-password"

# Generate at least 32 random bytes. Example:
# $bytes = New-Object byte[] 48
# [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
# [Convert]::ToBase64String($bytes)
$env:SJA_JWT_SECRET = "replace-with-at-least-32-random-bytes"

$env:SJA_CORS_ALLOWED_ORIGINS = "http://localhost:5175"
$env:SJA_API_DOCS_ENABLED = "false"
$env:SJA_REFRESH_COOKIE_SECURE = "false" # Set to true when production uses HTTPS.
$env:SJA_AVATAR_DIR = "C:\ProgramData\SJA-System\uploads\avatars"

# 202608140858 - 增加了登录限流、失败锁定和受信任反向代理的可选配置。
$env:SJA_LOGIN_ACCOUNT_ATTEMPT_LIMIT = "10"
$env:SJA_LOGIN_IP_ATTEMPT_LIMIT = "30"
$env:SJA_LOGIN_ATTEMPT_WINDOW_SECONDS = "300"
$env:SJA_LOGIN_ACCOUNT_FAILURE_LIMIT = "5"
$env:SJA_LOGIN_IP_FAILURE_LIMIT = "20"
$env:SJA_LOGIN_FAILURE_WINDOW_SECONDS = "900"
$env:SJA_LOGIN_LOCK_SECONDS = "900"
$env:SJA_TRUSTED_PROXY_REGEX = "127\.\d+\.\d+\.\d+|0:0:0:0:0:0:0:1|::1"
