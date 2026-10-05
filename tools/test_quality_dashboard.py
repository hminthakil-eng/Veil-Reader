#!/usr/bin/env python3
import importlib.util
from pathlib import Path

module_path = Path(__file__).with_name("build_quality_dashboard.py")
spec = importlib.util.spec_from_file_location("quality_dashboard", module_path)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)

def test_latest_by_name():
    rows = [
        {"id": 1, "name": "build", "conclusion": "failure"},
        {"id": 3, "name": "build", "conclusion": "success"},
        {"id": 2, "name": "room-on-android", "conclusion": "success"},
    ]
    latest = module.latest_by_name(rows)
    assert latest["build"]["id"] == 3
    assert latest["room-on-android"]["id"] == 2

def test_run_id():
    check = {"html_url": "https://github.com/o/r/actions/runs/12345/job/678"}
    assert module.run_id_from_check(check) == 12345
    assert module.run_id_from_check({}) is None

if __name__ == "__main__":
    test_latest_by_name()
    test_run_id()
    print("quality dashboard parser tests: PASS")
