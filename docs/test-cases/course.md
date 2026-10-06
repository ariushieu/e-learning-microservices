# Tình huống test course-service

Dùng tài khoản, biến và cách ghi kết quả trong [gateway.md](gateway.md).
Tất cả request đi qua `{{baseUrl}}`. **Chưa chạy Postman**; các nhãn CHỜ/CẦN CHỐT
là điều kiện nghiệm thu hoặc hợp đồng dự kiến, không phải hành vi đã được xác nhận.

## Đường dẫn đích và phụ thuộc

| Hiện tại | Đích dùng trong các ca dưới đây |
|---|---|
| GET /api/courses/instructor/{id} | GET /api/courses?instructorId={id} |
| POST /api/sections, courseId trong body | POST /api/courses/{courseId}/sections, bỏ courseId khỏi body |
| POST /api/lessons, sectionId trong body | POST /api/sections/{sectionId}/lessons, bỏ sectionId khỏi body |
| DELETE /api/lessons/resources/{id} | DELETE /api/lessons/{lessonId}/resources/{id} |

Bốn thay đổi trên **[CHỜ ROUTE]**. Quyền sở hữu của chương/bài học/tài liệu **[CHỜ SỬA]**.
Nội dung bài học **[CHỜ SỬA]**: DTO tạo/sửa và response hiện chưa có content/contentUrl;
ca nội dung phải chờ đường ghi dữ liệu và chính sách xem bài thường được chốt.
GET danh mục, khóa học, curriculum và lesson là công khai; DRAFT của người lạ bị 404.
Chỉ danh sách danh mục/khóa học có Pageable: sort sai phải 400. Các bảng chi tiết/ghi không
có sort; thay bằng ca trùng slug, trạng thái hoặc quyền sở hữu.

## Body và fixture

**COURSE-CATEGORY** (tạo/sửa): `{"name":"QA {{runId}}","slug":"qa-{{runId}}","position":0}`.
Đổi slug theo từng ca; thêm `parentId={{categoryId}}` để tạo danh mục con.

**COURSE-CREATE** (cũng dùng PUT với title/slug mới):

```json
{"categoryId":{{categoryId}},"title":"Khoa QA {{runId}}","slug":"khoa-qa-{{runId}}","level":"BEGINNER","language":"vi","price":0}
```

**COURSE-SECTION**: `{"title":"Chuong QA","position":1}`.
**COURSE-LESSON**: `{"title":"Bai QA","type":"ARTICLE","durationSeconds":60,"position":1,"isPreview":true}`.
**COURSE-RESOURCE**: `{"name":"Tai lieu QA","fileUrl":"https://example.com/qa.pdf"}`.
Không tự thêm instructorId/createdBy/userId vào body trừ ca cố tình thử giả danh.

Dựng fixture theo gateway.md. COURSE-01..04 cần danh mục có sẵn; tạo chúng bằng COURSE-05
trước. Mỗi ca DELETE dùng một tài nguyên mới dành riêng cho ca đó. Ca trùng slug dùng
hai bản ghi khác ID, không lấy chính slug hiện tại để kiểm trùng.

## COURSE-01 — GET /api/categories

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Danh sách có phân trang | — | GET /api/categories?page=0&size=2&sort=position,asc | 200; data.content tối đa 2 phần tử; metadata đúng |
| 2 | Học viên xem công khai | S | GET /api/categories | 200 |
| 3 | Admin xem công khai | ADM | GET /api/categories | 200 |
| 4 | Trang ngoài dữ liệu | — | GET /api/categories?page=999999&size=2 | 200; content=[] |
| 5 | Sort không tồn tại | — | GET /api/categories?sort=abcxyz | 400 |
| 6 | Sắp xếp giảm dần | — | GET /api/categories?sort=position,desc | 200; position không tăng |
| 7 | Giới hạn trang | — | GET /api/categories?page=0&size=1 | 200; tối đa 1 bản ghi, totalElements không đổi |

## COURSE-02 — GET /api/categories/tree

