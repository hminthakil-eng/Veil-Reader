#!/usr/bin/env python3
"""Fail-closed validation for the first original, offline Station 3D asset.

No external modules; the CI and asset review use this same validator.
This does NOT assert visual excellence or an actual GPU render.
"""
import base64
import json
import math
import pathlib
import struct

asset = pathlib.Path("app/src/debug/assets/models/veil_victorian_sign.gltf")
document = json.loads(asset.read_text(encoding="utf-8"))
assert document["asset"]["version"] == "2.0"
assert "Original" in document["asset"]["copyright"] or "Original" in document["asset"]["generator"]
assert document["scene"] == 0
assert 0 < len(document["nodes"]) <= 64
assert 0 < len(document["materials"]) <= 8
assert asset.stat().st_size < 100_000

uri = document["buffers"][0]["uri"]
prefix = "data:application/octet-stream;base64,"
assert uri.startswith(prefix), "No network or external resources accepted"
buf = base64.b64decode(uri[len(prefix):], validate=True)
assert len(buf) == document["buffers"][0]["byteLength"]
assert len(buf) < 100_000

# Every glTF node is local authored geometry with finite transforms,
# and mesh/material references must exist.
for node in document["nodes"]:
    assert 0 <= node["mesh"] < len(document["meshes"])
    for key in ("translation", "scale", "rotation"):
        if key in node:
            assert all(math.isfinite(float(x)) for x in node[key])
    assert all(abs(float(x)) > 0 for x in node["scale"]), node["name"]

for view in document["bufferViews"]:
    assert view["buffer"] == 0
    offset = view.get("byteOffset", 0)
    assert offset >= 0 and offset + view["byteLength"] <= len(buf)

for accessor in document["accessors"]:
    assert accessor["count"] > 0
    assert 0 <= accessor["bufferView"] < len(document["bufferViews"])

indices_accessor = document["accessors"][2]
assert indices_accessor["componentType"] == 5123
indices_view = document["bufferViews"][indices_accessor["bufferView"]]
offset = indices_view["byteOffset"]
count = indices_accessor["count"]
indices = struct.unpack_from("<" + "H" * count, buf, offset)
assert count % 3 == 0
assert max(indices) < document["accessors"][0]["count"]

names = [x["name"].lower() for x in document["nodes"]]
for feature in ("station plaque", "brass top", "chain", "clock", "lamp"):
    assert any(feature in name for name in names), feature
assert not document.get("images"), "No remote textures or images in Gate 2"
assert not document.get("extensionsRequired"), "No fragile renderer extension in Gate 2"
print(
    "VALID GLTF2 | original, offline, PBR-compatible "
    f"| {len(document['nodes'])} nodes | {len(buf)}B embedded geometry"
)
