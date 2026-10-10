"""Characterization and failure-path tests for the Persian resource contract."""
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from verify_reader_localizations import ENGLISH, PERSIAN, audit, format_signature, main


class ReaderLocalizationContractTests(unittest.TestCase):
    def check(self, en, fa):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp) / "values.xml"
            localized = Path(tmp) / "values-fa.xml"
            base.write_text(f'<resources>{en}</resources>', encoding="utf-8")
            localized.write_text(f'<resources>{fa}</resources>', encoding="utf-8")
            return audit(base, localized)

    def test_repository_resources_are_in_sync(self):
        self.assertTrue(ENGLISH.is_file())
        self.assertTrue(PERSIAN.is_file())
        self.assertEqual([], audit())

    def test_reordered_explicit_arguments_are_accepted(self):
        en = '<string name="count">%1$d records in %2$s</string>'
        fa = '<string name="count">در %2$s تعداد %1$d مورد</string>'
        self.assertEqual([], self.check(en, fa))

    def test_missing_translation_fails(self):
        errors = self.check('<string name="reader_search">Search</string>', '')
        self.assertTrue(any("missing Persian translation" in error for error in errors))

    def test_extra_translation_fails(self):
        errors = self.check('', '<string name="unexpected">اضافی</string>')
        self.assertTrue(any("unknown string" in error for error in errors))

    def test_missing_or_wrong_type_placeholder_fails(self):
        en = '<string name="notebook">%1$d notes from %2$s</string>'
        for fa in ('<string name="notebook">%1$d یادداشت</string>',
                   '<string name="notebook">%1$s یادداشت از %2$s</string>'):
            with self.subTest(fa=fa):
                self.assertTrue(any("formatting arguments differ" in e
                                    for e in self.check(en, fa)))

    def test_implicit_argument_order_must_be_preserved(self):
        en = '<string name="progress">%s / %d</string>'
        fa = '<string name="progress">%d / %s</string>'
        self.assertTrue(self.check(en, fa))

    def test_formatted_false_is_not_a_formatter_contract(self):
        en = '<string name="percentage" translatable="false">100% off</string>'
        self.assertEqual([], self.check(en, ''))
        fa = '<string name="percentage" translatable="false">۱۰۰ درصد</string>'
        self.assertEqual([], self.check(en, fa))

    def test_inconsistent_translatable_flag_is_rejected(self):
        en = '<string name="brand" translatable="false">Veil</string>'
        fa = '<string name="brand">ویل</string>'
        self.assertTrue(any("inconsistent translatable" in e for e in self.check(en, fa)))

    def test_duplicate_key_is_rejected(self):
        en = '<string name="reader">Read</string><string name="reader">Duplicate</string>'
        fa = '<string name="reader">مطالعه</string>'
        self.assertTrue(any("duplicate string" in e for e in self.check(en, fa)))

    def test_malformed_xml_is_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            base = Path(tmp) / "en.xml"
            fa = Path(tmp) / "fa.xml"
            base.write_text("<resources><string", encoding="utf-8")
            fa.write_text("<resources></resources>", encoding="utf-8")
            self.assertTrue(any("invalid or unreadable XML" in e for e in audit(base, fa)))

    def test_literal_percent_and_newline_do_not_add_arguments(self):
        self.assertEqual(format_signature("100%% %n %1$d"), format_signature("%1$d"))

    def test_cli_exits_cleanly_on_actual_resources(self):
        self.assertEqual(0, main([]))


if __name__ == "__main__":
    unittest.main()
