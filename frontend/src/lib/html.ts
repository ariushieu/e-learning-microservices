/**
 * Nội dung thông báo có thẻ <b> từ mẫu, nhưng tên khóa học / bài kiểm tra chèn vào không được
 * backend escape. Escape toàn bộ rồi chỉ trả lại đúng thẻ <b>, để tên khóa có "<script>" vẫn hiện
 * ra như chữ thường.
 */
export function safeNotificationHtml(content: string): string {
  return content
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/&lt;(\/?)b&gt;/g, "<$1b>");
}

/** Nội dung thông báo dạng chữ thường, cho chỗ không hiện HTML (toast). */
export function notificationText(content: string): string {
  return content.replace(/<\/?b>/g, "");
}
