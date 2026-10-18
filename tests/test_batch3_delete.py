"""Regression tests for the MeowAlarm alarm deletion patch."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]
HTML = (ROOT/'app/src/main/assets/index.html').read_text(encoding='utf-8')
KOTLIN = (ROOT/'app/src/main/java/com/meowalarm/app/Alarms.kt').read_text(encoding='utf-8')

class DeleteTests(unittest.TestCase):
    def test_html_javascript_syntax(self):
        script = re.search(r'<script>([\s\S]*?)</script>', HTML).group(1)
        check = subprocess.run(['node','--check'], input=script, capture_output=True, text=True)
        self.assertEqual(check.returncode, 0, check.stderr)

    def test_buttons_and_in_app_confirmation(self):
        self.assertIn("remove.textContent='🗑 حذف'", HTML)
        self.assertIn("remove.onclick=()=>askDelete(a.id)", HTML)
        self.assertIn("$('deleteAlarm').onclick=()=>{if(editingId!==null)askDelete(editingId)}", HTML)
        for name in ('deleteConfirm','deleteYes','deleteNo','deleteError'):
            self.assertIn(f'id="{name}"', HTML)
        self.assertNotRegex(HTML, r'\bif\s*\(\s*!?confirm\(')

    def test_android_deletion_reports_result_and_cancels_snooze(self):
        self.assertIn('fun remove(c: Context, id: Int): Boolean', KOTLIN)
        self.assertIn('if (!entries.removeAll { it.id == id }) return false', KOTLIN)
        self.assertIn('cancel(c, id)', KOTLIN)
        self.assertIn('== id) cancelSnooze(c)', KOTLIN)
        self.assertIn('@JavascriptInterface fun deleteAlarm(id: Int): Boolean = Alarms.remove(c, id)', KOTLIN)

    def test_no_unrelated_kotlin_sounds_changed(self):
        sound = (ROOT/'app/src/main/java/com/meowalarm/app/MeowAudio.kt').read_text()
        self.assertIn('fun normalizeMeow(value: String)', sound)
        self.assertIn('fun openPurr(c: Context', sound)

if __name__ == '__main__':
    unittest.main(verbosity=2)
