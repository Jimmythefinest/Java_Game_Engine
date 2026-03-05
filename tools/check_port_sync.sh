#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

MAIN_DIR="$ROOT_DIR/src/com/njst/gaming"
ANDROID_DIR="$ROOT_DIR/android/app/src/main/java/com/njst/gaming"
MAIN_SHADERS="$ROOT_DIR/src/resources/shaders"
ANDROID_GLSL_DIR="$ROOT_DIR/android/app/src/main/java/com"

if [[ ! -d "$MAIN_DIR" || ! -d "$ANDROID_DIR" ]]; then
  echo "Expected directories not found:"
  echo "  $MAIN_DIR"
  echo "  $ANDROID_DIR"
  exit 1
fi

tmp_dir="$(mktemp -d)"
trap 'rm -rf "$tmp_dir"' EXIT

main_list="$tmp_dir/main_files.txt"
android_list="$tmp_dir/android_files.txt"
common_list="$tmp_dir/common_files.txt"
drift_list="$tmp_dir/drift_files.txt"

(cd "$ROOT_DIR/src" && rg --files com/njst/gaming | sort) > "$main_list"
(cd "$ROOT_DIR/android/app/src/main/java" && rg --files com/njst/gaming | sort) > "$android_list"

comm -12 "$main_list" "$android_list" > "$common_list"
: > "$drift_list"

while IFS= read -r f; do
  if ! cmp -s "$ROOT_DIR/src/$f" "$ROOT_DIR/android/app/src/main/java/$f"; then
    echo "$f" >> "$drift_list"
  fi
done < "$common_list"

echo "=== CODE DRIFT ==="
echo "only-in-main:    $(comm -23 "$main_list" "$android_list" | wc -l)"
echo "only-in-android: $(comm -13 "$main_list" "$android_list" | wc -l)"
echo "shared-paths:    $(wc -l < "$common_list")"
echo "content-drift:   $(wc -l < "$drift_list")"
echo

echo "--- Only in main ---"
comm -23 "$main_list" "$android_list" || true
echo
echo "--- Only in android ---"
comm -13 "$main_list" "$android_list" || true
echo
echo "--- Shared but different ---"
cat "$drift_list" || true
echo

if [[ -d "$MAIN_SHADERS" && -d "$ANDROID_GLSL_DIR" ]]; then
  main_shaders="$tmp_dir/main_shaders.txt"
  android_shaders="$tmp_dir/android_shaders.txt"
  common_shaders="$tmp_dir/common_shaders.txt"
  drift_shaders="$tmp_dir/drift_shaders.txt"

  (cd "$MAIN_SHADERS" && rg --files | sort) > "$main_shaders"
  (cd "$ANDROID_GLSL_DIR" && rg --files -g '*.glsl' | sort) > "$android_shaders"
  comm -12 "$main_shaders" "$android_shaders" > "$common_shaders"
  : > "$drift_shaders"

  while IFS= read -r f; do
    if ! cmp -s "$MAIN_SHADERS/$f" "$ANDROID_GLSL_DIR/$f"; then
      echo "$f" >> "$drift_shaders"
    fi
  done < "$common_shaders"

  echo "=== SHADER DRIFT ==="
  echo "only-in-main:    $(comm -23 "$main_shaders" "$android_shaders" | wc -l)"
  echo "only-in-android: $(comm -13 "$main_shaders" "$android_shaders" | wc -l)"
  echo "shared-paths:    $(wc -l < "$common_shaders")"
  echo "content-drift:   $(wc -l < "$drift_shaders")"
  echo

  echo "--- Shaders only in main ---"
  comm -23 "$main_shaders" "$android_shaders" || true
  echo
  echo "--- Shaders only in android ---"
  comm -13 "$main_shaders" "$android_shaders" || true
  echo
  echo "--- Shared shaders with different content ---"
  cat "$drift_shaders" || true
fi
