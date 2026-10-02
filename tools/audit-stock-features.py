#!/usr/bin/env python3
"""Inventory stock vendor/odm files against extraction lists and build hints.

Presence is not proof that a feature works. Source-replacement hints need
review against the generated product; this tool does not enable features.
"""
import argparse
import csv
import json
from collections import Counter
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--workspace', type=Path, required=True)
parser.add_argument('--stock', type=Path, required=True)
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--built-files', type=Path,
                    help='Optional directory of generated installed-files-*.json reports')
args = parser.parse_args()
selected = {}
unpinned = []
for device in ('piano', 'sm8750-common'):
    manifest = args.workspace / f'device/xiaomi/{device}/proprietary-files.txt'
    for line in manifest.read_text().splitlines():
        if not line.strip() or line.startswith('#'):
            continue
        fields = line.split('|')
        spec = fields[0].lstrip('-').split(';')[0]
        source, _, destination = spec.partition(':')
        selected.setdefault(source, []).append((device, destination or source))
        if len(fields) < 2:
            unpinned.append(source)

build_text = ''
for device in ('piano', 'sm8750-common'):
    tree = args.workspace / f'device/xiaomi/{device}'
    for pattern in ('*.mk', '*.bp'):
        for path in tree.rglob(pattern):
            build_text += path.read_text(errors='replace') + '\n'

built = set()
if args.built_files:
    for report in args.built_files.glob('installed-files-*.json'):
        for entry in json.loads(report.read_text()):
            built.add(entry['Name'].lstrip('/'))

rows = []
for partition in ('vendor', 'odm'):
    base = args.stock / partition
    if not base.is_dir():
        raise SystemExit(f'Missing stock partition: {base}')
    for path in sorted(base.rglob('*')):
        if not path.is_file():
            continue
        relative = path.relative_to(args.stock).as_posix()
        entries = selected.get(relative, [])
        present = any((args.workspace / f'vendor/xiaomi/{device}/proprietary' / dest).is_file()
                      for device, dest in entries)
        # The text match is deliberately a hint, not an installed-file claim.
        hinted = path.name in build_text
        state = ('selected-present' if present else 'selected-missing') if entries else (
            'source-reference-review' if hinted else 'unselected-review')
        candidates = [dest for _, dest in entries] or [relative]
        installed = ','.join(dest for dest in candidates if dest in built)
        rows.append((relative, state, ','.join(f'{d}:{dst}' for d, dst in entries), installed))

args.output.mkdir(parents=True, exist_ok=True)
with (args.output / 'vendor-odm-inventory.tsv').open('w', newline='') as handle:
    writer = csv.writer(handle, delimiter='\t')
    writer.writerow(('stock_path', 'inventory_state', 'extraction_destination',
                     'generated_build_path'))
    writer.writerows(rows)
summary = {
    'scope': 'Regular files and resolvable file symlinks in supplied vendor/odm dump',
    'warning': 'Inventory states do not prove installed feature support or ABI compatibility',
    'stock_file_count': len(rows),
    'states': dict(Counter(row[1] for row in rows)),
    'generated_build_reports_supplied': args.built_files is not None,
    'paths_found_in_generated_build': sum(bool(row[3]) for row in rows),
    'unpinned_manifest_inputs': unpinned,
}
(args.output / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
