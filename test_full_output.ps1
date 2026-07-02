$headers = @{
    "Content-Type" = "application/json"
}

$body = @{
    jsonrpc = "2.0"
    id = "3"
    method = "tools/call"
    params = @{
        name = "diagnostics.get_full_output"
        arguments = @{}
    }
} | ConvertTo-Json -Compress

$response = Invoke-WebRequest -Uri "http://127.0.0.1:8765/mcp" -Method POST -Headers $headers -Body $body -UseBasicParsing
$result = $response.Content | ConvertFrom-Json
$text = $result.result.content[0].text | ConvertFrom-Json
Write-Host "=== run_output ==="
$text.run_output | ConvertTo-Json -Depth 5