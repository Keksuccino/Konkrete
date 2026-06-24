#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import os
import re
import shlex
import shutil
import subprocess
import sys
import tempfile
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Sequence, Tuple
from urllib import error, request


SCRIPT_VERSION = "1.0.0"
CONFIG_FILE_NAME = "mod_upload_config.json"
USER_AGENT = f"minecraft-mod-upload-script/{SCRIPT_VERSION} (local project upload tool)"

MODRINTH_API_BASE = "https://api.modrinth.com/v2"
CURSEFORGE_MINECRAFT_API_BASE = "https://minecraft.curseforge.com/api"

KEYCHAIN_SERVICES = {
    "modrinth": "modrinth.token",
    "curseforge": "curseforge.token",
}

REQUIRED_GRADLE_PROPERTIES = ("mod_id", "mod_version", "minecraft_version")
SUPPORTED_SIDES = ("client", "server")
IGNORED_JAR_SUFFIXES = ("-source.jar", "-sources.jar", "-unshaded.jar")
VERSIONED_JAR_PATTERN = re.compile(r"-\d+(?:\.\d+)+\.jar$")


@dataclass(frozen=True)
class LoaderInfo:
    module: str
    fancy_name: str
    modrinth_name: str
    curseforge_name: str


LOADER_INFOS: Dict[str, LoaderInfo] = {
    "fabric": LoaderInfo("fabric", "Fabric", "fabric", "Fabric"),
    "forge": LoaderInfo("forge", "Forge", "forge", "Forge"),
    "neoforge": LoaderInfo("neoforge", "NeoForge", "neoforge", "NeoForge"),
    "quilt": LoaderInfo("quilt", "Quilt", "quilt", "Quilt"),
}


@dataclass(frozen=True)
class SelectedJar:
    source_path: Path
    reason: str


@dataclass(frozen=True)
class StagedArtifact:
    loader: str
    source_path: Path
    staged_path: Path
    file_name: str
    display_name: str
    modrinth_version_number: str
    dependencies: Tuple[str, ...]


@dataclass(frozen=True)
class CurseForgeTags:
    minecraft_version_id: int
    minecraft_version_name: str
    loader_ids_by_module: Dict[str, int]
    environment_ids_by_side: Dict[str, int]


class UploadError(Exception):
    pass


class ApiError(UploadError):
    pass


def eprint(message: str = "") -> None:
    print(message, file=sys.stderr)


def section(title: str) -> None:
    print(f"\n== {title} ==", flush=True)


def ordered_sides(sides: Iterable[str]) -> List[str]:
    side_set = set(sides)
    return [side for side in SUPPORTED_SIDES if side in side_set]


def parse_side_choice(value: str) -> List[str]:
    normalized = value.strip().lower()
    if normalized == "both":
        return list(SUPPORTED_SIDES)
    if normalized == "none":
        return []
    if normalized in SUPPORTED_SIDES:
        return [normalized]
    raise ValueError(value)


def prompt_line(prompt: str, *, allow_empty: bool = False) -> str:
    if not sys.stdin.isatty():
        raise UploadError(
            "Missing configuration requires interactive input, but stdin is not a terminal."
        )

    while True:
        value = input(prompt).strip()
        if value or allow_empty:
            return value
        print("Please enter a value.")


def prompt_choice(prompt: str, choices: Sequence[str]) -> str:
    choices_text = "/".join(choices)
    while True:
        value = prompt_line(f"{prompt} [{choices_text}]: ").strip().lower()
        if value in choices:
            return value
        print(f"Please choose one of: {choices_text}")


def prompt_http_url(prompt: str) -> str:
    while True:
        value = prompt_line(prompt)
        if value.startswith("http://") or value.startswith("https://"):
            return value
        print("Please enter a full http:// or https:// URL.")


def prompt_numeric_id(prompt: str) -> str:
    while True:
        value = prompt_line(prompt)
        if value.isdigit():
            return value
        print("Please enter the numeric CurseForge project ID.")


def parse_gradle_properties(path: Path) -> Dict[str, str]:
    if not path.is_file():
        raise UploadError(f"Missing required file: {path}")

    properties: Dict[str, str] = {}
    with path.open("r", encoding="utf-8-sig") as handle:
        for line_number, raw_line in enumerate(handle, start=1):
            line = raw_line.strip()
            if not line or line.startswith("#") or line.startswith("!"):
                continue
            if "=" not in line:
                continue
            key, value = line.split("=", 1)
            key = key.strip()
            value = value.strip()
            if not key:
                raise UploadError(f"Invalid empty key in {path} at line {line_number}")
            properties[key] = value
    return properties


def require_gradle_properties(properties: Dict[str, str]) -> None:
    missing = [key for key in REQUIRED_GRADLE_PROPERTIES if not properties.get(key)]
    if missing:
        raise UploadError(
            "Missing required gradle.properties values: " + ", ".join(missing)
        )