Không có ID hoặc query được nhận; không có ca abc/404/sort cho endpoint này.

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Cây cha–con | — | GET /api/categories/tree sau tạo danh mục gốc/con | 200; con nằm trong subCategories của đúng cha |
| 2 | Học viên xem | S | GET /api/categories/tree | 200 |
| 3 | Giảng viên xem | A | GET /api/categories/tree | 200 |
| 4 | Admin xem | ADM | GET /api/categories/tree | 200 |
| 5 | Danh mục mới xuất hiện | — | Tạo danh mục gốc mới bằng A, GET /api/categories/tree | 200; có đúng ID mới |
| 6 | Sau xóa danh mục rỗng | — | A xóa một danh mục rỗng riêng; GET /api/categories/tree | 200; không còn ID đã xóa |

## COURSE-03 — GET /api/categories/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Tồn tại | — | GET /api/categories/{{categoryId}} | 200; data.id đúng |
| 2 | Học viên xem | S | Cùng request | 200 |
| 3 | Giảng viên xem | A | Cùng request | 200 |
| 4 | Không tồn tại | — | GET /api/categories/{{missingId}} | 404 |
| 5 | ID sai kiểu | — | GET /api/categories/abc | 400 |
| 6 | Danh mục con | — | GET /api/categories/{{childCategoryId}} | 200; parentId=categoryId |

## COURSE-04 — GET /api/categories/slug/{slug}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Slug tồn tại | — | GET /api/categories/slug/{{categorySlug}} | 200; id=categoryId |
| 2 | Học viên xem | S | Cùng request | 200 |
| 3 | Giảng viên xem | A | Cùng request | 200 |
| 4 | Không tồn tại | — | GET /api/categories/slug/{{missingSlug}} | 404 |
| 5 | Slug số vẫn là chuỗi | — | GET /api/categories/slug/9223372036854775807, xác nhận chưa có slug này | 404; không 400 do chuyển sang Long |
| 6 | Đổi slug | — | A PUT danh mục sang slug mới; GET slug cũ rồi slug mới | 404 ở slug cũ, 200 ở slug mới |

## COURSE-05 — POST /api/categories

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | POST /api/categories + COURSE-CATEGORY | 201; lưu categoryId/categorySlug |
| 2 | Không token | — | POST /api/categories + COURSE-CATEGORY | 401 |
| 3 | Sai vai trò | S | POST /api/categories + COURSE-CATEGORY | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | Body hợp lệ, parentId={{missingId}} | 404 |
| 5 | Sai kiểu/dữ liệu | A | COURSE-CATEGORY với position=-1 | 400 |
| 6 | Trùng slug | A | Body với slug của danh mục khác đã có | 409 |
| 7 | Cha là danh mục con | A | parentId={{childCategoryId}}, slug mới | 422; chỉ một cấp lồng |
| 8 | Admin tạo | ADM | Body có slug mới | 201 |

## COURSE-06 — PUT /api/categories/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | PUT /api/categories/{{categoryId}} + COURSE-CATEGORY | 200; tên/slug mới |
| 2 | Không token | — | PUT /api/categories/{{categoryId}} + COURSE-CATEGORY | 401 |
| 3 | Sai vai trò | S | PUT /api/categories/{{categoryId}} + COURSE-CATEGORY | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | PUT /api/categories/{{missingId}} + body hợp lệ | 404 |
| 5 | Sai kiểu/dữ liệu | A | PUT /api/categories/abc + body hợp lệ | 400 |
| 6 | Trùng slug khác ID | A | Body lấy slug của danh mục khác | 409 |
| 7 | Tự làm cha | A | Body parentId={{categoryId}} | 422 |
| 8 | Tên trống | A | Body name="" | 400 |

