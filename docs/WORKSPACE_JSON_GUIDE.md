# JetBrains Kotlin LSP — `workspace.json` Setup Guide

> **Purpose:** This document explains how to create and maintain the `workspace.json` file that
> powers the JetBrains Kotlin LSP extension in VS Code for this FTC Android/Kotlin project.

## Why This Exists

The JetBrains Kotlin LSP for VS Code (`jetbrains.kotlin`) supports two workspace import modes:

1. **Gradle import** — automatic, but **silently fails** on FTC Android projects (AGP + complex multi-module structure). When it fails, every `.kt` file shows "Package directive does not match."
2. **JSON import** — manual `workspace.json` at the project root. If valid, the LSP uses it *instead* of Gradle import.

We use option 2.

---

## Quick Start (TL;DR)

If `workspace.json` is lost, the fastest path to recreate it:

1. Run the classpath extraction Gradle task (Step 1 below)
2. Run the Python generation script (Step 2 below)
3. Clear the LSP cache and restart (Step 3 below)

---

## Architecture Overview

```
workspace.json
├── modules[]          — 6 modules (TeamCode.main/test, FtcRobotController.main/test, RD-D2.main/test)
│   ├── name           — "TeamCode.main"
│   ├── type           — "JAVA_MODULE"
│   ├── dependencies[] — moduleSource, sdk ref, library refs, module refs
│   └── contentRoots[] — source directories, excluded directories
├── libraries[]        — ~72 library entries (FTC SDK JARs, AndroidX, Kotlin stdlib, etc.)
│   ├── name           — matches dependency refs in modules
│   ├── roots[]        — [{path: "...", type: "CLASSES"}]
│   └── excludedRoots  — []
├── sdks[]             — 1 entry: android.jar from Android SDK
│   ├── name           — "Java SDK" (must match module dependency refs)
│   ├── type           — "JavaSDK"
│   ├── version        — "30"
│   ├── homePath       — Android SDK root
│   └── roots[]        — [{url: "jar://...android.jar!/", type: "CLASSES"}]
└── kotlinSettings[]   — 6 entries (one per module)
    ├── module          — matches module name
    ├── kind            — "default" (lowercase!)
    ├── compilerArguments — "J{\"jvmTarget\":\"1.8\",...}" (special format!)
    └── targetPlatform  — "JVM (1.8)"
```

---

## Step 1: Extract the Classpath from Gradle

Create a temporary Gradle init script to dump all resolved dependencies:

```groovy
// File: print-classpath.gradle  (temporary, delete after)
allprojects {
    task printClasspath {
        doLast {
            configurations.findAll { it.canBeResolved }.each { config ->
                try {
                    config.resolvedConfiguration.resolvedArtifacts.each { artifact ->
                        println "${project.name}|${config.name}|${artifact.file.absolutePath}"
                    }
                } catch (Exception e) {
                    // skip unresolvable configs
                }
            }
        }
    }
}
```

Run it:
```powershell
.\gradlew.bat -I print-classpath.gradle printClasspath > classpath.txt 2>&1
```

This gives you every JAR/AAR path used by the project.

---

## Step 2: Generate `workspace.json`

Use a Python script to transform the classpath dump into workspace.json.

### Key Rules Discovered by Decompiling the LSP

These are **non-obvious requirements** that took extensive reverse-engineering to discover:

#### File Format
- **No UTF-8 BOM** — PowerShell's `Set-Content -Encoding UTF8` adds a BOM. Use Python or `[System.IO.File]::WriteAllText()` instead.
- Use `\t` (tab) indentation for readability.

#### Module Dependencies
- **Allowed keys only:** `type`, `name`, `scope`, `kind`
- **NO `level` key** — the parser uses `ignoreUnknownKeys = false` and will reject it
- `scope` values: `"compile"`, `"test"` (lowercase)
- `type` values: `"moduleSource"`, `"sdk"`, `"library"`, `"module"`

