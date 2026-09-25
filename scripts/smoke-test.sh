#!/usr/bin/env bash
#
# Gọi thử mỗi service một lần QUA GATEWAY, để biết cả hệ thống đã lên và nói chuyện được với
# nhau chưa. Không phải bộ test nghiệp vụ — chỉ trả lời câu "có service nào chết hay không
# với tới database không".
#
#   docker compose --profile app up -d --build --wait
#   bash scripts/smoke-test.sh
#
# Chạy được cả khi các service chạy trong IntelliJ, miễn gateway ở cổng 8080. Đổi địa chỉ
# gateway bằng biến GATEWAY:
#
#   GATEWAY=http://localhost:9000 bash scripts/smoke-test.sh
#
# Mỗi endpoint dưới đây đều đọc database của service đó, nên trả 200 nghĩa là cả ba chặng
# gateway → service → MySQL đều thông. Healthcheck của Docker không nói được điều này: nó
# chỉ hỏi service còn sống không, không hỏi gateway có tìm được service không.

set -uo pipefail

GATEWAY="${GATEWAY:-http://localhost:8080}"
EMAIL="smoke$(date +%s)$RANDOM@hunre.edu.vn"
PASSWORD="Smoke@12345"
FAILED=0

# check <mô tả> <mã mong đợi> <curl args...>
check() {
    local label="$1" expected="$2"
    shift 2
    local status
    status=$(curl -s -o /tmp/smoke-body.$$ -w '%{http_code}' --max-time 15 "$@")
    if [ "$status" = "$expected" ]; then
        printf '  OK   %s  %s\n' "$status" "$label"
    else
        printf '  SAI  %s  %s (mong %s)\n' "$status" "$label" "$expected"
        head -c 300 /tmp/smoke-body.$$ | sed 's/^/         /'
        echo
        FAILED=1
    fi
}

echo "Gateway: $GATEWAY"

echo "gateway"
check "GET /actuator/health" 200 "$GATEWAY/actuator/health"

echo "auth-service"
check "POST /api/auth/register" 201 -X POST "$GATEWAY/api/auth/register" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\",\"fullName\":\"Smoke Test\"}"

TOKEN=$(curl -s --max-time 15 -X POST "$GATEWAY/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}" \
    | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')

if [ -z "$TOKEN" ]; then
    echo "  SAI  không đăng nhập được, không lấy được token — dừng ở đây"
    exit 1
fi
echo "  OK   200  POST /api/auth/login"
AUTH=(-H "Authorization: Bearer $TOKEN")

check "GET /api/auth/me" 200 "${AUTH[@]}" "$GATEWAY/api/auth/me"

echo "course-service"
check "GET /api/courses (công khai, không token)" 200 "$GATEWAY/api/courses"
check "GET /api/categories (công khai, không token)" 200 "$GATEWAY/api/categories"

echo "enrollment-service"
check "GET /api/enrollments/my-courses" 200 "${AUTH[@]}" "$GATEWAY/api/enrollments/my-courses"

echo "quiz-service"
check "GET /api/quizzes/course/1" 200 "${AUTH[@]}" "$GATEWAY/api/quizzes/course/1"

echo "notification-service"
check "GET /api/notifications" 200 "${AUTH[@]}" "$GATEWAY/api/notifications"

echo "xác thực"
check "GET /api/notifications không token phải bị chặn" 401 "$GATEWAY/api/notifications"

rm -f /tmp/smoke-body.$$

# Dọn tài khoản vừa tạo nếu MySQL chạy trong compose, để chạy nhiều lần không để lại rác trong
# database dev. Dùng mật khẩu root ngay trong container nên đổi mật khẩu ở .env vẫn chạy đúng.
if docker compose ps --status running --services 2>/dev/null | grep -qx mysql; then
    docker compose exec -T mysql sh -c "mysql -uroot -p\"\$MYSQL_ROOT_PASSWORD\" auth_db -e \"
        SET @u := (SELECT id FROM users WHERE email = '$EMAIL');
        DELETE FROM refresh_tokens WHERE user_id = @u;
        DELETE FROM user_roles     WHERE user_id = @u;
        DELETE FROM users          WHERE id = @u;\"" 2>/dev/null \
        && echo "(đã xóa tài khoản thử $EMAIL)"
fi

if [ "$FAILED" -ne 0 ]; then
    echo "CÓ LỖI — xem log: docker compose logs <tên-service>"
    exit 1
fi
echo "Cả 6 service trả lời qua gateway."
