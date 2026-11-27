"""Falla el pipeline si el reporte JSON de OWASP ZAP tiene alertas High.

Criterio de la cátedra (Eje D): 0 alertas High; las Medium se justifican una por una
en docs/testing/08-seguridad-y-resiliencia.md. Este script las lista para eso.

Uso: python3 ci/check-zap.py zap/zap-api.json
"""
import json
import os
from pathlib import Path
import sys

# Los reportes de ZAP solo se leen desde zap/ (donde los escribe el workflow):
# el argumento elige el nombre del archivo, no la carpeta (SonarCloud S8707).
ZAP_DIR = Path(__file__).resolve().parent.parent / "zap"

RISK = {"3": "HIGH", "2": "MEDIUM", "1": "LOW", "0": "INFO"}


def main(path: str) -> int:
    target = ZAP_DIR / os.path.basename(path)
    if not target.is_file():
        print(f"::error::No existe el reporte de ZAP {target.name} en zap/")
        return 1
    with open(target, encoding="utf-8") as fh:
        report = json.load(fh)

    counts = {name: 0 for name in RISK.values()}
    print(f"{'Riesgo':<8} {'Instancias':>10}  Alerta")
    for site in report.get("site", []):
        for alert in site.get("alerts", []):
            risk = RISK.get(str(alert.get("riskcode")), "INFO")
            counts[risk] += 1
            print(f"{risk:<8} {alert.get('count', '?'):>10}  "
                  f"{alert.get('name')} (pluginId {alert.get('pluginid')})")

    print("\nResumen:", ", ".join(f"{k}={v}" for k, v in counts.items()))
    if counts["HIGH"] > 0:
        print("::error::ZAP reportó alertas High. Criterio de la cátedra: 0 High.")
        return 1
    if counts["MEDIUM"] > 0:
        print("::warning::Hay alertas Medium: deben justificarse una por una en el informe de seguridad.")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
