// Sinh collection theo docs/test-cases/course.md; không chứa token của lần chạy.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const source = fs.readFileSync(path.join(root, 'docs/test-cases/course.md'), 'utf8');
const cases = new Map();
let group;
for (const line of source.split('\n')) {
  const heading = line.match(/^## COURSE-(\d+)/);
  if (heading) group = Number(heading[1]);
  const row = line.match(/^\| (\d+) \| (.+?) \| (.*?) \| (.*?) \| (.*?) \|/);
  if (row) cases.set(`${group}.${row[1]}`, { title: row[2], expected: row[5] });
}
const collection = {
  info: { _postman_id: 'c8b08e47-0a2c-4970-92bd-69ed9c1a01f7', name: 'Course — kiểm thử qua gateway', schema: 'https://schema.getpostman.com/json/collection/v2.1.0/collection.json',
    description: 'Chuyển từ course-service-v2 theo phân công 07/10. Import duy nhất file này, chọn No environment và chạy Runner từ 0. Chuẩn bị, một iteration. Mọi token/ID dùng collection variables; không nhập ID bằng tay. Mỗi lần chạy tạo runId và fixture riêng. Request có mã COURSE-xx.y khớp docs/test-cases/course.md; script kiểm cả HTTP và nghiệp vụ. Các bước chuẩn bị phụ dùng pm.sendRequest và dừng Runner khi thất bại. Tài khoản QA cố định theo gateway.md; 409 đăng ký chỉ được coi là tài khoản đã tồn tại sau khi đăng nhập thành công. Không chạy song song trên cùng tài khoản QA. Dữ liệu giữ lại để kiểm tra/demo, không xóa dữ liệu của người khác. Course có học viên lịch sử không thể xóa bằng API: chỉ lưu trữ. Không export collection đang chứa token để đưa lên Git. Xem docs/postman/README-course.md.' },
  auth: { type: 'noauth' },
  variable: Object.entries({ baseUrl: 'http://localhost:8080', adminEmail: 'admin@elearning.hunre.edu.vn', adminPassword: 'Admin@123456', qaPassword: 'Test@123456', adminToken: '', tokenA: '', tokenB: '', studentToken: '', missingId: '9007199254740991' }).map(([key,value])=>({key,value,type:'string'})),
  item: [],
};
const helpers = `
const cv = pm.collectionVariables;
const get = (key) => cv.get(key);
const set = (key,value) => cv.set(key,value);
const id = (key) => Number(get(key));
const unique = () => get('runId') + '-' + pm.variables.replaceIn('{{$guid}}').slice(0,8);
async function call(method, url, body, who='tokenA', expected=200) {
  const header = {'Content-Type':'application/json'};
  if (who) header.Authorization = 'Bearer ' + get(who);
  const response = await new Promise((resolve,reject)=>pm.sendRequest({method,url:get('baseUrl')+url,header,...(body === undefined ? {} : {body:{mode:'raw',raw:JSON.stringify(body)}})},(error,response)=>error?reject(error):resolve(response)));
  pm.expect(response.code,method+' '+url).to.eql(expected);
  const json = response.json();
  pm.expect(json.success,method+' '+url+' success').to.eql(expected < 400);
  return json.data;
}
const categoryBody = () => ({name:'QA '+unique(),slug:'qa-'+unique(),position:0});
const courseBody = () => ({categoryId:id('categoryId'),title:'Khoa QA '+unique(),slug:'khoa-qa-'+unique(),level:'BEGINNER',language:'vi',price:0});
const sectionBody = () => ({title:'Chuong QA',position:1});
const lessonBody = () => ({title:'Bai QA',type:'ARTICLE',durationSeconds:60,position:1,isPreview:true,content:'Noi dung xem thu QA',contentUrl:'https://example.com/preview'});
const resourceBody = () => ({name:'Tai lieu QA',fileUrl:'https://example.com/qa.pdf'});
async function category(body=categoryBody()) { return call('POST','/api/categories',body,'tokenA',201); }
async function course(body=courseBody(),who='tokenA') { return call('POST','/api/courses',body,who,201); }
async function section(courseId=id('publishedCourseId')) { return call('POST','/api/courses/'+courseId+'/sections',sectionBody(),'tokenA',201); }
async function lesson(sectionId=id('sectionAId'),body=lessonBody()) { return call('POST','/api/sections/'+sectionId+'/lessons',body,'tokenA',201); }
async function resource(lessonId=id('lessonAId')) { return call('POST','/api/lessons/'+lessonId+'/resources',resourceBody(),'tokenA',201); }
async function status(courseId,value) { return call('PATCH','/api/courses/'+courseId+'/status',{status:value}); }
async function eventually(fn, message) {
  let last;
  for (let n=0;n<30;n++) { try { return await fn(); } catch(e) { last=e; await new Promise(resolve=>setTimeout(resolve,1000)); } }
  throw new Error(message+': '+last.message);
}
`;
function script(code) { return {type:'text/javascript',exec:(`(async () => {\n${code}\n})().catch(error => { pm.test('Script chạy thành công', () => { throw error; }); pm.execution.setNextRequest(null); });`).trim().split('\n')}; }
function folder(name) { const f={name,item:[]};collection.item.push(f);return f; }
function request(f,name,method,url,who,body,expected,post='',pre='') {
  const item={name,request:{method,header:body ? [{key:'Content-Type',value:'application/json'}] : [],auth:who ? {type:'bearer',bearer:[{key:'token',value:`{{${who}}}`,type:'string'}]}:{type:'noauth'},url:'{{baseUrl}}'+url},event:[]};
  if(body) item.request.body={mode:'raw',raw:typeof body==='string'?body:JSON.stringify(body,null,2),options:{raw:{language:'json'}}};
  if(pre) item.event.push({listen:'prerequest',script:script(helpers+`\ntry {\n${pre}\n} catch(e) { pm.test('Chuẩn bị dữ liệu thành công',()=>{throw e;}); pm.execution.setNextRequest(null); pm.execution.skipRequest(); }`)});
  item.event.push({listen:'test',script:script(helpers+`
pm.test('HTTP ${JSON.stringify(expected)}',()=>pm.expect(pm.response.code).to.be.oneOf(${JSON.stringify(Array.isArray(expected)?expected:[expected])}));
const json = pm.response.json();
const d = json.data;
if (!pm.request.url.toString().includes('/actuator/health')) {
  pm.test('Vỏ response đúng',()=>{pm.expect(json.success).to.eql(pm.response.code < 400); if(pm.response.code >= 400) { pm.expect(json.code).to.be.a('string'); pm.expect(json.message).to.be.a('string'); }});
}
${post ? `try { ${post} } catch(e) { pm.test('Nghiệp vụ đúng',()=>{throw e;}); }` : ''}
`)});
  f.item.push(item);return item;
}
const setup=folder('0. Chuẩn bị');
request(setup,'0.1 Gateway và lượt kiểm thử mới','GET','/actuator/health',null,null,200,"pm.test('Gateway UP',()=>pm.expect(json.status).to.eql('UP'));",`for(const k of ['adminToken','tokenA','tokenB','studentToken']) cv.unset(k); set('runId',Date.now()+'-'+pm.variables.replaceIn('{{$guid}}').slice(0,8)); set('missingSlug','absent-'+get('runId'));`);
function login(name,email,token,userId,refresh) { request(setup,name,'POST','/api/auth/login',null,{email,password:email==='{{adminEmail}}'?'{{adminPassword}}':'{{qaPassword}}'},200,`if(pm.response.code!==200){pm.execution.setNextRequest(null);} else {set('${token}',d.accessToken);set('${userId}',d.user.id);set('${refresh}',d.refreshToken);}`); }
login('0.2 Đăng nhập admin','{{adminEmail}}','adminToken','adminId','adminRefresh');
for (const [tag,email,token,userId,refresh] of [['A','qa.instructor.a@example.com','tokenA','instructorAId','refreshA'],['B','qa.instructor.b@example.com','tokenB','instructorBId','refreshB'],['S','qa.student@example.com','studentToken','studentId','studentRefresh']]) {
  request(setup,`0.3.${tag} Đăng ký QA (409 phải đăng nhập xác nhận)`,'POST','/api/auth/register',null,{email,password:'{{qaPassword}}',fullName:'QA '+tag},[201,409]);
  login(`0.4.${tag} Đăng nhập lấy ID`,email,token,userId,refresh);
  request(setup,`0.5.${tag} Admin đặt vai trò QA`,'PATCH',`/api/users/{{${userId}}}/roles`,'adminToken',{roles:tag==='S'?['ROLE_STUDENT']:['ROLE_STUDENT','ROLE_INSTRUCTOR']},200);
  login(`0.6.${tag} Đăng nhập lại nhận quyền`,email,token,userId,refresh);
}
request(setup,'0.7 Dựng fixture và chờ ghi danh qua Kafka','GET','/api/courses/{{publishedCourseId}}','tokenA',null,200,"pm.test('Khóa mẫu đã xuất bản',()=>pm.expect(d.status).to.eql('PUBLISHED'));",`
await call('GET','/api/categories/'+get('missingId'),undefined,null,404);
await call('GET','/api/courses/'+get('missingId'),undefined,null,404);
const cat=await category(); set('categoryId',cat.id);set('categorySlug',cat.slug);
const child=await category({...categoryBody(),parentId:cat.id});set('childCategoryId',child.id);
const draft=await course();set('courseAId',draft.id);set('courseASlug',draft.slug);
const other=await course(courseBody(),'tokenB');set('courseBId',other.id);
const pub=await course();set('publishedCourseId',pub.id);set('publishedCourseSlug',pub.slug);
const ds=await section(draft.id);set('draftSectionId',ds.id);set('draftLessonId',(await lesson(ds.id)).id);
const sec=await section(pub.id);set('sectionAId',sec.id);
const preview=await lesson(sec.id);set('lessonAId',preview.id);set('previewLessonId',preview.id);
const regular=await lesson(sec.id,{...lessonBody(),position:2,isPreview:false,content:'Noi dung bao ve QA',contentUrl:'https://example.com/private'});set('lesson2Id',regular.id);
set('resourceId',(await resource(preview.id)).id);await resource(regular.id);
await status(pub.id,'PUBLISHED');
const enrollment=await eventually(()=>call('POST','/api/enrollments',{courseId:pub.id},'studentToken',201),'Chờ snapshot/ghi danh');set('enrollmentId',enrollment.id);
await eventually(async()=>{const c=await call('GET','/api/courses/'+pub.id);pm.expect(c.studentCount).to.eql(1);},'Chờ studentCount');
`);
const mappings={
1:[['','?page=0&size=2&sort=position,asc',200],['S','',200],['ADM','',200],['','?page=999999&size=2',200],['','?sort=abcxyz',400],['','?sort=position,desc',200],['','?page=0&size=1',200]],
2:[['','',200],['S','',200],['A','',200],['ADM','',200],['','',200],['','',200]],
3:[['','{{categoryId}}',200],['S','{{categoryId}}',200],['A','{{categoryId}}',200],['','{{missingId}}',404],['','abc',400],['','{{childCategoryId}}',200]],
4:[['','{{categorySlug}}',200],['S','{{categorySlug}}',200],['A','{{categorySlug}}',200],['','{{missingSlug}}',404],['','9223372036854775807',404],['','{{tempSlug}}',200]],
8:[['','?page=0&size=2',200],['S','?categoryId={{categoryId}}',200],['','?categoryId={{missingId}}',200],['','?categoryId=abc',400],['','?level=UNKNOWN',400],['','?sort=abcxyz',400],['A','?instructorId={{instructorAId}}&size=100',200],['B','?instructorId={{instructorAId}}&size=100',200],['ADM','?instructorId={{instructorAId}}&size=100',200],['','?instructorId=abc',400],['','?instructorId={{missingId}}',200]],
9:[['','{{publishedCourseId}}',200],['A','{{courseAId}}',200],['S','{{courseAId}}',404],['','{{courseAId}}',404],['','{{missingId}}',404],['','abc',400],['ADM','{{courseAId}}',200],['B','{{courseAId}}',404]],
10:[['','{{publishedCourseSlug}}',200],['A','{{courseASlug}}',200],['','{{courseASlug}}',404],['S','{{courseASlug}}',404],['','{{missingSlug}}',404],['ADM','{{courseASlug}}',200],['','9223372036854775807',404]],
15:[['','{{publishedCourseId}}',200],['','{{courseAId}}',404],['S','{{courseAId}}',404],['A','{{courseAId}}',200],['A','{{missingId}}',404],['A','abc',400],['ADM','{{courseAId}}',200]],
19:[['','{{lessonAId}}',200],['','{{draftLessonId}}',404],['S','{{draftLessonId}}',404],['A','{{draftLessonId}}',200],['','{{missingId}}',404],['','abc',400],['','{{previewLessonId}}',200],['B','{{lesson2Id}}',200],['S','{{lesson2Id}}',200],['A','{{lesson2Id}}',200]],
};
const tokens={'A':'tokenA','B':'tokenB','S':'studentToken','ADM':'adminToken','':null};
const seen=new Set();
function add(f,g,n,method,url,role,expected,body,pre='',post='') {
  const c=cases.get(`${g}.${n}`); if(!c)throw Error('Unknown case '+g+'.'+n); seen.add(`${g}.${n}`);
  const item=request(f,`COURSE-${String(g).padStart(2,'0')}.${n} ${c.title.replace(/ \[.*?\]/g,'')}`,method,url,tokens[role],body,expected,post,pre);
  item.request.description=`Mong đợi theo course.md: ${c.expected}. Dùng fixture riêng cho thao tác làm thay đổi/xóa dữ liệu.`;
}
for(const [gText,rows] of Object.entries(mappings)) {
  const g=Number(gText),f=folder(`COURSE-${String(g).padStart(2,'0')} — Đọc dữ liệu`);
  rows.forEach(([role,suffix,code],i)=>{
    const n=i+1;
    let url=({1:'/api/categories',2:'/api/categories/tree',3:'/api/categories/',4:'/api/categories/slug/',8:'/api/courses',9:'/api/courses/',10:'/api/courses/slug/',15:'/api/courses/',19:'/api/lessons/'})[g]+suffix;
    if(g===15)url+='/curriculum';
    let pre='',post='';
    if(g===1&&n===1)post="set('categoryTotal',d.totalElements);pm.test('Phân trang',()=>{pm.expect(d.content.length).at.most(2);pm.expect(d.page).eql(0);pm.expect(d.size).eql(2);pm.expect(d.totalPages).eql(Math.ceil(d.totalElements/2));});";
    if(g===1&&n===4)post="pm.test('Trang rỗng',()=>pm.expect(d.content).eql([]));";
    if(g===1&&n===6)post="pm.test('Position giảm dần',()=>{for(let i=1;i<d.content.length;i++)pm.expect(d.content[i].position).at.most(d.content[i-1].position);});";
    if(g===1&&n===7)post="pm.test('Size và total',()=>{pm.expect(d.content.length).at.most(1);pm.expect(d.totalElements).eql(id('categoryTotal'));});";
    if(g===2&&n===1)post="pm.test('Cây cha con',()=>pm.expect(d.find(c=>c.id===id('categoryId')).subCategories.map(c=>c.id)).include(id('childCategoryId')));";
    if(g===2&&n>=5){pre="set('tempId',(await category()).id);"+(n===6?"await call('DELETE','/api/categories/'+id('tempId'));":"");post=`pm.test('Danh mục ${n===5?'mới':'đã xóa'}',()=>pm.expect(d.map(c=>c.id)).${n===6?'not.':''}include(id('tempId')));`;}
    if(g===3&&n<4)post="pm.test('Đúng ID',()=>pm.expect(d.id).eql(id('categoryId')));";
    if(g===3&&n===6)post="pm.test('Đúng cha',()=>pm.expect(d.parentId).eql(id('categoryId')));";
    if(g===4&&n<4)post="pm.test('Đúng slug',()=>pm.expect(d.id).eql(id('categoryId')));";
    if(g===4&&n===6){pre="const c=await category();set('tempId',c.id);set('oldSlug',c.slug);const updated=await call('PUT','/api/categories/'+c.id,categoryBody());set('tempSlug',updated.slug);await call('GET','/api/categories/slug/'+c.slug,undefined,null,404);";post="pm.test('Slug mới cùng ID',()=>pm.expect(d.id).eql(id('tempId')));";}
    if(g===8&&code===200){post="pm.test('Bộ lọc khóa học',()=>{";
      if([1,2,8].includes(n))post+="pm.expect(d.content.every(c=>c.status==='PUBLISHED')).eql(true);";
      if(n===1)post+="pm.expect(d.content.length).at.most(2);";
      if(n===2)post+="pm.expect(d.content.map(c=>c.id)).not.include(id('courseAId'));pm.expect(d.content.every(c=>c.categoryId===id('categoryId'))).eql(true);";
      if([3,11].includes(n))post+="pm.expect(d.content).eql([]);";
      if([7,9].includes(n))post+="pm.expect(d.content.map(c=>c.id)).include(id('courseAId'));";
      if([7,8,9].includes(n))post+="pm.expect(d.content.every(c=>c.instructorId===id('instructorAId'))).eql(true);";
      post+='});';}
    if([9,10].includes(g)&&code===200)post=`pm.test('Đúng trạng thái',()=>pm.expect(d.status).eql('${n===1?'PUBLISHED':'DRAFT'}'));`;
    if(g===15&&n===1)post="pm.test('Đề cương đúng và có thứ tự',()=>{pm.expect(d.map(s=>s.id)).include(id('sectionAId'));for(let i=1;i<d.length;i++)pm.expect(d[i].position).at.least(d[i-1].position);for(const s of d)for(let i=1;i<s.lessons.length;i++)pm.expect(s.lessons[i].position).at.least(s.lessons[i-1].position);const lessons=d.flatMap(s=>s.lessons);pm.expect(lessons.find(l=>l.id===id('lesson2Id')).content).eql(null);});";
    if(g===19&&[1,4,7,9,10].includes(n))post=`pm.test('Nội dung đúng',()=>{pm.expect(d.content).eql('${n>=9?'Noi dung bao ve QA':'Noi dung xem thu QA'}');pm.expect(d.contentUrl).eql('https://example.com/${n>=9?'private':'preview'}');});`;
    if(g===19&&n===8)post="pm.test('Ẩn nội dung và tài liệu',()=>{pm.expect(d.content).eql(null);pm.expect(d.contentUrl).eql(null);pm.expect(d.resources).eql([]);});";
    add(f,g,n,'GET',url,role,code,null,pre,post);
  });
}
// Các ca ghi dùng bản sao riêng; lỗi quyền không được gây thay đổi dữ liệu.
const writeGroups={5:['POST','category',[201,401,403,404,400,409,422,201]],6:['PUT','category',[200,401,403,404,400,409,422,400]],7:['DELETE','category',[200,401,403,404,400,422,422,200]],11:['POST','course',[201,401,403,404,400,409,400,400,201]],12:['PUT','course',[200,401,403,404,400,403,200,422,409,200]],13:['PATCH','status',[200,401,403,404,400,403,400,400,200,200]],14:['DELETE','course',[200,401,403,404,400,403,422,200]],16:['POST','section',[201,401,403,404,400,403,201,400]],17:['PUT','section',[200,401,403,404,400,403,200,400]],18:['DELETE','section',[200,401,403,404,400,403,200,200]],20:['POST','lesson',[201,401,403,404,400,403,201,400,400]],21:['PUT','lesson',[200,401,403,404,400,403,200,200,400]],22:['DELETE','lesson',[200,401,403,404,400,403,200,404]],23:['POST','resource',[201,401,403,404,400,403,201,400]],24:['DELETE','resource',[200,401,403,404,400,403,404,200]]};
for(const [gText,[method,kind,codes]] of Object.entries(writeGroups)) {
  const g=Number(gText),f=folder(`COURSE-${String(g).padStart(2,'0')} — Ghi và phân quyền`);
  codes.forEach((code,i)=>{
    const n=i+1;
    let role=n===2?'':n===3?'S':'A';
    if([12,13,14,16,17,18,20,21,22,23,24].includes(g)&&n===6)role='B';
    if(([5,7,14,24].includes(g)&&n===8)||([12,16,17,18,20,21,22,23].includes(g)&&n===7)||(g===13&&n===9))role='ADM';
    let pre="let body;let target;",url='',post='',body=method==='DELETE'?null:'{{requestBody}}';
    if(kind==='category') {
      pre+='body=categoryBody();';url='/api/categories';
      if(method!=='POST') {pre+="target=await category();set('tempId',target.id);";url+='/{{tempId}}';}
      if(n===4){if(method==='POST')pre+="body.parentId=id('missingId');";else url='/api/categories/{{missingId}}';}
      if(n===5){if(method==='POST')pre+='body.position=-1;';else url='/api/categories/abc';}
      if([5,6].includes(g)&&n===6)pre+="body.slug=get('categorySlug');";
      if(g===5&&n===7)pre+="body.parentId=id('childCategoryId');";
      if(g===6&&n===7)pre+='body.parentId=target.id;';
      if(g===6&&n===8)pre+="body.name='';";
      if(g===7&&n===6){pre+="await category({...categoryBody(),parentId:target.id});";}
      if(g===7&&n===7){pre+="await course({...courseBody(),categoryId:target.id});";}
    }
    if(kind==='course'||kind==='status') {
      pre+='body=courseBody();';url='/api/courses';
      if(method!=='POST'){pre+="target=await course();set('tempId',target.id);";url+='/{{tempId}}';}
      if(kind==='status'){url+='/status';pre+="body={status:'PUBLISHED'};";}
      if(n===4){if(method==='POST')pre+="body.categoryId=id('missingId');";else url='/api/courses/{{missingId}}'+(kind==='status'?'/status':'');}
      if(n===5){if(method==='POST')pre+="body.categoryId='abc';";else url='/api/courses/abc'+(kind==='status'?'/status':'');}
      if(g===11&&n===6)pre+="body.slug=get('courseASlug');";
      if(g===11&&n===7)pre+='body.price=-1;';
      if(g===11&&n===8)pre+='delete body.title;';
      if(g===11&&n===9)pre+="body.instructorId=id('instructorBId');";
      if(g===12&&n===8)pre+="await status(target.id,'ARCHIVED');";
      if(g===12&&n===9)pre+="body.slug=get('courseASlug');";
      if(g===12&&n===10){url='/api/courses/{{publishedCourseId}}';pre+="body.slug=get('publishedCourseSlug');body.title='QA updated '+unique();set('updatedTitle',body.title);";post="await eventually(async()=>{const enrollment=await call('GET','/api/enrollments/'+id('enrollmentId'),undefined,'studentToken');pm.expect(enrollment.courseTitle).eql(get('updatedTitle'));},'Snapshot đổi tên');pm.test('Snapshot nhận course.updated',()=>pm.expect(d.title).eql(get('updatedTitle')));";}
      if(g===13&&n===7)pre+="body.status='INVALID';";
      if(g===13&&n===8)pre+='body={};';
      if(g===13&&[9,10].includes(n))pre+="await status(target.id,'PUBLISHED');body.status='ARCHIVED';";
      if(g===14&&n===7)pre+="await status(target.id,'PUBLISHED');";
    }
    if(kind==='section') {
      pre+='body=sectionBody();';
      if(method==='POST'){url='/api/courses/{{publishedCourseId}}/sections';}
      else {pre+="target=await section();set('tempId',target.id);";url='/api/sections/{{tempId}}';}
      if(n===4)url=method==='POST'?'/api/courses/{{missingId}}/sections':'/api/sections/{{missingId}}';
      if(n===5)url=method==='POST'?'/api/courses/abc/sections':'/api/sections/abc';
      if(g===16&&n===8)pre+="body.title='';";
      if(g===17&&n===8)pre+='body.position=-1;';
      if(g===18&&n===8)pre+='const l1=await lesson(target.id);const l2=await lesson(target.id);set("deletedLesson1",l1.id);set("deletedLesson2",l2.id);';
    }
    if(kind==='lesson') {
      pre+='body=lessonBody();';
      if(method==='POST')url='/api/sections/{{sectionAId}}/lessons';
      else {pre+="target=await lesson();set('tempId',target.id);";url='/api/lessons/{{tempId}}';}
      if(n===4)url=method==='POST'?'/api/sections/{{missingId}}/lessons':'/api/lessons/{{missingId}}';
      if(n===5)url=method==='POST'?'/api/sections/abc/lessons':'/api/lessons/abc';
      if(g===20&&n===8)pre+='body.durationSeconds=-1;';
      if(g===20&&n===9)pre+="body.type='INVALID';";
      if(g===21&&n===8)pre+='body.durationSeconds=120;';
      if(g===21&&n===9)pre+="body.title='';";
      if(g===22&&n===8)pre+="await call('DELETE','/api/lessons/'+target.id);";
    }
    if(kind==='resource') {
      pre+="body=resourceBody();const l=await lesson();set('tempLessonId',l.id);";url='/api/lessons/{{tempLessonId}}/resources';
      if(method==='DELETE'){pre+="target=await resource(l.id);set('tempId',target.id);";url+='/{{tempId}}';}
      if(n===4)url=method==='POST'?'/api/lessons/{{missingId}}/resources':'/api/lessons/{{tempLessonId}}/resources/{{missingId}}';
      if(n===5)url=method==='POST'?'/api/lessons/abc/resources':'/api/lessons/{{tempLessonId}}/resources/abc';
      if(g===23&&n===8)pre+='delete body.fileUrl;';
      if(g===24&&n===7)url='/api/lessons/{{lesson2Id}}/resources/{{tempId}}';
    }
    pre+="set('requestBody',JSON.stringify(body));";
    if(method==='POST'&&['category','course'].includes(kind)) {
      const list=kind==='category'?'/api/categories?size=1':"/api/courses?size=1&instructorId='+id('instructorAId')+'";
      pre+=`set('beforeTotal',(await call('GET','${list}')).totalElements);`;
      post+=`const total=(await call('GET','${list}')).totalElements;pm.test('Số bản ghi ${code<300?'tăng đúng một':'không đổi khi bị từ chối'}',()=>pm.expect(total).eql(id('beforeTotal')+${code<300?1:0}));`;
    }
    // Chụp trạng thái trước mọi thao tác để kiểm các request bị từ chối không ghi dữ liệu.
    if(['section','lesson','resource'].includes(kind))pre+="set('beforeCourse',JSON.stringify(await call('GET','/api/courses/'+id('publishedCourseId'))));set('beforeCurriculum',JSON.stringify(await call('GET','/api/courses/'+id('publishedCourseId')+'/curriculum')));";
    else if(targetType(kind)&&method!=='POST')pre+=`set('beforeTarget',JSON.stringify(await call('GET','/api/${kind==='category'?'categories':'courses'}/'+id('tempId'))));`;
    if(code<300&&method!=='DELETE')post+=`pm.test('Dữ liệu ghi đúng',()=>{const b=JSON.parse(get('requestBody'));${kind==='status'?"pm.expect(d.status).eql(b.status);if(b.status==='PUBLISHED')pm.expect(d.publishedAt).to.be.a('string');":kind==='category'?"pm.expect(d.name).eql(b.name);pm.expect(d.slug).eql(b.slug);":kind==='resource'?"pm.expect(d.name).eql(b.name);pm.expect(d.fileUrl).eql(b.fileUrl);": "pm.expect(d.title).eql(b.title);"}${g===11?"pm.expect(d.status).eql('DRAFT');pm.expect(d.instructorId).eql(id('instructorAId'));":''}});`;
    if(['section','lesson','resource'].includes(kind)) {
      post+="const after=await call('GET','/api/courses/'+id('publishedCourseId'));const curriculum=await call('GET','/api/courses/'+id('publishedCourseId')+'/curriculum');const all=curriculum.flatMap(s=>s.lessons);const before=JSON.parse(get('beforeCourse'));pm.test('Tổng bài và thời lượng khớp dữ liệu thật',()=>{pm.expect(after.totalLessons).eql(all.length);pm.expect(after.totalDurationSeconds).eql(all.reduce((sum,l)=>sum+l.durationSeconds,0));});";
      if(code>=400)post+="pm.test('Từ chối không đổi dữ liệu',()=>{pm.expect(curriculum).eql(JSON.parse(get('beforeCurriculum')));pm.expect(after.totalLessons).eql(before.totalLessons);pm.expect(after.totalDurationSeconds).eql(before.totalDurationSeconds);});";
      if(code<300&&g===20)post+="pm.test('Tăng đúng một bài',()=>{pm.expect(after.totalLessons).eql(before.totalLessons+1);pm.expect(d.sectionId).eql(id('sectionAId'));});";
      if(code<300&&g===21&&n===8)post+="pm.test('Thời lượng tăng 60',()=>pm.expect(after.totalDurationSeconds).eql(before.totalDurationSeconds+60));";
      if(code<300&&method==='DELETE') {
        if(kind==='lesson')post+="await call('GET','/api/lessons/'+id('tempId'),undefined,'tokenA',404);pm.test('Giảm một bài',()=>pm.expect(after.totalLessons).eql(before.totalLessons-1));";
        if(kind==='section')post+="pm.test('Chương đã mất',()=>pm.expect(curriculum.map(s=>s.id)).not.include(id('tempId')));";
        if(kind==='resource')post+="const l=await call('GET','/api/lessons/'+id('tempLessonId'));pm.test('Tài liệu đã mất',()=>pm.expect(l.resources.map(r=>r.id)).not.include(id('tempId')));";
        if(g===18&&n===8)post+="await call('GET','/api/lessons/'+id('deletedLesson1'),undefined,'tokenA',404);await call('GET','/api/lessons/'+id('deletedLesson2'),undefined,'tokenA',404);pm.test('Xóa đúng hai bài và 120 giây',()=>{pm.expect(after.totalLessons).eql(before.totalLessons-2);pm.expect(after.totalDurationSeconds).eql(before.totalDurationSeconds-120);});";
      }
    } else if(method==='DELETE'&&code===200)post+=`await call('GET','/api/${kind==='category'?'categories':'courses'}/'+id('tempId'),undefined,'tokenA',404);pm.test('Đọc lại 404 sau xóa',()=>pm.expect(true).eql(true));`;
    else if(code>=400&&method!=='POST')post+=`const after=await call('GET','/api/${kind==='category'?'categories':'courses'}/'+id('tempId'));pm.test('Bản sao không đổi sau từ chối',()=>pm.expect(after).eql(JSON.parse(get('beforeTarget'))));`;
    if(g===13&&n===10)post+="await call('GET','/api/courses/'+id('tempId'),undefined,null,404);";
    add(f,g,n,method,url,role,code,body,pre,post);
  });
}
function targetType(k){return ['category','course','status'].includes(k);}
const extra=folder('25. Hồi quy sắp xếp và quyền học khóa lưu trữ');
request(extra,'COURSE-SORT.1 Giá tăng và dữ liệu phân biệt','GET','/api/courses?categoryId={{sortCategoryId}}&sort=price,asc&sort=id,desc',null,null,200,
  "pm.test('Giá tăng đúng',()=>pm.expect(d.content.map(c=>c.price)).eql([0,100,300]));",`
  const cat=await category();set('sortCategoryId',cat.id);
  const ids=[];
  for(const price of [100,300,0]) {const c=await course({...courseBody(),categoryId:cat.id,price});ids.push(c.id);await status(c.id,'PUBLISHED');}
  set('sortCourseIds',JSON.stringify(ids));
  await eventually(()=>call('POST','/api/enrollments',{courseId:ids[0]},'studentToken',201),'Snapshot khóa kiểm sort');
  await eventually(async()=>pm.expect((await call('GET','/api/courses/'+ids[0])).studentCount).eql(1),'Số học viên khóa kiểm sort');
`);
for(const [suffix,sort,expected,field] of [['2','price,desc','[300,100,0]','price'],['3','studentCount,desc','[1,0,0]','studentCount'],['4','createdAt,desc',"JSON.parse(get('sortCourseIds')).reverse()",'id']]) {
  request(extra,`COURSE-SORT.${suffix} ${sort}`,'GET',`/api/courses?categoryId={{sortCategoryId}}&sort=${sort}&sort=id,desc`,null,null,200,`pm.test('Thứ tự đúng',()=>pm.expect(d.content.map(c=>c.${field})).eql(${expected}));`);
}
request(extra,'COURSE-SORT.5 Trường sort sai trả 400','GET','/api/courses?sort=notAField,desc',null,null,400);
request(extra,'COURSE-ARCHIVED.1 Học viên giữ nội dung','GET','/api/lessons/{{lesson2Id}}','studentToken',null,200,
  "pm.test('Đã ghi danh vẫn học được',()=>pm.expect(d.content).eql('Noi dung bao ve QA'));", "await status(id('publishedCourseId'),'ARCHIVED');");
request(extra,'COURSE-ARCHIVED.2 Khách không thấy đề cương','GET','/api/courses/{{publishedCourseId}}/curriculum',null,null,404);
request(extra,'COURSE-ARCHIVED.3 Người chưa ghi danh không thấy','GET','/api/courses/{{publishedCourseId}}/curriculum','tokenB',null,404);
request(extra,'COURSE-ARCHIVED.4 Học viên đọc được đề cương','GET','/api/courses/{{publishedCourseId}}/curriculum','studentToken',null,200,
  "pm.test('Đề cương giữ nội dung',()=>pm.expect(d.flatMap(s=>s.lessons).find(l=>l.id===id('lesson2Id')).content).eql('Noi dung bao ve QA'));");
// Snapshot đã cập nhật chặn bằng 422; snapshot còn cũ thì kiểm nguồn trả 404 vì khóa bị ẩn.
request(extra,'COURSE-ARCHIVED.5 Không nhận ghi danh mới','POST','/api/enrollments','tokenB','{"courseId":{{publishedCourseId}}}',[404,422],
  "const enrollments=await call('GET','/api/enrollments?size=100',undefined,'tokenB');pm.test('Không tạo lượt ghi danh',()=>pm.expect(enrollments.content.map(e=>e.courseId)).not.include(id('publishedCourseId')));await status(id('publishedCourseId'),'PUBLISHED');");
const categoryRegression=folder('26. Hồi quy danh mục cha và con');
request(categoryRegression,'COURSE-CATEGORY.1 Danh mục cha gồm khóa trực tiếp và khóa con','GET','/api/courses?categoryId={{treeParentId}}&sort=price,asc',null,null,200,
  "pm.test('Đủ ba khóa công khai, không lẫn danh mục khác/nháp/lưu trữ',()=>{pm.expect(d.content.map(c=>c.id)).eql(JSON.parse(get('treePublishedIds')));pm.expect(d.totalElements).eql(3);});",`
  const parent=await category();set('treeParentId',parent.id);
  const child=await category({...categoryBody(),parentId:parent.id});set('treeChildId',child.id);
  const sibling=await category({...categoryBody(),parentId:parent.id});set('treeSiblingId',sibling.id);
  const other=await category();
  const published=[];
  for(const [categoryId,price,level] of [[child.id,100,'BEGINNER'],[sibling.id,200,'ADVANCED'],[parent.id,300,'BEGINNER']]) {
    const c=await course({...courseBody(),categoryId,price,level,title:'Tree regression '+unique()});published.push(c.id);await status(c.id,'PUBLISHED');
  }
  set('treePublishedIds',JSON.stringify(published));
  await course({...courseBody(),categoryId:child.id});
  const archived=await course({...courseBody(),categoryId:child.id});await status(archived.id,'PUBLISHED');await status(archived.id,'ARCHIVED');
  const unrelated=await course({...courseBody(),categoryId:other.id});await status(unrelated.id,'PUBLISHED');
`);
request(categoryRegression,'COURSE-CATEGORY.2 Danh mục con không lấy khóa cha hoặc anh em','GET','/api/courses?categoryId={{treeChildId}}',null,null,200,
  "pm.test('Chỉ khóa của danh mục con',()=>{pm.expect(d.content.map(c=>c.id)).eql([JSON.parse(get('treePublishedIds'))[0]]);pm.expect(d.totalElements).eql(1);});");
request(categoryRegression,'COURSE-CATEGORY.3 Danh mục con cùng cha vẫn độc lập','GET','/api/courses?categoryId={{treeSiblingId}}',null,null,200,
  "pm.test('Chỉ khóa của danh mục anh em',()=>pm.expect(d.content.map(c=>c.id)).eql([JSON.parse(get('treePublishedIds'))[1]]));");
for(const [n,page,index] of [[4,0,0],[5,1,2]]) {
  request(categoryRegression,`COURSE-CATEGORY.${n} Lọc phối hợp và phân trang ${page}`,'GET',`/api/courses?categoryId={{treeParentId}}&instructorId={{instructorAId}}&level=BEGINNER&keyword=Tree%20regression&sort=price,asc&sort=id,desc&size=1&page=${page}`,null,null,200,
    `pm.test('Giữ bộ lọc và tổng phân trang, không trùng/mất khóa',()=>{pm.expect(d.content.map(c=>c.id)).eql([JSON.parse(get('treePublishedIds'))[${index}]]);pm.expect(d.totalElements).eql(2);pm.expect(d.totalPages).eql(2);});`);
}
request(categoryRegression,'COURSE-CATEGORY.6 Danh mục không tồn tại trả trang rỗng','GET','/api/courses?categoryId={{missingId}}',null,null,200,
  "pm.test('Trang rỗng',()=>{pm.expect(d.content).eql([]);pm.expect(d.totalElements).eql(0);});");
const reviewFolder=folder('COURSE-25 — Đánh giá khóa học');
const reviewUrl='/api/courses/{{reviewCourseId}}/reviews';
const stats=(count,avg)=>`const c=await call('GET','/api/courses/'+id('reviewCourseId'));pm.test('Điểm và số lượt chính xác',()=>{pm.expect(c.ratingCount).eql(${count});pm.expect(c.ratingAvg).eql(${avg});});`;
add(reviewFolder,25,1,'GET',reviewUrl,'',200,null,`
  const c=await course();set('reviewCourseId',c.id);await status(c.id,'PUBLISHED');
  for(const who of ['studentToken','tokenA']) {
    await eventually(()=>call('POST','/api/enrollments',{courseId:c.id},who,201),'Ghi danh khóa đánh giá');
    await eventually(async()=>pm.expect((await call('GET','/api/courses/'+c.id+'/reviews/me',undefined,who)).canReview).eql(true),'Chờ quyền đánh giá qua Kafka');
  }
`,"pm.test('Chưa có đánh giá',()=>{pm.expect(d.content).eql([]);pm.expect(d.totalElements).eql(0);});");
add(reviewFolder,25,2,'PUT',reviewUrl+'/me','',401,{rating:5});
add(reviewFolder,25,3,'PUT',reviewUrl+'/me','B',403,{rating:5},'',stats(0,0));
add(reviewFolder,25,4,'PUT',reviewUrl+'/me','S',200,{rating:5,comment:'Hữu ích'},'',stats(1,5)+"set('firstReviewId',d.id);");
add(reviewFolder,25,5,'PUT',reviewUrl+'/me','A',200,{rating:3,comment:'Khá tốt'},'',stats(2,4)+"set('secondReviewId',d.id);");
add(reviewFolder,25,6,'PUT',reviewUrl+'/me','S',200,{rating:1,comment:'Đã sửa'},'',stats(2,2)+"pm.test('Sửa đúng bản ghi',()=>pm.expect(d.id).eql(id('firstReviewId')));");
add(reviewFolder,25,7,'GET',reviewUrl+'/me','S',200,null,'',"pm.test('Đúng đánh giá của mình',()=>{pm.expect(d.canReview).eql(true);pm.expect(d.review.rating).eql(1);});");
add(reviewFolder,25,8,'GET',reviewUrl+'?size=1','',200,null,'',"pm.test('Mới nhất trước, không lộ email/ID người viết',()=>{pm.expect(d.totalElements).eql(2);pm.expect(d.content[0].id).eql(id('secondReviewId'));pm.expect(d.content[0]).not.have.property('email');pm.expect(d.content[0]).not.have.property('userId');});const next=await call('GET','/api/courses/'+id('reviewCourseId')+'/reviews?size=1&page=1',undefined,null);pm.test('Trang hai không trùng',()=>pm.expect(next.content[0].id).eql(id('firstReviewId')));");
add(reviewFolder,25,9,'DELETE',reviewUrl+'/me','B',403,null,'',stats(2,2));
add(reviewFolder,25,10,'DELETE',reviewUrl+'/me','S',200,null,'',stats(1,3));
add(reviewFolder,25,11,'DELETE',reviewUrl+'/me','A',200,null,'',stats(0,0));
add(reviewFolder,25,12,'DELETE',reviewUrl+'/me','S',404,null);
for(const [n,body] of [[13,{}],[14,{rating:0}],[15,{rating:6}],[16,{rating:2.5}],[17,{rating:5,comment:'x'.repeat(2001)}]])
  add(reviewFolder,25,n,'PUT',reviewUrl+'/me','S',400,body,'',stats(0,0));
add(reviewFolder,25,18,'PUT','/api/courses/{{missingId}}/reviews/me','S',404,{rating:5});
add(reviewFolder,25,19,'GET',reviewUrl+'/me','',401,null);
add(reviewFolder,25,20,'GET',reviewUrl,'',404,null,"await status(id('reviewCourseId'),'DRAFT');","await status(id('reviewCourseId'),'PUBLISHED');");
add(reviewFolder,25,21,'PUT',reviewUrl+'/me?userId={{studentId}}','B',403,'{"rating":5,"userId":{{studentId}}}','',stats(0,0));
add(reviewFolder,25,22,'PUT',reviewUrl+'/me','S',200,'{"rating":5,"userId":{{instructorAId}},"authorName":"Forged","email":"fake@example.com"}','',stats(1,5)+"const me=await call('GET','/api/auth/me',undefined,'studentToken');pm.test('Tên lấy từ tài khoản đã xác thực',()=>pm.expect(d.authorName).eql(me.fullName));");
add(reviewFolder,25,23,'PUT',reviewUrl+'/me','S',200,{rating:4},"await status(id('reviewCourseId'),'ARCHIVED');",stats(1,4)+"await status(id('reviewCourseId'),'PUBLISHED');");
add(reviewFolder,25,24,'GET',reviewUrl,'',200,null,"await Promise.all([call('PUT','/api/courses/'+id('reviewCourseId')+'/reviews/me',{rating:5},'studentToken'),call('PUT','/api/courses/'+id('reviewCourseId')+'/reviews/me',{rating:3},'tokenA')]);",stats(2,4)+"pm.test('Đúng hai người',()=>pm.expect(d.totalElements).eql(2));");
add(reviewFolder,25,25,'GET',reviewUrl,'',200,null,"await Promise.all(Array.from({length:6},()=>call('PUT','/api/courses/'+id('reviewCourseId')+'/reviews/me',{rating:2},'studentToken')));",stats(2,2.5)+"pm.test('Không nhân đôi bản ghi',()=>pm.expect(d.totalElements).eql(2));");
for(const key of cases.keys())if(!seen.has(key))throw Error('Missing case '+key);
// Giữ thứ tự đọc trước ghi để các tình huống đọc luôn có fixture nền nguyên vẹn.
fs.writeFileSync(path.join(root,'docs/postman/course.postman_collection.json'),JSON.stringify(collection,null,2)+'\n');
console.log(`Generated ${seen.size} mapped cases in ${collection.item.length} folders`);
