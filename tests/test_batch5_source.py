from pathlib import Path
import unittest, subprocess, tempfile
from PIL import Image
P=Path(__file__).resolve().parents[1]

class Batch5Checks(unittest.TestCase):
    def test_sprites(self):
        for n in ['avatar_male','avatar_female','cat_tabby','cat_orange','cat_black','cat_white','cat_calico']:
            file=P/'app/src/main/assets/sprites'/f'{n}.png'
            self.assertTrue(file.is_file())
            with Image.open(file) as im:
                self.assertEqual(im.mode,'RGBA')
                self.assertGreater(im.width,80)
    def test_independent_native_profile(self):
        kt=(P/'app/src/main/java/com/meowalarm/app/Alarms.kt').read_text()
        for key in ['profile_v1','getProfile()', 'setProfile(json: String)', 'onboardingComplete','alarms_v2']:
            self.assertIn(key,kt)
        self.assertIn('ProfileStore.init(this)',(P/'app/src/main/java/com/meowalarm/app/MainActivity.kt').read_text())
    def test_diagnostics_and_no_new_audio(self):
        svc=(P/'app/src/main/java/com/meowalarm/app/AlarmService.kt').read_text()
        self.assertIn('before_foreground',svc)
        self.assertIn('meow_started_or_fallback',svc)
        js=(P/'app/src/main/assets/index.html').read_text()
        self.assertIn('if(NATIVE)return;', js.replace('if(NATIVE||!aud||muted)return;', 'if(NATIVE)return;'))
        self.assertIn('profileCanUseNative',js)
        self.assertIn('gainPet(px)',js)
        self.assertIn('completeFeeding()',js)
        self.assertIn('window.nativeRing',js)
    def test_javascript_syntax(self):
        html=(P/'app/src/main/assets/index.html').read_text()
        script=html.split('<script>',1)[1].split('</script>',1)[0]
        with tempfile.NamedTemporaryFile(mode='w',suffix='.js',encoding='utf-8') as f:
            f.write(script);f.flush();subprocess.run(['node','--check',f.name],check=True,capture_output=True)
if __name__=='__main__':unittest.main(verbosity=2)
