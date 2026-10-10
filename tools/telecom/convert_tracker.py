#!/usr/bin/env python3
"""
Converts multi-workbook telecom Excel sheets (e.g. INFRA TRACKER.xlsx)
with composite headers to standard RFC 4180 CSV for CoMaps Android import.
"""

import sys
import os
import csv
import zipfile
import xml.etree.ElementTree as ET

def convert_xlsx_to_csv(xlsx_path, output_csv_path):
    if not os.path.isfile(xlsx_path):
        print(f"Error: File not found: {xlsx_path}", file=sys.stderr)
        sys.exit(1)

    print(f"Reading: {xlsx_path}...")
    with zipfile.ZipFile(xlsx_path) as z:
        shared_strings = []
        if 'xl/sharedStrings.xml' in z.namelist():
            tree = ET.fromstring(z.read('xl/sharedStrings.xml'))
            for si in tree.findall('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}si'):
                text = ''.join(t.text for t in si.findall('.//{http://schemas.openxmlformats.org/spreadsheetml/2006/main}t') if t.text)
                shared_strings.append(text)

        stree = ET.fromstring(z.read('xl/worksheets/sheet1.xml'))
        rows = stree.findall('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}sheetData/{http://schemas.openxmlformats.org/spreadsheetml/2006/main}row')

        def get_row(r):
            cells = {}
            for c in r.findall('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}c'):
                t = c.attrib.get('t')
                v = c.find('{http://schemas.openxmlformats.org/spreadsheetml/2006/main}v')
                val = v.text if v is not None else ''
                if t == 's' and val.isdigit():
                    val = shared_strings[int(val)]
                ref = c.attrib.get('r')
                col = ''.join(ch for ch in ref if ch.isalpha())
                cells[col] = val
            return cells

        if len(rows) < 2:
            print("Error: Sheet has fewer than 2 rows.", file=sys.stderr)
            sys.exit(1)

        h1 = get_row(rows[0])
        h2 = get_row(rows[1])
        all_col_letters = sorted(set(list(h1.keys()) + list(h2.keys())), key=lambda x: (len(x), x))
        composite_headers = []
        for c in all_col_letters:
            val1 = h1.get(c, '').strip()
            val2 = h2.get(c, '').strip()
            if val2 and val1 and val2.lower() not in val1.lower():
                name = f'{val1} {val2}'
            elif val2:
                name = val2
            else:
                name = val1
            composite_headers.append(name)

        records = []
        for r in rows[2:]:
            data = get_row(r)
            row_vals = [data.get(c, '').strip() for c in all_col_letters]
            if any(row_vals):
                records.append(row_vals)

        os.makedirs(os.path.dirname(os.path.abspath(output_csv_path)), exist_ok=True)
        with open(output_csv_path, 'w', newline='', encoding='utf-8') as f:
            writer = csv.writer(f)
            writer.writerow(composite_headers)
            writer.writerows(records)

    print(f"Successfully generated {output_csv_path} ({len(records)} records).")

if __name__ == '__main__':
    base_dir = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    default_in = os.path.join(base_dir, "INFRA TRACKER.xlsx")
    default_out = os.path.join(base_dir, "android", "app", "src", "main", "assets", "telecom", "infra_tracker_demo.csv")

    in_path = sys.argv[1] if len(sys.argv) > 1 else default_in
    out_path = sys.argv[2] if len(sys.argv) > 2 else default_out
    convert_xlsx_to_csv(in_path, out_path)
