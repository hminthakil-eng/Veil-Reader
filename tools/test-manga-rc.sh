#!/bin/sh
set -eu

./gradlew :app:testDebugUnitTest --stacktrace

RESULT_DIR="app/build/test-results/testDebugUnitTest"
test -d "$RESULT_DIR"

required_suites="
com.veilreader.app.manga.reader.MangaReaderModelTest
com.veilreader.app.manga.MangaAppRuntimeConfigTest
com.veilreader.app.manga.MangaRuntimeSourceAssemblyTest
com.veilreader.app.manga.MangaSourceCompositionTest
com.veilreader.app.manga.core.MangaHubTest
com.veilreader.app.manga.core.MangaSourceCatalogTest
com.veilreader.app.manga.sources.mangadex.MangaDexSourceProviderTest
com.veilreader.app.manga.storage.FileMangaStoresTest
com.veilreader.app.data.MangaLibraryBookModelTest
com.veilreader.app.data.MangaProgressionTest
com.veilreader.app.ui.navigation.VeilAppViewModelTest
"

missing=0
for suite in $required_suites; do
  report="$RESULT_DIR/TEST-$suite.xml"
  if [ ! -f "$report" ]; then
    echo "Missing Manga RC regression report: $suite"
    missing=1
  fi
done

if [ "$missing" -ne 0 ]; then
  exit 1
fi

echo "Manga RC regression contract: GREEN"
