import unittest

from core.segmentation import (OK, OUT_OF_RANGE, SEGMENT_FAULT, SegmentEntry,
                               build_table, translate)
from core.validation import ValidationError

# Classic textbook segment table
SEGS = [SegmentEntry(0, 1400, 1000), SegmentEntry(1, 6300, 400), SegmentEntry(2, 4300, 400),
        SegmentEntry(3, 3200, 1100), SegmentEntry(4, 4700, 1000)]


class TestSegmentation(unittest.TestCase):
    def setUp(self):
        self.table, self.warnings = build_table(SEGS)

    def test_valid_translations(self):
        self.assertEqual(translate(self.table, 2, 53).physical, 4353)
        self.assertEqual(translate(self.table, 3, 852).physical, 4052)
        self.assertEqual(translate(self.table, 0, 0).physical, 1400)
        self.assertEqual(translate(self.table, 1, 0).status, OK)

    def test_offset_boundary(self):
        self.assertEqual(translate(self.table, 1, 399).physical, 6699)     # last valid byte
        t = translate(self.table, 1, 400)                                    # offset == limit -> trap
        self.assertEqual(t.status, OUT_OF_RANGE)
        self.assertIsNone(t.physical)

    def test_out_of_range_and_bad_segment(self):
        self.assertEqual(translate(self.table, 0, 1222).status, OUT_OF_RANGE)
        self.assertEqual(translate(self.table, 4, -1).status, OUT_OF_RANGE)
        self.assertEqual(translate(self.table, 9, 0).status, SEGMENT_FAULT)

    def test_no_overlap_warnings_for_textbook_table(self):
        self.assertEqual(self.warnings, [])

    def test_overlap_warning(self):
        _, warns = build_table([SegmentEntry(0, 100, 50), SegmentEntry(1, 120, 50)])
        self.assertEqual(len(warns), 1)

    def test_physical_memory_check(self):
        build_table([SegmentEntry(0, 0, 100)], physical_memory=100)          # exactly fits
        with self.assertRaises(ValidationError):
            build_table([SegmentEntry(0, 1, 100)], physical_memory=100)      # one unit too far

    def test_invalid_tables(self):
        for bad in ([], [SegmentEntry(0, 0, 0)], [SegmentEntry(0, -5, 10)], [SegmentEntry(-1, 0, 10)],
                    [SegmentEntry(0, 0, 10), SegmentEntry(0, 50, 10)]):
            with self.assertRaises(ValidationError):
                build_table(bad)

    def test_large_table(self):
        segs = [SegmentEntry(i, i * 1000, 500) for i in range(3000)]
        table, warns = build_table(segs)
        self.assertEqual(warns, [])
        self.assertEqual(translate(table, 2999, 499).physical, 2999 * 1000 + 499)
        self.assertEqual(translate(table, 2999, 500).status, OUT_OF_RANGE)


if __name__ == "__main__":
    unittest.main()
