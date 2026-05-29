# 实习生管理系统

当前项目采用前后端分离结构：

- `frontend/`：静态前端页面与本地开发服务器
- `backend/`：Java 21 + Spring Boot 后端

后端当前使用 SQLite 文件持久化数据，默认位置为：

```text
backend/data/intern-manager.db
```

如果首次启动时数据库为空且 `backend/data/intern-records.csv` 存在，会自动导入旧数据。

当前存储结构不包含身份证号、手机号和紧急联系人手机号。

## 前端

本地开发：

```bash
cd frontend
npm install
npm run build
npm run dev
```

本地地址：

```text
http://localhost:5173
```

前端默认请求同源 `/api`。

## 后端

后端目录：

```text
backend/
```

后端固定监听：

```text
http://127.0.0.1:8080
```

启动方式：

```bash
cd backend
export APP_AUTH_MENTOR_TOKEN_SHA256="<sha256-hex>"
mvn spring-boot:run
```

PowerShell:

```powershell
cd backend
$env:APP_AUTH_MENTOR_TOKEN_SHA256 = "<sha256-hex>"
mvn spring-boot:run
```

`APP_AUTH_MENTOR_TOKEN_SHA256` 必填，值为 Mentor 登录 token 的 SHA-256 十六进制小写字符串。

## 接口职责

- 实习生提交：`POST /api/interns`
- Mentor 登录：`POST /api/mentor/auth/login`
- Mentor 会话检查：`GET /api/mentor/auth/session`
- Mentor 列表查询：`GET /api/mentor/interns`
- Mentor 编辑保存：`PUT /api/mentor/interns/{id}`
- Mentor 确认：`POST /api/mentor/interns/{id}/approve`
- Mentor 打回删除：`DELETE /api/mentor/interns/{id}`
- Mentor 清空：`DELETE /api/mentor/interns`

## 配置说明

当前项目要求通过环境变量提供 Mentor 登录 token 的 SHA-256：

```text
APP_AUTH_MENTOR_TOKEN_SHA256
```

建议流程：

1. 先确定一个新的 Mentor 明文 token。
2. 在本地或服务器上把这个明文 token 计算为 SHA-256。
3. 只把 SHA-256 结果写入环境变量，不把明文 token 写进仓库或配置文件。

Linux/macOS:

```bash
printf '%s' 'your-new-mentor-token' | sha256sum
```

PowerShell:

```powershell
$bytes = [System.Text.Encoding]::UTF8.GetBytes('your-new-mentor-token')
$hash = [System.Security.Cryptography.SHA256]::Create().ComputeHash($bytes)
-join ($hash | ForEach-Object { $_.ToString('x2') })
```

后端监听地址、端口和前端本地开发地址仍在源码配置文件中维护。

## 部署

公网 HTTPS 部署方案见：

```text
deploy/README.md
```
