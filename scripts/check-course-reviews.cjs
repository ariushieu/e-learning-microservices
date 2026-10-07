// Kiểm thử giao diện và API bằng fixture riêng; chỉ lưu bằng chứng không có token.
const {chromium}=require(process.env.COURSE_PLAYWRIGHT_MODULE || 'playwright');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const base=process.env.COURSE_GATEWAY_URL || 'http://localhost:8080';
const web=process.env.COURSE_WEB_URL || 'http://127.0.0.1:3000';
const output=process.env.COURSE_UI_OUTPUT || path.join(require('node:os').tmpdir(),'course-review-check');
fs.mkdirSync(output,{recursive:true});
const tag='reviews-ui-'+Date.now(), checks=[], errors=[], writers=[];
let browser,courseId,teacher;
const password=process.env.COURSE_QA_PASSWORD || 'Test@123456';
async function api(method,url,body,token,expected=200) {
  const r=await fetch(base+url,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(10000)});
  assert.equal(r.status,expected,`${method} ${url}`);
  return (await r.json()).data;
}
async function login(email){return (await api('POST','/api/auth/login',{email,password})).accessToken;}
async function retry(fn){let error;for(let i=0;i<30;i++){try{return await fn();}catch(e){error=e;await new Promise(r=>setTimeout(r,500));}}throw error;}
async function stats(count,avg){const c=await api('GET','/api/courses/'+courseId);assert.equal(c.ratingCount,count);assert.equal(c.ratingAvg,avg);}
async function run(){
  teacher=await login('qa.instructor.a@example.com');
  const student=await login('qa.student@example.com');
  const cat=await api('POST','/api/categories',{name:tag,slug:tag},teacher,201);
  const course=await api('POST','/api/courses',{title:'Trải nghiệm đánh giá '+tag,slug:tag,categoryId:cat.id,level:'BEGINNER',price:0,language:'vi'},teacher,201);
  courseId=course.id;
  await api('PATCH',`/api/courses/${courseId}/status`,{status:'PUBLISHED'},teacher);
  await retry(()=>api('POST','/api/enrollments',{courseId},student,201));
  await retry(async()=>assert.equal((await api('GET',`/api/courses/${courseId}/reviews/me`,undefined,student)).canReview,true));
  writers.push(student);
  browser=await chromium.launch({headless:true,...(process.env.COURSE_BROWSER_CHANNEL?{channel:process.env.COURSE_BROWSER_CHANNEL}:{})});
  const guest=await browser.newContext();
  const guestPage=await guest.newPage();
  await guestPage.goto(`${web}/courses/${courseId}`);
  await guestPage.locator('#reviews').getByText('Chưa có đánh giá nào',{exact:true}).waitFor();
  assert.equal(await guestPage.locator('#reviews input[name="rating"]').count(),0);
  checks.push('Khách thấy trạng thái rỗng và lời mời đăng nhập, không thấy form ghi');
  const context=await browser.newContext();
  await context.addCookies([{name:'el_access',value:student,url:web}]);
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message));
  await page.goto(`${web}/courses/${courseId}`);
  const section=page.locator('#reviews');
  const stars=n=>section.locator('label').filter({has:page.locator(`input[name="rating"][value="${n}"]`)});
  const comment=()=>page.getByRole('textbox',{name:'Nhận xét (không bắt buộc)'});
  await section.getByRole('button',{name:'Gửi đánh giá',exact:true}).click();
  await section.getByRole('alert').filter({hasText:'Hãy chọn số sao'}).waitFor();
  checks.push('Chưa chọn sao không gửi form');
  await stars(5).click();await comment().fill('Khóa học rõ ràng, ví dụ thực tế hữu ích.');
  await section.getByRole('button',{name:'Gửi đánh giá',exact:true}).click();
  await section.getByRole('button',{name:'Lưu thay đổi',exact:true}).waitFor();
  await retry(()=>stats(1,5));
  checks.push('Gửi 5 sao qua form, điểm và số lượt cập nhật');
  for(let i=0;i<5;i++){
    const email=`${tag}-${i}@example.com`;
    await api('POST','/api/auth/register',{email,password,fullName:`Học viên kiểm thử ${i}`},undefined,201);
    const token=await login(email);writers.push(token);
    await retry(()=>api('POST','/api/enrollments',{courseId},token,201));
    await retry(async()=>assert.equal((await api('GET',`/api/courses/${courseId}/reviews/me`,undefined,token)).canReview,true));
    await api('PUT',`/api/courses/${courseId}/reviews/me`,{rating:3,comment:i===4?'<script>window.reviewInjected=true</script>':'Ví dụ dễ hiểu.'},token);
  }
  await page.reload();await section.locator('article').first().waitFor();
  assert.equal(await section.locator('article').count(),5);
  assert.equal(await comment().inputValue(),'Khóa học rõ ràng, ví dụ thực tế hữu ích.');
  assert.equal(await page.evaluate(()=>window.reviewInjected),undefined);
  await section.getByText('<script>window.reviewInjected=true</script>',{exact:true}).waitFor();
  checks.push('Nhận xét hiển thị dạng văn bản, không thực thi script');
  checks.push('Form của mình vẫn hiện khi đánh giá nằm ngoài trang đầu');
  await section.getByRole('navigation',{name:'Phân trang'}).getByRole('link',{name:'Sau',exact:true}).click();
  await page.waitForURL(u=>u.searchParams.get('reviewPage')==='2');
  await section.locator('article').first().waitFor();assert.equal(await section.locator('article').count(),1);
  await section.locator('article').getByText('Khóa học rõ ràng, ví dụ thực tế hữu ích.',{exact:true}).waitFor();
  checks.push('Phân trang nhận xét không trùng và giữ form của mình');
  await stars(1).click();await comment().fill('Đã cập nhật nhận xét.');
  await section.getByRole('button',{name:'Lưu thay đổi',exact:true}).click();
  await section.locator('article').getByText('Đã cập nhật nhận xét.',{exact:true}).waitFor();
  await retry(()=>stats(6,2.67));checks.push('Sửa điểm giữ số lượt, làm tròn trung bình chính xác');
  await page.route(`**/api/courses/${courseId}/reviews/me`,route=>route.fulfill({status:503,contentType:'application/json',body:JSON.stringify({success:false,message:'Máy chủ tạm thời không sẵn sàng',code:'TEST_UNAVAILABLE'})}));
  await comment().fill('Bản nháp phải được giữ khi lỗi.');
  await section.getByRole('button',{name:'Lưu thay đổi',exact:true}).click();
  await section.getByRole('alert').waitFor();assert.equal(await comment().inputValue(),'Bản nháp phải được giữ khi lỗi.');
  await page.unroute(`**/api/courses/${courseId}/reviews/me`);
  checks.push('Lỗi API hiện thông báo và giữ nội dung đang viết');
  await page.reload();await section.getByRole('button',{name:'Xóa đánh giá',exact:true}).waitFor();
  for(const width of [375,768,1366]){
    await page.setViewportSize({width,height:1000});await section.scrollIntoViewIfNeeded();
    const b=await page.evaluate(()=>({width:document.documentElement.clientWidth,scroll:document.documentElement.scrollWidth}));
    assert.ok(b.scroll<=b.width+1,JSON.stringify(b));
    await page.screenshot({path:path.join(output,`reviews-${width}.png`)});
    checks.push(`Form và danh sách không tràn ngang tại ${width}px`);
  }
  await section.getByRole('button',{name:'Xóa đánh giá',exact:true}).click();
  await page.getByRole('button',{name:'Giữ lại',exact:true}).click();await stats(6,2.67);
  checks.push('Hủy hộp xác nhận không xóa nhận xét');
  await section.getByRole('button',{name:'Xóa đánh giá',exact:true}).click();
  await page.getByRole('button',{name:'Xác nhận xóa',exact:true}).click();
  await section.getByRole('button',{name:'Gửi đánh giá',exact:true}).waitFor();
  await page.waitForURL(u=>!u.searchParams.has('reviewPage'));
  await section.locator('article').first().waitFor();assert.equal(await section.locator('article').count(),5);
  await retry(()=>stats(5,3));checks.push('Xóa qua hộp xác nhận cập nhật điểm và mở lại form tạo');
  await guestPage.goto(`${web}/?keyword=${encodeURIComponent(tag)}`);
  await guestPage.getByLabel('3.0 trên 5 sao, 5 đánh giá').waitFor();checks.push('Thẻ khóa học công khai có điểm và số lượt mới');
  const deniedContext=await browser.newContext();await deniedContext.addCookies([{name:'el_access',value:teacher,url:web}]);
  const denied=await deniedContext.newPage();await denied.goto(`${web}/courses/${courseId}`);
  await denied.locator('#reviews').getByText(/Bạn cần ghi danh khóa học/).waitFor();
  assert.equal(await denied.locator('#reviews input[name="rating"]').count(),0);
  checks.push('Chủ khóa chưa ghi danh cũng không có form đánh giá');
  assert.deepEqual(errors,[]);checks.push('Không có lỗi JavaScript');
}
(async()=>{try{await run();}catch(e){errors.push(e.message);console.error(e);process.exitCode=1;}finally{
  if(browser)await browser.close();
  if(courseId){
    for(const token of writers){try{const mine=await api('GET',`/api/courses/${courseId}/reviews/me`,undefined,token);if(mine.review)await api('DELETE',`/api/courses/${courseId}/reviews/me`,undefined,token);}catch(e){errors.push('Cleanup: '+e.message);process.exitCode=1;}}
    try{await api('PATCH',`/api/courses/${courseId}/status`,{status:'ARCHIVED'},teacher);}catch(e){errors.push('Archive: '+e.message);process.exitCode=1;}
  }
  fs.writeFileSync(path.join(output,'review-ui-results.json'),JSON.stringify({tag,courseId,checks,errors},null,2));
  console.log(JSON.stringify({passed:checks.length,errors}));
}})();
