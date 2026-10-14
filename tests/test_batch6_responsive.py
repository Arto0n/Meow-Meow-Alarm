from pathlib import Path
from playwright.sync_api import sync_playwright
import importlib.util
s=importlib.util.spec_from_file_location('b6',Path(__file__).with_name('test_batch6_story.py'));m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox'])
 for w in (320,390,520):
  ctx=browser.new_context(viewport={'width':w,'height':800},color_scheme='dark')
  page=ctx.new_page();errors=[];page.on('pageerror',lambda e:errors.append(str(e)))
  page.evaluate(m.STUB);page.set_content(m.HTML);page.wait_for_timeout(260)
  assert page.locator('#pixelHouse').is_visible()
  assert page.evaluate('story.avatar')=='female'
  assert page.evaluate('story.cat')=='orange'
  assert page.evaluate('document.documentElement.scrollWidth <= innerWidth+2'),(w,'horizontal overflow')
  assert page.evaluate("document.getElementById('storyHuman').naturalWidth>0")
  assert page.evaluate("document.getElementById('storyCat').naturalWidth>0")
  page.evaluate('window.nativeRing()')
  assert page.evaluate('story.active')
  assert page.locator('#storyStop').is_visible()
  page.evaluate('story.emergencyStop()')
  assert page.evaluate("window.events.filter(x=>x==='dismiss').length") == 1
  assert not errors,(w,errors)
  print('PASS',w,'responsive house, loaded sprites, native ring and emergency stop')
  ctx.close()
 browser.close()
