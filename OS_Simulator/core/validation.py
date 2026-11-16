"""Shared input-validation helpers.

Every parser raises ValidationError with a human-readable message.
The GUI catches ValidationError and shows it in a dialog, so bad input
never crashes the application.
"""
import re


class ValidationError(ValueError):
    """Raised when user input is empty, malformed or inconsistent."""


def parse_int(text, name, minimum=None, maximum=None):
    """Convert text to int. Rejects blanks, decimals, letters and out-of-range values."""
    s = str(text).strip()
    if s == "":
        raise ValidationError(f"{name} is required (the field is empty).")
    body = s[1:] if s[0] in "+-" else s
    if not (body.isascii() and body.isdigit()):
        raise ValidationError(f"{name} must be a whole number (got '{s}').")
    value = int(s)
    if minimum is not None and value < minimum:
        raise ValidationError(f"{name} must be at least {minimum} (got {value}).")
    if maximum is not None and value > maximum:
        raise ValidationError(f"{name} must be at most {maximum} (got {value}).")
    return value


def parse_int_list(text, name, minimum=None, maximum=None):
    """Parse '1, 2 3;4' into [1, 2, 3, 4]. At least one value is required."""
    s = str(text).strip()
    if s == "":
        raise ValidationError(f"{name} is required (the field is empty).")
    tokens = [t for t in re.split(r"[,\s;]+", s) if t]
    return [parse_int(t, f"{name} item {i}", minimum, maximum)
            for i, t in enumerate(tokens, start=1)]


def require_text(text, name):
    s = str(text).strip()
    if s == "":
        raise ValidationError(f"{name} is required (the field is empty).")
    return s
