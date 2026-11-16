"""Segmentation: segment table and logical (segment, offset) -> physical translation.

Rules:
  * A logical address is (segment number, offset).
  * Valid when the segment exists AND 0 <= offset < limit.
  * Physical address = base + offset.
  * Optional physical memory size: every segment must fit inside it (base + limit <= size).
  * Overlapping segments are allowed (they can be shared) but reported as warnings.
"""
from dataclasses import dataclass
from typing import Optional

from .validation import ValidationError

OK = "OK"
SEGMENT_FAULT = "SEGMENT ERROR"
OUT_OF_RANGE = "OUT OF RANGE"


@dataclass
class SegmentEntry:
    seg_id: int
    base: int
    limit: int


@dataclass
class SegTranslation:
    segment: int
    offset: int
    base: Optional[int]
    limit: Optional[int]
    physical: Optional[int]
    status: str
    message: str


def build_table(segments, physical_memory=None):
    """Validate the segments; return (table_dict, warnings_list)."""
    if not segments:
        raise ValidationError("Define at least one segment.")
    table = {}
    for s in segments:
        if s.seg_id < 0:
            raise ValidationError(f"Segment {s.seg_id}: segment number cannot be negative.")
        if s.seg_id in table:
            raise ValidationError(f"Duplicate segment number {s.seg_id}. Segment numbers must be unique.")
        if s.base < 0:
            raise ValidationError(f"Segment {s.seg_id}: base address cannot be negative.")
        if s.limit <= 0:
            raise ValidationError(f"Segment {s.seg_id}: limit must be greater than 0.")
        if physical_memory is not None and s.base + s.limit > physical_memory:
            raise ValidationError(
                f"Segment {s.seg_id} (base {s.base}, limit {s.limit}) ends at {s.base + s.limit - 1}, "
                f"beyond physical memory size {physical_memory}.")
        table[s.seg_id] = s
    warnings = []
    ordered = sorted(table.values(), key=lambda s: s.base)
    for a, b in zip(ordered, ordered[1:]):
        if b.base < a.base + a.limit:
            warnings.append(f"Segments {a.seg_id} and {b.seg_id} overlap in physical memory.")
    return table, warnings


def translate(table, segment, offset):
    """Translate one (segment, offset) pair. Never raises; problems go into `status`."""
    seg = table.get(segment)
    if seg is None:
        return SegTranslation(segment, offset, None, None, None, SEGMENT_FAULT,
                              f"Segment {segment} does not exist in the segment table.")
    if offset < 0:
        return SegTranslation(segment, offset, seg.base, seg.limit, None, OUT_OF_RANGE,
                              "Offset cannot be negative.")
    if offset >= seg.limit:
        return SegTranslation(segment, offset, seg.base, seg.limit, None, OUT_OF_RANGE,
                              f"Offset {offset} >= limit {seg.limit}: trap (addressing error).")
    physical = seg.base + offset
    return SegTranslation(segment, offset, seg.base, seg.limit, physical, OK,
                          f"{seg.base} + {offset} = {physical}")
