#!/usr/bin/env python3
"""Offline island-game content gate. Python 3.10+, stdlib only.

Consumes a small versioned, engine-neutral intermediate package. It is NOT a
native LDtk/.tmx parser; conversion from editors must pass this contract.
Never opens or indexes real user ebooks.
"""
from __future__ import annotations
import argparse
import json
import re
from collections import deque
from pathlib import Path

ID = re.compile(r"^[a-z][a-z0-9_.:-]{1,95}$")
MODES = {"npc", "prop", "portal"}
RESERVED = {"locator", "epub", "fileuri", "userbookid", "privatebook", "readingtext"}

class ContentError(ValueError):
    pass

def check(ok: bool, message: str) -> None:
    if not ok:
        raise ContentError(message)

def identifier(value: object, context: str) -> str:
    check(isinstance(value, str) and ID.fullmatch(value) is not None,
          f"{context}: invalid stable ID {value!r}")
    return value

def read_json(root: Path, relative: str) -> dict:
    check(isinstance(relative, str) and relative.endswith(".json"),
          f"invalid json path: {relative!r}")
    target = (root / relative).resolve()
    check(target.is_relative_to(root.resolve()), f"unsafe path: {relative}")
    check(target.is_file(), f"missing file: {relative}")
    try:
        raw = target.read_text(encoding="utf-8")
        result = json.loads(raw)
    except (ValueError, UnicodeError) as exc:
        raise ContentError(f"cannot read JSON {relative}: {exc}") from exc
    check(isinstance(result, dict), f"{relative}: expected object")
    return result

def unique_ids(rows: list, context: str) -> dict:
    check(isinstance(rows, list), f"{context}: expected list")
    result = {}
    for row in rows:
        check(isinstance(row, dict), f"{context}: expected objects")
        item_id = identifier(row.get("id"), context)
        check(item_id not in result, f"{context}: duplicate ID {item_id}")
        result[item_id] = row
    return result

def neighbors(x: int, y: int):
    yield (x + 1, y)
    yield (x - 1, y)
    yield (x, y + 1)
    yield (x, y - 1)

