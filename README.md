# 实习生管理系统

当前项目采用前后端分离结构：

- `frontend/`：静态前端页面与本地开发服务器
- `backend/`：Java 21 + Spring Boot 后端

后端当前使用 SQLite 文件持久化数据，默认位置为：

```text
backend/data/intern-manager.db
```

如果首次启动时数据库为空且 `backend/data/intern-records.csv` 存在，会自动导入旧数据。

敏感字段会在后端加密存储：

- `phone`
- `idNumber`
- `emergencyPhone`

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
mvn spring-boot:run
```

## 接口职责

- 实习生提交：`POST /api/interns`
- Mentor 登录：`POST /api/mentor/auth/login`
- Mentor 会话检查：`GET /api/mentor/auth/session`
- Mentor 列表查询：`GET /api/mentor/interns`
- Mentor 编辑保存：`PUT /api/mentor/interns/{id}`
- Mentor 确认：`POST /api/mentor/interns/{id}/approve`
- Mentor 打回删除：`DELETE /api/mentor/interns/{id}`
- Mentor 清空：`DELETE /api/mentor/interns`

## 固定配置

为了降低部署难度，当前项目已经把这些值直接写死在代码里：

- Mentor 登录 token：`mentor-2026`
- 后端落盘加密 key
- 前后端传输加密 key
- 后端监听地址和端口
- 前端本地开发代理地址

如果后续需要修改这些值，请同步更新对应源码文件。

## 部署

公网 HTTPS 部署方案见：

```text
deploy/README.md
```