#### Library Roots
- **`path`** field (NOT `url`) — plain filesystem path, forward slashes
- **`type`** must be `"CLASSES"` (not `"classPath"`, not `"compiled"`)
  - This comes from `LibraryRootTypeId.COMPILED` whose internal name is `"CLASSES"`
  - For source roots, use `"SOURCES"`
- **AAR files don't work** — must point to the extracted `classes.jar` inside Gradle's transform cache:
  ```
  C:/Users/<user>/.gradle/caches/8.13/transforms/<hash>/transformed/<ArtifactName>/jars/classes.jar
  ```
  The `<hash>` can be found by searching the transforms directory for the artifact name.

#### SDK Section
- `name` must be `"Java SDK"` — this must match the `"name"` in module SDK dependencies
- `type` must be `"JavaSDK"`
- `roots[].url` uses `jar://` protocol for JAR files: `"jar://C:/.../android.jar!/"`
- `roots[].type` should be `"CLASSES"` (though the code hardcodes `"classPath"` internally, `"CLASSES"` in the JSON works)
- **Use `android.jar`, NOT the JDK** — this is an Android project. The JDK's `jrt://` module URLs won't provide `java.lang.Object` etc. in the Android context
- `homePath` points to the Android SDK root directory

#### Kotlin Settings
- `kind` must be **lowercase**: `"default"` (not `"DEFAULT"`)
  - kotlinx.serialization 1.9.0 changed to case-sensitive enum decoding
- `compilerArguments` uses a special prefix format:
  ```
  J{"jvmTarget":"1.8","apiVersion":"2.2","languageVersion":"2.2"}
  ```
  The first character is a **type prefix**: `J` = JVM, `S` = JS, `M` = Multiplatform, `N` = Native.
  The rest is a Gson-compatible JSON object.
- `targetPlatform`: `"JVM (1.8)"`

#### Path Placeholders
- `<WORKSPACE>/` at the start of a path resolves to the workspace root (`D:/FTC/RD-D2`)
- `<HOME>/` resolves to the user home directory
- All other paths are absolute filesystem paths with forward slashes

---

## Step 3: Clear Cache & Restart LSP

After editing `workspace.json`, you **must** clear the LSP cache:

```powershell
# 1. Kill the LSP process
Get-WmiObject Win32_Process -Filter "Name='java.exe'" |
  Where-Object { $_.CommandLine -match 'jetbrains\.kotlin' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

# 2. Wait for it to die
Start-Sleep 2

# 3. Delete the cache
Remove-Item -Recurse -Force "c:\Users\jmorr\AppData\Roaming\Code\User\workspaceStorage\5faf7293ede448c6968f8fcd98f3038a\JetBrains.kotlin\system"

# 4. The LSP auto-restarts when you open a .kt file — wait ~15-30s for indexing
```

The workspace storage ID (`5faf7293...`) is specific to this VS Code workspace folder. If you open the project from a different location, the ID will be different — check:
```
c:\Users\jmorr\AppData\Roaming\Code\User\workspaceStorage\
```
for the folder that contains a `JetBrains.kotlin` subdirectory.

---

## Complete Generation Script

Below is a self-contained Python script that generates `workspace.json` from a Gradle classpath dump. Save it as `gen_workspace.py`, run it, then delete it.

