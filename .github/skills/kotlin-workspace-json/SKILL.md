---
name: kotlin-workspace-json
description: 'Generate or fix workspace.json for JetBrains Kotlin LSP on Gradle and Android projects. Use when Kotlin files show "Package directive does not match", unresolved references to SDK classes, or LSP Gradle import fails.'
---

# Generate JetBrains Kotlin LSP Workspace

## When to Use

- JetBrains Kotlin LSP is showing "Package directive does not match" on all `.kt` files.
- LSP silently fails to import Kotlin/Android projects via Gradle.
- `workspace.json` needs to be regenerated due to updated dependencies.
- You see unresolved reference errors in Kotlin files for SDK or external library classes.

## Procedure

1. Create a `classpath.txt` file by extracting the Gradle classpath using the bundled init script.

   **Linux / Mac:**

   ```sh
   ./gradlew -I .github/skills/kotlin-workspace-json/scripts/print-classpath.gradle printClasspath > classpath.txt 2>&1
   ```

   **Windows:**

   ```powershell
   .\gradlew.bat -I .github/skills/kotlin-workspace-json/scripts/print-classpath.gradle printClasspath > classpath.txt 2>&1
   ```

2. Run the bundled Python script to parse `classpath.txt` and generate `workspace.json` at the root of the workspace:

   ```sh
   python .github/skills/kotlin-workspace-json/scripts/gen_workspace.py
   ```

3. Clean up by deleting the temporary `classpath.txt` file.

4. Restart the Kotlin LSP to pick up the new workspace.json.

   **Linux / Mac:**

   ```sh
   pkill -f 'kotlin-language-server' || true
   ```

   **Windows (PowerShell):**

   ```powershell
   Get-WmiObject Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'jetbrains\.kotlin' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
   ```

   Or use **Ctrl+Shift+P → "Kotlin: Restart Language Server"** in VS Code.

5. The Kotlin LSP will auto-restart when a `.kt` file is opened.

## Known Limitation: Android Application Modules

AGP (`com.android.application`) marks `debugCompileClasspath` as not resolv­able when
called from a Gradle init script context. This means libraries declared only in an
application module's dependencies (e.g. via a private Maven repo) may not appear in
`classpath.txt`.

**`gen_workspace.py` automatically compensates** by scanning the Gradle transforms cache
(`~/.gradle/caches/*/transforms`) for `*-api.jar` files after a successful build. These
jars are produced by AGP from every AAR at compile time and contain the full public API
surface — enough for LSP resolution. As long as the project has been built at least once,
all AAR dependencies will be found regardless of whether they appear in `classpath.txt`.

If you add a new dependency and the LSP still cannot resolve it, run `./gradlew assembleDebug`
once to populate the transforms cache, then regenerate `workspace.json`.

## Resources

- [print-classpath.gradle](./scripts/print-classpath.gradle): Gradle init script to export the resolution tree.
- [gen_workspace.py](./scripts/gen_workspace.py): Python script to generate the JSON model.
