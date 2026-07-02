$headers = @{ "Content-Type" = "application/json" }

# 1. 运行报错脚本
$body1 = @{
    jsonrpc = "2.0"
    id = "1"
    method = "tools/call"
    params = @{
        name = "run.start"
        arguments = @{ file_path = "D:/cursor-bridge-plugin/test_error.py" }
    }
} | ConvertTo-Json -Depth 3 -Compress

Write-Host ">>> 运行 test_error.py..."
Invoke-RestMethod -Uri "http://127.0.0.1:8765/mcp" -Method POST -Headers $headers -Body $body1 | Out-Null

Write-Host ">>> 等待 6 秒..."
Start-Sleep -Seconds 6

# 2. 查诊断
$body2 = @{
    jsonrpc = "2.0"
    id = "2"
    method = "tools/call"
    params = @{
        name = "diagnostics.get_all"
        arguments = @{}
    }
} | ConvertTo-Json -Depth 3 -Compress

$r = Invoke-RestMethod -Uri "http://127.0.0.1:8765/mcp" -Method POST -Headers $headers -Body $body2
$text = $r.result.content[0].text | ConvertFrom-Json

Write-Host "`n=== run_output.messages ==="
$text.run_output.messages | ConvertTo-Json -Depth 5