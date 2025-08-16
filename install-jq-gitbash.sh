set -euo pipefail

# 1) 설치 경로(사용자 권한으로 안전)
INSTALL_DIR="$HOME/bin"
mkdir -p "$INSTALL_DIR"

# 2) jq 다운로드 (64-bit용 예시 URL)
JQ_URL="https://github.com/jqlang/jq/releases/download/jq-1.7.1/jq-win64.exe"
echo "[INFO] Downloading jq from: $JQ_URL"
curl -L -o "$INSTALL_DIR/jq.exe" "$JQ_URL"

# 3) 실행 권한 부여
chmod +x "$INSTALL_DIR/jq.exe"

# 4) PATH 등록 (중복 방지)
PROFILE="$HOME/.bashrc"
if ! grep -q 'export PATH="$HOME/bin:$PATH"' "$PROFILE" 2>/dev/null; then
  echo 'export PATH="$HOME/bin:$PATH"' >> "$PROFILE"
  ADDED=1
else
  ADDED=0
fi

echo
echo "✅ jq installed: $INSTALL_DIR/jq.exe"
echo "   Version check (current shell):"
export PATH="$HOME/bin:$PATH"
"$INSTALL_DIR/jq.exe" --version || true

if [ "$ADDED" -eq 1 ]; then
  echo
  echo "ℹ️  PATH가 ~/.bashrc에 추가되었습니다. 새 Git Bash를 열면 자동 적용됩니다."
  echo "    (현재 세션에는 아래 명령으로 즉시 반영됨)"
  echo '    export PATH="$HOME/bin:$PATH"'
fi
