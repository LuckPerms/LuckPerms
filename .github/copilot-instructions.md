**Überblick**
- **Projekttyp:** Multi-Modul Java/Gradle-Repository mit mehreren Plattform-Implementierungen (Bukkit, Bungee, Fabric, Forge, Sponge, Velocity, Nukkit, Standalone).
- **Ziel:** `api/` enthält die öffentliche API, plattformspezifische Implementierungen leben in Modulverzeichnissen wie `bukkit/`, `bungee/`, `fabric/`.

**Build & Tests**
- **Build (gesamt):** Nutze den Wrapper: 

```bash
./gradlew build
```
- **Modul-Targeted Build:** Beispiel für nur Bukkit: `./gradlew :bukkit:build`.
- **Tests:** Modultests mit `./gradlew :<modul>:test` (z. B. `:common:test`).
- **Docker / Run:** Es gibt ein Beispiel-Dockerfile unter [standalone/docker/Dockerfile](standalone/docker/Dockerfile).

**Konventionen & Architektur**
- **API vs Implementierung:** Änderungen an öffentlichen Schnittstellen in [api/src/main/java](api/src/main/java) sind sensitiver; vermeide nicht abwärtskompatible Änderungen ohne Koordination.
- **Wiederverwendbare Loader-Logik:** Viele Plattform-Module haben interne `loader/`-Unterprojekte (z. B. `bukkit/loader`, `fabric/loader`) — dort liegen gemeinsame Adaptions-/Bootstrap-Utilities.
- **Versionsmanagement:** Versions werden zentral in `gradle/libs.versions.toml` und `gradle.properties` gepflegt.
- **Lizenz/Headers:** Projekt nutzt `HEADER.txt` und `LICENSE.txt` — respektiere die Kopfzeilen beim Patchen von Quellen.

**Wichtige Pfade (Beispiele)**
- **API:** [api/src/main/java](api/src/main/java)
- **Gemeinsame Logik / Tests:** [common/src/main/java](common/src/main/java) und [common/src/test](common/src/test)
- **Bukkit-Impl:** [bukkit/src/main/java](bukkit/src/main/java)
- **Forge / Fabric spezifisch:** [forge/](forge/) und [fabric/](fabric/) (beide enthalten spezielle Build-/Loom-Anpassungen)
- **Root-Buildscript:** [build.gradle](build.gradle) und modul-spezifische `build.gradle` Dateien in den Modulen.

**Code-Patterns / Implementierungs-Hinweise**
- **Keep API thin:** Implementierungen delegieren meist an gemeinsame Services in `common/` oder `loader/`-Unterprojekten.
- **Platform adaptors:** When adding features, implement core logic in `common/` (or `api` if public), and add platform bindings under the platform module.
- **Avoid shading surprises:** Some modules use relocation/shading during their build—check the module's `build.gradle` before changing package names.

**PR / Review checklist for automated agents**
- **Build locally:** Ensure `./gradlew build` passes (or at least `:<affected-module>:build`).
- **Tests:** Run relevant module tests (e.g. `:common:test`).
- **Respect public API:** If you touch `api/`, add a migration note in PR description.
- **Small, focused changes:** Keep PRs module-scoped when possible (easier to review across many platform modules).

**When to ask for human help**
- **Breaking API changes:** Stop and request guidance.
- **Platform-specific runtime issues:** e.g., classloading differences on Bukkit vs Bungee.
- **Release/version decisions:** Modifying `libs.versions.toml` or `gradle.properties`.

**Weiteres / Referenzen**
- **Contributing Guide:** [CONTRIBUTING.md](CONTRIBUTING.md)
- **License & headers:** [LICENSE.txt](LICENSE.txt), [HEADER.txt](HEADER.txt)

Wenn etwas unklar ist oder du ein größeres API-Änderungsset planst, frage bitte: Welche Module müssen verändert werden, und welche Kompatibilitätsgarantien sollen erhalten bleiben?