def parse_included_modules(project_root: Path) -> List[str]:
    settings_paths = (
        project_root / "settings.gradle",
        project_root / "settings.gradle.kts",
    )
    settings_path = next((path for path in settings_paths if path.is_file()), None)
    if settings_path is None:
        return []

    text = settings_path.read_text(encoding="utf-8-sig")
    modules: List[str] = []
    seen = set()
    for match in re.finditer(r"include\s*\(?([^\n)]*)\)?", text):
        include_body = match.group(1)
        for quoted in re.findall(r"['\"]:?(.*?)['\"]", include_body):
            module = quoted.split(":")[-1].strip()
            if module and module not in seen:
                seen.add(module)
                modules.append(module)
    return modules


def discover_loader_modules(project_root: Path) -> List[str]:
    if not (project_root / "common").is_dir():
        raise UploadError(f"Expected a common module folder at {project_root / 'common'}")

    folder_modules = [
        module
        for module in LOADER_INFOS
        if (project_root / module).is_dir()
    ]
    included_modules = parse_included_modules(project_root)
    if included_modules:
        included_set = set(included_modules)
        modules = [module for module in folder_modules if module in included_set]
    else:
        modules = folder_modules

    if not modules:
        raise UploadError(
            "No supported loader modules found. Expected at least one of: "
            + ", ".join(LOADER_INFOS)
        )
    return modules


def load_config(config_path: Path) -> Dict[str, Any]:
    if not config_path.exists():
        return {"config_version": 1, "projects": {}}

    try:
        with config_path.open("r", encoding="utf-8") as handle:
            config = json.load(handle)
    except json.JSONDecodeError as exc:
        raise UploadError(f"Invalid JSON in {config_path}: {exc}") from exc

    if not isinstance(config, dict):
        raise UploadError(f"Config file must contain a JSON object: {config_path}")
    if "projects" not in config:
        config["projects"] = {}
    if not isinstance(config["projects"], dict):
        raise UploadError(f"Config field 'projects' must be an object: {config_path}")
    if "config_version" not in config:
        config["config_version"] = 1
    return config


def save_config(config_path: Path, config: Dict[str, Any]) -> None:
    config_path.parent.mkdir(parents=True, exist_ok=True)
    serialized = json.dumps(config, indent=2, sort_keys=True) + "\n"
    with tempfile.NamedTemporaryFile(
        "w",
        encoding="utf-8",
        dir=str(config_path.parent),
        prefix=f".{config_path.name}.",
        delete=False,
    ) as handle:
        temporary_path = Path(handle.name)
        handle.write(serialized)

    os.chmod(temporary_path, 0o600)
    os.replace(temporary_path, config_path)


def normalize_slug_list(raw: Any) -> List[str]:
    if raw is None:
        return []
    if isinstance(raw, str):
        pieces = raw.split(",")
    elif isinstance(raw, list):
        pieces = [str(item) for item in raw]
    else:
        raise UploadError("Dependency configuration must be a list or comma-separated string.")

    normalized: List[str] = []
    seen = set()
    for piece in pieces:
        slug = piece.strip().lower()
        if not slug or slug in seen:
            continue
        seen.add(slug)
        normalized.append(slug)
    return normalized


def normalize_dependencies_for_loader(loader: str, raw_dependencies: Any) -> List[str]:
    dependencies = normalize_slug_list(raw_dependencies)
    if loader == "fabric" and "fabric-api" not in dependencies:
        dependencies.insert(0, "fabric-api")
    return dependencies


def ensure_project_config(
    config: Dict[str, Any],
    project_key: str,
    loaders: Sequence[str],
    *,
    reset_project_config: bool,
) -> Tuple[Dict[str, Any], bool]:
    projects = config.setdefault("projects", {})
    if not isinstance(projects, dict):
        raise UploadError("Config field 'projects' must be an object.")

    if reset_project_config and project_key in projects:
        del projects[project_key]

    project_config = projects.setdefault(project_key, {})
    if not isinstance(project_config, dict):
        raise UploadError(f"Config for project key '{project_key}' must be an object.")

    changed = False

    if not project_config.get("modrinth_project_id"):
        section("Modrinth Target")
        project_config["modrinth_project_id"] = prompt_line(
            "Modrinth project ID or slug: "
        )
        changed = True

    if not project_config.get("curseforge_project_id"):
        section("CurseForge Target")
        project_config["curseforge_project_id"] = prompt_numeric_id(
            "CurseForge numeric project ID: "
        )
        changed = True

    if not project_config.get("supported_environments"):
        section("Supported Environments")
        choice = prompt_choice(
            "Where does this mod work?",
            ("client", "server", "both"),
        )
        project_config["supported_environments"] = ordered_sides(parse_side_choice(choice))
        changed = True

    supported = set(project_config.get("supported_environments", []))
    if not supported or not supported.issubset(set(SUPPORTED_SIDES)):
        raise UploadError(
            "Config field 'supported_environments' must contain client, server, or both."
        )

    if "mandatory_environments" not in project_config:
        section("Mandatory Environments")
        while True:
            choice = prompt_choice(
                "Where is this mod mandatory?",
                ("none", "client", "server", "both"),
            )
            mandatory = set(parse_side_choice(choice))
            if mandatory.issubset(supported):
                project_config["mandatory_environments"] = ordered_sides(mandatory)
                changed = True
                break
            supported_text = "/".join(ordered_sides(supported))
            print(f"Mandatory environments must be within supported environments: {supported_text}")

    mandatory = set(project_config.get("mandatory_environments", []))
    if not mandatory.issubset(supported):
        raise UploadError(
            "Config field 'mandatory_environments' must be a subset of supported_environments."
        )

    if not project_config.get("changelog_url"):
        section("Changelog")
        project_config["changelog_url"] = prompt_http_url("Stable changelog URL: ")
        changed = True

    dependencies_by_loader = project_config.setdefault("dependencies", {})
    if not isinstance(dependencies_by_loader, dict):
        raise UploadError("Config field 'dependencies' must be an object.")

    for loader in loaders:
        if loader not in dependencies_by_loader:
            section(f"{loader_fancy_name(loader)} Dependencies")
            raw = prompt_line(
                "Mandatory dependency slugs, comma-separated. Leave empty for none: ",
                allow_empty=True,
            )
            dependencies_by_loader[loader] = normalize_dependencies_for_loader(loader, raw)
            changed = True
        else:
            normalized = normalize_dependencies_for_loader(loader, dependencies_by_loader[loader])
            if normalized != dependencies_by_loader[loader]:
                dependencies_by_loader[loader] = normalized
                changed = True

    return project_config, changed


