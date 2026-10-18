"""Offline regression checks for the sound-selector / colors patch.
These are static/source checks, NOT substitutes for Android compilation or device testing.
"""
from pathlib import Path
import wave
import xml.etree.ElementTree as ET
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT/'app'

class SoundFixChecks(unittest.TestCase):
    def test_bundled_cute_meow_is_real_audio_file(self):
        for kind,count in (("meow",6),("purr",3)):
            for i in range(1,count+1):
                p=APP/f'src/main/res/raw/{kind}_{i}.mp3'
                self.assertTrue(p.is_file() and p.stat().st_size>5000, p.name)

    def test_previews_and_alarm_use_same_media_player_helper(self):
        helper=(APP/'src/main/java/com/meowalarm/app/MeowAudio.kt').read_text()
        service=(APP/'src/main/java/com/meowalarm/app/AlarmService.kt').read_text()
        activity=(APP/'src/main/java/com/meowalarm/app/MainActivity.kt').read_text()
        self.assertIn('fun openPurr(', helper)
        self.assertIn('MeowAudio.open(this, sound, AudioAttributes.USAGE_ALARM',service)
        self.assertIn('fun previewAudio(',activity)
        self.assertNotIn('AudioTrack',service)

    def test_custom_sound_is_validated_before_success(self):
        activity=(APP/'src/main/java/com/meowalarm/app/MainActivity.kt').read_text()
        self.assertLess(activity.index('MeowAudio.validateImportedFile(pending)'),activity.index('importResult(kind, alarmId, true,'))
        self.assertIn('StandardCopyOption.REPLACE_EXISTING', activity)
        self.assertIn('importResult(kind, alarmId, false,',activity)

    def test_library_selection_keeps_universal_and_per_alarm_separate(self):
        html=(APP/'src/main/assets/index.html').read_text()
        self.assertIn('const MEOW_PRESETS', html)
        self.assertIn('const PURR_PRESETS', html)
        self.assertIn('id="alarmSoundOverride"', html)
        self.assertIn('if(NATIVE)Android.setDefaultSound(sound)', html)
        self.assertNotIn('persistAlarm({...active,sound})', html)
        self.assertIn("if(ok)selectSound('custom')", html)

    def test_light_settings_card_uses_dark_text_even_in_dark_mode(self):
        html=(APP/'src/main/assets/index.html').read_text()
        self.assertIn('.card{color:#382b3b}',html)
        self.assertIn('.extras summary,.extras h3,.extras label',html)

    def test_version_increment(self):
        gradle=(APP/'build.gradle.kts').read_text()
        self.assertIn('versionCode =', gradle)

    def test_javascript_and_xml(self):
        ET.parse(APP/'src/main/AndroidManifest.xml')
        html=(APP/'src/main/assets/index.html').read_text()
        script=re.search(r'<script>([\s\S]*?)</script>',html).group(1)
        result=subprocess.run(['node','--check'],input=script,text=True,capture_output=True)
        self.assertEqual(result.returncode,0,result.stderr)

if __name__=='__main__': unittest.main(verbosity=2)
