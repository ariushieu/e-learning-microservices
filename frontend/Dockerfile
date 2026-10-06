# syntax=docker/dockerfile:1
#
# Image của frontend Next.js. Build context là thư mục frontend/:
#
#   docker build -t elearning/frontend frontend
#
# Bình thường không cần gõ lệnh này — docker compose --profile app đã khai sẵn.

FROM node:24-alpine AS base
# Dùng đúng phiên bản pnpm ghi trong package.json (trường packageManager).
RUN corepack enable
WORKDIR /app

# =============================================================================
# Giai đoạn 1: cài thư viện. Chép riêng file khóa để Docker giữ cache khi chỉ sửa code.
# =============================================================================
FROM base AS deps
COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

# =============================================================================
# Giai đoạn 2: build
# =============================================================================
FROM base AS build
COPY --from=deps /app/node_modules ./node_modules
COPY . .
ENV NEXT_TELEMETRY_DISABLED=1
RUN pnpm build

# =============================================================================
# Giai đoạn 3: chạy. Chỉ có server.js + thư viện cần thiết, không có mã nguồn hay pnpm.
# =============================================================================
FROM node:24-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production \
    NEXT_TELEMETRY_DISABLED=1 \
    PORT=3000 \
    HOSTNAME=0.0.0.0

RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /app/.next/standalone ./
COPY --from=build --chown=app:app /app/.next/static ./.next/static
COPY --from=build --chown=app:app /app/public ./public
USER app

EXPOSE 3000
# GATEWAY_URL đọc lúc chạy chứ không lúc build, nên một image dùng được cho mọi môi trường.
CMD ["node", "server.js"]
