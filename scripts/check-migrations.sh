#!/usr/bin/env bash
#
# Chặn việc sửa file migration đã có trong main.
#
# Vì sao. Flyway ghi một checksum của nội dung file vào bảng flyway_schema_history lúc áp
# dụng. Sửa file đó về sau thì máy nào đã chạy nó sẽ báo checksum mismatch và service không
# khởi động; máy nào chưa chạy lại nhận schema khác hẳn. Kết quả là mỗi người một schema mà
# không ai biết, cho tới lúc có người gặp lỗi lạ.
#
# Job "Schema matches entities" KHÔNG bắt được chuyện này, vì CI luôn dựng database mới
# tinh nên bản migration mới lúc nào cũng chạy trót lọt. Lỗi chỉ hiện trên máy đã có sẵn
# database — tức là máy của mọi người trừ người viết.
#
# Muốn đổi schema thì thêm file mới V<n>__*.sql với các câu ALTER TABLE.
#
# Dùng ở máy mình (so nhánh hiện tại với main):
#   bash scripts/check-migrations.sh
#
# Dùng trong CI:
#   bash scripts/check-migrations.sh <base-sha> <head-sha>

set -euo pipefail

BASE="${1:-origin/main}"
HEAD_REF="${2:-HEAD}"

MIGRATION_GLOB='*/src/main/resources/db/migration/*'

# --diff-filter=MDR: Modified, Deleted, Renamed. Thêm file mới (A) thì hoàn toàn bình thường,
# đó chính là cách đúng để đổi schema.
changed="$(git diff --name-status --diff-filter=MDR "${BASE}...${HEAD_REF}" -- "$MIGRATION_GLOB" || true)"

if [ -z "$changed" ]; then
    printf '\033[32m✔ Không có file migration nào bị sửa hay xóa.\033[0m\n'
    exit 0
fi

printf '\033[31m✘ Pull request này sửa file migration đã có trong main:\033[0m\n\n'

while IFS=$'\t' read -r status file rest; do
    [ -n "${file:-}" ] || continue
    case "$status" in
        M*) label="đã sửa nội dung" ;;
        D*) label="đã xóa" ;;
        R*) label="đã đổi tên thành ${rest:-?}" ;;
        *)  label="$status" ;;
    esac
    printf '     %s  (%s)\n' "$file" "$label"
done <<< "$changed"

cat <<'HUONGDAN'

   Flyway lưu checksum của từng file migration lúc áp dụng. Sửa file đã chạy rồi thì:

     - máy nào đã chạy nó  -> service không khởi động, báo checksum mismatch
     - máy nào chưa chạy   -> nhận một schema khác, không ai biết là đã lệch

   Cách đúng: giữ nguyên file cũ, thêm file mới với ALTER TABLE.

     course-service/src/main/resources/db/migration/V3__them_cot_thumbnail.sql

         ALTER TABLE courses ADD COLUMN thumbnail_url VARCHAR(500) NULL;

   Nếu cả nhóm thống nhất viết lại migration cũ (ví dụ nó mới vào main và chắc chắn chưa
   ai chạy), thì vẫn merge được — check này chỉ cảnh báo chứ không khóa. Nhưng phải báo
   mọi người cùng DROP DATABASE một lượt, đừng để lặng lẽ trong một PR tính năng.

HUONGDAN

exit 1
