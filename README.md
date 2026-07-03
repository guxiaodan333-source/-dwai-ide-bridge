# DWAI IDE Bridge

AI Agent 与 JetBrains IDE 的桥接工具。让 AI 助手获取光标代码上下文、诊断运行报错、执行脚本、推送 Diff 修改供开发者审批。

## 功能

| 功能 | 说明 |
|------|------|
| **代码上下文** | 获取光标位置、选中内容、附近代码（`editor.get_context`） |
| **诊断报错** | 编辑器静态错误 + Run 窗口报错信息（`diagnostics.get_all`） |
| **脚本运行** | 在 IDE 中运行脚本，捕获完整输出（`run.start`） |
| **Diff 审批** | 推送代码修改，开发者确认后生效（`diff.apply`） |
| **终端交互** | 发送命令到 IDE 终端（Terminal API） |
| **文件操作** | 打开/列出/保存关闭文件（`file.list_open` / `file.open`） |

## 安装

### 前置要求

- **JetBrains IDE**: IntelliJ IDEA 2023.3+、PyCharm 2023.3+、GoLand、WebStorm 等
- **Java**: 17+

### 安装步骤

1. 下载 [DWAI-1.0.0.zip](https://github.com/guxiaodan333-source/-dwai-ide-bridge/releases) （或自行编译）
2. 打开 IDE → **Settings** → **Plugins**
3. 点击齿轮 ⚙ → **Install Plugin from Disk...**
4. 选择下载的 `DWAI-1.0.0.zip`
5. 重启 IDE

### 自行编译

```bash
git clone https://github.com/guxiaodan333-source/-dwai-ide-bridge.git
cd dwai-ide-bridge
# Windows:
gradlew.bat buildPlugin
# macOS/Linux:
./gradlew buildPlugin
```

编译产物在 `build/distributions/DWAI-1.0.0.zip`

## 使用方式

插件安装后自动在 `127.0.0.1:8765` 启动 HTTP + MCP 服务。

### MCP 协议（推荐）

AI Agent 可直接通过 MCP JSON-RPC 协议调用：

```json
// 查询光标位置和代码上下文
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/call",
  "params": {
    "name": "editor.get_context",
    "arguments": { "radius": 10 }
  }
}
```

```json
// 推送代码修改供审批
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "diff.apply",
    "arguments": {
      "files": [{
        "path": "/path/to/file.py",
        "modified": "修改后的完整文件内容",
        "description": "修改说明"
      }]
    }
  }
}
```

**可用工具列表：**

| 工具 | 参数 | 说明 |
|------|------|------|
| `editor.get_context` | `radius` (默认 20) | 获取光标位置 + 选中 + 附近代码 + 项目信息 |
| `diagnostics.get_all` | — | 编辑器静态错误 + 运行报错摘要 |
| `diagnostics.get_full_output` | — | Run 窗口完整输出 |
| `editor.insert` | `text` | 在光标处插入代码 |
| `run.start` | `file_path` | 在 IDE 中运行脚本（非阻塞） |
| `diff.apply` | `files:[{path, modified, description, skip_review}]` | 推送代码修改供审批 |
| `diff.get_pending` | — | 查询待审批的 diff |
| `file.list_open` | — | 列出 IDE 中打开的文件 |
| `file.open` | `file_path, line` | 在 IDE 中打开文件 |
| `file.save_close` | `paths` (可选) | 保存并关闭文件 |

### HTTP API（备选）

```
GET  /api/cursor          — 光标位置
GET  /api/code?radius=10  — 光标附近代码
GET  /api/selection       — 选中内容
GET  /api/project         — 项目信息
GET  /api/problems        — 编辑器静态错误
GET  /api/run-output      — 运行记录
GET  /api/open-files      — 打开的文件列表
POST /api/apply-diff      — 推送代码修改
POST /api/terminal/exec   — 执行终端命令
GET  /api/health          — 健康检查
```

### Diff 审批流程

1. AI Agent 调用 `diff.apply` 推送修改
2. IDE 右下角弹出 **DWAI Changes** 对话框
3. 开发者可逐条 **✓ 接受** 或 **✕ 拒绝**
4. 也可使用 **"接受全部"** / **"拒绝全部"**
5. 点击文件名可查看详细对比

![DWAI Changes 对话框](docs/dialog.png)

**提示：** 测试文件、调试脚本等非核心修改可设置 `skip_review: true` 跳过审批。

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `DWAI_PORT` | `8765` | HTTP 服务端口 |

## 构建

```bash
# 编译
./gradlew buildPlugin

# 清理
./gradlew clean

# 本地调试（启动测试 IDE）
./gradlew runIde
```

## 项目结构

```
src/main/kotlin/com/dwai/idebridge/
├── DwaiPlugin.kt              # 插件入口
├── api/                       # 接口定义
├── bridge/                    # 核心逻辑（Diff/VFS/PSI）
├── executor/                  # 执行器（编辑器/终端/运行）
├── handler/                   # HTTP 路由处理
├── impl/                      # 接口实现
├── inspector/                 # 检查器（编辑器/项目/报错/符号）
├── mcp/                       # MCP 协议服务
├── model/                     # 数据模型
├── ui/                        # 对话框 UI
└── util/                      # 工具类
```

## License

MIT
