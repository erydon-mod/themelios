import importlib.util
import sys
import unittest
from pathlib import Path


sys.path.insert(0, str(Path(__file__).resolve().parent))
SCRIPT = Path(__file__).with_name("generate_rock_family.py")
SPEC = importlib.util.spec_from_file_location("themelios_generate_rock_family", SCRIPT)
ROCK_FAMILY = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(ROCK_FAMILY)


class RockFamilyAssetsTest(unittest.TestCase):
    def test_committed_outputs_are_complete(self) -> None:
        ROCK_FAMILY.validate_outputs()


if __name__ == "__main__":
    unittest.main()
