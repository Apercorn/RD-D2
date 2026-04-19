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
PROJECT_ROOT = Path(".").resolve()
USER_HOME = Path.home()
GRADLE_CACHE = USER_HOME / ".gradle" / "caches"


def _find_android_sdk():
    """Locate Android SDK across Linux, WSL, and Windows environments."""
    import os

    # 1. Respect explicit env var
    env = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if env:
        return Path(env)
    # 2. Linux / native WSL default
    linux_default = USER_HOME / "Android" / "Sdk"
    if linux_default.exists():
        return linux_default
    # 3. WSL: Windows-side SDK via /mnt/c
    wsl_windows = (
        Path("/mnt/c/Users") / USER_HOME.name / "AppData" / "Local" / "Android" / "Sdk"
    )
    if wsl_windows.exists():
        return wsl_windows
    # 4. Windows fallback (native Windows Python)
    win_default = USER_HOME / "AppData" / "Local" / "Android" / "Sdk"
    if win_default.exists():
        return win_default
    # Return the Linux default path even if absent (gen_workspace will warn about missing android.jar)
    return linux_default


ANDROID_SDK = _find_android_sdk()
KOTLIN_VERSION = "2.2"
JVM_TARGET = "1.8"


def find_highest_android_api():
    platforms = ANDROID_SDK / "platforms"
    if not platforms.exists():
        return 30
    apis = []
    for d in platforms.iterdir():
        if d.is_dir() and d.name.startswith("android-"):
            try:
                apis.append(int(d.name.split("-")[1]))
            except ValueError:
                pass
    return max(apis) if apis else 30


ANDROID_API = find_highest_android_api()


# ── Parse classpath.txt ───────────────────────────────────────
def parse_classpath(classpath_file="classpath.txt"):
    """Parse Gradle classpath dump into {artifact_name: jar_path} and projects dict."""
    libs = {}
    projects = {}  # name -> relative path
    with open(classpath_file, "r") as f:
        for line in f:
            line = line.strip()
            if "|" not in line:
                continue
            parts = line.split("|")

            if parts[0] == "PROJECT_DIR" and len(parts) >= 3:
                name = parts[1]
                abs_path = parts[2]
                try:
                    rel_path = Path(abs_path).relative_to(PROJECT_ROOT).as_posix()
                    projects[name] = "" if rel_path == "." else rel_path
                except ValueError:
                    projects[name] = ""
                continue

            if parts[0] == "LIB" and len(parts) >= 4:
                project, config, path = parts[1], parts[2], parts[3]
                # Only care about compile/runtime configs
                if "compile" not in config.lower() and "runtime" not in config.lower():
                    continue
                path = path.replace("\\", "/")
                name = Path(path).stem
                if path.endswith(".aar"):
                    jar = find_aar_classes_jar(path)
                    if jar:
                        path = jar
                    else:
                        print(f"WARNING: Could not find classes.jar for {path}")
                        continue
                libs[name] = path

    return libs, projects


def find_aar_classes_jar(aar_path):
    """Find or extract classes.jar from an AAR in Gradle's transform/cache."""
    import zipfile, tempfile, shutil

    artifact_name = Path(aar_path).stem

    # 1. Check Gradle 8.x transforms cache first (populated after a compile run)
    for version_dir in GRADLE_CACHE.iterdir():
        transforms_dir = version_dir / "transforms"
        if not transforms_dir.exists():
            continue
        for hash_dir in transforms_dir.iterdir():
            candidate = (
                hash_dir / "transformed" / artifact_name / "jars" / "classes.jar"
            )
            if candidate.exists():
                return str(candidate).replace("\\", "/")

    # 2. Fallback: extract classes.jar from the .aar in-place into our own cache dir
    extract_root = GRADLE_CACHE / "aar-extracted"
    dest = extract_root / artifact_name / "classes.jar"
    if dest.exists():
        return str(dest).replace("\\", "/")
    aar = Path(aar_path)
    if not aar.exists():
        return None
    try:
        dest.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(aar, "r") as z:
            if "classes.jar" not in z.namelist():
                return None
            z.extract("classes.jar", dest.parent)
        return str(dest).replace("\\", "/")
    except Exception as e:
        print(f"WARNING: Failed to extract classes.jar from {aar}: {e}")
        return None


# ── Build workspace.json structure ────────────────────────────
def make_library(name, jar_path):
    return {
        "name": name,
        "type": "repository",
        "roots": [{"type": "CLASSES", "path": jar_path}],
        "excludedRoots": [],
    }


def make_android_platform_library():
    jar = ANDROID_SDK / "platforms" / f"android-{ANDROID_API}" / "android.jar"
    return {
        "name": "android-platform",
        "type": None,
        "roots": [{"type": "CLASSES", "path": str(jar).replace("\\", "/")}],
        "excludedRoots": [],
    }


def make_sdk():
    jar_path = f"{ANDROID_SDK}/platforms/android-{ANDROID_API}/android.jar".replace(
        "\\", "/"
    )
    return {
        "name": "Java SDK",  # Must match module dependency refs!
        "type": "JavaSDK",
        "version": str(ANDROID_API),
        "homePath": str(ANDROID_SDK).replace("\\", "/"),
        "roots": [{"url": f"jar://{jar_path}!/", "type": "CLASSES"}],
        "additionalData": "",
    }