def loader_fancy_name(loader: str) -> str:
    info = LOADER_INFOS.get(loader)
    if info:
        return info.fancy_name
    return loader[:1].upper() + loader[1:]


def keychain_token(service_name: str) -> Optional[str]:
    security_binary = Path("/usr/bin/security")
    if not security_binary.exists():
        raise UploadError("macOS security command not found at /usr/bin/security.")

    result = subprocess.run(
        [str(security_binary), "find-generic-password", "-s", service_name, "-w"],
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
    )
    if result.returncode != 0:
        return None

    token = result.stdout.strip()
    return token or None


def load_tokens_from_keychain() -> Dict[str, str]:
    missing: List[str] = []
    tokens: Dict[str, str] = {}

    for platform, service_name in KEYCHAIN_SERVICES.items():
        token = keychain_token(service_name)
        if not token:
            missing.append(service_name)
        else:
            tokens[platform] = token

    if missing:
        raise UploadError(
            "Could not find required token(s) in the macOS Keychain: "
            + ", ".join(missing)
            + ". Add them to the keychain and run this script again."
        )
    return tokens


def gradle_command(project_root: Path) -> List[str]:
    wrapper = project_root / "gradlew"
    if wrapper.is_file():
        if os.access(wrapper, os.X_OK):
            return [str(wrapper)]
        return ["sh", str(wrapper)]
    return ["gradle"]


def build_loader_modules(project_root: Path, loaders: Sequence[str]) -> None:
    section("Gradle Build")
    command_prefix = gradle_command(project_root)
    for loader in loaders:
        task = f":{loader}:build"
        command = command_prefix + [task]
        print("+ " + shlex.join(command), flush=True)
        result = subprocess.run(command, cwd=str(project_root))
        if result.returncode != 0:
            raise UploadError(
                f"Build failed for module '{loader}' with exit code {result.returncode}."
            )


def format_size(byte_count: int) -> str:
    units = ("B", "KiB", "MiB", "GiB")
    value = float(byte_count)
    for unit in units:
        if value < 1024.0 or unit == units[-1]:
            if unit == "B":
                return f"{int(value)} {unit}"
            return f"{value:.1f} {unit}"
        value /= 1024.0
    return f"{byte_count} B"


def choose_largest(paths: Sequence[Path]) -> Path:
    return max(paths, key=lambda path: (path.stat().st_size, path.name))


def confirm_ambiguous_jar(
    loader: str,
    candidates: Sequence[Path],
    selected: Path,
    *,
    assume_yes: bool,
) -> None:
    print()
    print(f"Could not confidently identify the {loader} upload JAR.")
    print("Remaining candidates:")
    for candidate in candidates:
        print(f"  - {candidate.name} ({format_size(candidate.stat().st_size)})")
    print(f"Selected the largest candidate: {selected.name}")

    if assume_yes:
        print("Accepted because --yes was provided.")
        return

    if not sys.stdin.isatty():
        raise UploadError(
            f"Ambiguous {loader} JAR selection requires confirmation, but stdin is not a terminal."
        )

    answer = input("Is this the correct file to upload? [y/N]: ").strip().lower()
    if answer not in ("y", "yes"):
        raise UploadError(f"Aborted because the {loader} upload JAR was not confirmed.")


