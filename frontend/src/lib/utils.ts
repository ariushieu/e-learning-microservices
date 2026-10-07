import { createCn } from "cn/config";

/**
 * Ghép class Tailwind và bỏ class bị ghi đè. Phải khai báo thang chữ tự định nghĩa
 * (globals.css) là cỡ chữ; không thì `text-display` bị coi là màu chữ và bị xóa khi ghép với
 * `text-primary`.
 */
export const cn = createCn({
  extend: { classGroups: { "font-size": [{ text: ["display", "title", "heading", "subheading", "caption", "eyebrow"] }] } },
});
