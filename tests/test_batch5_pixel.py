"""Legacy Batch 5 browser smoke: onboarding/profile/alarms after the Batch 6 story upgrade.
The full touch-driven story is covered in test_batch6_story.py.
"""
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
from test_batch6_story import HTML,STUB
from playwright.sync_api import sync_playwright
with sync_playwright() as p:
 browser=p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox'])
 page=browser.new_page(viewport={'width':390,'height':800});errors=[]
 page.on('pageerror',lambda e:errors.append(str(e)))
 page.evaluate(STUB);page.set_content(HTML)
 assert page.locator('#pixelHouse').is_visible()
 assert page.evaluate('story.avatar')=='female'
 assert page.evaluate('story.cat')=='orange'
 page.locator('#classicMode').click();assert page.locator('#classicRoom').is_visible()
 page.locator('#pixelMode').click();assert page.locator('#pixelHouse').is_visible()
 assert page.locator('.alarm-card').count()==1
 assert not errors,errors
 browser.close()
print('PASS legacy Batch 5: avatar/cat settings, mode switch, preserved alarms')
