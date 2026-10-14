"""Adversarial content-gate tests; no Android SDK or model API needed."""
import json
import shutil
import tempfile
import unittest
from pathlib import Path

from tools.island_content.validate import ContentError, validate

BASE = Path(__file__).resolve().parents[3] / "game-content" / "island" / "v1"

class ContentValidationTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name) / "island"
        shutil.copytree(BASE, self.root)

    def tearDown(self):
        self.temp.cleanup()

    def edit(self, rel, mutation):
        file = self.root / rel
        data = json.loads(file.read_text(encoding="utf-8"))
        mutation(data)
        file.write_text(json.dumps(data, ensure_ascii=False), encoding="utf-8")

    def invalid(self, text):
        with self.assertRaisesRegex(ContentError, text):
            validate(self.root)

    def test_valid_dual_language_package(self):
        receipt = validate(self.root)
        self.assertEqual(receipt["scenes"], 2)
        self.assertEqual(receipt["objects"], 7)
        self.assertEqual(receipt["dialogueNodes"], 4)

    def test_planned_art_not_allowed_to_ship(self):
        with self.assertRaisesRegex(ContentError, "placeholder cannot ship"):
            validate(self.root, require_final_art=True)

    def test_missing_translation_fails(self):
        self.edit("dialogues/fa.json", lambda d: d["nodes"].pop())
        self.invalid("translation: node IDs")

    def test_dialogue_choice_links_are_language_consistent(self):
        self.edit("dialogues/fa.json", lambda d: d["nodes"][0]["choices"][0].update(next="island.story.mirror"))
        self.invalid("translation: choices differ")

    def test_invalid_dialogue_reference_fails(self):
        self.edit("scenes/shore.json", lambda d: d["objects"][0].update(dialogueId="missing.node"))
        self.invalid("unknown dialogue")

    def test_duplicate_world_object_id_fails(self):
        self.edit("scenes/shore.json", lambda d: d["objects"][0].update(id="island.bookshop.elder.v1"))
        self.invalid("globally duplicate object")

    def test_portal_target_fails_if_scene_missing(self):
        self.edit("scenes/shore.json", lambda d: d["objects"][-1].update(toScene="no.such.scene"))
        self.invalid("missing destination")

    def test_portal_target_fails_if_spawn_missing(self):
        self.edit("scenes/shore.json", lambda d: d["objects"][-1].update(toSpawn="not.an.entry"))
        self.invalid("missing destination spawn")

    def test_collision_grid_size_is_checked(self):
        self.edit("scenes/bookshop.json", lambda d: d["collisionRows"].__setitem__(2, "###"))
        self.invalid("collision row")

    def test_unreachable_interaction_is_rejected(self):
        def block(d):
            d["collisionRows"][3] = "#...########"
            d["collisionRows"][4] = "#...########"
            d["collisionRows"][5] = "#...########"
            d["objects"][0]["x"] = 8
            d["objects"][0]["y"] = 4
        self.edit("scenes/bookshop.json", block)
        self.invalid("unreachable|blocked/outside|disconnected")

    def test_unknown_art_id_is_rejected(self):
        self.edit("scenes/bookshop.json", lambda d: d["objects"][0].update(artId="art.does.not.exist"))
        self.invalid("undefined pixel art")

    def test_private_reader_fields_never_belong_to_game_map(self):
        self.edit("scenes/shore.json", lambda d: d.update(locator={"chapter": 3}))
        self.invalid("private reader key forbidden")

    def test_path_escape_is_rejected(self):
        self.edit("manifest.json", lambda d: d["scenes"][0].update(file="../private.json"))
        self.invalid("invalid json path|unsafe path")

    def test_no_real_book_files_are_accepted_as_art(self):
        self.edit("art_catalog.json", lambda d: d["assets"][0].update(status="ready", file="../../book.epub"))
        self.invalid("invalid art file")

if __name__ == "__main__":
    unittest.main()