def select_mod_jar(
    libs_dir: Path,
    loader: str,
    mod_version: str,
    *,
    assume_yes: bool,
) -> SelectedJar:
    if not libs_dir.is_dir():
        raise UploadError(f"Missing build output folder for {loader}: {libs_dir}")

    jars = sorted(path for path in libs_dir.iterdir() if path.is_file() and path.suffix == ".jar")
    candidates = [
        path for path in jars
        if not any(path.name.endswith(suffix) for suffix in IGNORED_JAR_SUFFIXES)
    ]

    if not candidates:
        raise UploadError(
            f"No uploadable JAR candidates found in {libs_dir}. "
            f"Ignored suffixes: {', '.join(IGNORED_JAR_SUFFIXES)}"
        )

    all_jars = [path for path in candidates if path.name.endswith("-all.jar")]
    if all_jars:
        selected = choose_largest(all_jars)
        return SelectedJar(selected, "found -all.jar candidate")

    if len(candidates) == 1:
        return SelectedJar(candidates[0], "only one uploadable JAR candidate remained")

    exact_mod_version_jars = [
        path for path in candidates
        if path.name.endswith(f"-{mod_version}.jar") and VERSIONED_JAR_PATTERN.search(path.name)
    ]
    if len(exact_mod_version_jars) == 1:
        return SelectedJar(
            exact_mod_version_jars[0],
            "found a version-suffixed JAR matching mod_version",
        )

    versioned_jars = [
        path for path in candidates
        if VERSIONED_JAR_PATTERN.search(path.name)
    ]
    if len(versioned_jars) == 1:
        return SelectedJar(versioned_jars[0], "found one numeric version-suffixed JAR")

    selected = choose_largest(candidates)
    confirm_ambiguous_jar(loader, candidates, selected, assume_yes=assume_yes)
    return SelectedJar(selected, "largest candidate accepted after confirmation")


def safe_file_component(value: str) -> str:
    cleaned = re.sub(r"[^A-Za-z0-9._-]+", "_", value.strip())
    cleaned = cleaned.strip("._-")
    if not cleaned:
        raise UploadError(f"Could not create a safe file-name component from: {value!r}")
    return cleaned


def stage_artifacts(
    project_root: Path,
    loaders: Sequence[str],
    properties: Dict[str, str],
    project_config: Dict[str, Any],
    *,
    assume_yes: bool,
) -> List[StagedArtifact]:
    mod_id = properties["mod_id"]
    mod_version = properties["mod_version"]
    minecraft_version = properties["minecraft_version"]
    output_dir = project_root / "build" / "mod-upload"
    output_dir.mkdir(parents=True, exist_ok=True)

    artifacts: List[StagedArtifact] = []
    section("Upload JAR Selection")
    for loader in loaders:
        selected = select_mod_jar(
            project_root / loader / "build" / "libs",
            loader,
            mod_version,
            assume_yes=assume_yes,
        )

        file_name = (
            f"{safe_file_component(mod_id)}_"
            f"{safe_file_component(loader)}_"
            f"{safe_file_component(mod_version)}_MC_"
            f"{safe_file_component(minecraft_version)}.jar"
        )
        staged_path = output_dir / file_name
        shutil.copy2(selected.source_path, staged_path)

        dependencies = tuple(
            normalize_dependencies_for_loader(
                loader,
                project_config.get("dependencies", {}).get(loader, []),
            )
        )

        display_name = (
            f"[{loader_fancy_name(loader)}] "
            f"v{mod_version} MC {minecraft_version}"
        )
        modrinth_version_number = f"{mod_version}-{minecraft_version}-{loader}"

        print(
            f"{loader}: {selected.source_path.name} -> {file_name} "
            f"({selected.reason})"
        )

        artifacts.append(
            StagedArtifact(
                loader=loader,
                source_path=selected.source_path,
                staged_path=staged_path,
                file_name=file_name,
                display_name=display_name,
                modrinth_version_number=modrinth_version_number,
                dependencies=dependencies,
            )
        )

    return artifacts


class HttpClient:
    def __init__(self, default_headers: Optional[Dict[str, str]] = None) -> None:
        self.default_headers = default_headers or {}

    def json_request(
        self,
        method: str,
        url: str,
        *,
        headers: Optional[Dict[str, str]] = None,
        body: Optional[Any] = None,
        expected_statuses: Sequence[int] = (200,),
        timeout: int = 60,
    ) -> Any:
        request_headers = dict(self.default_headers)
        if headers:
            request_headers.update(headers)

        data: Optional[bytes] = None
        if body is not None:
            data = json.dumps(body).encode("utf-8")
            request_headers.setdefault("Content-Type", "application/json")

        response_body, status = self.raw_request(
            method,
            url,
            headers=request_headers,
            data=data,
            expected_statuses=expected_statuses,
            timeout=timeout,
        )
        if not response_body:
            return None
        try:
            return json.loads(response_body.decode("utf-8"))
        except json.JSONDecodeError as exc:
            raise ApiError(f"Expected JSON from {url}, got: {response_body[:500]!r}") from exc

    def raw_request(
        self,
        method: str,
        url: str,
        *,
        headers: Optional[Dict[str, str]] = None,
        data: Optional[bytes] = None,
        expected_statuses: Sequence[int] = (200,),
        timeout: int = 60,
    ) -> Tuple[bytes, int]:
        request_headers = dict(self.default_headers)
        if headers:
            request_headers.update(headers)

        http_request = request.Request(
            url,
            data=data,
            headers=request_headers,
            method=method,
        )

        try:
            with request.urlopen(http_request, timeout=timeout) as response:
                status = response.getcode()
                response_body = response.read()
        except error.HTTPError as exc:
            response_body = exc.read()
            message = response_body.decode("utf-8", errors="replace")
            raise ApiError(
                f"HTTP {exc.code} from {method} {url}: {message[:1000]}"
            ) from exc
        except error.URLError as exc:
            raise ApiError(f"Request failed for {method} {url}: {exc}") from exc

        if status not in expected_statuses:
            message = response_body.decode("utf-8", errors="replace")
            raise ApiError(f"Unexpected HTTP {status} from {method} {url}: {message[:1000]}")

        return response_body, status


