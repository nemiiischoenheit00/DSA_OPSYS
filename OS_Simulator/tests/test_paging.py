import unittest

from core.paging import (INVALID, OK, PAGE_FAULT, PagingConfig, auto_map,
                         build_page_table, frame_table, translate)
from core.validation import ValidationError

CFG = PagingConfig(page_size=100, num_frames=5, num_pages=8)      # logical space 0..799
TABLE = build_page_table(CFG, [(0, 3), (1, 1), (2, None), (3, 0)])


class TestPaging(unittest.TestCase):
    def test_basic_translation(self):
        t = translate(CFG, TABLE, 117)          # page 1, offset 17 -> frame 1 -> 117
        self.assertEqual((t.page, t.offset, t.frame, t.physical, t.status), (1, 17, 1, 117, OK))
        t = translate(CFG, TABLE, 5)            # page 0 -> frame 3 -> 305
        self.assertEqual((t.page, t.offset, t.frame, t.physical), (0, 5, 3, 305))
        t = translate(CFG, TABLE, 345)          # page 3 -> frame 0 -> 45
        self.assertEqual(t.physical, 45)

    def test_page_boundaries(self):
        self.assertEqual(translate(CFG, TABLE, 99).physical, 399)    # last byte of page 0
        self.assertEqual(translate(CFG, TABLE, 100).physical, 100)   # first byte of page 1
        self.assertEqual(translate(CFG, TABLE, 0).physical, 300)

    def test_unmapped_page_is_page_fault(self):
        self.assertEqual(translate(CFG, TABLE, 250).status, PAGE_FAULT)   # page 2: frame blank
        self.assertEqual(translate(CFG, TABLE, 799).status, PAGE_FAULT)   # page 7: not in table
        self.assertIsNone(translate(CFG, TABLE, 250).physical)

    def test_invalid_addresses(self):
        self.assertEqual(translate(CFG, TABLE, 800).status, INVALID)      # first address past the end
        self.assertEqual(translate(CFG, TABLE, -1).status, INVALID)
        self.assertEqual(translate(CFG, TABLE, 10 ** 9).status, INVALID)

    def test_frame_table_and_auto_map(self):
        ft = frame_table(CFG, TABLE)
        self.assertEqual(ft, [(0, 3), (1, 1), (2, None), (3, 0), (4, None)])
        self.assertEqual(auto_map(CFG), [(0, 0), (1, 1), (2, 2), (3, 3), (4, 4)])

    def test_page_size_one_and_non_power_of_two(self):
        cfg = PagingConfig(1, 3, 3)
        tab = build_page_table(cfg, auto_map(cfg))
        self.assertEqual(translate(cfg, tab, 2).physical, 2)
        cfg = PagingConfig(7, 4, 4)
        tab = build_page_table(cfg, [(2, 3)])
        self.assertEqual(translate(cfg, tab, 15).physical, 3 * 7 + 1)

    def test_invalid_configuration_and_table(self):
        with self.assertRaises(ValidationError):
            build_page_table(PagingConfig(0, 4, 4), [])
        with self.assertRaises(ValidationError):
            build_page_table(PagingConfig(10, 0, 4), [])
        with self.assertRaises(ValidationError):
            build_page_table(CFG, [(9, 1)])                  # page outside address space
        with self.assertRaises(ValidationError):
            build_page_table(CFG, [(0, 7)])                  # frame outside physical memory
        with self.assertRaises(ValidationError):
            build_page_table(CFG, [(0, 1), (1, 1)])          # two pages, one frame
        with self.assertRaises(ValidationError):
            build_page_table(CFG, [(0, 1), (0, 2)])          # duplicate page

    def test_large_configuration(self):
        cfg = PagingConfig(4096, 5000, 10000)
        entries = [(p, p) for p in range(5000)]
        tab = build_page_table(cfg, entries)
        t = translate(cfg, tab, 4096 * 4321 + 123)
        self.assertEqual(t.physical, 4096 * 4321 + 123)
        self.assertEqual(translate(cfg, tab, 4096 * 7000).status, PAGE_FAULT)


if __name__ == "__main__":
    unittest.main()
