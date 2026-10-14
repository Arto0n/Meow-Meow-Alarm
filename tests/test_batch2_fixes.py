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
        p = APP/'src/main/res/raw/cute_cat_meow.wav'
        self.assertTrue(p.is_file())
        with wave.open(str(p), 'rb') as w:
            self.assertEqual(w.getnchannels(), 1)
            self.assertEqual(w.getsampwidth(), 2)
            self.assertGreater(w.getnframes(), 22050)

    def test_previews_and_alarm_use_same_media_player_helper(self):
        helper=(APP/'src/main/java/com/meowalarm/app/MeowAudio.kt').read_text()
        service=(APP/'src/main/java/com/meowalarm/app/AlarmService.kt').read_text()
        activity=(APP/'src/main/java/com/meowalarm/app/MainActivity.kt').read_text()
        self.assertIn('R.raw.cute_cat_meow', helper)
        self.assertIn('MeowAudio.open(this, sound, AudioAttributes.USAGE_ALARM',service)
        self.assertIn('MeowAudio.open(this, type, AudioAttributes.USAGE_MEDIA',activity)
        self.assertNotIn('AudioTrack',service)

    def test_custom_sound_is_validated_before_success(self):
        activity=(APP/'src/main/java/com/meowalarm/app/MainActivity.kt').read_text()
        self.assertLess(activity.index('MeowAudio.validateImportedFile(pending)'),activity.index('importResult(true,'))
        self.assertIn('StandardCopyOption.REPLACE_EXISTING', activity)
        self.assertIn('importResult(false,',activity)

    def test_library_selection_updates_alarm_and_default(self):
        html=(APP/'src/main/assets/index.html').read_text()
        for sound in ('real','cute','custom'):
            self.assertIn('data-select="'+sound+'"',html)
        self.assertIn('persistAlarm({...active,sound})',html)
        self.assertIn('if(ok)selectSound(\'custom\')',html)
        self.assertIn('Android.setDefaultSound(sound)',html)

    def test_light_settings_card_uses_dark_text_even_in_dark_mode(self):
        html=(APP/'src/main/assets/index.html').read_text()
        self.assertIn('.card{color:#382b3b}',html)
        self.assertIn('.extras summary,.extras h3,.extras label',html)

    def test_version_increment(self):
        gradle=(APP/'build.gradle.kts').read_text()
        self.assertIn('versionCode = 6', gradle)

    def test_javascript_and_xml(self):
        ET.parse(APP/'src/main/AndroidManifest.xml')
        html=(APP/'src/main/assets/index.html').read_text()
        script=re.search(r'<script>([\s\S]*?)</script>',html).group(1)
        result=subprocess.run(['node','--check'],input=script,text=True,capture_output=True)
        self.assertEqual(result.returncode,0,result.stderr)

if __name__=='__main__': unittest.main(verbosity=2)