def multipart_body(
    fields: Dict[str, str],
    files: Sequence[Tuple[str, Path, str, str]],
) -> Tuple[bytes, str]:
    boundary = f"----mod-upload-{uuid.uuid4().hex}"
    chunks: List[bytes] = []

    for name, value in fields.items():
        chunks.append(f"--{boundary}\r\n".encode("utf-8"))
        chunks.append(
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode("utf-8")
        )
        chunks.append(value.encode("utf-8"))
        chunks.append(b"\r\n")

    for field_name, path, filename, content_type in files:
        chunks.append(f"--{boundary}\r\n".encode("utf-8"))
        chunks.append(
            (
                f'Content-Disposition: form-data; name="{field_name}"; '
                f'filename="{filename}"\r\n'
            ).encode("utf-8")
        )
        chunks.append(f"Content-Type: {content_type}\r\n\r\n".encode("utf-8"))
        with path.open("rb") as handle:
            chunks.append(handle.read())
        chunks.append(b"\r\n")

    chunks.append(f"--{boundary}--\r\n".encode("utf-8"))
    return b"".join(chunks), boundary


class ModrinthClient:
    def __init__(self, token: str) -> None:
        self.http = HttpClient(
            {
                "Authorization": token,
                "User-Agent": USER_AGENT,
            }
        )

    def get_loaders(self) -> List[Dict[str, Any]]:
        return self.http.json_request("GET", f"{MODRINTH_API_BASE}/tag/loader")

    def get_game_versions(self) -> List[Dict[str, Any]]:
        return self.http.json_request("GET", f"{MODRINTH_API_BASE}/tag/game_version")

    def get_project(self, project_id_or_slug: str) -> Dict[str, Any]:
        return self.http.json_request(
            "GET",
            f"{MODRINTH_API_BASE}/project/{project_id_or_slug}",
        )

    def patch_project_environment(
        self,
        project_id_or_slug: str,
        *,
        client_side: str,
        server_side: str,
    ) -> None:
        self.http.json_request(
            "PATCH",
            f"{MODRINTH_API_BASE}/project/{project_id_or_slug}",
            body={
                "client_side": client_side,
                "server_side": server_side,
            },
            expected_statuses=(200, 204),
        )

    def create_version(self, metadata: Dict[str, Any], artifact: StagedArtifact) -> Any:
        body, boundary = multipart_body(
            {"data": json.dumps(metadata, separators=(",", ":"))},
            [("primary", artifact.staged_path, artifact.file_name, "application/java-archive")],
        )
        response_body, _ = self.http.raw_request(
            "POST",
            f"{MODRINTH_API_BASE}/version",
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
            data=body,
            expected_statuses=(200, 201),
            timeout=300,
        )
        return json.loads(response_body.decode("utf-8")) if response_body else None


