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

1. Create a `classpath.txt` file by extracting the Gradle classpath using the bundled init script:
   Run `.\gradlew.bat -I .github/skills/kotlin-workspace-json/scripts/print-classpath.gradle printClasspath > classpath.txt 2>&1`
2. Run the bundled Python script to parse `classpath.txt` and generate `workspace.json` at the root of the workspace:
   Run `python .github/skills/kotlin-workspace-json/scripts/gen_workspace.py`
3. Clean up by deleting the temporary `classpath.txt` file.
4. Guide the user to clear the LSP cache. You can run these commands in the terminal for them:
   ```powershell
   Get-WmiObject Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'jetbrains\.kotlin' } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
   ```
   _Note: Finding and deleting the workspace cache folder in `%APPDATA%\Code\User\workspaceStorage` requires matching the specific workspace ID, so ask the user if they'd like help finding and clearing it, or they can do it manually._
5. The Kotlin LSP will auto-restart when a `.kt` file is opened.

## Resources

- [print-classpath.gradle](./scripts/print-classpath.gradle): Gradle init script to export the resolution tree.
- [gen_workspace.py](./scripts/gen_workspace.py): Python script to generate the JSON model.

#181818
#191a1b