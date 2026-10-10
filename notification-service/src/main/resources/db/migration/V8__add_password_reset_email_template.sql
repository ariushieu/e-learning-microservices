-- Thư đặt lại mật khẩu. Luôn gửi, kể cả khi người dùng tắt email thông báo: chính họ yêu cầu.
INSERT INTO notification_templates (code, channel, title_template, body_template)
VALUES ('PASSWORD_RESET', 'EMAIL', 'Đặt lại mật khẩu E-Learning HUNRE',
        'Chào {fullName},\n\nCó người vừa yêu cầu đặt lại mật khẩu cho tài khoản {email}. Bấm vào liên kết dưới đây để đặt mật khẩu mới:\n\n{url}\n\nLiên kết dùng được một lần, hết hạn lúc {expiresAt} (giờ Việt Nam). Nếu không phải bạn yêu cầu, hãy bỏ qua thư này; mật khẩu hiện tại vẫn giữ nguyên.\n\nE-Learning HUNRE');
