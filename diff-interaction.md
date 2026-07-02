# Hermes Diff 交互逻辑文档

## 概述

Hermes 插件的 Diff 交互采用 **"预览 → 统一审批"** 模式：

1. 代码块提供"查看差异"按钮，仅打开 Diff 预览，**不写盘**
2. 所有待审批文件汇总在底部**待办列表**中
3. 用户在待办列表中统一审批（接受/拒绝），支持单个操作和批量操作

---

## 交互流程

```
用户看到代码块 [查看差异] 按钮
  → 点击 → bridge.showDiff(filePath, code)
  → 后端打开 IntelliJ Diff 窗口，创建 PendingEdit，推送待办列表

待办列表出现文件项
  → 点击文件名 → 重新打开 Diff 预览
  → 点击 ✓ → acceptTodo → 后端写盘 + 关闭 Diff + 推送 diff_accepted
  → 点击 ✕ → rejectTodo → 后端关闭 Diff + 推送 diff_rejected
  → 全部接受/全部拒绝 → 批量操作

代码块状态徽章：
  pending → 显示"查看差异"按钮
  applied → 显示"已应用"绿色徽章
  rejected → 显示"已拒绝"红色徽章
```

---

## 文件结构

```
webview/src/
├── App.tsx                    # 主组件：状态管理 + 事件处理
├── bridge.ts                  # Kotlin 桥接层
├── components/
│   ├── CodeBlock.tsx          # 代码块组件
│   ├── TodoListBar.tsx        # 待办列表组件
│   └── ChatMessage.tsx        # 消息渲染（透传回调）
└── styles/
    ├── CodeBlock.css          # 代码块样式
    └── TodoListBar.css        # 待办列表样式
```

---

## 1. bridge.ts — 桥接层

`callKotlinBridge` 通过 `window.hermesQuery` 调用 Kotlin 后端，格式为 `action|data`。

```typescript
async showDiff(filePath: string, code: string): Promise<{ok:boolean; error?:string}> {
    const result = await callKotlinBridge<{ ok: boolean; error?: string }>('showDiff', `${filePath}|${code}`)
    return result
}
```

---

## 2. App.tsx — 状态管理与事件处理

### 核心数据结构

```typescript
interface CodeBlockState {
  id: string
  language: string
  code: string
  filePath?: string
  status: 'pending' | 'applied' | 'rejected'  // 三大状态
}

interface TodoItem {
  filePath: string
  fileName: string
  newCreate: boolean
  insertLineCount: number
  deleteLineCount: number
}
```

### 关键回调函数

```typescript
// 代码块上的"查看差异"按钮 → 仅打开 Diff 预览，不写盘
const handleViewDiff = useCallback(async (msgIndex: number, blockIndex: number) => {
    const block = messages[msgIndex]?.codeBlocks?.[blockIndex]
    if (!block) return
    try {
        await bridge.showDiff(block.filePath || '', block.code)
    } catch (e: any) {
        alert('打开 Diff 失败: ' + e.message)
    }
}, [messages])

// 待办列表 ✓ 接受单个文件 → 触发后端写盘 + 关闭 Diff
const handleTodoAccept = useCallback(async (filePath: string) => {
    try {
        await callKotlinBridge('acceptTodo', JSON.stringify(filePath))
    } catch (e) {
        console.error('[Hermes] acceptTodo error:', e)
    }
}, [])

// 待办列表 ✕ 拒绝单个文件 → 触发后端关闭 Diff（不写盘）
const handleTodoReject = useCallback(async (filePath: string) => {
    try {
        await callKotlinBridge('rejectTodo', JSON.stringify(filePath))
    } catch (e) {
        console.error('[Hermes] rejectTodo error:', e)
    }
}, [])

// 全部接受
const handleTodoAcceptAll = useCallback(async () => {
    await callKotlinBridge('acceptAllTodos')
}, [])

// 全部拒绝
const handleTodoRejectAll = useCallback(async () => {
    await callKotlinBridge('rejectAllTodos')
}, [])

// 点击待办项文件名 → 重新打开 Diff 预览
const handleTodoOpenDiff = useCallback(async (filePath: string) => {
    await callKotlinBridge('openDiffEditor', JSON.stringify(filePath))
}, [])
```

### 后端事件处理

