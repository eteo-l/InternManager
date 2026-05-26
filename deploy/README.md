# 部署说明

当前仓库的生产部署目标是：

- `nginx` 对外提供 `https://www.casintern.cn`
- `casintern.cn` 自动跳转到 `https://www.casintern.cn`
- `Spring Boot` 后端只监听 `127.0.0.1:8080`
- 前端固定通过同源 `/api` 访问后端
- 生产环境启用 `prod` profile，Session Cookie 走 `Secure`

## 固定值位置

- 生产 HTTPS nginx 配置：`deploy/nginx/intern-manager.conf`
- 首次申请证书用的 HTTP bootstrap 配置：`deploy/nginx/intern-manager-bootstrap.conf`
- systemd 服务文件：`deploy/systemd/intern-manager-backend.service`
- 后端基础配置：`backend/src/main/resources/application.yml`
- 后端生产配置：`backend/src/main/resources/application-prod.yml`
- 前端传输加密 key：`frontend/app.config.js`
- 后端传输加密 key：`backend/src/main/java/com/example/internmanager/service/ClientTransferCryptoService.java`

## 目录建议

建议在 Ubuntu 22.04 上使用下面的目录：

```text
/opt/intern-manager/backend
/var/www/intern-manager/frontend/current
```

其中：

- 后端 jar：`/opt/intern-manager/backend/intern-manager-backend.jar`
- SQLite 数据文件：`/opt/intern-manager/backend/data/intern-manager.db`
- 旧 CSV 迁移文件：`/opt/intern-manager/backend/data/intern-records.csv`
- 前端静态资源目录：`/var/www/intern-manager/frontend/current`

## 一次性构建

前端：

```bash
cd frontend
npm install
npm run build
```

后端：

```bash
cd backend
mvn clean package
```

构建完成后，后端 jar 为：

```text
backend/target/intern-manager-backend.jar
```

## 首次部署

### 1. 安装依赖

```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx openjdk-21-jre-headless
sudo systemctl enable --now certbot.timer
```

### 2. 创建目录

```bash
sudo mkdir -p /opt/intern-manager/backend/data
sudo mkdir -p /var/www/intern-manager/frontend/current
sudo chown -R www-data:www-data /opt/intern-manager/backend /var/www/intern-manager/frontend
```

### 3. 上传产物

上传前端构建产物到：

```text
/var/www/intern-manager/frontend/current
```

上传后端 jar 到：

```text
/opt/intern-manager/backend/intern-manager-backend.jar
```

如果要从旧 CSV 迁移，再额外上传：

```text
/opt/intern-manager/backend/data/intern-records.csv
```

### 4. 安装 systemd 服务

```bash
sudo cp deploy/systemd/intern-manager-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable intern-manager-backend
sudo systemctl restart intern-manager-backend
sudo systemctl status intern-manager-backend
```

这个服务文件已经固定：

- `WorkingDirectory=/opt/intern-manager/backend`
- `ExecStart=/usr/bin/java -jar /opt/intern-manager/backend/intern-manager-backend.jar`
- `SPRING_PROFILES_ACTIVE=prod`

### 5. 首次签发 Let's Encrypt 证书

第一次签发证书前，先安装 bootstrap nginx 配置：

```bash
sudo cp deploy/nginx/intern-manager-bootstrap.conf /etc/nginx/conf.d/intern-manager.conf
sudo nginx -t
sudo systemctl reload nginx
```

然后申请证书：

```bash
sudo certbot certonly --webroot -w /var/www/intern-manager/frontend/current -d casintern.cn -d www.casintern.cn
```

证书成功后，切换到正式 HTTPS 配置：

```bash
sudo cp deploy/nginx/intern-manager.conf /etc/nginx/conf.d/intern-manager.conf
sudo nginx -t
sudo systemctl reload nginx
```

如果证书已经存在于标准路径 `/etc/letsencrypt/live/casintern.cn/`，可以跳过 bootstrap，直接安装正式配置。

## 日常更新

前端更新：

```bash
sudo rsync -av --delete frontend/dist/ /var/www/intern-manager/frontend/current/
sudo chown -R www-data:www-data /var/www/intern-manager/frontend
```

后端更新：

```bash
sudo install -o www-data -g www-data -m 0644 backend/target/intern-manager-backend.jar /opt/intern-manager/backend/intern-manager-backend.jar
sudo systemctl restart intern-manager-backend
```

如果 nginx 配置没变，日常更新不需要重新申请证书。

## nginx 说明

正式配置做了这些事情：

- `http://casintern.cn` 和 `http://www.casintern.cn` 都跳转到 `https://www.casintern.cn`
- `https://casintern.cn` 跳转到 `https://www.casintern.cn`
- `https://www.casintern.cn` 提供前端页面
- `https://www.casintern.cn/api/...` 反向代理到 `http://127.0.0.1:8080/api/...`

`location /api/` 里的 `proxy_pass` 必须保持当前写法：

```nginx
proxy_pass http://127.0.0.1:8080;
```

不要改成会去掉 `/api` 前缀的写法，否则后端会找不到路由。

## 发布检查

上线后建议按这个顺序检查：

1. `curl http://127.0.0.1:8080/api/mentor/auth/session`
2. `curl -I http://casintern.cn`
3. `curl -I https://casintern.cn`
4. `curl -I https://www.casintern.cn`
5. 浏览器打开 `https://www.casintern.cn/`
6. 提交一条实习生数据
7. 使用 `mentor-2026` 登录 Mentor 页面
8. 检查 `/opt/intern-manager/backend/data/intern-manager.db` 是否有写入
