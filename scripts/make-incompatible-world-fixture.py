#!/usr/bin/env python3
"""Create a disposable World JAR with a missing registration API for GameTests.

Usage: python3 scripts/make-incompatible-world-fixture.py WORLD.jar build/world-incompatible-api.jar
Append --helper to rename the internal helper and its World call sites instead.
Never use the output in a real game instance.
"""
import re
import sys
import zipfile

with zipfile.ZipFile(sys.argv[1]) as source, zipfile.ZipFile(sys.argv[2], 'w') as target:
    found = False
    for entry in source.infolist():
        data = source.read(entry.filename)
        if '--helper' in sys.argv[3:]:
            if entry.filename.endswith('.class') and b'handleRandomTick' in data:
                data = data.replace(b'handleRandomTick', b'handleRandomTock')
                found = True
        elif entry.filename.endswith('/RegnumWorldApi.class'):
            old, new = b'registerGrowthController', b'invalidxGrowthController'
            assert len(old) == len(new) and old in data
            data = data.replace(old, new)
            found = True
        if entry.filename == 'META-INF/MANIFEST.MF':
            # This synthetic fixture tests runtime ABI rejection, not Loom tool compatibility.
            data = re.sub(rb'Fabric-Loom-Version: [^\r\n]*\r?\n', b'', data)
        target.writestr(entry, data)
    assert found, 'World registration API class was not found'
