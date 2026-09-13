# 后端一键启动脚本（Windows PowerShell）
# 前置要求：JDK 21+ 与 Maven 3.9+ 已安装并加入 PATH
# 启动前先设置数据库密码（也可放在系统环境变量中）：
#   $env:PRAESIDIUM_DB_PASSWORD='你的密码'
#   powershell -NoProfile -ExecutionPolicy Bypass -File start-admin.ps1

# 前端开发端口白名单（Vite 默认 5173；使用其他端口时按需追加）
$env:PRAESIDIUM_CORS_ORIGINS = 'http://localhost:5173,http://127.0.0.1:5173'

mvn -f praesidium-admin/pom.xml spring-boot:run
