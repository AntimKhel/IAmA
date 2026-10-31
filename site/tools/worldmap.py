# Builds src/jsMain/kotlin/dev/ritwik/iama/data/WorldMap.kt (the Stats app's visitor map) from Natural Earth
# (public domain): rasterizes the 110m land polygons into a dot grid and takes each country's label point
# from the 50m admin-0 countries. Usage:
#   curl -fsSLO https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_110m_land.geojson
#   curl -fsSLO https://raw.githubusercontent.com/nvkelso/natural-earth-vector/master/geojson/ne_50m_admin_0_countries.geojson
#   python3 tools/worldmap.py ne_110m_land.geojson ne_50m_admin_0_countries.geojson src/jsMain/kotlin/dev/ritwik/iama/data/WorldMap.kt
import json, sys, base64, math
land_path, countries_path, out_path = sys.argv[1:4]
COLS, LAT_TOP, LAT_BOTTOM = 200, 75.0, -56.0
STEP = 360.0 / COLS
ROWS = int(round((LAT_TOP - LAT_BOTTOM) / STEP))

def rings(geom):
    if geom['type'] == 'Polygon': return [geom['coordinates']]
    if geom['type'] == 'MultiPolygon': return geom['coordinates']
    return []

polys = []
for f in json.load(open(land_path))['features']:
    for poly in rings(f['geometry']):
        outer = poly[0]
        xs = [p[0] for p in outer]; ys = [p[1] for p in outer]
        polys.append((min(xs), max(xs), min(ys), max(ys), poly))

def inside(ring, x, y):
    c = False
    n = len(ring)
    j = n - 1
    for i in range(n):
        xi, yi = ring[i][0], ring[i][1]; xj, yj = ring[j][0], ring[j][1]
        if ((yi > y) != (yj > y)) and (x < (xj - xi) * (y - yi) / (yj - yi + 1e-12) + xi): c = not c
        j = i
    return c

def on_land(x, y):
    for (x0, x1, y0, y1, poly) in polys:
        if x0 <= x <= x1 and y0 <= y <= y1 and inside(poly[0], x, y):
            if not any(inside(h, x, y) for h in poly[1:]): return True
    return False

bits = bytearray((COLS * ROWS + 7) // 8)
count = 0
for r in range(ROWS):
    lat = LAT_TOP - (r + 0.5) * STEP
    for c in range(COLS):
        lon = -180 + (c + 0.5) * STEP
        if on_land(lon, lat):
            k = r * COLS + c
            bits[k >> 3] |= 1 << (7 - (k & 7))
            count += 1

points = {}
for f in json.load(open(countries_path))['features']:
    p = f['properties']
    code = p.get('ISO_A2_EH') or p.get('ISO_A2')
    if not code or len(code) != 2 or code == '-9': continue
    x, y = p.get('LABEL_X'), p.get('LABEL_Y')
    if x is None or y is None: continue
    points[code] = (round(x, 1), round(y, 1))

pts = ';'.join(f'{k}:{v[0]},{v[1]}' for k, v in sorted(points.items()))
b64 = base64.b64encode(bytes(bits)).decode()
kt = f'''package dev.ritwik.iama.data

// Generated from Natural Earth (public domain): ne_110m_land rasterized to a dot grid, and label points
// from ne_50m_admin_0_countries. Regenerate with site/tools/worldmap.py; don't edit by hand.

/** The visitor map's dot grid: equirectangular, {COLS}×{ROWS} cells from {LAT_TOP}°N to {-LAT_BOTTOM}°S, one bit per cell (1 = land). */
object WorldMap {{
    const val COLS = {COLS}
    const val ROWS = {ROWS}
    const val LAT_TOP = {LAT_TOP}
    const val LAT_BOTTOM = {LAT_BOTTOM}
    const val LAND_BITS = "{b64}"

    /** Where to put each country's pulse: ISO 3166-1 alpha-2 → label point (longitude, latitude). */
    const val COUNTRY_POINTS = "{pts}"
}}
'''
open(out_path, 'w').write(kt)
print(f'grid {COLS}x{ROWS}, land cells {count} ({count*100//(COLS*ROWS)}%), countries {len(points)}, bits {len(b64)} chars, points {len(pts)} chars')
