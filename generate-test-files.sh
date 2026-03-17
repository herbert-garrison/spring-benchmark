#!/usr/bin/env bash

set -e
DEST="load-testing/src/test/resources/bodies"
mkdir -p "$DEST"
for SIZE_MB in 1 5 10 25 50; do
    FILEPATH="$DEST/test_${SIZE_MB}mb.bin"
    [ -f "$FILEPATH" ] && echo "skip $FILEPATH" && continue
    echo -n "generating test_${SIZE_MB}mb.bin... "
    dd if=/dev/urandom of="$FILEPATH" bs=1M count="$SIZE_MB" 2>/dev/null
    echo "done"
done
echo "All files ready in $DEST"