```python
#!/usr/bin/env python3
"""
Generate workspace.json for JetBrains Kotlin LSP.
Run from the project root: python gen_workspace.py

Prerequisites:
  1. Run: .\gradlew.bat -I print-classpath.gradle printClasspath > classpath.txt 2>&1
  2. Ensure Android SDK is installed (ANDROID_HOME or default location)
"""

import json, os, glob, re
from pathlib import Path

# ── Configuration ──────────────────────────────────────────────
PROJECT_ROOT = Path(".")
USER_HOME = Path.home()
GRADLE_CACHE = USER_HOME / ".gradle" / "caches"
ANDROID_SDK = USER_HOME / "AppData" / "Local" / "Android" / "Sdk"
ANDROID_API = 30  # FTC target API
KOTLIN_VERSION = "2.2"
JVM_TARGET = "1.8"

# ── Parse classpath.txt ───────────────────────────────────────
def parse_classpath(classpath_file="classpath.txt"):
    """Parse Gradle classpath dump into {artifact_name: jar_path} dict."""
    libs = {}
    with open(classpath_file, "r") as f:
        for line in f:
            line = line.strip()
            if "|" not in line:
                continue
            parts = line.split("|")
            if len(parts) < 3:
                continue
            project, config, path = parts[0], parts[1], parts[2]
            # Only care about compile/runtime configs
            if "compile" not in config.lower() and "runtime" not in config.lower():
                continue
            path = path.replace("\\", "/")
            # Derive a library name from the filename
            name = Path(path).stem
            # Handle AAR → find extracted classes.jar
            if path.endswith(".aar"):
                jar = find_aar_classes_jar(path)
                if jar:
                    path = jar
                else:
                    print(f"WARNING: Could not find classes.jar for {path}")
                    continue
            libs[name] = path
    return libs


def find_aar_classes_jar(aar_path):
    """Find the extracted classes.jar in Gradle's transform cache for an AAR."""
    artifact_name = Path(aar_path).stem
    # Gradle 8.x stores transforms in caches/<version>/transforms/
    for version_dir in GRADLE_CACHE.iterdir():
        transforms_dir = version_dir / "transforms"
        if not transforms_dir.exists():
            continue
        for hash_dir in transforms_dir.iterdir():
            candidate = hash_dir / "transformed" / artifact_name / "jars" / "classes.jar"
            if candidate.exists():
                return str(candidate).replace("\\", "/")
    return None


# ── Build workspace.json structure ────────────────────────────
def make_library(name, jar_path):
    return {
        "name": name,
        "type": "repository",
        "roots": [{"type": "CLASSES", "path": jar_path}],
        "excludedRoots": []
    }


def make_android_platform_library():
    jar = ANDROID_SDK / "platforms" / f"android-{ANDROID_API}" / "android.jar"
    return {
        "name": "android-platform",
        "type": None,
        "roots": [{"type": "CLASSES", "path": str(jar).replace("\\", "/")}],
        "excludedRoots": []
    }


def make_sdk():
    jar_path = f"{ANDROID_SDK}/platforms/android-{ANDROID_API}/android.jar".replace("\\", "/")
    return {
        "name": "Java SDK",          # Must match module dependency refs!
        "type": "JavaSDK",
        "version": str(ANDROID_API),
        "homePath": str(ANDROID_SDK).replace("\\", "/"),
        "roots": [{"url": f"jar://{jar_path}!/", "type": "CLASSES"}],
        "additionalData": ""
    }


def make_module_deps(lib_names, extra_module_deps=None):
    """Build dependency list for a module."""
    deps = [
        {"type": "moduleSource"},
        {"type": "sdk", "name": "Java SDK", "kind": "JavaSDK"},
    ]
    if extra_module_deps:
        for mod_name in extra_module_deps:
            deps.append({"type": "module", "name": mod_name, "scope": "compile"})
    for name in lib_names:
        deps.append({"type": "library", "name": name, "scope": "compile"})
    # Always add android-platform
    deps.append({"type": "library", "name": "android-platform", "scope": "compile"})
    return deps


def make_kotlin_settings(module_name, is_main=True):
    src = ["<WORKSPACE>/TeamCode/src/main/java"] if is_main and "TeamCode" in module_name else []
    return {
        "name": "Kotlin",
        "sourceRoots": src,
        "configFileItems": [],
        "module": module_name,
        "useProjectSettings": True,
        "implementedModuleNames": [],
        "dependsOnModuleNames": [],
        "additionalVisibleModuleNames": [module_name.replace(".test", ".main")] if ".test" in module_name else [],
        "productionOutputPath": None,
        "testOutputPath": None,
        "sourceSetNames": ["main"] if is_main else [],
        "isTestModule": not is_main,
        "externalProjectId": module_name.split(".")[0],
        "isHmppEnabled": False,
        "pureKotlinSourceFolders": src,
        "kind": "default",          # MUST be lowercase!
        "compilerArguments": f'J{{"jvmTarget":"{JVM_TARGET}","apiVersion":"{KOTLIN_VERSION}","languageVersion":"{KOTLIN_VERSION}"}}' if is_main and "TeamCode" in module_name else None,
        "additionalArguments": None,
        "scriptTemplates": None,
        "scriptTemplatesClasspath": None,
        "outputDirectoryForJsLibraryFiles": None,
        "targetPlatform": f"JVM ({JVM_TARGET})",
        "externalSystemRunTasks": [],
        "version": 5,
        "flushNeeded": False
    }


def generate():
    libs = parse_classpath()
    lib_names = sorted(libs.keys())

    # Libraries
    libraries = [make_library(name, libs[name]) for name in lib_names]
    libraries.append(make_android_platform_library())

    # Modules
    modules = [
        {
            "name": "TeamCode.main",
            "type": "JAVA_MODULE",
            "dependencies": make_module_deps(lib_names),
            "contentRoots": [{
                "path": "<WORKSPACE>/TeamCode",
                "sourceRoots": [{"path": "<WORKSPACE>/TeamCode/src/main/java", "type": "java-source"}],
                "excludedUrls": ["<WORKSPACE>/TeamCode/build"]
            }]
        },
        {
            "name": "TeamCode.test",
            "type": "JAVA_MODULE",
            "dependencies": make_module_deps(lib_names, extra_module_deps=["TeamCode.main"]),
            "contentRoots": [{"path": "<WORKSPACE>/TeamCode"}]
        },
        {
            "name": "FtcRobotController.main",
            "type": "JAVA_MODULE",
            "dependencies": make_module_deps(lib_names),
            "contentRoots": [{
                "path": "<WORKSPACE>/FtcRobotController",
                "sourceRoots": [{"path": "<WORKSPACE>/FtcRobotController/src/main/java", "type": "java-source"}]
            }]
        },
        {
            "name": "FtcRobotController.test",
            "type": "JAVA_MODULE",
            "dependencies": make_module_deps(lib_names, extra_module_deps=["FtcRobotController.main"]),
            "contentRoots": [{"path": "<WORKSPACE>/FtcRobotController"}]
        },
        {
            "name": "RD-D2.main",
            "type": "JAVA_MODULE",
            "dependencies": make_module_deps(lib_names),
            "contentRoots": [{"path": "<WORKSPACE>"}]
        },
        {
            "name": "RD-D2.test",
            "type": "JAVA_MODULE",
            "dependencies": make_module_deps(lib_names, extra_module_deps=["RD-D2.main"]),
            "contentRoots": [{"path": "<WORKSPACE>"}]
        },
    ]

    # Kotlin Settings
    kotlin_settings = [
        make_kotlin_settings("TeamCode.main", is_main=True),
        make_kotlin_settings("TeamCode.test", is_main=False),
        make_kotlin_settings("FtcRobotController.main", is_main=True),
        make_kotlin_settings("FtcRobotController.test", is_main=False),
        make_kotlin_settings("RD-D2.main", is_main=True),
        make_kotlin_settings("RD-D2.test", is_main=False),
    ]

    workspace = {
        "modules": modules,
        "libraries": libraries,
        "sdks": [make_sdk()],
        "kotlinSettings": kotlin_settings,
    }

    with open("workspace.json", "w", encoding="utf-8", newline="\n") as f:
        json.dump(workspace, f, indent="\t", ensure_ascii=False)

    print(f"Generated workspace.json:")
    print(f"  Modules:        {len(modules)}")
    print(f"  Libraries:      {len(libraries)}")
    print(f"  SDKs:           1")
    print(f"  KotlinSettings: {len(kotlin_settings)}")


if __name__ == "__main__":
    generate()
```