def make_module_deps(lib_names, all_projects, extra_module_deps=None):
    """Build dependency list for a module."""
    deps = [
        {"type": "moduleSource"},
        {"type": "sdk", "name": "Java SDK", "kind": "JavaSDK"},
    ]
    if extra_module_deps:
        for mod_name in extra_module_deps:
            deps.append({"type": "module", "name": mod_name, "scope": "compile"})
    # Also add other projects as module dependencies
    for p in all_projects:
        deps.append({"type": "module", "name": f"{p}.main", "scope": "compile"})
    for name in lib_names:
        deps.append({"type": "library", "name": name, "scope": "compile"})
    # Always add android-platform
    deps.append({"type": "library", "name": "android-platform", "scope": "compile"})
    return deps


def make_kotlin_settings(module_name, project_path, is_main=True):
    # Only assign sourceRoots if the directories actually exist
    src = []
    if project_path:
        java_path = (
            PROJECT_ROOT
            / project_path
            / "src"
            / ("main" if is_main else "test")
            / "java"
        )
        kt_path = (
            PROJECT_ROOT
            / project_path
            / "src"
            / ("main" if is_main else "test")
            / "kotlin"
        )
        if java_path.exists():
            src.append(
                f"<WORKSPACE>/{project_path}/src/{'main' if is_main else 'test'}/java"
            )
        if kt_path.exists():
            src.append(
                f"<WORKSPACE>/{project_path}/src/{'main' if is_main else 'test'}/kotlin"
            )
    else:
        java_path = PROJECT_ROOT / "src" / ("main" if is_main else "test") / "java"
        kt_path = PROJECT_ROOT / "src" / ("main" if is_main else "test") / "kotlin"
        if java_path.exists():
            src.append(f"<WORKSPACE>/src/{'main' if is_main else 'test'}/java")
        if kt_path.exists():
            src.append(f"<WORKSPACE>/src/{'main' if is_main else 'test'}/kotlin")

    return {
        "name": "Kotlin",
        "sourceRoots": src,
        "configFileItems": [],
        "module": module_name,
        "useProjectSettings": True,
        "implementedModuleNames": [],
        "dependsOnModuleNames": [],
        "additionalVisibleModuleNames": (
            [module_name.replace(".test", ".main")] if ".test" in module_name else []
        ),
        "productionOutputPath": None,
        "testOutputPath": None,
        "sourceSetNames": ["main"] if is_main else [],
        "isTestModule": not is_main,
        "externalProjectId": module_name.split(".")[0],
        "isHmppEnabled": False,
        "pureKotlinSourceFolders": src,
        "kind": "default",  # MUST be lowercase!
        "compilerArguments": (
            f'J{{"jvmTarget":"{JVM_TARGET}","apiVersion":"{KOTLIN_VERSION}","languageVersion":"{KOTLIN_VERSION}"}}'
            if is_main
            else None
        ),
        "additionalArguments": None,
        "scriptTemplates": None,
        "scriptTemplatesClasspath": None,
        "outputDirectoryForJsLibraryFiles": None,
        "targetPlatform": f"JVM ({JVM_TARGET})",
        "externalSystemRunTasks": [],
        "version": 5,
        "flushNeeded": False,
    }


def generate():
    libs, projects = parse_classpath()
    lib_names = sorted(libs.keys())
    all_project_names = [p for p in projects.keys()]

    # Libraries
    libraries = [make_library(name, libs[name]) for name in lib_names]
    libraries.append(make_android_platform_library())

    # Modules
    modules = []
    kotlin_settings = []

    if not projects:
        # Fallback if no projects discovered
        projects = {PROJECT_ROOT.name: ""}

    for project_name, rel_path in projects.items():
        base_path = f"<WORKSPACE>/{rel_path}" if rel_path else "<WORKSPACE>"
        main_mod_name = f"{project_name}.main"
        test_mod_name = f"{project_name}.test"

        # Discover source roots for main & test
        main_src = []
        if (PROJECT_ROOT / rel_path / "src" / "main" / "java").exists():
            main_src.append(
                {"path": f"{base_path}/src/main/java", "type": "java-source"}
            )
        if (PROJECT_ROOT / rel_path / "src" / "main" / "kotlin").exists():
            main_src.append(
                {"path": f"{base_path}/src/main/kotlin", "type": "java-source"}
            )

        test_src = []
        if (PROJECT_ROOT / rel_path / "src" / "test" / "java").exists():
            test_src.append({"path": f"{base_path}/src/test/java", "type": "java-test"})
        if (PROJECT_ROOT / rel_path / "src" / "test" / "kotlin").exists():
            test_src.append(
                {"path": f"{base_path}/src/test/kotlin", "type": "java-test"}
            )

        other_projects = [p for p in all_project_names if p != project_name]

        modules.append(
            {
                "name": main_mod_name,
                "type": "JAVA_MODULE",
                "dependencies": make_module_deps(lib_names, other_projects),
                "contentRoots": [
                    {
                        "path": base_path,
                        **({"sourceRoots": main_src} if main_src else {}),
                        "excludedUrls": [f"{base_path}/build"],
                    }
                ],
            }
        )
        modules.append(
            {
                "name": test_mod_name,
                "type": "JAVA_MODULE",
                "dependencies": make_module_deps(
                    lib_names, other_projects, extra_module_deps=[main_mod_name]
                ),
                "contentRoots": [
                    {
                        "path": base_path,
                        **({"sourceRoots": test_src} if test_src else {}),
                    }
                ],
            }
        )

        kotlin_settings.append(
            make_kotlin_settings(main_mod_name, rel_path, is_main=True)
        )
        kotlin_settings.append(
            make_kotlin_settings(test_mod_name, rel_path, is_main=False)
        )

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
