"""Batch 4 cozy skin: regression and small-screen smoke tests.
Requires playwright + system Chromium; no Android runtime assumed.
"""
from pathlib import Path
from playwright.sync_api import sync_playwright
html=Path(__file__).resolve().parents[1].joinpath('app/src/main/assets/index.html').read_text(encoding='utf-8')
with sync_playwright() as p:
  browser=p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox','--disable-dev-shm-usage'])
  for width in [320,360,390,430,610]:
    ctx=browser.new_context(viewport={'width':width,'height':800},color_scheme='dark',reduced_motion='reduce')
    page=ctx.new_page(); errors=[];page.on('pageerror', lambda e: errors.append(str(e)))
    page.set_content(html)
    assert page.locator('#s').count()==1
    assert page.locator('#addAlarm').count()==1
    assert page.locator('#pickTime').count()==1
    assert page.locator('#clock').count()==1
    assert page.locator('.quick-nav button').count()==3
    # No horizontal page overflow on common phones.
    measured=page.evaluate('({scroll:document.documentElement.scrollWidth,viewport:innerWidth})')
    assert measured['scroll']<=measured['viewport']+2, (width, measured)
    # Home navigation opens settings and returns to the alarm list.
    page.locator('[data-jump="settingsSection"]').click()
    assert page.locator('#settingsSection').get_attribute('open') is not None
    page.locator('[data-jump="alarmsSection"]').click()
    assert page.locator('[data-jump="alarmsSection"]').get_attribute('aria-current')=='page'
    page.locator('#addAlarm').click()
    assert page.locator('#editor').is_visible()
    page.locator('#label').fill('کوچولوی من')
    page.locator('#at').fill('06:35')
    page.locator('#alarmSound').select_option('cute')
    page.locator('#saveAlarm').click()
    assert page.locator('.alarm-card').count()==1
    assert 'کوچولوی من' in page.locator('#nextLabel').inner_text()
    assert '06:35' in page.locator('#nextTime').inner_text()
    assert 'میوی بانمک' in page.locator('.alarm-sub').inner_text()
    page.locator('.alarm-card [data-action="delete"]').click()
    assert page.locator('#deleteConfirm').is_visible()
    page.locator('#deleteNo').click()
    assert page.locator('.alarm-card').count()==1
    assert errors==[], (width,errors)
    print('PASS:',width,'px; nav/editor/alarms/sounds/delete, no page errors or horizontal overflow')
    ctx.close()
  browser.close()
