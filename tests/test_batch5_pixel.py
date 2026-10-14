"""Interactive Batch 5 smoke tests: genuine Chromium using the shipped file:// assets."""
from pathlib import Path
from playwright.sync_api import sync_playwright
import base64
ASSETS=(Path(__file__).resolve().parents[1]/'app/src/main/assets')
HTML=(ASSETS/'index.html').read_text(encoding='utf-8')
for sprite in (ASSETS/'sprites').glob('*.png'):
    uri='data:image/png;base64,'+base64.b64encode(sprite.read_bytes()).decode()
    HTML=HTML.replace('sprites/'+sprite.name,uri)
STUB=r'''() => {
 window.fakeProfile={avatar:'male',cat:'tabby',roomMode:'classic',onboardingComplete:false};
 window.saved=[{id:100,h:7,m:0,on:true,days:127,label:'Wake up',sound:'cute'}, {id:101,h:8,m:10,on:true,days:0,label:'Morning',sound:'real'}];
 window.events=[];
 window.Android={
  getProfile:()=>JSON.stringify(window.fakeProfile),setProfile:raw=>{window.fakeProfile=JSON.parse(raw);window.events.push('saved_profile');return true},getLastAlarmStage:()=> 'meow_started_or_fallback',openAppSettings:()=>window.events.push('open_settings'),
  getAlarms:()=>JSON.stringify(window.saved),getSettings:()=>'{}',getDefaultSound:()=>'real',
  saveAlarm:raw=>{const a=JSON.parse(raw);if(!a.id)a.id=102;const ix=window.saved.findIndex(x=>x.id===a.id);if(ix<0)window.saved.push(a);else window.saved[ix]=a;return JSON.stringify(a)},
  deleteAlarm:id=>{window.saved=window.saved.filter(x=>x.id!==id);return true},
  canSetExactAlarms:()=>true,hasCustomMeow:()=>false,previewMeow:()=>{},chooseCustomMeow:()=>{},isRinging:()=>false,
  setDefaultSound:()=>{},setSettings:()=>{},pickTime:()=>{},requestExactAlarmPermission:()=>{},
  testAlarm:id=>window.events.push('test:'+id),ringIn:n=>window.events.push('snooze:'+n),silence:()=>window.events.push('silence'),dismiss:()=>window.events.push('dismiss')
 };
}'''
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True, executable_path='/usr/bin/chromium', args=['--no-sandbox','--disable-dev-shm-usage'])
 for width in (320,390,520):
  context=browser.new_context(viewport={'width':width,'height':810},color_scheme='dark')
  page=context.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
  page.evaluate(STUB);page.set_content(HTML)
  page.wait_for_timeout(150)
  assert page.locator('#onboarding').is_visible(),('missing onboarding',width)
  page.locator('[data-avatar="female"]').click()
  assert page.locator('[data-avatar="female"]').get_attribute('aria-pressed')=='true'
  page.locator('#obNext').click()
  page.locator('[data-cat="calico"]').click()
  page.locator('#obNext').click()
  assert page.locator('#onboarding').is_hidden()
  assert page.evaluate('window.fakeProfile.avatar')=='female'
  assert page.evaluate('window.fakeProfile.cat')=='calico'
  assert page.locator('#pixelCat').get_attribute('src')=='sprites/cat_calico.png'
  page.locator('#pixelMode').click()
  assert page.locator('#pixelHouse').is_visible()
  assert page.evaluate('window.fakeProfile.roomMode')=='pixel'
  assert page.locator('.alarm-card').count()==2
  assert page.locator('#alarmDiagnostic').text_content()=='meow_started_or_fallback'
  # Native callback drives game states without playing a second WebView meow.
  page.evaluate('timings.petSec=1;timings.foodDelaySec=1;timings.feedSec=1;timings.snoozeSec=10;window.nativeRing()')
  page.wait_for_timeout(300)
  assert page.locator('#pixelPet').is_visible(),width
  page.locator('#pixelPet').click()
  page.wait_for_timeout(1400)
  assert page.evaluate("mode")=='snooze',(width,page.evaluate('mode'))
  assert 'snooze:10' in page.evaluate('window.events')
  page.wait_for_function("mode==='snooze' && food===1 && !document.getElementById('pixelFeed').hidden",timeout=3500)
  assert page.locator('#pixelFeed').is_visible(),(width,page.locator('#pixelStageTitle').inner_text())
  page.locator('#pixelFeed').click()
  page.wait_for_timeout(1400)
  assert page.evaluate('mode') in ('fed','idle'),page.evaluate('mode')
  assert 'dismiss' in page.evaluate('window.events')
  assert not errors,(width,errors)
  print('PASS',width,'px onboarding / mode / snooze / feed / preserved alarms')
  if width==390:
   assets={item.name:'data:image/png;base64,'+base64.b64encode(item.read_bytes()).decode() for item in (ASSETS/'sprites').glob('*.png')}
   page.evaluate("assets=>{document.querySelector('#pixelManTop').src=assets['avatar_female.png'];document.querySelector('#pixelManBottom').src=assets['avatar_female.png'];document.querySelector('#pixelCat').src=assets['cat_calico.png'];}",assets)
   page.wait_for_timeout(220)
   Path('/mnt/data/meow_batch5_build/BATCH5_PHONE_PREVIEW.png').write_bytes(page.screenshot(full_page=True))
  context.close()
 browser.close()