## COURSE-07 — DELETE /api/categories/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | DELETE /api/categories/{{categoryId}} (danh mục rỗng riêng) | 200; đọc lại 404 |
| 2 | Không token | — | DELETE /api/categories/{{categoryId}} (danh mục rỗng riêng) | 401 |
| 3 | Sai vai trò | S | DELETE /api/categories/{{categoryId}} (danh mục rỗng riêng) | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | DELETE /api/categories/{{missingId}} | 404 |
| 5 | Sai kiểu/dữ liệu | A | DELETE /api/categories/abc | 400 |
| 6 | Có danh mục con | A | DELETE danh mục cha còn con | 422 |
| 7 | Có khóa học | A | DELETE danh mục đang chứa khóa học | 422 |
| 8 | Admin xóa | ADM | DELETE danh mục rỗng riêng | 200 |

## COURSE-08 — GET /api/courses (kèm instructorId)

Hành vi lọc instructor kế thừa endpoint cũ: chủ sở hữu/admin xem mọi trạng thái; người ngoài chỉ thấy PUBLISHED. Trước chuẩn hóa, query instructorId bị bỏ qua là BLOCKED, không PASS.

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Công khai, có phân trang | — | GET /api/courses?page=0&size=2 | 200; chỉ PUBLISHED, content tối đa 2 |
| 2 | Học viên vẫn không thấy DRAFT | S | GET /api/courses?categoryId={{categoryId}} | 200; không có courseAId DRAFT |
| 3 | Lọc không có dữ liệu | — | GET /api/courses?categoryId={{missingId}} | 200; content=[] |
| 4 | Sai kiểu bộ lọc | — | GET /api/courses?categoryId=abc | 400 |
| 5 | Sai enum | — | GET /api/courses?level=UNKNOWN | 400 |
| 6 | Sort bịa | — | GET /api/courses?sort=abcxyz | 400 |
| 7 | Giảng viên xem khóa mình [CHỜ ROUTE] | A | GET /api/courses?instructorId={{instructorAId}} | 200; có DRAFT của A, không có khóa B |
| 8 | Người lạ lọc theo A [CHỜ ROUTE] | B | GET /api/courses?instructorId={{instructorAId}} | 200; chỉ khóa PUBLISHED của A |
| 9 | Admin lọc theo A [CHỜ ROUTE] | ADM | GET /api/courses?instructorId={{instructorAId}} | 200; thấy DRAFT của A |
| 10 | Instructor ID sai kiểu [CHỜ ROUTE] | — | GET /api/courses?instructorId=abc | 400 |
| 11 | Instructor không có khóa [CHỜ ROUTE] | — | GET /api/courses?instructorId={{missingId}} | 200; content=[] |

## COURSE-09 — GET /api/courses/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Khóa xuất bản | — | GET /api/courses/{{publishedCourseId}} | 200; status=PUBLISHED |
| 2 | Chủ xem DRAFT | A | GET /api/courses/{{courseAId}} | 200; status=DRAFT |
| 3 | Học viên xem DRAFT | S | GET /api/courses/{{courseAId}} | 404; che sự tồn tại |
| 4 | Khách xem DRAFT | — | GET /api/courses/{{courseAId}} | 404 |
| 5 | Không tồn tại | — | GET /api/courses/{{missingId}} | 404 |
| 6 | Sai ID | — | GET /api/courses/abc | 400 |
| 7 | Admin xem DRAFT | ADM | GET /api/courses/{{courseAId}} | 200 |
| 8 | Giảng viên khác xem DRAFT | B | GET /api/courses/{{courseAId}} | 404 |

## COURSE-10 — GET /api/courses/slug/{slug}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Slug xuất bản | — | GET /api/courses/slug/{{publishedCourseSlug}} | 200 |
| 2 | Chủ xem DRAFT | A | GET /api/courses/slug/{{courseASlug}} | 200 |
| 3 | Khách xem DRAFT | — | GET /api/courses/slug/{{courseASlug}} | 404 |
| 4 | Học viên xem DRAFT | S | GET /api/courses/slug/{{courseASlug}} | 404 |
| 5 | Slug không tồn tại | — | GET /api/courses/slug/{{missingSlug}} | 404 |
| 6 | Admin xem DRAFT | ADM | GET /api/courses/slug/{{courseASlug}} | 200 |
| 7 | Chuỗi số không phải slug có sẵn | — | GET /api/courses/slug/9223372036854775807 | 404; slug không phải ID số |

