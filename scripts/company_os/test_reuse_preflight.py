#!/usr/bin/env python3
import importlib.util
from pathlib import Path

MODULE = Path(__file__).with_name("reuse_preflight.py")
spec = importlib.util.spec_from_file_location("reuse_preflight", MODULE)
mod = importlib.util.module_from_spec(spec)
assert spec.loader is not None
spec.loader.exec_module(mod)

same = mod.score(
    "Evaluate DeepSeek Harness runtime session replay and recovery",
    "DeepSeek Harness session recovery replay runtime evaluation",
)
distinct = mod.score(
    "Evaluate DeepSeek Harness runtime session replay and recovery",
    "Redesign EPUB typography controls for reader appearance",
)
partial = mod.score(
    "Build Company OS duplicate work prevention registry",
    "Company work reuse gate to prevent duplicate implementation",
)

assert same > 0.50, same
assert partial >= 0.30, partial
assert distinct < 0.30, distinct

print("reuse preflight scoring tests passed")