def validate(root: Path, require_final_art: bool = False) -> dict:
    root = root.resolve()
    manifest = read_json(root, "manifest.json")
    check(manifest.get("schemaVersion") == 1, "manifest: unknown schema version")
    identifier(manifest.get("worldId"), "world")
    scene_specs = unique_ids(manifest.get("scenes"), "manifest scenes")
    identifier(manifest.get("initialSceneId"), "initialSceneId")
    check(manifest["initialSceneId"] in scene_specs, "unknown initial scene")
    check(len(scene_specs) > 0, "no scenes declared")

    art = read_json(root, manifest.get("artCatalog"))
    check(art.get("schemaVersion") == 1, "art schema unsupported")
    assets = unique_ids(art.get("assets"), "art assets")
    check(assets, "art: no assets")
    for aid, item in assets.items():
        status = item.get("status")
        check(status in ("planned", "ready"), f"art {aid}: invalid status")
        if require_final_art:
            check(status == "ready", f"art {aid}: placeholder cannot ship")
        if status == "ready":
            path = item.get("file")
            check(isinstance(path, str) and path.lower().endswith((".png", ".webp")),
                  f"art {aid}: invalid art file")
            target = (root / path).resolve()
            check(target.is_relative_to(root) and target.is_file(),
                  f"art {aid}: missing/unsafe asset")
        check(item.get("kind") in ("pixel", "portrait", "tilemap"),
              f"art {aid}: invalid kind")

    avatars = read_json(root, manifest.get("avatarCatalog"))
    check(avatars.get("schemaVersion") == 1, "avatar schema unsupported")
    variants = unique_ids(avatars.get("variants"), "avatar variants")
    check(variants, "no avatar variants")
    for vid, item in variants.items():
        for key, kind in (("portraitId", "portrait"), ("pixelSpriteId", "pixel")):
            art_id = identifier(item.get(key), f"avatar {vid}.{key}")
            check(art_id in assets and assets[art_id]["kind"] == kind,
                  f"avatar {vid}: bad {key} {art_id}")
    check(identifier(avatars.get("defaultVariantId"), "default avatar") in variants,
          "avatar default unknown")

    dialogue = {}
    for lang in ("fa", "en"):
        rel = manifest.get("dialogues", {}).get(lang)
        doc = read_json(root, rel)
        check(doc.get("locale") == lang, f"language mismatch: {lang}")
        nodes = unique_ids(doc.get("nodes"), f"{lang} dialogue")
        check(nodes, f"{lang}: no dialogue nodes")
        for nid, node in nodes.items():
            check(isinstance(node.get("text"), str) and node["text"].strip(),
                  f"{lang} dialogue {nid}: missing text")
            choices = unique_ids(node.get("choices", []), f"{lang} {nid} choices")
            for cid, choice in choices.items():
                check(isinstance(choice.get("text"), str) and choice["text"].strip(),
                      f"{lang} {nid}.{cid}: empty choice text")
                following = choice.get("next")
                if following is not None:
                    identifier(following, f"{lang} choice next")
                    check(following in nodes, f"{lang} {nid}: broken next {following}")
        dialogue[lang] = nodes
    check(set(dialogue["fa"]) == set(dialogue["en"]),
          "translation: node IDs differ across locales")
    for nid in dialogue["fa"]:
        fa_choices = {x["id"]: x.get("next") for x in dialogue["fa"][nid].get("choices", [])}
        en_choices = {x["id"]: x.get("next") for x in dialogue["en"][nid].get("choices", [])}
        check(fa_choices == en_choices, f"translation: choices differ for {nid}")

    scenes = {}
    all_object_ids = set()
    for sid, spec in scene_specs.items():
        scene = read_json(root, spec.get("file"))
        check(scene.get("schemaVersion") == 1 and scene.get("id") == sid,
              f"scene {sid}: schema/ID mismatch")
        width, height = scene.get("width"), scene.get("height")
        check(type(width) is int and type(height) is int and 4 <= width <= 128
              and 4 <= height <= 128, f"scene {sid}: invalid dimensions")
        check(scene.get("tileSize") in (8, 16, 24, 32),
              f"scene {sid}: invalid tile size")
        rows = scene.get("collisionRows")
        check(isinstance(rows, list) and len(rows) == height and
              all(isinstance(s, str) and len(s) == width
                  and set(s) <= {"#", "."} for s in rows),
              f"scene {sid}: collision row size/symbol mismatch")
        def inside(p):
            return isinstance(p, dict) and type(p.get("x")) is int and type(p.get("y")) is int and 0 <= p["x"] < width and 0 <= p["y"] < height
        def walk(p):
            return inside(p) and rows[p["y"]][p["x"]] == "."
        spawns = scene.get("spawns")
        check(isinstance(spawns, dict) and spawns, f"scene {sid}: no spawns")
        for name, point in spawns.items():
            identifier(name, f"scene {sid} spawn")
            check(walk(point), f"scene {sid}: blocked/outside spawn {name}")
        objects = unique_ids(scene.get("objects"), f"scene {sid} objects")
        for oid, item in objects.items():
            check(oid not in all_object_ids, f"globally duplicate object {oid}")
            all_object_ids.add(oid)
            check(item.get("kind") in MODES, f"object {oid}: unsupported kind")
            check(inside(item), f"object {oid}: outside world")
            asset_id = identifier(item.get("artId"), f"object {oid}.artId")
            check(asset_id in assets and assets[asset_id]["kind"] == "pixel",
                  f"object {oid}: undefined pixel art")
            if "dialogueId" in item:
                check(item["dialogueId"] in dialogue["en"],
                      f"object {oid}: unknown dialogue node")
        # Reachable physical adjacency to all interactable objects, never through a wall.
        begin = spawns.get("entry", next(iter(spawns.values())))
        queue = deque([(begin["x"], begin["y"])])
        reachable = {(begin["x"], begin["y"])}
        while queue:
            x, y = queue.popleft()
            for nx, ny in neighbors(x, y):
                if 0 <= nx < width and 0 <= ny < height and rows[ny][nx] == "." and (nx, ny) not in reachable:
                    reachable.add((nx, ny))
                    queue.append((nx, ny))
        for name, point in spawns.items():
            check((point["x"], point["y"]) in reachable,
                  f"scene {sid}: spawn {name} disconnected")
        for oid, item in objects.items():
            target = (item["x"], item["y"])
            check(target in reachable or any(pos in reachable for pos in neighbors(*target)),
                  f"scene {sid}: unreachable object {oid}")
        scenes[sid] = scene
    for sid, scene in scenes.items():
        for obj in scene["objects"]:
            if obj["kind"] != "portal":
                continue
            dest = obj.get("toScene")
            spawn = obj.get("toSpawn")
            check(dest in scenes, f"portal {obj['id']}: missing destination")
            check(isinstance(spawn, str) and spawn in scenes[dest]["spawns"],
                  f"portal {obj['id']}: missing destination spawn")
    # Explicit source guard: no private-reader data is serialized in in-world content.
    def guard(value):
        if isinstance(value, dict):
            for key, element in value.items():
                check(key.lower() not in RESERVED, f"private reader key forbidden: {key}")
                guard(element)
        elif isinstance(value, list):
            for element in value:
                guard(element)
    for scene in scenes.values():
        guard(scene)
    return {"world": manifest["worldId"], "scenes": len(scenes),
            "objects": len(all_object_ids), "dialogueNodes": len(dialogue["fa"]),
            "art": len(assets), "placeholders": sum(v["status"] == "planned" for v in assets.values())}

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("game-content/island/v1"))
    parser.add_argument("--release", action="store_true", help="forbid any planned placeholder art")
    args = parser.parse_args()
    try:
        result = validate(args.root, require_final_art=args.release)
    except (ContentError, OSError) as exc:
        print("CONTENT FAIL:", exc)
        return 1
    print("CONTENT VALID (prototype):" if result["placeholders"] else "CONTENT VALID:", result)
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