## COURSE-11 — POST /api/courses

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | POST /api/courses + COURSE-CREATE | 201; status=DRAFT, instructorId=instructorAId |
| 2 | Không token | — | POST /api/courses + COURSE-CREATE | 401 |
| 3 | Sai vai trò | S | POST /api/courses + COURSE-CREATE | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | COURSE-CREATE thay categoryId={{missingId}} | 404 |
| 5 | Sai kiểu/dữ liệu | A | COURSE-CREATE thay categoryId="abc" | 400 |
| 6 | Trùng slug | A | COURSE-CREATE lấy slug đã tồn tại | 409 |
| 7 | Giá âm | A | COURSE-CREATE với price=-1 | 400 |
| 8 | Thiếu title | A | COURSE-CREATE bỏ title | 400 |
| 9 | Giả instructor | A | Body thêm instructorId={{instructorBId}}, slug mới | 201; instructorId vẫn instructorAId |

## COURSE-12 — PUT /api/courses/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | PUT /api/courses/{{courseAId}} + COURSE-CREATE | 200; đổi nội dung đúng |
| 2 | Không token | — | PUT /api/courses/{{courseAId}} + COURSE-CREATE | 401 |
| 3 | Sai vai trò | S | PUT /api/courses/{{courseAId}} + COURSE-CREATE | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | PUT /api/courses/{{missingId}} + body hợp lệ | 404 |
| 5 | Sai kiểu/dữ liệu | A | PUT /api/courses/abc + body hợp lệ | 400 |
| 6 | Sửa khóa người khác | B | PUT khóa DRAFT của A + body hợp lệ | 403 |
| 7 | Admin sửa | ADM | PUT khóa DRAFT của A + body hợp lệ | 200 |
| 8 | Khóa lưu trữ | A | PUT khóa của A đã ARCHIVED | 422 |
| 9 | Trùng slug khác ID | A | Body lấy slug của khóa khác | 409 |
| 10 | Đổi khóa đã xuất bản | A | PUT publishedCourseId, đổi title | 200; course.updated phát ra, snapshot đổi title sau consumer xử lý [CHỜ SỬA consumer] |

## COURSE-13 — PATCH /api/courses/{id}/status

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | PATCH /api/courses/{{courseAId}}/status + {"status":"PUBLISHED"} | 200; status=PUBLISHED, publishedAt có giá trị |
| 2 | Không token | — | PATCH /api/courses/{{courseAId}}/status + {"status":"PUBLISHED"} | 401 |
| 3 | Sai vai trò | S | PATCH /api/courses/{{courseAId}}/status + {"status":"PUBLISHED"} | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | PATCH /api/courses/{{missingId}}/status + {"status":"PUBLISHED"} | 404 |
| 5 | Sai kiểu/dữ liệu | A | PATCH /api/courses/abc/status + {"status":"PUBLISHED"} | 400 |
| 6 | Đổi khóa người khác | B | PATCH khóa A + {"status":"PUBLISHED"} | 403 |
| 7 | Enum sai | A | Body {"status":"INVALID"} | 400 |
| 8 | Thiếu trạng thái | A | Body {} | 400 |
| 9 | Admin đổi trạng thái | ADM | PATCH khóa A + {"status":"ARCHIVED"} | 200 |
| 10 | Bỏ xuất bản | A | PUBLISHED → ARCHIVED bằng cùng endpoint | 200; course.updated mang trạng thái ARCHIVED; GET công khai khóa đó 404 |

## COURSE-14 — DELETE /api/courses/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | DELETE /api/courses/{{courseAId}} (DRAFT riêng chưa ghi danh) | 200; đọc lại 404 |
| 2 | Không token | — | DELETE /api/courses/{{courseAId}} (DRAFT riêng chưa ghi danh) | 401 |
| 3 | Sai vai trò | S | DELETE /api/courses/{{courseAId}} (DRAFT riêng chưa ghi danh) | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | DELETE /api/courses/{{missingId}} | 404 |
| 5 | Sai kiểu/dữ liệu | A | DELETE /api/courses/abc | 400 |
| 6 | Xóa khóa người khác | B | DELETE khóa DRAFT của A | 403 |
| 7 | Khóa PUBLISHED | A | DELETE /api/courses/{{publishedCourseId}} | 422 |
| 8 | Admin xóa DRAFT | ADM | DELETE khóa DRAFT riêng của A | 200 |

