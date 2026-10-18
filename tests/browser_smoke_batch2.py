"""Compatibility entry point for the Batch 2 smoke checks, updated for the new sound model."""
from pathlib import Path
from playwright.sync_api import sync_playwright
HTML=(Path(__file__).resolve().parents[1]/'app/src/main/assets/index.html').read_text(encoding='utf-8')
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox'])
    page=browser.new_page(viewport={'width':390,'height':820},color_scheme='dark')
    errors=[];page.on('pageerror',lambda exc: errors.append(str(exc)))
    page.set_content(HTML)
    page.locator('#addAlarm').click()
    page.locator('#alarmSoundOverride').check()
    page.locator('#alarmSound').select_option('meow_2')
    page.locator('#saveAlarm').click()
    assert page.locator('.alarm-card').count()==1
    assert 'اختصاصی: میو 2' in page.locator('.alarm-sub').inner_text()
    page.locator('#settingsSection').evaluate('(el)=>el.open=true')
    color=page.locator('details.extras h3').first.evaluate('(el)=>getComputedStyle(el).color')
    assert color=='rgb(56, 43, 59)',color
    page.locator('#meowLibrary .sound-option').first.locator('[data-select]').click()
    assert 'اختصاصی: میو 2' in page.locator('.alarm-sub').inner_text()
    assert not errors,errors
    print('PASS: existing sound and settings controls preserved, new override logic verified')
    browser.close()
