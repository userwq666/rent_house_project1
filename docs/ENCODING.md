# 编码规范与终端设置

项目统一使用 `UTF-8`。

## 1. 基本约定

- 文本文件编码：`UTF-8`
- 换行符：`LF`
- 约束文件：`.editorconfig`、`.gitattributes`

## 2. Windows / PowerShell 建议

执行命令前先切换代码页：

```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

读取文件时建议显式指定编码：

```powershell
Get-Content -Raw -Encoding UTF8 README.md
```

## 3. 常见乱码原因

- 文件是 UTF-8，但终端按 GBK/ANSI 解码显示。
- IDE 打开文件时未使用 UTF-8。

## 4. IDE 建议

- IntelliJ IDEA: `File | Settings | Editor | File Encodings` 全部设为 `UTF-8`
- VS Code: `files.encoding` 设为 `utf8`
