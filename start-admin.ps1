$env:JAVA_HOME = 'D:\Program Files\Java\jdk-25.0.4'
$env:PRAESIDIUM_CORS_ORIGINS = 'http://localhost:5173,http://127.0.0.1:5173,http://localhost:5175,http://127.0.0.1:5175,http://localhost:5176,http://127.0.0.1:5176'
& 'D:\Program Files\apache-maven-3.9.16\bin\mvn.cmd' -f 'd:\project\praesidium\praesidium-admin\pom.xml' spring-boot:run 2>&1
