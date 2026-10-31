import math
import random
from datetime import datetime, timedelta
from pathlib import Path
from zoneinfo import ZoneInfo

KST = ZoneInfo("Asia/Seoul")
random.seed(42)

# Approx Gyeongbu corridor Daegu -> Daejeon
waypoints = [
    (35.8714, 128.6014, 40.0),
    (35.9200, 128.4500, 55.0),
    (36.0000, 128.2500, 80.0),
    (36.0800, 128.0500, 120.0),
    (36.1500, 127.9000, 150.0),
    (36.2200, 127.7500, 180.0),
    (36.2800, 127.6000, 200.0),
    (36.3300, 127.4800, 90.0),
    (36.3504, 127.3845, 60.0),
]


def haversine_m(lat1, lon1, lat2, lon2):
    r = 6371000.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dl / 2) ** 2
    return 2 * r * math.asin(math.sqrt(a))


dense = []
for i in range(len(waypoints) - 1):
    lat1, lon1, alt1 = waypoints[i]
    lat2, lon2, alt2 = waypoints[i + 1]
    dist = haversine_m(lat1, lon1, lat2, lon2)
    steps = max(1, int(dist / 200))
    for s in range(steps):
        t = s / steps
        dense.append(
            (
                lat1 + (lat2 - lat1) * t,
                lon1 + (lon2 - lon1) * t,
                alt1 + (alt2 - alt1) * t,
            )
        )
dense.append(waypoints[-1])

interval = 5.0
start = datetime(2026, 11, 1, 9, 0, 0, tzinfo=KST)
t = start
idx = 0.0
rows = ["time,latitude,longitude,altitude"]

lat, lon, alt = dense[0]
rows.append(f"{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]},{lat:.6f},{lon:.6f},{alt:.1f}")

while idx < len(dense) - 1:
    speed = max(80.0, min(120.0, random.uniform(80, 120) + random.uniform(-5, 5)))
    remaining = (speed * 1000.0 / 3600.0) * interval

    while remaining > 0 and idx < len(dense) - 1:
        i0 = int(idx)
        frac = idx - i0
        if i0 >= len(dense) - 1:
            break
        lat0, lon0, alt0 = dense[i0]
        lat1, lon1, alt1 = dense[i0 + 1]
        seg_len = haversine_m(lat0, lon0, lat1, lon1)
        if seg_len < 1e-3:
            idx = float(i0 + 1)
            continue
        pos_along = frac * seg_len
        left = seg_len - pos_along
        if remaining < left:
            idx = i0 + (pos_along + remaining) / seg_len
            remaining = 0
        else:
            remaining -= left
            idx = float(i0 + 1)

    t += timedelta(seconds=interval)
    if idx >= len(dense) - 1:
        lat, lon, alt = dense[-1]
        jlat = random.uniform(-0.00002, 0.00002)
        jlon = random.uniform(-0.00002, 0.00002)
        jalt = random.uniform(-2, 2)
        rows.append(
            f"{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]},{lat + jlat:.6f},{lon + jlon:.6f},{alt + jalt:.1f}"
        )
        break

    i0 = min(int(idx), len(dense) - 2)
    frac = idx - i0
    lat0, lon0, alt0 = dense[i0]
    lat1, lon1, alt1 = dense[i0 + 1]
    lat = lat0 + (lat1 - lat0) * frac
    lon = lon0 + (lon1 - lon0) * frac
    alt = alt0 + (alt1 - alt0) * frac
    jlat = random.uniform(-0.00002, 0.00002)
    jlon = random.uniform(-0.00002, 0.00002)
    jalt = random.uniform(-2, 2)
    rows.append(
        f"{t.strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]},{lat + jlat:.6f},{lon + jlon:.6f},{alt + jalt:.1f}"
    )

out_dir = Path(__file__).resolve().parent / "cluster" / "gps"
out_dir.mkdir(parents=True, exist_ok=True)
out_file = out_dir / "gps_20261101.csv"
out_file.write_text("\n".join(rows) + "\n", encoding="utf-8")

n = len(rows) - 1
duration_min = (n - 1) * interval / 60.0
total = 0.0
prev = None
for line in rows[1:]:
    parts = line.split(",")
    p = (float(parts[1]), float(parts[2]))
    if prev:
        total += haversine_m(prev[0], prev[1], p[0], p[1])
    prev = p

print(f"points={n}")
print(f"duration_min={duration_min:.1f}")
print(f"distance_km={total / 1000:.1f}")
print(f"avg_speed_kmh={(total / 1000) / (duration_min / 60):.1f}")
print(f"file={out_file}")
print(f"start={rows[1]}")
print(f"end={rows[-1]}")