class CurseForgeClient:
    def __init__(self, token: str) -> None:
        self.http = HttpClient(
            {
                "X-Api-Token": token,
                "User-Agent": USER_AGENT,
            }
        )
        self._version_types: Optional[List[Dict[str, Any]]] = None
        self._game_versions: Optional[List[Dict[str, Any]]] = None

    def get_version_types(self) -> List[Dict[str, Any]]:
        if self._version_types is None:
            self._version_types = self.http.json_request(
                "GET",
                f"{CURSEFORGE_MINECRAFT_API_BASE}/game/version-types",
            )
        return self._version_types

    def get_game_versions(self) -> List[Dict[str, Any]]:
        if self._game_versions is None:
            self._game_versions = self.http.json_request(
                "GET",
                f"{CURSEFORGE_MINECRAFT_API_BASE}/game/versions",
            )
        return self._game_versions

    def version_type_slugs_by_id(self) -> Dict[int, str]:
        return {
            int(version_type["id"]): str(version_type["slug"])
            for version_type in self.get_version_types()
        }

    def resolve_minecraft_version_id(self, minecraft_version: str) -> int:
        type_slugs = self.version_type_slugs_by_id()
        candidates = [
            item
            for item in self.get_game_versions()
            if str(item.get("name")) == minecraft_version
            and type_slugs.get(int(item.get("gameVersionTypeID", -1)), "").startswith("minecraft-")
        ]

        if not candidates:
            raise UploadError(
                f"CurseForge does not list Minecraft version '{minecraft_version}' "
                "as a Minecraft game-version tag."
            )

        candidates.sort(
            key=lambda item: (
                item.get("apiVersion") is not None,
                int(item.get("id", 0)),
            )
        )
        return int(candidates[0]["id"])

    def resolve_tag_id(self, name: str, expected_type_slug: str) -> int:
        type_slugs = self.version_type_slugs_by_id()
        candidates = [
            item
            for item in self.get_game_versions()
            if str(item.get("name")) == name
            and type_slugs.get(int(item.get("gameVersionTypeID", -1))) == expected_type_slug
        ]
        if not candidates:
            raise UploadError(
                f"CurseForge does not list required tag '{name}' "
                f"under version type '{expected_type_slug}'."
            )
        candidates.sort(key=lambda item: int(item.get("id", 0)))
        return int(candidates[0]["id"])

    def upload_file(
        self,
        project_id: str,
        metadata: Dict[str, Any],
        artifact: StagedArtifact,
    ) -> Any:
        body, boundary = multipart_body(
            {"metadata": json.dumps(metadata, separators=(",", ":"))},
            [("file", artifact.staged_path, artifact.file_name, "application/java-archive")],
        )
        response_body, _ = self.http.raw_request(
            "POST",
            f"{CURSEFORGE_MINECRAFT_API_BASE}/projects/{project_id}/upload-file",
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
            data=body,
            expected_statuses=(200, 201),
            timeout=300,
        )
        return json.loads(response_body.decode("utf-8")) if response_body else None


def modrinth_environment_values(project_config: Dict[str, Any]) -> Tuple[str, str]:
    supported = set(project_config["supported_environments"])
    mandatory = set(project_config.get("mandatory_environments", []))

    values = []
    for side in SUPPORTED_SIDES:
        if side not in supported:
            values.append("unsupported")
        elif side in mandatory:
            values.append("required")
        else:
            values.append("optional")
    return values[0], values[1]


def validate_modrinth_targets(
    client: ModrinthClient,
    loaders: Sequence[str],
    minecraft_version: str,
    project_id_or_slug: str,
) -> Dict[str, Any]:
    section("Modrinth Validation")

    project = client.get_project(project_id_or_slug)
    project_title = project.get("title") or project.get("slug") or project_id_or_slug
    print(f"Project: {project_title}")

    loader_names = {
        str(loader.get("name"))
        for loader in client.get_loaders()
        if "mod" in loader.get("supported_project_types", [])
        or "project" in loader.get("supported_project_types", [])
    }
    for loader in loaders:
        modrinth_loader = LOADER_INFOS[loader].modrinth_name
        if modrinth_loader not in loader_names:
            raise UploadError(f"Modrinth does not list loader '{modrinth_loader}'.")
        print(f"Loader: {modrinth_loader}")

    game_versions = {
        str(version.get("version"))
        for version in client.get_game_versions()
    }
    if minecraft_version not in game_versions:
        raise UploadError(
            f"Modrinth does not list Minecraft version '{minecraft_version}'."
        )
    print(f"Minecraft version: {minecraft_version}")
    return project


def resolve_curseforge_tags(
    client: CurseForgeClient,
    loaders: Sequence[str],
    minecraft_version: str,
    supported_sides: Sequence[str],
) -> CurseForgeTags:
    section("CurseForge Validation")
    minecraft_version_id = client.resolve_minecraft_version_id(minecraft_version)
    print(f"Minecraft version: {minecraft_version} -> ID {minecraft_version_id}")

    loader_ids_by_module: Dict[str, int] = {}
    for loader in loaders:
        curseforge_loader = LOADER_INFOS[loader].curseforge_name
        loader_id = client.resolve_tag_id(curseforge_loader, "modloader")
        loader_ids_by_module[loader] = loader_id
        print(f"Loader: {curseforge_loader} -> ID {loader_id}")

    environment_ids_by_side: Dict[str, int] = {}
    for side in supported_sides:
        tag_name = side[:1].upper() + side[1:]
        tag_id = client.resolve_tag_id(tag_name, "environment")
        environment_ids_by_side[side] = tag_id
        print(f"Environment: {tag_name} -> ID {tag_id}")

    return CurseForgeTags(
        minecraft_version_id=minecraft_version_id,
        minecraft_version_name=minecraft_version,
        loader_ids_by_module=loader_ids_by_module,
        environment_ids_by_side=environment_ids_by_side,
    )


