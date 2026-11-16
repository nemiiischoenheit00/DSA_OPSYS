import random
import unittest

from core.bankers_algorithm import (calculate_need, check_safety, explain,
                                    request_resources, validate_state)
from core.validation import ValidationError

# Classic textbook state (5 processes, resources A B C)
ALLOC = [[0, 1, 0], [2, 0, 0], [3, 0, 2], [2, 1, 1], [0, 0, 2]]
MAX = [[7, 5, 3], [3, 2, 2], [9, 0, 2], [2, 2, 2], [4, 3, 3]]
AVAIL = [3, 3, 2]


def sequence_is_valid(alloc, maxi, avail, seq):
    """Independent checker: replay a sequence and confirm each Need fits in Work."""
    need = calculate_need(alloc, maxi)
    work = list(avail)
    for i in seq:
        if any(need[i][j] > work[j] for j in range(len(work))):
            return False
        work = [w + a for w, a in zip(work, alloc[i])]
    return sorted(seq) == list(range(len(alloc)))


class TestBankers(unittest.TestCase):
    def test_need_matrix(self):
        self.assertEqual(calculate_need(ALLOC, MAX),
                         [[7, 4, 3], [1, 2, 2], [6, 0, 0], [0, 1, 1], [4, 3, 1]])

    def test_safe_state_and_sequence(self):
        r = check_safety(ALLOC, MAX, AVAIL)
        self.assertTrue(r.safe)
        self.assertEqual(r.sequence, [1, 3, 0, 2, 4])        # lowest-index-first rule
        self.assertTrue(sequence_is_valid(ALLOC, MAX, AVAIL, r.sequence))
        self.assertEqual(r.final_work, [10, 5, 7])           # total resources recovered

    def test_unsafe_state(self):
        # Two processes each hold 1 and need 1 more, but nothing is available -> deadlock
        r = check_safety([[1], [1]], [[2], [2]], [0])
        self.assertFalse(r.safe)
        self.assertEqual(r.sequence, [])
        self.assertEqual([p for p, _ in r.blocked], [0, 1])
        self.assertIn("UNSAFE", explain(r, [0]))

    def test_partially_runnable_but_unsafe(self):
        r = check_safety([[0], [1], [1]], [[1], [3], [3]], [1])  # P0 can run, then stuck
        self.assertFalse(r.safe)
        self.assertEqual(r.sequence, [0])

    def test_request_granted(self):
        r = request_resources(ALLOC, MAX, AVAIL, 1, [1, 0, 2])
        self.assertTrue(r.granted)

    def test_request_denied_unsafe(self):
        # P4 asking for (3,3,0) is available now, but would leave the system unsafe.
        r = request_resources(ALLOC, MAX, AVAIL, 4, [3, 3, 0])
        self.assertFalse(r.granted)
        self.assertIn("unsafe", r.reason)

    def test_textbook_follow_up_requests(self):
        # After P1's (1,0,2) is granted: Allocation[1] = (3,0,2), Available = (2,3,0)
        alloc = [list(row) for row in ALLOC]
        alloc[1] = [3, 0, 2]
        avail = [2, 3, 0]
        self.assertTrue(check_safety(alloc, MAX, avail).safe)
        # P4 (3,3,0): more than Available -> must wait
        self.assertIn("not available", request_resources(alloc, MAX, avail, 4, [3, 3, 0]).reason)
        # P0 (0,2,0): available, but resulting state is unsafe -> denied
        r = request_resources(alloc, MAX, avail, 0, [0, 2, 0])
        self.assertFalse(r.granted)
        self.assertIn("unsafe", r.reason)

    def test_request_exceeds_need_or_available(self):
        self.assertIn("more than", request_resources(ALLOC, MAX, AVAIL, 1, [5, 0, 0]).reason)
        self.assertIn("not available", request_resources(ALLOC, MAX, AVAIL, 0, [4, 0, 0]).reason)

    def test_boundary_single_process_single_resource(self):
        r = check_safety([[0]], [[5]], [5])
        self.assertTrue(r.safe)
        r = check_safety([[0]], [[5]], [4])
        self.assertFalse(r.safe)

    def test_zero_need_everywhere(self):
        r = check_safety([[2, 2], [1, 1]], [[2, 2], [1, 1]], [0, 0])
        self.assertTrue(r.safe)
        self.assertEqual(r.sequence, [0, 1])

    # ---- validation -------------------------------------------------------------------
    def test_allocation_exceeds_maximum(self):
        with self.assertRaises(ValidationError) as cm:
            validate_state([[3, 0]], [[2, 5]], [1, 1])
        self.assertIn("exceeds", str(cm.exception))

    def test_negative_values_and_shape_errors(self):
        with self.assertRaises(ValidationError):
            validate_state([[-1]], [[2]], [1])
        with self.assertRaises(ValidationError):
            validate_state([[1]], [[2]], [-1])
        with self.assertRaises(ValidationError):
            validate_state([[1, 1]], [[2]], [1, 1])        # dimension mismatch
        with self.assertRaises(ValidationError):
            validate_state([[1]], [[2]], [1, 1])           # available wrong length
        with self.assertRaises(ValidationError):
            validate_state([], [], [])
        with self.assertRaises(ValidationError):
            request_resources(ALLOC, MAX, AVAIL, 9, [0, 0, 0])   # bad process index

    # ---- larger dataset ---------------------------------------------------------------
    def test_large_random_safe_state(self):
        rng = random.Random(11)
        n, m = 300, 12
        maxi = [[rng.randint(0, 9) for _ in range(m)] for _ in range(n)]
        alloc = [[rng.randint(0, mx) for mx in row] for row in maxi]
        avail = [sum(maxi[i][j] for i in range(n)) for j in range(m)]   # plenty available
        r = check_safety(alloc, maxi, avail)
        self.assertTrue(r.safe)
        self.assertTrue(sequence_is_valid(alloc, maxi, avail, r.sequence))

    def test_large_unsafe_state(self):
        n = 200
        alloc = [[1] for _ in range(n)]
        maxi = [[2] for _ in range(n)]
        self.assertFalse(check_safety(alloc, maxi, [0]).safe)


if __name__ == "__main__":
    unittest.main()
