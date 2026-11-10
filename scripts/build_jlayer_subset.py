from pathlib import Path
import zipfile,hashlib
root=Path(__file__).resolve().parents[1]
original=root/'notices/jlayer-1.0.1-original.jar'
assert hashlib.sha256(original.read_bytes()).hexdigest()=='850508c837454a1b06017c32a36876fae516de1e89a829f725fee1e6dcc52000'
with zipfile.ZipFile(original) as src,zipfile.ZipFile(root/'app/libs/jlayer-1.0.1.jar','w') as dst:
 for info in src.infolist():
  if info.filename.startswith('javazoom/jl/decoder/'):
   dst.writestr(info,src.read(info.filename))
