with open(r'E:\code\MOBILE\generate_report.py', 'r', encoding='utf-8') as f:
    lines = f.readlines()
for i, line in enumerate(lines, 1):
    stripped = line.strip()
    if stripped.startswith("'") and "{'" in stripped and not stripped.startswith("add_"):
        print(f"Line {i}: {stripped[:120]}")
