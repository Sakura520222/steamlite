import re, pathlib, sys

ROOT = pathlib.Path(r"D:\CodexMigration\Workspaces\2026-11-08\files-pasted-by-the-user-steam\outputs\SteamLite\app\src\main\java\org\steamlite")

def strip_code(src: str) -> str:
    out = []
    i = 0
    n = len(src)
    while i < n:
        c = src[i]
        if c == '/' and i + 1 < n and src[i+1] == '/':
            j = src.find('\n', i)
            i = n if j < 0 else j
            continue
        if c == '/' and i + 1 < n and src[i+1] == '*':
            j = src.find('*/', i + 2)
            i = n if j < 0 else j + 2
            continue
        if c == '"':
            if src.startswith('"""', i):
                j = src.find('"""', i + 3)
                i = n if j < 0 else j + 3
                out.append('""')
                continue
            j = i + 1
            while j < n:
                if src[j] == '\\':
                    j += 2
                    continue
                if src[j] == '"':
                    j += 1
                    break
                j += 1
            i = j
            out.append('""')
            continue
        if c == "'":
            j = i + 1
            while j < n:
                if src[j] == '\\':
                    j += 2
                    continue
                if src[j] == "'":
                    j += 1
                    break
                j += 1
            i = j
            out.append("''")
            continue
        out.append(c)
        i += 1
    return ''.join(out)

pairs = {')': '(', ']': '[', '}': '{'}
openers = set('([{')

bad = 0
for f in sorted(ROOT.glob("*.kt")):
    src = f.read_text(encoding="utf-8")
    code = strip_code(src)
    stack = []
    line = 1
    err = None
    for ch in code:
        if ch == '\n':
            line += 1
        elif ch in openers:
            stack.append((ch, line))
        elif ch in pairs:
            if not stack or stack[-1][0] != pairs[ch]:
                err = f"line {line}: unexpected '{ch}'"
                break
            stack.pop()
    if err is None and stack:
        ch, ln = stack[-1]
        err = f"unclosed '{ch}' opened at line {ln}"
    status = "OK " if not err else "BAD"
    if err:
        bad += 1
        print(f"{status} {f.name}: {err}")
    else:
        print(f"{status} {f.name}")

print("-" * 40)
print(f"{bad} file(s) with bracket problems")
