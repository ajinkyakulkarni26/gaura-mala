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

            errors, warnings = inspect_release_artifacts(aab)
            self.assertEqual([], errors)
            self.assertEqual([], warnings)

    def test_rejects_bundle_missing_arm64_abi(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, [
                "base/lib/x86_64/libcounter.so",
                "BUNDLE-METADATA/com.android.tools.build.debugsymbols/arm64-v8a/libcounter.so.sym",
            ])

            errors, _ = inspect_release_artifacts(aab)

            self.assertTrue(any("missing required Wear OS ABI arm64-v8a" in error for error in errors))

    def test_rejects_32_bit_abi_without_its_64_bit_pair(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, [
                "base/lib/arm64-v8a/libcounter.so",
                "base/lib/x86/libcounter.so",
                "BUNDLE-METADATA/com.android.tools.build.debugsymbols/arm64-v8a/libcounter.so.sym",
            ])

            errors, _ = inspect_release_artifacts(aab)

            self.assertTrue(any("without matching x86_64" in error for error in errors))

    def test_warns_when_bundle_has_no_embedded_native_symbols(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, ["base/lib/arm64-v8a/libcounter.so"])

            errors, warnings = inspect_release_artifacts(aab)

            self.assertEqual([], errors)
            self.assertTrue(any("no embedded native debug symbols" in warning for warning in warnings))

    def test_warns_when_symbols_exist_only_for_other_abis(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            self.write_aab(aab, [
                "base/lib/arm64-v8a/libcounter.so",
                "BUNDLE-METADATA/com.android.tools.build.debugsymbols/x86_64/libcounter.so.sym",
            ])

            errors, warnings = inspect_release_artifacts(aab)

            self.assertEqual([], errors)
            self.assertTrue(any("no embedded native debug symbols for arm64-v8a" in warning for warning in warnings))

    def test_warns_when_embedded_symbol_file_is_empty(self) -> None:
        with tempfile.TemporaryDirectory() as temp_dir:
            aab = Path(temp_dir) / "release.aab"
            with ZipFile(aab, "w") as archive:
                archive.writestr("base/lib/arm64-v8a/libcounter.so", b"binary")
                archive.writestr(
                    "BUNDLE-METADATA/com.android.tools.build.debugsymbols/arm64-v8a/libcounter.so.sym",
                    b"",
                )

            errors, warnings = inspect_release_artifacts(aab)

            self.assertEqual([], errors)
            self.assertTrue(any("no embedded native debug symbols" in warning for warning in warnings))


if __name__ == "__main__":
    unittest.main()
