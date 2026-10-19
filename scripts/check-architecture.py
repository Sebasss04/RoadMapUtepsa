"""Dependency and text inventory checks, without contacting Android or Firebase."""
from pathlib import Path
import json,re,xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'app/src/main/java/com/example/roadmap_utepsa'
OUT=ROOT/'app/build/reports/reorganization'
OUT.mkdir(parents=True,exist_ok=True)
failures=[]; inventory=[]; kotlin_literals=[]
for p in BASE.rglob('*.kt'):
    relative=p.relative_to(BASE).as_posix();s=p.read_text(encoding='utf-8')
    imports=re.findall(r'^import (\S+)',s,re.M)
    if relative.startswith(('presentation/','platform/')):
        for match in re.finditer(r'"(?:\\.|[^"\\])*"',s):
            kotlin_literals.append({'file':relative,'line':s.count('\n',0,match.start())+1,
                'literal':match[0], 'review':'keys, dynamic formatting, annotations, or internal diagnostics; visible static copy uses resources'})
    if relative.startswith('domain/'):
        forbidden=r'^(android\.|androidx\.|com\.google\.|com\.example\.roadmap_utepsa\.(?:data|presentation|platform|di|R)(?:\.|$))'
    elif relative.startswith('data/'):
        forbidden=r'^com\.example\.roadmap_utepsa\.presentation\.'
    elif relative.startswith('presentation/'):
        forbidden=r'^(com\.google\.firebase\.|androidx\.credentials\.|com\.example\.roadmap_utepsa\.data\.)'
    else:forbidden=r'(?!)'
    for imp in imports:
        if re.search(forbidden,imp):failures.append(f'{relative}: forbidden dependency {imp}')
        if re.search(r'com\.example\.roadmap_utepsa\.(?:services|security|ui\.theme|presentation\.viewmodel|domain\.model)\.',imp):
            failures.append(f'{relative}: retired package {imp}')
    if relative.startswith('presentation/') and re.search(r'(?:exception|error|throwable)\.message',s,re.I):
        failures.append(f'{relative}: raw exception message in presentation')
    expected='com.example.roadmap_utepsa'+('.'+str(p.parent.relative_to(BASE)).replace('\\','.').replace('/','.') if p.parent!=BASE else '')
    actual=re.search(r'^package (\S+)',s,re.M)[1]
    if actual!=expected:failures.append(f'{relative}: package mismatch {actual}')

for p in BASE.rglob('*'):
    if p.name=='.gitkeep':failures.append(f'Obsolete placeholder: {p.relative_to(BASE)}')
    if p.is_dir() and not any(p.iterdir()):failures.append(f'Empty package: {p.relative_to(BASE)}')

seen=set()
for p in (ROOT/'app/src/main/res/values').glob('*.xml'):
    for element in ET.parse(p).getroot():
        if element.tag not in ['string','plurals']:continue
        key=(element.tag,element.get('name'))
        if key in seen:failures.append(f'Duplicate resource: {key}')
        seen.add(key)
        text=' '.join(''.join(element.itertext()).split())
        inventory.append({'file':p.name,'type':element.tag,'name':element.get('name'),'text':text})
        if re.search(r'Firebase|Firestore|\bSDK\b|\bUID\b|\btoken\b|credencial|transacci[oó]n',text,re.I):
            failures.append(f'Technical UI wording: {p.name}:{element.get("name")}')

report={'sourceFiles':len(list(BASE.rglob('*.kt'))),'resources':len(inventory),'failures':failures}
(OUT/'architecture.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'text-inventory.json').write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'kotlin-literals.json').write_text(json.dumps(kotlin_literals,ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
raise SystemExit(bool(failures))