---

## Troubleshooting

### "Error parsing workspace.json"
Check the LSP Output panel (Output → "Kotlin (JetBrains)") for the exact error. Common causes:
- **Unknown key** — the parser is strict. Only use documented keys (see rules above).
- **UTF-8 BOM** — re-save with Python: `open('workspace.json','w',encoding='utf-8',newline='\n')`
- **Wrong enum case** — `kind` must be `"default"` not `"DEFAULT"`

### "Cannot access 'java.lang.Object'"
The SDK is misconfigured. Ensure:
- The `sdks` section points to `android.jar` (not JDK `jrt://` modules)
- The `android-platform` library is in `libraries[]` AND referenced in module `dependencies[]`
- The SDK `name` matches the `name` in module SDK dependency references

### "Unresolved reference 'qualcomm'" (or any FTC class)
A library JAR is missing or points to a non-existent file. Check:
- All `.aar` paths have been converted to extracted `classes.jar` paths
- The Gradle transform cache hasn't been cleaned (`.\gradlew.bat clean` doesn't clear it, but `rm -rf ~/.gradle/caches/*/transforms` does)

### LSP falls back to Gradle import
Visible in the output log: "Trying to import using GradleWorkspaceImporter". This means workspace.json failed to parse. Fix the JSON errors first.

