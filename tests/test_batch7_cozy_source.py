from pathlib import Path
import unittest, subprocess, re
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/'app/src/main/assets'

class Batch7Checks(unittest.TestCase):
 def test_assets(self):
  for f in ['index.html','cozy2d.css','cozy2d.js','cozy2d/room.webp','cozy2d/purr_fallback.wav','cozy2d/bag.wav','cozy2d/kibble.wav']:
   self.assertTrue((SRC/f).is_file(),f)
 def test_js_syntax(self):
  subprocess.run(['node','--check',str(SRC/'cozy2d.js')],check=True)
 def test_presets_and_cat_choices(self):
  js=(SRC/'cozy2d.js').read_text()
  for n in ['tabby','orange','black','white','calico','female','male','lazy','breakfast','playtime','grumpy','custom']:
   self.assertIn(n,js)
  self.assertIn('pointermove',js)
 def test_native_persistence(self):
  kt=(ROOT/'app/src/main/java/com/meowalarm/app/Alarms.kt').read_text()
  self.assertIn('getCozyProfile()',kt)
  self.assertIn('setCozyProfile(json: String)',kt)
  self.assertIn('.put("preset", preset)',kt)
  self.assertIn('getActiveAlarmId()',kt)
 def test_dont_change_alarm_engine(self):
  service=(ROOT/'app/src/main/java/com/meowalarm/app/AlarmService.kt').read_text()
  self.assertIn('startForeground(',service)
  self.assertIn('Ring.active',service)
  self.assertIn('playSelected(selected)',service)
 def test_sound_script(self):
  script=(ROOT/'install-cozy-audio.ps1').read_text()
  self.assertIn('Meow_of_a_pleading_cat.oga',script)
  self.assertIn('Purring_cat_bertie.ogg',script)
  self.assertIn('Maullido_de_gata_hembra_joven.ogg',script)

if __name__=='__main__':unittest.main()
