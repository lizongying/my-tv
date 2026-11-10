# JLayer 1.0.1 decoder subset

Licensed under the LGPL; distributed using the LGPL-2.1 license option allowed by the original source (version2-or-later headers). The license text is in this directory and the APK assets.

Official binary: https://repo.maven.apache.org/maven2/javazoom/jlayer/1.0.1/jlayer-1.0.1.jar
Official source: https://repo.maven.apache.org/maven2/javazoom/jlayer/1.0.1/jlayer-1.0.1-sources.jar
Original SHA256: 850508c837454a1b06017c32a36876fae516de1e89a829f725fee1e6dcc52000
Decoder-subset SHA256: a14e12e261aab053e7978f1dafa114d924b61574420cec4e3202db4f9416c17d

The APK JAR contains only javazoom/jl/decoder entries, copied byte-for-byte from the original. Unused desktop players/converters and JavaSound references are omitted. Reproduce the filter with scripts/build_jlayer_subset.py. This folder includes both the full original binary and the complete unmodified corresponding source archive. The app and build source are available so users can replace the library and rebuild. There are no native libraries.
