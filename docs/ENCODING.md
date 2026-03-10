# 编码规范与乱码排查

本项目统一使用 `UTF-8` 编码。

## 项目约定

- 文本文件统一 `UTF-8`
- 换行符默认 `LF`
- 已通过 `.editorconfig` 和 `.gitattributes` 约束

## Windows / PowerShell 建议

在终端查看文件前，先切换代码页并设置输出编码：

```powershell
chcp 65001
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
```

读取文本时建议显式指定 UTF-8：

```powershell
Get-Content -Raw -Encoding UTF8 README.md
```

## 常见“伪乱码”现象

若看到类似 `鐢ㄦ埛`、`鍚堝悓` 这类文本，通常是 UTF-8 文件被按 GBK/ANSI 方式读取导致，不代表文件本身损坏。

## IDE 配置建议

- IntelliJ IDEA: `File | Settings | Editor | File Encodings` 全部设置为 `UTF-8`
- VS Code: `files.encoding` 设置为 `utf8`
