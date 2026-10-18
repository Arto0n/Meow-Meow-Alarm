"""Simulated Android bridge for new universal/per-alarm sounds and personal purr imports."""
from pathlib import Path
from playwright.sync_api import sync_playwright
HTML=(Path(__file__).resolve().parents[1]/'app/src/main/assets/index.html').read_text(encoding='utf-8')
setup=r'''() => {
  window.saved=[{id:100,h:6,m:0,on:true,days:127,label:'آلارم اول',sound:'meow_2',soundOverride:false}];
  window.globalMeow='meow_1';window.globalPurr='purr_1';window.catName='گربه';window.catColor='gray';
  window.events=[];
  window.Android={
    getAlarms:()=>JSON.stringify(window.saved),
    saveAlarm:(raw)=>{let a=JSON.parse(raw);if(!a.id)a.id=102; const i=window.saved.findIndex(x=>x.id===a.id); if(i<0)window.saved.push(a);else window.saved[i]=a;return JSON.stringify(a)},
    deleteAlarm:(id)=>{window.saved=window.saved.filter(x=>x.id!==id);return true},
    getSettings:()=>'{}',setSettings:()=>{},
    getDefaultSound:()=>window.globalMeow,setDefaultSound:(x)=>{window.globalMeow=x;window.events.push(['global',x])},
    getDefaultPurr:()=>window.globalPurr,setDefaultPurr:(x)=>{window.globalPurr=x;window.events.push(['purr',x])},
    getCatName:()=>window.catName,setCatName:(x)=>window.catName=x,
    getCatColor:()=>window.catColor,setCatColor:(x)=>window.catColor=x,
    hasCustomMeow:()=>true,hasCustomPurr:()=>true,hasAlarmMeow:()=>true,
    chooseCustomMeow:()=>window.events.push(['choose-meow']),
    chooseCustomPurr:()=>window.events.push(['choose-purr']),
    chooseAlarmMeow:(id)=>window.events.push(['choose-alarm',id]),
    previewMeow:(x)=>window.events.push(['preview-meow',x]),
    previewPurr:(x)=>window.events.push(['preview-purr',x]),
    previewAlarmMeow:(id)=>window.events.push(['preview-alarm',id]),
    startPurr:()=>window.events.push(['start-purr']),stopPurr:()=>window.events.push(['stop-purr']),
    canSetExactAlarms:()=>true,isRinging:()=>false,
    testAlarm:(id)=>window.events.push(['test',id]),
    pickTime:()=>{},dismiss:()=>{},ringIn:()=>{},silence:()=>{},
    requestExactAlarmPermission:()=>{}
  }
}'''
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True,executable_path='/usr/bin/chromium',args=['--no-sandbox'])
    page=browser.new_page(viewport={'width':390,'height':820})
    page.evaluate(setup)
    errors=[];page.on('pageerror', lambda e: errors.append(str(e)))
    page.set_content(HTML)
    assert page.locator('.alarm-card').count()==1
    page.locator('#settingsSection').evaluate('(e)=>e.open=true')
    # Changing universal does not write a new per-alarm sound value.
    page.locator('#meowLibrary .sound-option').nth(4).locator('[data-select]').click()
    assert page.evaluate('globalMeow')=='meow_5'
    assert page.evaluate('saved[0].soundOverride')==False
    assert 'سراسری: میو 5' in page.locator('.alarm-sub').inner_text()
    # Explicit override is saved and persists under later global changes.
    page.locator('.alarm-card [data-action="edit"]').click()
    page.locator('#alarmSoundOverride').check()
    page.locator('#alarmSound').select_option('meow_6')
    page.locator('#saveAlarm').click()
    assert page.evaluate('saved[0].sound')=='meow_6'
    assert page.evaluate('saved[0].soundOverride')==True
    page.locator('#meowLibrary .sound-option').nth(0).locator('[data-select]').click()
    assert 'اختصاصی: میو 6' in page.locator('.alarm-sub').inner_text()
    # Custom audio for selected alarm rather than global one.
    page.locator('.alarm-card [data-action="edit"]').click()
    page.locator('#importAlarmSound').click()
    assert ['choose-alarm',100] in page.evaluate('events')
    page.evaluate('window.saved[0].sound="alarm_custom";window.saved[0].soundOverride=true;window.onAlarmMeowImported(100,true,"ok")')
    assert page.locator('#alarmSound').input_value()=='alarm_custom'
    # Purr preview and own import are independent of meows.
    page.locator('#purrLibrary .sound-option').nth(2).locator('[data-preview]').click()
    assert ['preview-purr','purr_3'] in page.evaluate('events')
    page.locator('#importPurr').click()
    assert ['choose-purr'] in page.evaluate('events')
    page.evaluate('onCustomPurrImported(true,"ok")')
    assert page.evaluate('globalPurr')=='custom'
    page.locator('#catName').fill('موچی')
    page.locator('#catColor').select_option('calico')
    page.locator('#saveCatProfile').click()
    assert page.evaluate('catName')=='موچی' and page.evaluate('catColor')=='calico'
    assert page.locator('#catCalicoPatches').get_attribute('style')!='display:none'
    assert not errors,errors
    print('PASS: native bridge simulation, independent sounds, custom purr, cat profile, no JS errors')
    browser.close()
