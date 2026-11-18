# Counting Sort - test cases and results

**21 of 21 tests passed**

| # | Category | Test case | Input | Expected | Actual | Result |
|---|----------|-----------|-------|----------|--------|--------|
| 1 | Normal | Classic example | 4, 2, 2, 8, 3, 3, 1 | 1, 2, 2, 3, 3, 4, 8 | 1, 2, 2, 3, 3, 4, 8 | PASS |
| 2 | Normal | Duplicate values | 3 3 3 1 1 2 2 3 | 1, 1, 2, 2, 3, 3, 3, 3 | 1, 1, 2, 2, 3, 3, 3, 3 | PASS |
| 3 | Normal | Already sorted | 1 2 3 4 5 6 | 1, 2, 3, 4, 5, 6 | 1, 2, 3, 4, 5, 6 | PASS |
| 4 | Normal | Reverse order | 9 8 7 6 5 4 3 2 1 | 1, 2, 3, 4, 5, 6, 7, 8, 9 | 1, 2, 3, 4, 5, 6, 7, 8, 9 | PASS |
| 5 | Normal | Negative and positive | 5 -3 0 -3 2 -1 | -3, -3, -1, 0, 2, 5 | -3, -3, -1, 0, 2, 5 | PASS |
| 6 | Normal | Mixed separators | 7; 3,  5\t1\n4 | 1, 3, 4, 5, 7 | 1, 3, 4, 5, 7 | PASS |
| 7 | Boundary | Single element | 42 | 42 | 42 | PASS |
| 8 | Boundary | Two elements, reversed | 2 1 | 1, 2 | 1, 2 | PASS |
| 9 | Boundary | All elements equal (range = 1) | 5 5 5 5 5 | 5, 5, 5, 5, 5 | 5, 5, 5, 5, 5 | PASS |
| 10 | Boundary | All negative | -5 -9 -1 -7 | -9, -7, -5, -1 | -9, -7, -5, -1 | PASS |
| 11 | Boundary | Zeros only | 0 0 0 | 0, 0, 0 | 0, 0, 0 | PASS |
| 12 | Boundary | Near Integer.MAX_VALUE | 2147483647 2147483645 2147483646 | 2147483645, 2147483646, 2147483647 | 2147483645, 2147483646, 2147483647 | PASS |
| 13 | Boundary | Near Integer.MIN_VALUE | -2147483648 -2147483646 -2147483647 | -2147483648, -2147483647, -2147483646 | -2147483648, -2147483647, -2147483646 | PASS |
| 14 | Boundary | Wide range (0 .. 100,000) | 0 100000 50000 1 | 0, 1, 50000, 100000 | 0, 1, 50000, 100000 | PASS |
| 15 | Boundary | Large input: 200,000 numbers | 200,000 random numbers in 0..999 | 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, … (200,000 numbers) | 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, … (200,000 numbers) | PASS |
| 16 | Invalid | Empty input | (nothing) | Rejected with a clear message | Rejected: Please enter at least one whole number. | PASS |
| 17 | Invalid | Letters instead of numbers | 3, abc, 5 | Rejected with a clear message | Rejected: 'abc' is not a whole number | PASS |
| 18 | Invalid | Decimal number | 1.5 2 | Rejected with a clear message | Rejected: '1.5' has a decimal point; only whole numbers are supported | PASS |
| 19 | Invalid | Number too large for 32 bits | 99999999999 | Rejected with a clear message | Rejected: '99999999999' is outside the 32-bit range (-2,147,483,648 to 2,147,483,647) | PASS |
| 20 | Invalid | Lone minus sign | - 3 | Rejected with a clear message | Rejected: '-' is not a whole number | PASS |
| 21 | Invalid | Range larger than a Java array | -2147483648 2147483647 | Rejected with a clear message | Rejected: Range too large: max - min + 1 = 4,294,967,296 is more than a Java array can hold. | PASS |