## COURSE-15 — GET /api/courses/{courseId}/curriculum

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Khóa PUBLISHED | — | GET /api/courses/{{publishedCourseId}}/curriculum | 200; đúng chương/bài, position tăng dần |
| 2 | Khách xem DRAFT | — | GET /api/courses/{{courseAId}}/curriculum | 404 |
| 3 | Học viên xem DRAFT | S | GET /api/courses/{{courseAId}}/curriculum | 404 |
| 4 | Chủ xem DRAFT | A | GET /api/courses/{{courseAId}}/curriculum | 200 |
| 5 | Khóa không tồn tại | A | GET /api/courses/{{missingId}}/curriculum | 404 |
| 6 | ID sai kiểu | A | GET /api/courses/abc/curriculum | 400 |
| 7 | Admin xem DRAFT | ADM | GET /api/courses/{{courseAId}}/curriculum | 200 |

## COURSE-16 — POST /api/courses/{courseId}/sections [CHỜ ROUTE]

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | POST /api/courses/{{publishedCourseId}}/sections + COURSE-SECTION | 201; chương thuộc khóa A |
| 2 | Không token | — | POST /api/courses/{{publishedCourseId}}/sections + COURSE-SECTION | 401 |
| 3 | Sai vai trò | S | POST /api/courses/{{publishedCourseId}}/sections + COURSE-SECTION | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | POST /api/courses/{{missingId}}/sections + body hợp lệ | 404 |
| 5 | Sai kiểu/dữ liệu | A | POST /api/courses/abc/sections + body hợp lệ | 400 |
| 6 | Chèn chương vào khóa người khác [CHỜ SỬA] | B | POST khóa A + COURSE-SECTION | 403 |
| 7 | Admin thêm vào khóa A | ADM | POST khóa A + COURSE-SECTION | 201 |
| 8 | Title trống | A | COURSE-SECTION với title="" | 400 |

## COURSE-17 — PUT /api/sections/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | PUT /api/sections/{{sectionAId}} + COURSE-SECTION | 200; cập nhật title/position |
| 2 | Không token | — | PUT /api/sections/{{sectionAId}} + COURSE-SECTION | 401 |
| 3 | Sai vai trò | S | PUT /api/sections/{{sectionAId}} + COURSE-SECTION | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | PUT /api/sections/{{missingId}} + body hợp lệ | 404 |
| 5 | Sai kiểu/dữ liệu | A | PUT /api/sections/abc + body hợp lệ | 400 |
| 6 | Sửa chương của A [CHỜ SỬA] | B | PUT sectionAId + body hợp lệ | 403 |
| 7 | Admin sửa | ADM | PUT sectionAId + body hợp lệ | 200 |
| 8 | Position âm | A | Body position=-1 | 400 |

## COURSE-18 — DELETE /api/sections/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | DELETE /api/sections/{{sectionAId}} (bản sao riêng) | 200; curriculum không còn chương |
| 2 | Không token | — | DELETE /api/sections/{{sectionAId}} (bản sao riêng) | 401 |
| 3 | Sai vai trò | S | DELETE /api/sections/{{sectionAId}} (bản sao riêng) | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | DELETE /api/sections/{{missingId}} | 404 |
| 5 | Sai kiểu/dữ liệu | A | DELETE /api/sections/abc | 400 |
| 6 | Xóa chương của A [CHỜ SỬA] | B | DELETE sectionAId | 403 |
| 7 | Admin xóa | ADM | DELETE chương riêng của A | 200 |
| 8 | Xóa chương có bài | A | DELETE chương test có 2 bài; đọc lại khóa và curriculum | 200; bài con mất, totalLessons/totalDurationSeconds giảm đúng |

