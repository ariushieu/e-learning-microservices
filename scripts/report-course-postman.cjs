// Chỉ xuất response nghiệp vụ, không xuất header/token/request đăng nhập từ báo cáo Newman.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execFileSync } = require('node:child_process');
const root = path.resolve(__dirname, '..');
if (!process.argv[2]) throw Error('Usage: node scripts/report-course-postman.cjs <newman-report.json>');
const report = JSON.parse(fs.readFileSync(process.argv[2], 'utf8'));
const commit = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim();
const output = path.join(root, 'docs/test-cases/ket-qua');
fs.mkdirSync(output, { recursive: true });
const unique = new Map();
for (const e of report.run.executions) {
  if (e.item.name.startsWith('COURSE-')) unique.set(e.item.id, e);
}
const entries = [...unique.values()].map(e => {
  let response;
  try { response = JSON.parse(Buffer.from(e.response.stream.data).toString('utf8')); } catch { response = null; }
  const failures = report.run.failures.filter(f => f.source?.id === e.item.id);
  const assertions = (e.assertions || []).map(a => ({ name: a.assertion, passed: !a.error && !a.skipped }));
  return { case: e.item.name.split(' ')[0], title: e.item.name, method: e.request.method,
    url: e.request.url.raw || `${e.request.url.protocol}://${e.request.url.host.join('.')}${e.request.url.port ? ':'+e.request.url.port : ''}/${e.request.url.path.join('/')}${e.request.url.query?.length ? '?'+e.request.url.query.map(q=>q.key+'='+q.value).join('&') : ''}`,
    http: e.response?.code ?? null, result: failures.length || assertions.some(a => !a.passed) ? 'FAIL' : 'PASS',
    assertions, errors: failures.map(f => f.error.message), response };
});
const expected = [];
let section;
for (const line of fs.readFileSync(path.join(root,'docs/test-cases/course.md'),'utf8').split('\n')) {
  const h=line.match(/^## COURSE-(\d+)/);if(h)section=h[1];
  const row=line.match(/^\| (\d+) \|/);if(row)expected.push(`COURSE-${section}.${row[1]}`);
}
for (const id of expected) if(!entries.some(e=>e.case===id)) throw Error('Missing executed case '+id);
const rawCollection=fs.readFileSync(path.join(root,'docs/postman/course.postman_collection.json'));
const data={ commit, gateway:'http://localhost:8080', runner:'Newman 6.2.2 (Postman Runtime)',
  collectionSha256:crypto.createHash('sha256').update(rawCollection).digest('hex'),
  started:new Date(report.run.timings.started).toISOString(),completed:new Date(report.run.timings.completed).toISOString(),
  stats:report.run.stats, entries };
fs.writeFileSync(path.join(output,'course-evidence.json'),JSON.stringify(data,null,2)+'\n');
const stamp=new Intl.DateTimeFormat('vi-VN',{dateStyle:'short',timeStyle:'medium',timeZone:'Asia/Bangkok'}).format(new Date(report.run.timings.started));
const rows=entries.map(e=>`| ${e.case} | \`${commit.slice(0,7)}\`, native/gateway 8080 | ${e.http} | **${e.result}** | ${e.method} \`${new URL(e.url).pathname}\`; ${e.assertions.length} kiểm tra; [JSON](course-evidence.json) → \`${e.case}\` |`).join('\n');
const text=`# Biên bản kiểm thử course-service

- Chạy ngày **${stamp} (UTC+7)**; commit nguồn: \`${commit}\`.
- Collection: [course.postman_collection.json](../../postman/course.postman_collection.json), SHA-256 \`${data.collectionSha256}\`.
- Chạy bằng **Newman 6.2.2 / Postman Runtime**, không phải thao tác trên Postman Desktop.
- Môi trường: Windows, Java 21 (biên dịch release 17), MySQL 8 cài trên máy, Kafka 4.2.1 KRaft;
  auth/course/enrollment/gateway chạy JAR, request qua **http://localhost:8080**. JWT bật;
  rate limit tắt trong phiên kiểm thử. Máy không có Docker; không ghi nhận đã chạy smoke test toàn bộ
  stack Docker/Redis/quiz/notification. Auth dùng database dev đã khởi tạo, tắt Flyway lúc chạy local.
- Backend có đánh giá khóa học, tính lại số sao dưới khóa dòng và migration V5 lưu tên người viết.
- Kết quả: **${expected.length}/${expected.length} mã ca gốc đã chạy**, cộng ${entries.length-expected.length} ca bổ sung;
  ${report.run.stats.items.total} request chính (bao gồm chuẩn bị), ${report.run.stats.requests.total} HTTP tính cả bước phụ,
  **${report.run.stats.assertions.total-report.run.stats.assertions.failed}/${report.run.stats.assertions.total} assertion đạt**;
  ${report.run.failures.length} lỗi được Newman ghi nhận. HTTP và assertion từng ca nằm trong JSON bằng chứng.

## Hợp đồng và giới hạn

- Nhãn CHỜ trong course.md là kế hoạch cũ. Route/quyền sở hữu/nội dung đã vào #37;
  quyền học ARCHIVED vào #44; chống đếm trùng vào #51. Không sửa kỳ vọng của file tình huống.
- COURSE-19.8 dùng chính sách đã triển khai: 200 metadata, \`content/contentUrl=null\`, tài liệu rỗng.
  COURSE-24.7 trả 404 khi sai lesson cha và dữ liệu không đổi.
- COURSE-12.10 kiểm cả tên khóa mới trong response enrollment sau khi snapshot được cập nhật;
  HTTP 200 của PUT một mình không đủ để chứng minh Kafka đã đồng bộ.
- Ca bổ sung COURSE-ARCHIVED.5 cho phép 422 từ snapshot đã ARCHIVED hoặc 404 từ bước kiểm nguồn
  khi snapshot còn trễ. Phải xác nhận không có lượt ghi danh mới. Không chấp nhận 2xx/5xx.
- COURSE-25 kiểm vòng đời đánh giá, quyền theo sổ học viên, dữ liệu đầu vào, tên từ token,
  phân trang và ghi đồng thời qua gateway trên MySQL. Điểm lẻ không được tự làm tròn thành số sao.
- COURSE-26 kiểm URL tài liệu HTTP/HTTPS, chặn giao thức nguy hiểm và URL sai cấu trúc,
  lỗi theo trường fileUrl, xác nhận không lưu dữ liệu sai và đọc lại sau khi xóa.
- COURSE-27 kiểm quyền admin gỡ đánh giá, sai khóa, điểm sau khi gỡ, người viết tạo lại,
  gỡ đồng thời với sửa điểm và gỡ ở trạng thái ARCHIVED/DRAFT trên MySQL.
- Fixture riêng theo runId; các ca xóa dùng bản sao. Tài khoản QA cố định theo gateway.md.
  Dữ liệu được giữ cho demo. Không có token/password/header đăng nhập trong bằng chứng đã xuất.
- Đây là biên bản API của course-service. Không suy ra các service khác hay mọi tình huống đồng thời
  đều đã được kiểm thử từ kết quả này.

## Kết quả từng ca

| Mã ca | Commit/môi trường | HTTP thực tế | PASS / FAIL / BLOCKED | Bằng chứng |
|---|---|---|---|---|
${rows}

## Chạy lại

Import collection mới, chọn No environment, chạy từ **0. Chuẩn bị**. Hoặc:

\`\`\`bash
pnpm dlx newman@6.2.2 run docs/postman/course.postman_collection.json --timeout-script 65000 --timeout-request 10000 --reporters cli,json --reporter-json-export /tmp/course-newman.json
node scripts/report-course-postman.cjs /tmp/course-newman.json
\`\`\`

Báo cáo Newman gốc có token thật, không commit. Generator biên bản chỉ xuất response nghiệp vụ.
Collection và biên bản đi cùng PR; nếu phát hiện lỗi sản phẩm, sửa bằng PR riêng theo phân công.
`;
fs.writeFileSync(path.join(output,'course.md'),text);
console.log(JSON.stringify({cases:entries.length,failures:report.run.failures.length,commit}));
