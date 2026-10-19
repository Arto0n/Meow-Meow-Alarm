"""Contract checks for the lockscreen patch (not a replacement for device testing)."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
APP=ROOT/'app/src/main'

class LockScreenAlarmChecks(unittest.TestCase):
    def test_ringing_activity_registered(self):
        manifest=ET.parse(APP/'AndroidManifest.xml').getroot()
        android='{http://schemas.android.com/apk/res/android}'
        entries=[n for n in manifest.findall('.//activity') if n.get(android+'name')=='.RingingActivity']
        self.assertEqual(len(entries),1)
        self.assertEqual(entries[0].get(android+'showWhenLocked'),'true')
        self.assertEqual(entries[0].get(android+'turnScreenOn'),'true')

    def test_high_priority_fullscreen_alarm_launch(self):
        src=(APP/'java/com/meowalarm/app/AlarmService.kt').read_text()
        self.assertIn('Intent(this, RingingActivity::class.java)',src)
        self.assertIn('NotificationManager.IMPORTANCE_HIGH',src)
        self.assertIn('.setFullScreenIntent(open, true)',src)
        self.assertIn('Notification.FOREGROUND_SERVICE_IMMEDIATE',src)
        self.assertLess(src.index('Ring.active = true\n            goForeground()'),src.index('startSound()\n'))

    def test_webview_ready_ringing_delivery(self):
        activity=(APP/'java/com/meowalarm/app/MainActivity.kt').read_text()
        self.assertIn('override fun onPageFinished(',activity)
        self.assertIn('if (Ring.active) {\n                    updateAlarmWindow(true)\n                    syncRingJs()',activity)
        self.assertIn('if (this !is RingingActivity) askPermissions()',activity)
        html=(APP/'assets/index.html').read_text()
        self.assertIn("$('roomSection').scrollIntoView({behavior:'auto',block:'start'})",html)

    def test_permission_recovery(self):
        src=(APP/'java/com/meowalarm/app/Alarms.kt').read_text()
        for key in ('canUseFullScreenIntent()', 'ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT', 'areNotificationsEnabled()', 'ACTION_APP_NOTIFICATION_SETTINGS'):
            self.assertIn(key,src)
        html=(APP/'assets/index.html').read_text()
        for key in ('id="lockScreenHelp"','id="requestLockScreenPermission"','Android.canShowFullScreenAlarm()', 'Android.openAlarmDisplaySettings()'):
            self.assertIn(key,html)

if __name__=='__main__': unittest.main(verbosity=2)
