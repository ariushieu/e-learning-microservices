#!/usr/bin/env bash
# Kiểm tra một dòng tiêu đề commit (hoặc tiêu đề pull request) có đúng quy ước không.
#
# Dùng chung cho hai nơi, để quy tắc chỉ được định nghĩa một lần:
#   - .githooks/commit-msg  : chặn ngay trên máy lập trình viên
#   - CI job "Commit style" : chặn ở pull request, phòng khi ai đó bỏ qua hook
#
# Dùng: bash scripts/check-commit-subject.sh "<tiêu đề>" ["<nguồn hiển thị khi báo lỗi>"]

set -uo pipefail

SUBJECT="${1-}"
SOURCE="${2-Tiêu đề commit}"

TYPES='feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert'
MAX_LENGTH=72

fail() {
    echo "" >&2
    echo "❌ $SOURCE không đạt quy ước:" >&2
    echo "   $SUBJECT" >&2
    echo "" >&2
    echo "   Lý do: $1" >&2
    echo "" >&2
    echo "   Định dạng bắt buộc:  <loại>(<phạm vi tùy chọn>): <mô tả bằng tiếng Anh>" >&2
    echo "   Loại hợp lệ:         ${TYPES//|/, }" >&2
    echo "" >&2
    echo "   Ví dụ đúng:" >&2
    echo "     feat(auth): add login endpoint with JWT" >&2
    echo "     fix(course): reject duplicate slug on update" >&2
    echo "     docs: describe the enrollment event flow" >&2
    echo "" >&2
    exit 1
}

# Bỏ qua các commit do git tự sinh
case "$SUBJECT" in
    "Merge "* | "Revert "* | "fixup!"* | "squash!"*) exit 0 ;;
esac

[ -n "$SUBJECT" ] || fail "tiêu đề đang để trống"

# Chỉ cho phép ký tự ASCII in được. Lịch sử git của dự án bắt buộc dùng tiếng Anh,
# nên tiếng Việt có dấu sẽ bị chặn ở đây.
if printf '%s' "$SUBJECT" | LC_ALL=C grep -q '[^ -~]'; then
    fail "có ký tự ngoài bảng ASCII. Lịch sử git của dự án chỉ dùng tiếng Anh, không dấu tiếng Việt và không emoji"
fi

# Tiếng Việt viết không dấu lọt qua được bộ lọc ASCII ở trên, nên dò thêm các cụm
# từ hai tiếng thường gặp. Dùng cụm thay vì từ đơn để không báo nhầm tiếng Anh
# (ví dụ "them" vừa là tiếng Việt vừa là đại từ tiếng Anh).
# Đây là lưới an toàn, không phải bộ kiểm tra ngôn ngữ đầy đủ.
VIETNAMESE='chuc nang|nguoi dung|dang nhap|dang ky|dang xuat|co so du lieu|cap nhat'
VIETNAMESE="$VIETNAMESE|sua loi|hoan thanh|thong bao|khoa hoc|bai hoc|kiem tra|xu ly"
VIETNAMESE="$VIETNAMESE|giao dien|man hinh|thay doi|chinh sua|them moi|tinh nang"
VIETNAMESE="$VIETNAMESE|du lieu|he thong|quan ly|hien thi|danh sach|tim kiem"
VIETNAMESE="$VIETNAMESE|phan quyen|ghi danh|tien do|chung chi|mat khau|tai khoan"
VIETNAMESE="$VIETNAMESE|noi dung|trang chu|sua lai|toi uu|trien khai|cai dat"

if printf '%s' "$SUBJECT" | tr '[:upper:]' '[:lower:]' | grep -Eq "$VIETNAMESE"; then
    fail "có vẻ đang viết tiếng Việt không dấu. Lịch sử git của dự án chỉ dùng tiếng Anh"
fi

if ! printf '%s' "$SUBJECT" | grep -Eq "^($TYPES)(\([a-z0-9._/-]+\))?!?: .+"; then
    fail "thiếu tiền tố loại commit, hoặc thiếu dấu hai chấm và khoảng trắng sau nó"
fi

length=${#SUBJECT}
if [ "$length" -gt "$MAX_LENGTH" ]; then
    fail "dài $length ký tự, tối đa $MAX_LENGTH. Đưa phần chi tiết xuống phần thân commit"
fi

case "$SUBJECT" in
    *.) fail "không đặt dấu chấm ở cuối tiêu đề" ;;
esac

exit 0
