import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from check_release_aab import inspect_release_artifacts


class ReleaseAabCheckTest(unittest.TestCase):
    def write_aab(self, path: Path, members: list[str]) -> None:
        with ZipFile(path, "w") as archive:
            for member in members:
                archive.writestr(member, b"binary")

    def test_accepts_64_bit_wear_abis_and_embedded_arm64_symbols(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, [
                "base/lib/arm64-v8a/libcounter.so",
                "base/lib/armeabi-v7a/libcounter.so",
                "base/lib/x86/libcounter.so",
                "base/lib/x86_64/libcounter.so",
                "BUNDLE-METADATA/com.android.tools.build.debugsymbols/arm64-v8a/libcounter.so.sym",
            ])

            self.assertEqual([], inspect_release_artifacts(aab))

    def test_rejects_bundle_missing_arm64_abi(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, [
                "base/lib/x86_64/libcounter.so",
                "BUNDLE-METADATA/com.android.tools.build.debugsymbols/arm64-v8a/libcounter.so.sym",
            ])

            errors = inspect_release_artifacts(aab)

            self.assertTrue(any("missing required Wear OS ABI arm64-v8a" in error for error in errors))

    def test_rejects_32_bit_abi_without_its_64_bit_pair(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, [
                "base/lib/arm64-v8a/libcounter.so",
                "base/lib/x86/libcounter.so",
                "BUNDLE-METADATA/com.android.tools.build.debugsymbols/arm64-v8a/libcounter.so.sym",
            ])

            errors = inspect_release_artifacts(aab)

            self.assertTrue(any("without matching x86_64" in error for error in errors))

    def test_rejects_bundle_without_embedded_native_symbols(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, ["base/lib/arm64-v8a/libcounter.so"])

            errors = inspect_release_artifacts(aab)

            self.assertTrue(any("no embedded native debug symbols" in error for error in errors))

    def test_rejects_symbols_for_other_abis_without_arm64_symbols(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, [
                "base/lib/arm64-v8a/libcounter.so",
                "BUNDLE-METADATA/com.android.tools.build.debugsymbols/x86_64/libcounter.so.sym",
            ])

            errors = inspect_release_artifacts(aab)

            self.assertTrue(any("no embedded native debug symbols for arm64-v8a" in error for error in errors))

    def test_rejects_empty_embedded_symbol_file(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            with ZipFile(aab, "w") as archive:
                archive.writestr("base/lib/arm64-v8a/libcounter.so", b"binary")
                archive.writestr(
                    "BUNDLE-METADATA/com.android.tools.build.debugsymbols/arm64-v8a/libcounter.so.sym",
                    b"",
                )

            errors = inspect_release_artifacts(aab)

            self.assertTrue(any("no embedded native debug symbols" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
