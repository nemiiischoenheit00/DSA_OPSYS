"""Paging: page table + logical-to-physical address translation.

Simulation assumptions (shown in the GUI):
  * Page size == frame size. Physical memory = number_of_frames * page_size.
  * Logical address space = number_of_pages * page_size  (addresses 0 .. size-1).
  * The user supplies the page table: page -> frame. A blank frame means the page is
    NOT in memory (valid bit = 0), so touching it causes a page fault.
  * Two pages cannot share one frame.
  * page = address // page_size,  offset = address % page_size,
    physical = frame * page_size + offset.
"""
from dataclasses import dataclass
from typing import Optional

from .validation import ValidationError

OK = "OK"
PAGE_FAULT = "PAGE FAULT"
INVALID = "INVALID"


@dataclass
class PagingConfig:
    page_size: int
    num_frames: int
    num_pages: int

    @property
    def logical_size(self):
        return self.num_pages * self.page_size

    @property
    def physical_size(self):
        return self.num_frames * self.page_size


@dataclass
class Translation:
    address: int
    page: Optional[int]
    offset: Optional[int]
    frame: Optional[int]
    physical: Optional[int]
    status: str
    message: str


def validate_config(config):
    if config.page_size <= 0:
        raise ValidationError("Page size must be greater than 0.")
    if config.num_frames <= 0:
        raise ValidationError("Number of frames must be greater than 0.")
    if config.num_pages <= 0:
        raise ValidationError("Number of logical pages must be greater than 0.")


def build_page_table(config, entries):
    """entries: list of (page, frame_or_None). Returns {page: frame_or_None} after validation."""
    validate_config(config)
    table = {}
    used_frames = {}
    for page, frame in entries:
        if not 0 <= page < config.num_pages:
            raise ValidationError(
                f"Page {page} is outside the logical address space (pages 0..{config.num_pages - 1}).")
        if page in table:
            raise ValidationError(f"Page {page} appears more than once in the page table.")
        if frame is not None:
            if not 0 <= frame < config.num_frames:
                raise ValidationError(
                    f"Page {page} maps to frame {frame}, but valid frames are 0..{config.num_frames - 1}.")
            if frame in used_frames:
                raise ValidationError(
                    f"Frame {frame} is mapped by both page {used_frames[frame]} and page {page}.")
            used_frames[frame] = page
        table[page] = frame
    return table


def auto_map(config):
    """Sequential mapping page i -> frame i for as many pages as there are frames."""
    return [(p, p) for p in range(min(config.num_pages, config.num_frames))]


def translate(config, page_table, address):
    """Translate one logical address. Never raises; problems are reported in `status`."""
    if address < 0:
        return Translation(address, None, None, None, None, INVALID,
                           "Negative addresses are not allowed.")
    if address >= config.logical_size:
        return Translation(address, None, None, None, None, INVALID,
                           f"Address is beyond the logical address space (0..{config.logical_size - 1}).")
    page, offset = divmod(address, config.page_size)
    frame = page_table.get(page)
    if frame is None:
        why = "page is not in the page table" if page not in page_table else "page is not in memory"
        return Translation(address, page, offset, None, None, PAGE_FAULT,
                           f"Page {page}: {why} (page fault, the OS would load it).")
    physical = frame * config.page_size + offset
    return Translation(address, page, offset, frame, physical, OK,
                       f"{frame} x {config.page_size} + {offset} = {physical}")


def frame_table(config, page_table):
    """List of (frame, page or None) for every physical frame."""
    owner = {f: p for p, f in page_table.items() if f is not None}
    return [(f, owner.get(f)) for f in range(config.num_frames)]