## COURSE-19 — GET /api/lessons/{id}

Phân công chỉ chốt người lạ không được thấy nội dung, chưa chốt 200 metadata hay 403; ca CẦN CHỐT không được tự coi là FAIL theo một phương án chưa thống nhất.

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Bài trong khóa PUBLISHED | — | GET /api/lessons/{{lessonAId}} | 200; đúng id/title |
| 2 | Khách xem bài trong DRAFT | — | GET /api/lessons/{{draftLessonId}} | 404 |
| 3 | Học viên xem bài trong DRAFT | S | GET /api/lessons/{{draftLessonId}} | 404 |
| 4 | Chủ xem bài trong DRAFT | A | GET /api/lessons/{{draftLessonId}} | 200 |
| 5 | Bài không tồn tại | — | GET /api/lessons/{{missingId}} | 404 |
| 6 | ID sai kiểu | — | GET /api/lessons/abc | 400 |
| 7 | Nội dung bài preview [CHỜ SỬA] | — | GET previewLessonId sau khi có API ghi content/contentUrl | 200; trả nội dung preview đã lưu |
| 8 | Nội dung bài thường [CẦN CHỐT] | B (chưa ghi danh) | GET lesson2Id thuộc khóa A | 200, chỉ metadata; không content/contentUrl (đề xuất); nếu nhóm chọn 403 thì cập nhật hợp đồng trước khi chạy |
| 9 | Người đã ghi danh xem nội dung [CHỜ SỬA] | S | GET lesson2Id sau ghi danh ACTIVE | 200; trả đúng nội dung |
| 10 | Chủ khóa xem nội dung [CHỜ SỬA] | A | GET lesson2Id | 200; có nội dung |

## COURSE-20 — POST /api/sections/{sectionId}/lessons [CHỜ ROUTE]

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | POST /api/sections/{{sectionAId}}/lessons + COURSE-LESSON | 201; bài thuộc đúng chương/khóa, totalLessons tăng 1 |
| 2 | Không token | — | POST /api/sections/{{sectionAId}}/lessons + COURSE-LESSON | 401 |
| 3 | Sai vai trò | S | POST /api/sections/{{sectionAId}}/lessons + COURSE-LESSON | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | POST /api/sections/{{missingId}}/lessons + body hợp lệ | 404 |
| 5 | Sai kiểu/dữ liệu | A | POST /api/sections/abc/lessons + body hợp lệ | 400 |
| 6 | Chèn bài vào khóa A [CHỜ SỬA] | B | POST sectionAId + COURSE-LESSON | 403 |
| 7 | Admin tạo bài | ADM | POST sectionAId + COURSE-LESSON | 201 |
| 8 | Duration âm | A | Body durationSeconds=-1 | 400 |
| 9 | Loại bài sai | A | Body type="INVALID" | 400 |

## COURSE-21 — PUT /api/lessons/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | PUT /api/lessons/{{lessonAId}} + COURSE-LESSON | 200; trường cập nhật đúng |
| 2 | Không token | — | PUT /api/lessons/{{lessonAId}} + COURSE-LESSON | 401 |
| 3 | Sai vai trò | S | PUT /api/lessons/{{lessonAId}} + COURSE-LESSON | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | PUT /api/lessons/{{missingId}} + body hợp lệ | 404 |
| 5 | Sai kiểu/dữ liệu | A | PUT /api/lessons/abc + body hợp lệ | 400 |
| 6 | Sửa bài của A [CHỜ SỬA] | B | PUT lessonAId + body hợp lệ | 403 |
| 7 | Admin sửa | ADM | PUT lessonAId + body hợp lệ | 200 |
| 8 | Thời lượng thay đổi | A | PUT durationSeconds từ 60 thành 120 | 200; totalDurationSeconds của khóa tăng đúng 60 |
| 9 | Title trống | A | Body title="" | 400 |

