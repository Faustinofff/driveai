"""Valida >=90 de accesibilidad y conserva el detalle de auditorias fallidas."""
import json
from pathlib import Path
import sys

reports = sorted(Path(sys.argv[1] if len(sys.argv) > 1 else "work/lighthouse").glob("*.json"))
if len(reports) != 6:
    raise SystemExit(f"Se esperaban seis reportes (tres pantallas, mobile/desktop), recibidos {len(reports)}")
failed = False
for path in reports:
    report = json.loads(path.read_text(encoding="utf-8"))
    score = report.get("categories", {}).get("accessibility", {}).get("score")
    print(f"{path.name}: accesibilidad={None if score is None else round(score * 100)}")
    if score is None or score < 0.90:
        failed = True
    for audit in report.get("audits", {}).values():
        if audit.get("score") == 0:
            print("  Falla:", audit.get("id"), audit.get("title"))
            for item in audit.get("details", {}).get("items", [])[:8]:
                node = item.get("node", {})
                print("   ", node.get("selector", ""), node.get("explanation", ""))
raise SystemExit(1 if failed else 0)