def modrinth_version_metadata(
    project_config: Dict[str, Any],
    properties: Dict[str, str],
    artifact: StagedArtifact,
    release_type: str,
) -> Dict[str, Any]:
    dependencies = [
        {
            "project_id": dependency,
            "version_id": None,
            "file_name": None,
            "dependency_type": "required",
        }
        for dependency in artifact.dependencies
    ]

    return {
        "project_id": project_config["modrinth_project_id"],
        "name": artifact.display_name,
        "version_number": artifact.modrinth_version_number,
        "changelog": f"CHANGELOG: {project_config['changelog_url']}",
        "dependencies": dependencies,
        "game_versions": [properties["minecraft_version"]],
        "version_type": release_type,
        "loaders": [LOADER_INFOS[artifact.loader].modrinth_name],
        "featured": False,
        "status": "listed",
        "requested_status": None,
        "file_parts": ["primary"],
        "primary_file": "primary",
    }


def curseforge_file_metadata(
    project_config: Dict[str, Any],
    artifact: StagedArtifact,
    cf_tags: CurseForgeTags,
    release_type: str,
) -> Dict[str, Any]:
    game_version_ids = [
        cf_tags.minecraft_version_id,
        cf_tags.loader_ids_by_module[artifact.loader],
    ]
    game_version_ids.extend(cf_tags.environment_ids_by_side.values())

    metadata: Dict[str, Any] = {
        "changelog": f"CHANGELOG: {project_config['changelog_url']}",
        "changelogType": "markdown",
        "displayName": artifact.display_name,
        "gameVersions": game_version_ids,
        "releaseType": release_type,
        "isMarkedForManualRelease": False,
    }

    if artifact.dependencies:
        metadata["relations"] = {
            "projects": [
                {
                    "slug": dependency,
                    "type": "requiredDependency",
                }
                for dependency in artifact.dependencies
            ]
        }

    return metadata


def sync_modrinth_environment(
    client: ModrinthClient,
    project_config: Dict[str, Any],
    project_data: Dict[str, Any],
    *,
    upload_enabled: bool,
) -> None:
    desired_client_side, desired_server_side = modrinth_environment_values(project_config)
    current_client_side = project_data.get("client_side")
    current_server_side = project_data.get("server_side")

    if (
        current_client_side == desired_client_side
        and current_server_side == desired_server_side
    ):
        print(
            "Modrinth environment is already "
            f"client={desired_client_side}, server={desired_server_side}."
        )
        return

    message = (
        "Modrinth project environment "
        f"{current_client_side}/{current_server_side} -> "
        f"{desired_client_side}/{desired_server_side}"
    )
    if upload_enabled:
        client.patch_project_environment(
            project_config["modrinth_project_id"],
            client_side=desired_client_side,
            server_side=desired_server_side,
        )
        print(message + " (updated)")
    else:
        print(message + " (dry run, not updated)")


def print_plan_summary(
    *,
    project_root: Path,
    project_key: str,
    properties: Dict[str, str],
    project_config: Dict[str, Any],
    artifacts: Sequence[StagedArtifact],
    upload_enabled: bool,
    release_type: str,
) -> None:
    section("Upload Plan")
    print(f"Project root: {project_root}")
    print(f"Config project key: {project_key}")
    print(f"Mod ID: {properties['mod_id']}")
    print(f"Mod version: {properties['mod_version']} (from mod_version)")
    print(f"Minecraft version: {properties['minecraft_version']}")
    print(f"Release type: {release_type}")
    print(f"Mode: {'upload after confirmation' if upload_enabled else 'dry run'}")
    print(
        "Supported environments: "
        + ", ".join(project_config["supported_environments"])
    )
    mandatory = project_config.get("mandatory_environments", [])
    print("Mandatory environments: " + (", ".join(mandatory) if mandatory else "none"))

    for artifact in artifacts:
        dependencies = ", ".join(artifact.dependencies) if artifact.dependencies else "none"
        print()
        print(f"{loader_fancy_name(artifact.loader)}")
        print(f"  Source: {artifact.source_path}")
        print(f"  Staged: {artifact.staged_path}")
        print(f"  Display name: {artifact.display_name}")
        print(f"  Modrinth version number: {artifact.modrinth_version_number}")
        print(f"  Required dependencies: {dependencies}")
    sys.stdout.flush()


def confirm_real_upload(artifacts: Sequence[StagedArtifact], *, assume_yes: bool) -> None:
    if assume_yes:
        return
    if not sys.stdin.isatty():
        raise UploadError("Real upload requires confirmation, but stdin is not a terminal.")

    print()
    print("This will upload these files to Modrinth and CurseForge:")
    for artifact in artifacts:
        print(f"  - {artifact.file_name}")
    answer = input("Type 'upload' to continue: ").strip()
    if answer != "upload":
        raise UploadError("Upload aborted before any files were sent.")


