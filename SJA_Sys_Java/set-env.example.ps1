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

$env:SJA_CORS_ALLOWED_ORIGINS = "http://localhost:5173,http://127.0.0.1:5173,http://localhost:5175,http://127.0.0.1:5175"
$env:SJA_API_DOCS_ENABLED = "false"
$env:SJA_REFRESH_COOKIE_SECURE = "false" # Set to true when production uses HTTPS.
