#!/usr/bin/env bash
#
# Chặn việc tắt xác thực trong cấu hình được commit vào repo.
#
#   bash scripts/check-security-config.sh
#
# Vì sao cần: elearning.security.enabled=false không làm service trả lỗi — nó làm service
# gán cho MỌI request cùng một danh tính giả lập. Mọi endpoint vẫn trả 200, test vẫn xanh,
# chỉ có điều dữ liệu của mọi người dồn về một tài khoản. PR #29 từng đặt dòng này vào
# application.properties của enrollment-service để tiện test Postman không cần token: học
# viên A ghi danh thì database lưu user_id = 1, học viên B chưa ghi danh bao giờ lại nhận
# 409 "đã đăng ký rồi". Cả 7 job CI khi đó đều xanh.
#
# Được phép tắt ở đâu:
#   - src/test/resources  — test tắt có chủ đích, không đụng tới.
#   - application-local.properties — đã .gitignore, không bao giờ vào repo. Đây là chỗ
#     CONTRIBUTING mục 8 hướng dẫn đặt khi muốn test không cần token.
#
# Script chỉ đọc file git đang theo dõi (git ls-files), nên file local của từng người
# không bao giờ làm CI đỏ.

set -uo pipefail
cd "$(git rev-parse --show-toplevel)"

FOUND=0

report() {
    local file="$1" line="$2" text="$3"
    printf '  %s:%s\n      %s\n' "$file" "$line" "$text"
    FOUND=1
}

# Cờ trong file cấu hình Spring của mã chạy thật. Bắt các cách viết đều hợp lệ trong
# .properties: dấu = hoặc :, có hay không khoảng trắng, FALSE viết hoa, và giá trị mặc
# định qua biến môi trường ${...:false} — bỏ quên biến là tắt.
PROP='^[[:space:]]*elearning\.security\.enabled[[:space:]]*[=:][[:space:]]*(false|\$\{[^}]*:false\})[[:space:]]*$'

while IFS= read -r file; do
    [ -f "$file" ] || continue
    while IFS=: read -r line text; do
        report "$file" "$line" "$text"
    done < <(grep -inE "$PROP" "$file")
done < <(git ls-files '*/src/main/resources/application*.properties')

# Spring cũng nhận cờ này qua biến môi trường ELEARNING_SECURITY_ENABLED, nên đặt nó trong
# compose hay file env mẫu cũng tắt xác thực y như vậy.
ENV='ELEARNING_SECURITY_ENABLED[[:space:]]*[:=][[:space:]]*["'"'"']?false'

while IFS= read -r file; do
    [ -f "$file" ] || continue
    while IFS=: read -r line text; do
        # Bỏ qua dòng comment
        [[ "$text" =~ ^[[:space:]]*# ]] && continue
        report "$file" "$line" "$text"
    done < <(grep -inE "$ENV" "$file")
done < <(git ls-files 'docker-compose*.yml' 'docker-compose*.yaml' '.env.example')

if [ "$FOUND" -ne 0 ]; then
    cat <<'MSG'

Xác thực đang bị tắt trong cấu hình được commit (xem các dòng trên).

Khi tắt, service không đọc token nữa mà coi mọi request là cùng một người dùng giả lập,
nên dữ liệu của mọi tài khoản dồn về một chỗ — mà không endpoint nào báo lỗi.

Muốn test không cần token trên máy mình thì làm đủ hai bước:
  1. đặt cờ trong <service>/src/main/resources/application-local.properties
     (đã được .gitignore, không bao giờ bị commit);
  2. chạy service với biến môi trường SPRING_PROFILES_ACTIVE=local — thiếu bước này thì
     Spring bỏ qua file ở bước 1.
Chi tiết ở CONTRIBUTING.md mục 8, phần "Test bằng Postman".
MSG
    exit 1
fi

echo "Xác thực không bị tắt trong cấu hình nào được commit."
