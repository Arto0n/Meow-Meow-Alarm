"""Browser and Android-bridge simulated deletion smoke tests. Requires Playwright+Chromium."""
from pathlib import Path
from playwright.sync_api import sync_playwright
HTML = (Path(__file__).resolve().parents[1]/'app/src/main/assets/index.html').read_text(encoding='utf-8')

def native_stub():
    return r'''() => {
      window.saved=[
       {id:100,h:6,m:30,on:true,days:127,label:'صبح',sound:'real'},
       {id:101,h:8,m:45,on:true,days:127,label:'ورزش',sound:'cute'},
       {id:102,h:12,m:10,on:true,days:0,label:'ناهار',sound:'real'}
      ];
      window.cancelled=[];
      window.Android={
        getAlarms:()=>JSON.stringify(window.saved),getSettings:()=>'{}',getDefaultSound:()=>'real',
        saveAlarm:(raw)=>{const a=JSON.parse(raw); const i=window.saved.findIndex(x=>x.id===a.id);
           if(i<0){a.id=103;window.saved.push(a)}else window.saved[i]=a;return JSON.stringify(a)},
        deleteAlarm:(id)=>{window.cancelled.push(id);let before=window.saved.length;
           window.saved=window.saved.filter(a=>a.id!==id);return window.saved.length!==before},
        hasCustomMeow:()=>false,previewMeow:()=>{},chooseCustomMeow:()=>{},
        canSetExactAlarms:()=>true,isRinging:()=>false,pickTime:()=>{},
        testAlarm:()=>{},setDefaultSound:()=>{},setSettings:()=>{},dismiss:()=>{},
        ringIn:()=>{},requestExactAlarmPermission:()=>{}
      }
    }'''

with sync_playwright() as p:
    browser=p.chromium.launch(headless=True, executable_path='/usr/bin/chromium', args=['--no-sandbox','--disable-dev-shm-usage'])
    # Browser fallback: deletion from card, cancel works, other alarms intact.
    ctx=browser.new_context(viewport={'width':390,'height':845})
    page=ctx.new_page()
    errors=[]
    page.on('pageerror', lambda e:errors.append(str(e)))
    page.set_content(HTML)
    page.locator('#addAlarm').click()
    page.locator('#label').fill('اول')
    page.locator('#saveAlarm').click()
    page.locator('#addAlarm').click()
    page.locator('#label').fill('دوم')
    page.locator('#saveAlarm').click()
    assert page.locator('.alarm-card').count()==2
    page.locator('.alarm-card').first.locator('button.danger').click()
    assert page.locator('#deleteConfirm').is_visible()
    page.locator('#deleteNo').click()
    assert page.locator('.alarm-card').count()==2
    assert not page.locator('#deleteConfirm').is_visible()
    page.locator('.alarm-card').first.locator('button.danger').click()
    page.locator('#deleteYes').click()
    assert page.locator('.alarm-card').count()==1
    assert 'دوم' in page.locator('.alarm-card').inner_text()
    assert errors==[],errors
    print('PASS browser: visible delete, cancel, confirmation, remove only selected alarm')
    ctx.close()

    # Simulated Android bridge: the editor delete must work without JS confirm.
    nativeCtx=browser.new_context(viewport={'width':390,'height':845},color_scheme='dark')
    native=nativeCtx.new_page()
    native.evaluate(native_stub())
    errs=[]
    native.on('pageerror', lambda e:errs.append(str(e)))
    native.set_content(HTML)
    assert native.locator('.alarm-card').count()==3
    native.locator('.alarm-card').nth(1).get_by_role('button',name='ویرایش').click()
    assert native.locator('#editor').is_visible()
    native.locator('#deleteAlarm').click()
    assert native.locator('#deleteConfirm').is_visible()
    native.locator('#deleteYes').click()
    assert native.locator('.alarm-card').count()==2
    assert not native.locator('#editor').is_visible()
    assert native.evaluate('window.saved.map(a=>a.id)')==[100,102]
    assert native.evaluate('window.cancelled')==[101]
    # Delete the remaining first one from its card.
    native.locator('.alarm-card').first.locator('button.danger').click()
    native.locator('#deleteYes').click()
    assert native.evaluate('window.saved.map(a=>a.id)')==[102]
    assert native.locator('.alarm-card').count()==1
    assert errs==[],errs
    print('PASS Android bridge simulation: editor delete and card delete persisted, others intact')
    nativeCtx.close()
    browser.close()
