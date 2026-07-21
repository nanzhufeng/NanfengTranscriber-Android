#!/bin/zsh
set -euo pipefail

KEYCHAIN_SERVICE='com.nanzhufeng.transcriber.signing'
KEYCHAIN_ACCOUNT='keystore-password'

SIGNING_PASSWORD="$(/usr/bin/security find-generic-password \
  -w \
  -a "$KEYCHAIN_ACCOUNT" \
  -s "$KEYCHAIN_SERVICE")"

printf '%s' "$SIGNING_PASSWORD" | /usr/bin/pbcopy
unset SIGNING_PASSWORD

/usr/bin/osascript -e 'display notification "已复制到剪贴板，可粘贴到目标平台；不会写入项目文件。" with title "南枫转写签名密码"'