def upload_artifacts(
    *,
    modrinth_client: ModrinthClient,
    curseforge_client: CurseForgeClient,
    project_config: Dict[str, Any],
    properties: Dict[str, str],
    artifacts: Sequence[StagedArtifact],
    cf_tags: CurseForgeTags,
    release_type: str,
) -> None:
    section("Uploading")
    for artifact in artifacts:
        print(f"\n{loader_fancy_name(artifact.loader)}")

        modrinth_metadata = modrinth_version_metadata(
            project_config,
            properties,
            artifact,
            release_type,
        )
        modrinth_result = modrinth_client.create_version(modrinth_metadata, artifact)
        modrinth_id = (
            modrinth_result.get("id")
            if isinstance(modrinth_result, dict)
            else modrinth_result
        )
        print(f"Modrinth version created: {modrinth_id}")

        curseforge_metadata = curseforge_file_metadata(
            project_config,
            artifact,
            cf_tags,
            release_type,
        )
        curseforge_result = curseforge_client.upload_file(
            project_config["curseforge_project_id"],
            curseforge_metadata,
            artifact,
        )
        curseforge_id = (
            curseforge_result.get("id")
            if isinstance(curseforge_result, dict)
            else curseforge_result
        )
        print(f"CurseForge file uploaded: {curseforge_id}")


def parse_args(argv: Sequence[str]) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Build loader modules, identify upload JARs, stage clean filenames, "
            "and upload Minecraft mods to Modrinth and CurseForge."
        )
    )
    parser.add_argument(
        "--project-root",
        type=Path,
        default=Path.cwd(),
        help="Project root containing gradle.properties, common, and loader modules.",
    )
    parser.add_argument(
        "--config-project-key",
        help="Project key inside the script-local config file. Defaults to mod_id.",
    )
    parser.add_argument(
        "--reset-config",
        action="store_true",
        help="Forget this project's saved config and ask for it again.",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Build, validate, and stage files without uploading or modifying platform metadata.",
    )
    parser.add_argument(
        "--skip-build",
        action="store_true",
        help="Skip Gradle builds. Useful only when build/libs outputs are already current.",
    )
    parser.add_argument(
        "--yes",
        action="store_true",
        help="Accept ambiguous JAR choices and real upload confirmation.",
    )
    parser.add_argument(
        "--release-type",
        choices=("release", "beta", "alpha"),
        default="release",
        help="Release type to send to Modrinth and CurseForge.",
    )
    parser.add_argument(
        "--config-path",
        type=Path,
        help=(
            "Override config path. Defaults to mod_upload_config.json next to this script."
        ),
    )
    return parser.parse_args(argv)


def main(argv: Sequence[str]) -> int:
    args = parse_args(argv)
    script_dir = Path(__file__).resolve().parent
    config_path = (args.config_path or (script_dir / CONFIG_FILE_NAME)).resolve()
    project_root = args.project_root.resolve()

    try:
        properties = parse_gradle_properties(project_root / "gradle.properties")
        require_gradle_properties(properties)

        project_key = args.config_project_key or properties["mod_id"]
        loaders = discover_loader_modules(project_root)

        config = load_config(config_path)
        project_config, config_changed = ensure_project_config(
            config,
            project_key,
            loaders,
            reset_project_config=args.reset_config,
        )
        if config_changed or not config_path.exists():
            save_config(config_path, config)
            print(f"\nSaved config: {config_path}")

        if args.skip_build:
            section("Gradle Build")
            print("Skipped because --skip-build was provided.")
        else:
            build_loader_modules(project_root, loaders)

        artifacts = stage_artifacts(
            project_root,
            loaders,
            properties,
            project_config,
            assume_yes=args.yes,
        )

        tokens = load_tokens_from_keychain()

        modrinth_client = ModrinthClient(tokens["modrinth"])
        curseforge_client = CurseForgeClient(tokens["curseforge"])

        modrinth_project = validate_modrinth_targets(
            modrinth_client,
            loaders,
            properties["minecraft_version"],
            project_config["modrinth_project_id"],
        )
        cf_tags = resolve_curseforge_tags(
            curseforge_client,
            loaders,
            properties["minecraft_version"],
            project_config["supported_environments"],
        )

        print_plan_summary(
            project_root=project_root,
            project_key=project_key,
            properties=properties,
            project_config=project_config,
            artifacts=artifacts,
            upload_enabled=not args.dry_run,
            release_type=args.release_type,
        )

        if args.dry_run:
            sync_modrinth_environment(
                modrinth_client,
                project_config,
                modrinth_project,
                upload_enabled=False,
            )
            print("\nDry run complete. Re-run without --dry-run to upload.")
        else:
            confirm_real_upload(artifacts, assume_yes=args.yes)
            sync_modrinth_environment(
                modrinth_client,
                project_config,
                modrinth_project,
                upload_enabled=True,
            )
            upload_artifacts(
                modrinth_client=modrinth_client,
                curseforge_client=curseforge_client,
                project_config=project_config,
                properties=properties,
                artifacts=artifacts,
                cf_tags=cf_tags,
                release_type=args.release_type,
            )
            print("\nDone.")

        return 0

    except UploadError as exc:
        eprint(f"\nERROR: {exc}")
        return 1
    except KeyboardInterrupt:
        eprint("\nAborted.")
        return 130


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
