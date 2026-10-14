"""Batch 6 browser state/gesture regression tests on actual packaged file URL."""
from pathlib import Path
from playwright.sync_api import sync_playwright
ROOT=Path(__file__).resolve().parents[1]
HTML=(ROOT/'app/src/main/assets/index.html').read_text(encoding='utf-8')
import base64,json
for img in (ROOT/'app/src/main/assets/pixel').glob('*'):
 if img.suffix in ('.png','.webp'):
  uri='data:image/'+('webp' if img.suffix=='.webp' else 'png')+';base64,'+base64.b64encode(img.read_bytes()).decode()
  HTML=HTML.replace('pixel/'+img.name,uri)
DYNAMIC={}
for img in (ROOT/'app/src/main/assets/pixel').glob('*.png'):
 DYNAMIC[img.stem]='data:image/png;base64,'+base64.b64encode(img.read_bytes()).decode()
HTML=HTML.replace("this.humanEl.src='pixel/human_'+this.avatar+'_'+pose+'.png'", "this.humanEl.src=window.dynamicSprites['human_'+this.avatar+'_'+pose]")
HTML=HTML.replace("this.catEl.src='pixel/cat_'+this.cat+'_'+pose+'.png'", "this.catEl.src=window.dynamicSprites['cat_'+this.cat+'_'+pose]")
HTML=HTML.replace('<script>', '<script>window.dynamicSprites='+json.dumps(DYNAMIC)+';</script><script>',1)
for img in (ROOT/'app/src/main/assets/sprites').glob('*.png'):
 uri='data:image/png;base64,'+base64.b64encode(img.read_bytes()).decode()
 HTML=HTML.replace('sprites/'+img.name,uri)
STUB=r'''(()=>{
window.events=[];window.fakeProfile={avatar:'female',cat:'orange',roomMode:'pixel',onboardingComplete:true};
window.saved=[{id:100,h:7,m:0,on:true,days:127,label:'Morning',sound:'real'}];
window.Android={
getProfile:()=>JSON.stringify(window.fakeProfile),setProfile:r=>{window.fakeProfile=JSON.parse(r);return true},getLastAlarmStage:()=>'before_foreground',openAppSettings:()=>{},
getAlarms:()=>JSON.stringify(window.saved),getSettings:()=>'{}',getDefaultSound:()=>'real',
saveAlarm:r=>r,deleteAlarm:id=>true,canSetExactAlarms:()=>true,hasCustomMeow:()=>false,previewMeow:()=>{},chooseCustomMeow:()=>{},isRinging:()=>false,
setDefaultSound:()=>{},setSettings:()=>{},pickTime:()=>{},requestExactAlarmPermission:()=>{},testAlarm:id=>window.events.push('test:'+id),
ringIn:n=>window.events.push('snooze:'+n),silence:()=>window.events.push('silence'),dismiss:()=>window.events.push('dismiss')};
})();'''

def main():
 with sync_playwright() as p:
  browser=p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox'])
  for width in (390,):
   context=browser.new_context(viewport={'width':width,'height':820},device_scale_factor=1,color_scheme='dark')
   page=context.new_page();errs=[];page.on('pageerror',lambda e:errs.append(str(e)))
   page.evaluate(STUB);page.set_content(HTML);page.wait_for_timeout(250)
   assert page.locator('#pixelHouse').is_visible(),(width,'house missing')
   assert page.evaluate('story.avatar')=='female'
   assert page.evaluate('story.cat')=='orange'
   assert page.evaluate("PixelStory.phase(6)")=='morning'
   assert page.evaluate("PixelStory.phase(12)")=='afternoon'
   assert page.evaluate("PixelStory.phase(18)")=='sunset'
   assert page.evaluate("PixelStory.phase(23)")=='night'
   assert page.locator('.alarm-card').count()==1
   # Ring from native without restarting a WebView audio player
   page.evaluate("timings.petSec=1;timings.feedSec=1;timings.foodDelaySec=1;timings.snoozeSec=10;window.nativeRing()")
   assert page.evaluate('story.stage')=='ALARM_REACT'
   page.wait_for_function("story.stage==='PET_READY'",timeout=17000)
   zone=page.locator('#storyPetZone').bounding_box();assert zone
   # Tapping cannot complete petting
   page.mouse.click(zone['x']+zone['width']*.5,zone['y']+zone['height']*.5)
   assert page.evaluate('story.petProgress')==0
   # Multiple real pointer strokes on cat
   for i in range(7):
    y=zone['y']+zone['height']*(.32+.04*(i%4))
    page.mouse.move(zone['x']+zone['width']*.19,y);page.mouse.down()
    for j in range(9):page.mouse.move(zone['x']+zone['width']*(.2+.062*j),y+(-2 if j%2 else 2),steps=1)
    page.mouse.up()
    if page.evaluate('story.snoozeScheduled'):break
   assert page.evaluate('story.snoozeScheduled'),(width,page.evaluate('story.petProgress'))
   assert page.evaluate('window.events.filter(x=>x==="silence").length')==1
   assert page.evaluate('window.events.filter(x=>x.startsWith("snooze:")).length')==1
   # Native snooze re-ring must not reset the food quest
   page.evaluate('window.nativeRing()')
   assert page.evaluate('story.snoozeScheduled')
   page.wait_for_function("story.stage==='FEED_READY'",timeout=19000)
   page.locator('#storyBag').click()
   assert page.evaluate('story.pourProgress')==0
   # The bag must overlap bowl and be held, not just tapped
   bag=page.locator('#storyBag').bounding_box();art=page.locator('#storyArt').bounding_box()
   page.mouse.move(bag['x']+bag['width']/2,bag['y']+bag['height']/2);page.mouse.down()
   page.mouse.move(art['x']+art['width']*.64,art['y']+art['height']*.734,steps=12)
   page.wait_for_timeout(1300);page.mouse.up()
   assert page.evaluate('story.dismissed'),(width,page.evaluate('story.pourProgress'),page.evaluate('story.bag'))
   assert page.evaluate('window.events.filter(x=>x==="dismiss").length')==1
   page.wait_for_function("story.stage==='COMPLETED'",timeout=18000)
   page.locator('#classicMode').click()
   assert page.locator('#classicRoom').is_visible()
   assert page.locator('#pixelHouse').is_hidden()
   assert page.locator('.alarm-card').count()==1
   assert not errs,(width,errs)
   if width==390:
    page.locator('#pixelMode').click()
    page.evaluate('story.reset()')
    page.locator('#roomSection').scroll_into_view_if_needed()
    page.screenshot(path=str(ROOT/'BATCH6_PHONE_PREVIEW.png'),full_page=True)
   print('PASS',width,'native ring -> waypoint walk -> stroke -> snooze -> food quest -> drag hold -> dismiss -> classic')
   context.close()
  browser.close()
if __name__=='__main__':main()
