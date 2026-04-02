# Music Player 当前实现与后续优化计划

## 1. 当前项目实现

### 1.1 技术栈与运行结构
- 前端采用 `Vue 3 + Vue Router + HTML5 Audio`，以单页应用方式运行，静态资源位于 `web/` 目录。
- 后端采用原生 Java，基于 `com.sun.net.httpserver.HttpServer` 提供 HTTP 服务，不依赖 Spring 等框架。
- 数据访问采用 JDBC，当前默认连接本机 MySQL，数据库名为 `musicplayer`。
- 应用启动入口为 `src/main/java/com/musicplayer/App.java`，负责启动 API、静态资源、示例音频和本地上传媒体服务。
- 媒体资源分为两类：
  - `media/demo/`：项目自带示例音频
  - `media/uploads/`：用户导入的音频与封面文件

### 1.2 已实现功能
- 用户注册与登录：前端通过 `/api/auth/register` 和 `/api/auth/login` 完成认证，请求结果写入本地存储并恢复登录状态。
- 歌曲列表：前端可从 `/api/songs` 获取公共曲库数据并展示歌曲封面、歌名、歌手和专辑信息。
- 播放控制：使用 HTML5 `Audio` 实现播放、暂停、上一首、下一首、进度拖动和音量调节。
- 歌单管理：支持创建歌单、删除歌单、向歌单添加歌曲、从歌单移除歌曲。
- 播放历史：登录用户播放歌曲后会写入 `/api/history`，并在首页和右侧信息区展示最近播放。
- 本地导入歌曲：歌曲页支持上传音频文件和封面文件，后端保存文件后写入 MySQL `song` 表，导入歌曲进入公共曲库。
- 静态媒体访问：导入后的封面和音频可通过 `/media/uploads/...` 直接访问。

### 1.3 当前前端布局与交互
- 页面结构采用亮色版 Spotify Web 风格三段式布局：左侧导航、中间主内容区、右侧信息区、底部全局播放器。
- 首页展示推荐歌曲、继续收听和最近播放列表。
- 歌曲页展示本地导入表单和全部歌曲列表。
- 歌单页展示歌单创建、歌曲加入歌单和歌单内容管理。
- 正在播放页展示大封面、播放进度和核心控制按钮。
- 歌曲卡片采用方形封面，播放按钮固定在封面右上角。

### 1.4 当前后端接口
- `POST /api/auth/login`
- `POST /api/auth/register`
- `GET /api/songs`
- `GET /api/songs/{id}`
- `POST /api/songs/import`
- `GET /api/playlists?userId=...`
- `POST /api/playlists`
- `DELETE /api/playlists/{id}?userId=...`
- `POST /api/playlists/{id}/songs`
- `DELETE /api/playlists/{id}/songs/{songId}`
- `GET /api/history?userId=...`
- `POST /api/history`
- `GET /api/health`

### 1.5 当前数据库与文件存储
- MySQL 数据库默认名称：`musicplayer`
- 当前核心表：
  - `app_user`
  - `song`
  - `playlist`
  - `playlist_song`
  - `play_history`
- `song` 表当前用于保存歌曲元信息与资源地址，主要字段包括：
  - `title`
  - `artist`
  - `album`
  - `cover_url`
  - `audio_url`
  - `duration_seconds`
- 上传文件保存策略：
  - 音频文件保存到 `media/uploads/audio/`
  - 封面文件保存到 `media/uploads/covers/`

## 2. 当前项目已完成的重点改造
- 已将默认数据源切换到本机 MySQL JDBC 模式。
- 已实现注册、登录、歌曲查询、歌单管理、播放历史的后端 JDBC 逻辑。
- 已新增本地导入歌曲功能，并完成音频与封面文件的后端落盘。
- 已将前端主界面重构为更接近音乐平台网页端的布局。
- 已将前端脚本重写为可稳定运行的 UTF-8 无 BOM 文件，并用 Unicode 转义方式规避终端编码污染问题。

## 3. 后续优化方向
- 修复后端源码中仍然存在的部分中文乱码提示，统一接口返回文案。
- 继续完善前端文案细节，使全部提示语更自然、更贴近真实音乐应用。
- 为本地导入歌曲补充更友好的校验提示，例如文件类型、缺失字段、重复导入提醒。
- 增强播放器体验，例如队列排序、单曲循环、列表循环和播放状态记忆。
- 进一步整理 README 与课程交付文档，使项目说明、运行方式和功能展示更加统一。

## 4. 本轮确认事项
- `PLAN.md` 采用正常中文 UTF-8 无 BOM 重写，不继续保留乱码内容。
- Web 前端显示的项目名统一为 `Music Player`。
- 本轮不修改后端启动日志和 API 返回中的项目名称。
- 本轮不调整数据库结构、接口路径和页面布局。