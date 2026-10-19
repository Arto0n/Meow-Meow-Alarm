"""Static contract tests for one-time native alarm permission setup.
These do NOT replace APK compilation and device testing.
"""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'app/src/main/java/com/meowalarm/app'
HTML = ROOT / 'app/src/main/assets/index.html'
ANDROID = '{http://schemas.android.com/apk/res/android}'

class PermissionWizardChecks(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.setup = (JAVA / 'PermissionSetupActivity.kt').read_text(encoding='utf-8')
        cls.main = (JAVA / 'MainActivity.kt').read_text(encoding='utf-8')
        cls.bridge = (JAVA / 'Alarms.kt').read_text(encoding='utf-8')
        cls.html = HTML.read_text(encoding='utf-8')

    def test_registered_non_exported(self):
        xml=ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
        items=[e for e in xml.findall('.//activity') if e.get(ANDROID+'name')=='.PermissionSetupActivity']
        self.assertEqual(len(items),1)
        self.assertEqual(items[0].get(ANDROID+'exported'),'false')

    def test_launch_only_first_visit_and_never_on_alarm(self):
        self.assertIn('!Ring.active',self.main)
        self.assertIn('this@MainActivity !is RingingActivity',self.main)
        self.assertIn('maybeLaunchPermissionSetup()',self.main)
        self.assertIn('PermissionSetupActivity.DONE_KEY',self.main)
        self.assertIn('if (Ring.active) { finish(); return }',self.setup)
        self.assertNotIn('if (this !is RingingActivity) askPermissions()',self.main)

    def test_core_permissions_are_checked_and_user_controlled(self):
        self.assertIn('Manifest.permission.POST_NOTIFICATIONS',self.setup)
        self.assertIn('requestPermissions(',self.setup)
        self.assertIn('Alarms.canSchedule(this)',self.setup)
        self.assertIn('ACTION_REQUEST_SCHEDULE_EXACT_ALARM',self.setup)
        self.assertIn('canUseFullScreenIntent()',self.setup)
        self.assertIn('ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT',self.setup)
        self.assertIn('areNotificationsEnabled()',self.setup)
        self.assertIn('IMPORTANCE_HIGH',self.setup)
        self.assertIn('ACTION_APP_NOTIFICATION_SETTINGS',self.setup)
        self.assertIn('ACTION_CHANNEL_NOTIFICATION_SETTINGS',self.setup)
        self.assertIn('override fun onResume()',self.setup)
        self.assertIn('override fun onRequestPermissionsResult(',self.setup)

    def test_optional_xiaomi_battery_and_explicit_skip(self):
        self.assertIn('isIgnoringBatteryOptimizations(',self.setup)
        self.assertIn('ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS',self.setup)
        self.assertIn('Autostart',self.setup)
        self.assertIn('No restrictions',self.setup)
        self.assertIn('AlertDialog.Builder(this)',self.setup)
        self.assertIn('فعلاً ورود بدون همهٔ مجوزها',self.setup)
        self.assertNotIn('REQUEST_IGNORE_BATTERY_OPTIMIZATIONS',self.setup)
        self.assertNotIn('ACTION_MANAGE_OVERLAY_PERMISSION',self.setup)

    def test_reopen_from_existing_settings(self):
        self.assertIn('openPermissionSetup()',self.bridge)
        self.assertIn('fun showPermissionSetup()',self.main)
        self.assertIn('id="openPermissionSetup"',self.html)
        self.assertIn('Android.openPermissionSetup()',self.html)

    def test_reschedule_when_exact_alarm_access_changes(self):
        self.assertIn('!lastExactAllowed && exactNow',self.setup)
        self.assertIn('Alarms.rescheduleAll(this)',self.setup)

if __name__=='__main__':
    unittest.main(verbosity=2)