### Package directive errors persist after fix
Clear the LSP cache (Step 3). The LSP caches the workspace model aggressively.

---

## Environment Reference

| Component   | Value                                                             |
| ----------- | ----------------------------------------------------------------- |
| FTC SDK     | 11.1.0                                                            |
| Kotlin      | 2.2.20                                                            |
| Gradle      | 8.13                                                              |
| AGP         | 8.13.2                                                            |
| Android API | 30 (FTC target)                                                   |
| JDK         | Zulu 21 at `C:\Program Files\Zulu\zulu-21`                        |
| Android SDK | `C:\Users\jmorr\AppData\Local\Android\Sdk`                        |
| Kotlin LSP  | `jetbrains.kotlin` v261.13587.0                                   |
| VS Code     | 1.106.0                                                           |
| OS          | Windows 11                                                        |
| LSP Storage | `%APPDATA%\Code\User\workspaceStorage\<id>\JetBrains.kotlin`      |
| LSP JARs    | `%USERPROFILE%\.vscode\extensions\jetbrains.kotlin-*\server\lib\` |

---

## Files

| File                           | Purpose                     | Committed?         |
| ------------------------------ | --------------------------- | ------------------ |
| `workspace.json`               | LSP project model           | ✅ Yes              |
| `docs/WORKSPACE_JSON_GUIDE.md` | This guide                  | ✅ Yes              |
| `print-classpath.gradle`       | Temp: classpath extraction  | ❌ Delete after use |
| `classpath.txt`                | Temp: classpath dump output | ❌ Delete after use |
| `gen_workspace.py`             | Temp: generation script     | ❌ Delete after use |

---

## Schema Discoveries (for future debugging)

These were found by decompiling the LSP server JARs with `javap`:

| Class                                 | JAR                                   | Key Finding                                                                    |
| ------------------------------------- | ------------------------------------- | ------------------------------------------------------------------------------ |
| `SdkRootData`                         | `language-server.project-import.jar`  | `url: String`, `type: String`                                                  |
| `LibraryRootData`                     | `language-server.project-import.jar`  | `path: String`, `type: String`, `inclusionOptions: InclusionOptions?`          |
| `LibraryRootTypeId`                   | `intellij.platform.workspace.jps.jar` | `COMPILED.name = "CLASSES"`, `SOURCES.name = "SOURCES"`                        |
| `SdkRootTypeId`                       | `intellij.platform.workspace.jps.jar` | Constructor takes a String name                                                |
| `KotlinSettingsData.KotlinModuleKind` | `language-server.project-import.jar`  | Enum uses **lowercase** values                                                 |
| `ConversionKt`                        | `language-server.project-import.jar`  | `compilerArguments` first char = type prefix (J/S/M/N), rest = Gson JSON       |
| `UriKt.toIntellijUri`                 | `language-server.project-import.jar`  | SDK root URLs pass through `asIntelliJUriString()` → `UriConverter` round-trip |
| `ConversionKt.toAbsolutePath`         | `language-server.project-import.jar`  | `<WORKSPACE>/` and `<HOME>/` are special path prefixes                         |
| `DependencyData.*`                    | `language-server.project-import.jar`  | Strict deserialization — no unknown keys allowed                               |
