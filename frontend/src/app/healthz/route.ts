// Healthcheck của Docker: chỉ hỏi server Next.js còn sống, không gọi sang gateway — gateway
// chết thì web vẫn phải lên để báo lỗi tử tế cho người dùng.
export function GET() {
  return Response.json({ status: "UP" });
}
