JMkvpropedit

Changes the title, track names, languages and flags of many MKV files at
once, using mkvpropedit from MKVToolNix (https://mkvtoolnix.download/),
which must be installed separately.

The Windows download has Java 21 bundled, so you don't need to install
Java. Unzip it and run jmkvpropedit.exe, keeping it together with the app
and runtime folders. Then, on the Options tab, point the app at
mkvpropedit.exe (usually C:\Program Files\MKVToolNix\mkvpropedit.exe).

Changes are written straight into your files and can't be undone, so try
a new batch on a copy of one or two files first.

Building from source needs a JDK 21:
    gradlew build

The portable app or a Windows installer is made with jpackage (output in
build/jpackage):
    gradlew jpackage -PjpackageType=app-image  portable app, no WiX needed
    gradlew jpackage                           exe installer (needs WiX Toolset 3)
    gradlew jpackage -PjpackageType=msi        msi installer (needs WiX Toolset 3)

Developer notes are in BUILDING.md in the source repository.

Downloads and updates: https://github.com/cybergrimes/jmkvpropedit/releases
Original project by Bruno Barbieri: https://github.com/BrunoReX/jmkvpropedit