```typescript
// 后端推送待办列表更新
if (evt.type === 'updateTodoList') {
    if (evt.items && Array.isArray(evt.items)) {
        setTodoItems(evt.items.map((item: any) => ({
            filePath: item.filePath || '',
            fileName: item.fileName || '',
            newCreate: !!item.newCreate,
            insertLineCount: item.insertLineCount || 0,
            deleteLineCount: item.deleteLineCount || 0,
        })))
    }
}

// 接受/拒绝后更新代码块状态徽章
if (evt.type === 'diff_accepted' || evt.type === 'diff_rejected') {
    const filePath = evt.filePath
    const newStatus = evt.type === 'diff_accepted' ? 'applied' : 'rejected'
    setMessages(prev => {
        const updated = [...prev]
        for (let i = 0; i < updated.length; i++) {
            const blocks = updated[i].codeBlocks
            if (blocks) {
                for (let j = 0; j < blocks.length; j++) {
                    if (blocks[j].filePath === filePath && blocks[j].status === 'pending') {
                        const newBlocks = [...blocks]
                        newBlocks[j] = { ...newBlocks[j], status: newStatus }
                        updated[i] = { ...updated[i], codeBlocks: newBlocks }
                        return updated
                    }
                }
            }
        }
        return prev
    })
    return
}

// 文件编辑事件 → 新增代码块
case 'file_edit': {
    const ext = (evt.filePath || '').split('.').pop()?.toLowerCase() || ''
    const langMap: Record<string, string> = {
        py: 'python', js: 'javascript', ts: 'typescript', tsx: 'typescript',
        jsx: 'javascript', java: 'java', kt: 'kotlin', go: 'go', rs: 'rust',
        html: 'html', css: 'css', json: 'json', yaml: 'yaml', yml: 'yaml',
        md: 'markdown', sql: 'sql', sh: 'bash', bash: 'bash'
    }
    state.codeBlocks = [...state.codeBlocks, {
        id: nextBlockId(),
        language: langMap[ext] || 'text',
        code: evt.fileContent || '',
        filePath: evt.filePath,
        status: 'pending' as const
    }]
    blocksChanged = true
    break
}
```

### JSX 连线

```tsx
// 代码块回调
<ChatMessage
    key={msg.id}
    message={msg}
    msgIndex={index}
    onViewDiff={(blockIndex) => handleViewDiff(index, blockIndex)}
    onAcpAction={handleAcpAction}
/>

// 待办列表回调
<TodoListBar
    items={todoItems}
    onAccept={handleTodoAccept}
    onReject={handleTodoReject}
    onAcceptAll={handleTodoAcceptAll}
    onRejectAll={handleTodoRejectAll}
    onOpenDiff={handleTodoOpenDiff}
/>
```

---

## 3. CodeBlock.tsx — 代码块组件

### Props

```typescript
interface CodeBlockProps {
  language: string
  code: string
  filePath?: string
  status?: 'pending' | 'applied' | 'rejected'
  onViewDiff?: () => void
  onAcpAction?: (action: 'explain' | 'test' | 'fix', code: string, language: string) => void
}
```

### 核心逻辑

**只有 `status === 'pending'` 且 `onViewDiff` 存在时才显示"查看差异"按钮。**

```tsx
{onViewDiff && status === 'pending' && (
    <button className="hermes-code-btn hermes-code-btn--view-diff" onClick={onViewDiff}>
        查看差异
    </button>
)}
{status === 'applied' && <span className="hermes-code-badge hermes-code-badge--applied">已应用</span>}
{status === 'rejected' && <span className="hermes-code-badge hermes-code-badge--rejected">已拒绝</span>}
```

### 完整组件

```tsx
export default function CodeBlock({ language, code, filePath, status, onViewDiff, onAcpAction }: CodeBlockProps) {
  const [copied, setCopied] = useState(false)

  const highlighted = useMemo(() => {
    try {
      const lang = language || 'plaintext'
      if (hljs.getLanguage(lang)) return hljs.highlight(code, { language: lang }).value
      return hljs.highlightAuto(code).value
    } catch {
      return code.replace(/</g, '&lt;').replace(/>/g, '&gt;')
    }
  }, [code, language])

  const handleCopy = () => {
    navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  const handleAction = (action: 'explain' | 'test' | 'fix') => {
    if (onAcpAction) onAcpAction(action, code, language)
  }

  const langLabel = language || 'text'
  const fileName = filePath?.split(/[/\\]/).pop()

  return (
    <div className="hermes-code-block">
      <div className="hermes-code-toolbar">
        <div className="hermes-code-toolbar-left">
          {fileName && <span className="hermes-code-filename">{fileName}</span>}
          <span className="hermes-code-lang">{langLabel}</span>
        </div>
        <div className="hermes-code-toolbar-right">
          <button className="hermes-code-btn" onClick={handleCopy}>
            {copied ? '已复制' : '复制'}
          </button>
          <button className="hermes-code-btn" onClick={() => handleAction('explain')}>
            解释
          </button>
          <button className="hermes-code-btn" onClick={() => handleAction('test')}>
            单测
          </button>
          <button className="hermes-code-btn" onClick={() => handleAction('fix')}>
            修复
          </button>
          {onViewDiff && status === 'pending' && (
            <button className="hermes-code-btn hermes-code-btn--view-diff" onClick={onViewDiff}>
              查看差异
            </button>
          )}
          {status === 'applied' && <span className="hermes-code-badge hermes-code-badge--applied">已应用</span>}
          {status === 'rejected' && <span className="hermes-code-badge hermes-code-badge--rejected">已拒绝</span>}
        </div>
      </div>

      <pre className="hermes-code-content">
        <code dangerouslySetInnerHTML={{ __html: highlighted }} />
      </pre>
    </div>
  )
}
```