## COURSE-22 — DELETE /api/lessons/{id}

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | DELETE /api/lessons/{{lessonAId}} (bài riêng) | 200; GET lại 404; tổng số bài giảm 1 |
| 2 | Không token | — | DELETE /api/lessons/{{lessonAId}} (bài riêng) | 401 |
| 3 | Sai vai trò | S | DELETE /api/lessons/{{lessonAId}} (bài riêng) | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | DELETE /api/lessons/{{missingId}} | 404 |
| 5 | Sai kiểu/dữ liệu | A | DELETE /api/lessons/abc | 400 |
| 6 | Xóa bài của A [CHỜ SỬA] | B | DELETE lessonAId | 403 |
| 7 | Admin xóa | ADM | DELETE bài riêng của A | 200 |
| 8 | Xóa lại | A | DELETE cùng ID đã xóa | 404 |

## COURSE-23 — POST /api/lessons/{lessonId}/resources

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | POST /api/lessons/{{lessonAId}}/resources + COURSE-RESOURCE | 201; tài liệu thuộc đúng bài |
| 2 | Không token | — | POST /api/lessons/{{lessonAId}}/resources + COURSE-RESOURCE | 401 |
| 3 | Sai vai trò | S | POST /api/lessons/{{lessonAId}}/resources + COURSE-RESOURCE | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | POST /api/lessons/{{missingId}}/resources + body hợp lệ | 404 |
| 5 | Sai kiểu/dữ liệu | A | POST /api/lessons/abc/resources + body hợp lệ | 400 |
| 6 | Gắn tài liệu vào bài A [CHỜ SỬA] | B | POST lessonAId/resources + body hợp lệ | 403 |
| 7 | Admin gắn tài liệu | ADM | POST lessonAId/resources + body hợp lệ | 201 |
| 8 | Thiếu URL | A | Body {"name":"Tai lieu"} | 400 |

## COURSE-24 — DELETE /api/lessons/{lessonId}/resources/{id} [CHỜ ROUTE]

| # | Tình huống | Tài khoản | Request | Mong đợi |
|---|---|---|---|---|
| 1 | Hợp lệ | A | DELETE /api/lessons/{{lessonAId}}/resources/{{resourceId}} (tài liệu riêng) | 200; GET lesson không còn resource |
| 2 | Không token | — | DELETE /api/lessons/{{lessonAId}}/resources/{{resourceId}} (tài liệu riêng) | 401 |
| 3 | Sai vai trò | S | DELETE /api/lessons/{{lessonAId}}/resources/{{resourceId}} (tài liệu riêng) | 403; dữ liệu không đổi |
| 4 | Không tồn tại | A | DELETE /api/lessons/{{lessonAId}}/resources/{{missingId}} | 404 |
| 5 | Sai kiểu/dữ liệu | A | DELETE /api/lessons/{{lessonAId}}/resources/abc | 400 |
| 6 | Xóa tài liệu của A [CHỜ SỬA] | B | DELETE resourceId của A | 403 |
| 7 | Sai cha [CẦN CHỐT mã] | A | DELETE /api/lessons/{{lesson2Id}}/resources/{{resourceId}}, resource thuộc lessonAId | 404 đề xuất; tuyệt đối không xóa resource (B3) |
| 8 | Admin xóa đúng cha | ADM | DELETE tài liệu riêng của A trong đúng bài | 200 |

## Truy vết nguồn

- [Controllers](../../course-service/src/main/java/com/hunre/courseservice/controller).
- [DTO request](../../course-service/src/main/java/com/hunre/courseservice/dto/request).
- [CategoryServiceImpl](../../course-service/src/main/java/com/hunre/courseservice/service/impl/CategoryServiceImpl.java).
- [CourseServiceImpl](../../course-service/src/main/java/com/hunre/courseservice/service/impl/CourseServiceImpl.java).
- [CurriculumServiceImpl](../../course-service/src/main/java/com/hunre/courseservice/service/impl/CurriculumServiceImpl.java).
- [Các thay đổi đang giao](../phan-cong.md), [quy ước đường dẫn](../api-conventions.md).
