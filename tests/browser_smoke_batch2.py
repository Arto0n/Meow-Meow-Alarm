"""Functional browser and simulated-Android-bridge smoke checks."""
from pathlib import Path
from playwright.sync_api import sync_playwright

HTML=Path(__file__).resolve().parents[1].joinpath('app/src/main/assets/index.html').read_text(encoding='utf-8')
with sync_playwright() as p:
    browser=p.chromium.launch(headless=True, executable_path='/usr/bin/chromium',args=['--no-sandbox','--disable-dev-shm-usage'])
    ctx=browser.new_context(color_scheme='dark', viewport={'width':395,'height':850})
    page=ctx.new_page()
    errs=[]
    page.on('pageerror', lambda e: errs.append(str(e)))
    page.set_content(HTML)
    page.wait_for_timeout(400)
    assert page.locator('#addAlarm').count()==1
    page.locator('#addAlarm').click()
    page.locator('#alarmSound').select_option('cute')
    page.locator('#saveAlarm').click()
    assert page.locator('.alarm-card').count()==1
    assert 'میوی بانمک' in page.locator('.alarm-sub').inner_text()
    page.locator('details.extras summary').click()
    color=page.locator('details.extras h3').first.evaluate('(e) => getComputedStyle(e).color')
    bg=page.locator('details.extras').evaluate('(e) => getComputedStyle(e).backgroundColor')
    assert color=='rgb(56, 43, 59)',(color,bg)
    assert bg=='rgb(255, 241, 217)',(color,bg)
    page.locator('[data-select="real"]').click()
    assert 'میوی واقعی' in page.locator('.alarm-sub').inner_text()
    assert 'واقعی' in page.locator('#soundStatus').inner_text()
    assert errs==[],errs
    print('PASS browser: create/edit alarm; switch meow; dark-mode text contrast; no runtime JS errors')
    ctx.close()

    androidCtx=browser.new_context(color_scheme='dark')
    native=androidCtx.new_page()
    native.evaluate('''() => {
      window.saved=[{id:100,h:7,m:15,on:true,days:127,label:'صبح',sound:'real'}];
      window.selected='real';window.lastTest=null;window.hasFile=false;
      window.Android={
        getAlarms:()=>JSON.stringify(window.saved),getSettings:()=> '{}',
        saveAlarm:(raw)=>{let a=JSON.parse(raw);if(!a.id)a.id=101;const i=window.saved.findIndex(x=>x.id===a.id);if(i<0)window.saved.push(a);else window.saved[i]=a;return JSON.stringify(a)},
        deleteAlarm:(id)=>window.saved=window.saved.filter(a=>a.id!==id),
        getDefaultSound:()=>window.selected,setDefaultSound:(s)=>window.selected=s,
        hasCustomMeow:()=>window.hasFile,chooseCustomMeow:()=>{window.pickerOpened=true},
        previewMeow:(s)=>window.preview=s,canSetExactAlarms:()=>true,
        isRinging:()=>false,testAlarm:(id)=>window.lastTest=id,
        pickTime:(h,m)=>{},requestExactAlarmPermission:()=>{},
        setSettings:(s)=>{},dismiss:()=>{},ringIn:(s)=>{}
      };
    }''')
    errors=[]
    native.on('pageerror',lambda e: errors.append(str(e)))
    native.set_content(HTML)
    native.wait_for_timeout(250)
    native.locator('details.extras summary').click()
    native.locator('[data-select="cute"]').click()
    assert native.evaluate('window.saved[0].sound')=='cute'
    native.locator('#importSound').click()
    assert native.evaluate('window.pickerOpened') is True
    native.evaluate('window.hasFile=true;window.onCustomMeowImported(true,"فایل آماده است")')
    assert native.evaluate('window.saved[0].sound')=='custom'
    assert native.evaluate('window.selected')=='custom'
    assert 'میوی من' in native.locator('.alarm-sub').inner_text()
    native.locator('.alarm-ops button').last.click()
    assert native.evaluate('window.lastTest')==100
    assert errors==[], errors
    print('PASS simulated Android: cute/custom persist to alarm, import selects custom, 5 sec targets chosen alarm')
    androidCtx.close()
    browser.close()