---

## 4. TodoListBar.tsx — 待办列表组件

### Props

```typescript
interface TodoListBarProps {
  items: TodoItem[]
  onAccept: (filePath: string) => void
  onReject: (filePath: string) => void
  onAcceptAll: () => void
  onRejectAll: () => void
  onOpenDiff: (filePath: string) => void
}
```

### 核心交互

- **点击文件名** → `onOpenDiff(filePath)` 重新打开 Diff 预览
- **点击 ✓** → `onAccept(filePath)` 接受单个
- **点击 ✕** → `onReject(filePath)` 拒绝单个
- **全部接受/拒绝** → 对应回调

```tsx
export default function TodoListBar({ items, onAccept, onReject, onAcceptAll, onRejectAll, onOpenDiff }: TodoListBarProps) {
  if (items.length === 0) return null

  return (
    <div className="todo-list-bar">
      <div className="todo-list-header">
        <span className="todo-list-title">
          {items.length} 个文件变更
        </span>
        <div className="todo-list-actions">
          <button className="todo-btn todo-btn-accept-all" onClick={onAcceptAll}>
            全部接受
          </button>
          <button className="todo-btn todo-btn-reject-all" onClick={onRejectAll}>
            全部拒绝
          </button>
        </div>
      </div>
      <div className="todo-list-items">
        {items.map(item => (
          <div key={item.filePath} className="todo-item" onClick={() => onOpenDiff(item.filePath)}>
            <div className="todo-item-info">
              <span className="todo-item-name">{item.fileName}</span>
              <span className="todo-item-stats">
                {item.newCreate ? (
                  <span className="todo-stat todo-stat-add">+{item.insertLineCount}</span>
                ) : (
                  <>
                    <span className="todo-stat todo-stat-add">+{item.insertLineCount}</span>
                    <span className="todo-stat todo-stat-del">-{item.deleteLineCount}</span>
                  </>
                )}
              </span>
            </div>
            <div className="todo-item-actions" onClick={e => e.stopPropagation()}>
              <button className="todo-btn todo-btn-accept" onClick={() => onAccept(item.filePath)}>
                ✓
              </button>
              <button className="todo-btn todo-btn-reject" onClick={() => onReject(item.filePath)}>
                ✕
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
```

---

## 5. ChatMessage.tsx — 消息渲染（透传回调）

```tsx
interface ChatMessageProps {
  message: Message
  msgIndex: number
  onViewDiff?: (blockIndex: number) => void
  onAcpAction?: (action: 'explain' | 'test' | 'fix', code: string, language: string) => void
}

// 渲染代码块时透传回调
<CodeBlock
    key={`c-${i}`}
    language={block?.language || 'text'}
    code={part.content}
    filePath={block?.filePath}
    status={block?.status}
    onViewDiff={() => onViewDiff?.(part.blockIndex!)}
    onAcpAction={onAcpAction}
/>
```

---

## 关键设计决策

1. **预览不写盘**：点击"查看差异"只打开 Diff 窗口，不修改文件，避免误操作
2. **统一审批**：所有待审批文件集中在底部待办列表，支持单文件审批和批量操作
3. **状态追踪**：每个代码块有 `pending → applied/rejected` 三态，用户可清晰看到每个文件的审批状态
4. **事件驱动**：后端通过 `diff_accepted` / `diff_rejected` / `updateTodoList` 事件驱动前端状态更新
5. **多文件独立**：点击文件名可单独预览某个文件的 Diff，每个文件的审批操作互不影响