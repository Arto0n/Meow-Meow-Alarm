"""Batch 8 functional WebView/browser regression; no Android APK compilation."""
import re, subprocess, unittest
from pathlib import Path
from playwright.sync_api import sync_playwright

ROOT = Path(__file__).resolve().parents[1]
HTML = (ROOT/'app/src/main/assets/index.html').read_text()

class Batch8Tests(unittest.TestCase):
    def test_preset_assets(self):
        for kind, total in [('meow', 6), ('purr',3)]:
            for i in range(1,total+1):
                file = ROOT/f'app/src/main/res/raw/{kind}_{i}.mp3'
                self.assertTrue(file.is_file() and file.stat().st_size>4000, file.name)
                probe=subprocess.run(['ffprobe','-v','error','-show_entries','format=duration',
                                      '-of','default=noprint_wrappers=1:nokey=1',str(file)],
                                      capture_output=True,text=True,check=True)
                self.assertGreater(float(probe.stdout),1)
    def test_source_contract(self):
        alarms=(ROOT/'app/src/main/java/com/meowalarm/app/Alarms.kt').read_text()
        service=(ROOT/'app/src/main/java/com/meowalarm/app/AlarmService.kt').read_text()
        main=(ROOT/'app/src/main/java/com/meowalarm/app/MainActivity.kt').read_text()
        audio=(ROOT/'app/src/main/java/com/meowalarm/app/MeowAudio.kt').read_text()
        for contract in ['soundOverride','effectiveSound(c: Context','getDefaultPurr()',
                         'chooseCustomPurr()', 'chooseAlarmMeow(id: Int)', 'getCatName()', 'getCatColor()']:
            self.assertIn(contract, alarms)
        self.assertIn('Alarms.effectiveSound(this, entry)', service)
        for contract in ['fun startPurr()', 'fun stopPurr()', 'fun previewAudio(', 'fun selectCustomAudio(']:
            self.assertIn(contract, main)
        self.assertIn('fun openPurr(',audio)
        self.assertIn('fun alarmFile(',audio)
    def test_ui_flow(self):
        with sync_playwright() as p:
            browser = p.chromium.launch(executable_path='/usr/bin/chromium', headless=True,
                          args=['--no-sandbox','--disable-dev-shm-usage'])
            for width in [320,390,520]:
                context=browser.new_context(viewport={'width':width,'height':820},reduced_motion='reduce',
                                             color_scheme='dark')
                page=context.new_page()
                errors=[]
                page.on('pageerror',lambda ex:errors.append(str(ex)))
                page.set_content(HTML)
                self.assertEqual(page.locator('#meowLibrary .sound-option').count(),7)
                self.assertEqual(page.locator('#purrLibrary .sound-option').count(),4)
                page.locator('#settingsSection').evaluate('(el)=>el.open=true')
                page.locator('#meowLibrary .sound-option').nth(3).locator('[data-select]').click()
                self.assertIn('میو 4',page.locator('#soundStatus').inner_text())
                page.locator('#purrLibrary .sound-option').nth(1).locator('[data-select]').click()
                page.locator('#catName').fill('موچی')
                page.locator('#catColor').select_option('orange')
                page.locator('#saveCatProfile').click()
                self.assertIn('موچی',page.locator('#catProfileStatus').inner_text())
                self.assertIn('#de9a52',page.locator('#catSprite').get_attribute('style'))
                page.locator('#addAlarm').click()
                self.assertFalse(page.locator('#alarmSoundOverride').is_checked())
                page.locator('#label').fill('Morning')
                page.locator('#saveAlarm').click()
                self.assertEqual(page.locator('.alarm-card').count(),1)
                self.assertIn('سراسری: میو 4',page.locator('.alarm-sub').inner_text())
                page.locator('.alarm-card [data-action="edit"]').click()
                page.locator('#alarmSoundOverride').check()
                page.locator('#alarmSound').select_option('meow_6')
                page.locator('#saveAlarm').click()
                self.assertIn('اختصاصی: میو 6',page.locator('.alarm-sub').inner_text())
                page.locator('#meowLibrary .sound-option').nth(2).locator('[data-select]').click()
                self.assertIn('اختصاصی: میو 6',page.locator('.alarm-sub').inner_text())
                page.locator('#addAlarm').click()
                page.locator('#label').fill('Second')
                page.locator('#saveAlarm').click()
                self.assertEqual(page.locator('.alarm-card').count(),2)
                self.assertIn('سراسری: میو 3',page.locator('.alarm-card').nth(1).locator('.alarm-sub').inner_text())
                page.locator('#alarmList [data-action="delete"]').first.click()
                page.locator('#deleteYes').click()
                self.assertEqual(page.locator('.alarm-card').count(),1)
                page.locator('#settingsSection').evaluate('(el)=>el.open=false')
                self.assertLessEqual(page.evaluate('document.documentElement.scrollWidth'),width+2)
                self.assertFalse(errors, (width,errors))
                context.close()
                print('PASS UI',width)
            browser.close()

if __name__=='__main__': unittest.main(verbosity=2)
