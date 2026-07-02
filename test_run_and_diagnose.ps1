$headers = @{
    "Content-Type" = "application/json"
}
$body = @{
    jsonrpc = "2.0"
    id = "1"
    method = "tools/call"
    params = @{
        name = "run.start"
        arguments = @{
            file_path = "D:/cursor-bridge-plugin/test_error.py"
        }
    }
} | ConvertTo-Json -Compress

Write-Host "=== 调用 run.start 运行测试脚本 ==="
$response = Invoke-WebRequest -Uri "http://127.0.0.1:8765/mcp" -Method POST -Headers $headers -Body $body
Write-Host $response.Content
Write-Host ""

Start-Sleep -Seconds 5

Write-Host "=== 调用 diagnostics.get_all 获取诊断结果 ==="
$body2 = @{
    jsonrpc = "2.0"
    id = "2"
    method = "tools/call"
    params = @{
        name = "diagnostics.get_all"
        arguments = @{}
    }
} | ConvertTo-Json -Compress

$response2 = Invoke-WebRequest -Uri "http://127.0.0.1:8765/mcp" -Method POST -Headers $headers -Body $body2
Write-Host $response2.Content