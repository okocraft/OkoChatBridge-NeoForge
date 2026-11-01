"""Boot the distribution jar on a fresh, loopback-only NeoForge server."""

import json
import os
from pathlib import Path
import shutil
import socket
import struct
import subprocess
import threading
import urllib.request


ROOT = Path(__file__).resolve().parents[3]
PROPERTIES = dict(
    line.split("=", 1) for line in (ROOT / "gradle.properties").read_text().splitlines()
    if "=" in line and not line.startswith("#")
)
NEO_VERSION = PROPERTIES["neo_version"]
WORK = ROOT / "build" / "smoke"
SERVER = WORK / "server"


def varint(value):
    value &= 0xFFFFFFFF
    result = bytearray()
    while value > 127:
        result.append((value & 127) | 128)
        value >>= 7
    return bytes(result + bytes([value]))


def read_varint(stream):
    value = 0
    for shift in range(0, 35, 7):
        raw = stream.read(1)
        if not raw:
            raise EOFError("Server closed the status connection")
        value |= (raw[0] & 127) << shift
        if not raw[0] & 128:
            return value
    raise ValueError("Invalid Minecraft VarInt")


def verify_status():
    with socket.create_connection(("127.0.0.1", 25585), timeout=5) as connection:
        handshake = b"\x00" + varint(-1) + b"\x09localhost" + struct.pack(">H", 25585) + b"\x01"
        connection.sendall(varint(len(handshake)) + handshake + b"\x01\x00")
        with connection.makefile("rb") as stream:
            read_varint(stream)
            assert read_varint(stream) == 0, "Expected a Minecraft status response"
            length = read_varint(stream)
            status = json.loads(stream.read(length))
            assert status["version"]["name"] == "26.2", status["version"]
            assert status["players"]["online"] == 0, status["players"]
            print("Status response:", status["version"], flush=True)


def main():
    SERVER.mkdir(parents=True, exist_ok=True)
    installer = WORK / "installer.jar"
    url = ("https://maven.neoforged.net/releases/net/neoforged/neoforge/"
           f"{NEO_VERSION}/neoforge-{NEO_VERSION}-installer.jar")
    with urllib.request.urlopen(url, timeout=60) as response, installer.open("wb") as output:
        shutil.copyfileobj(response, output)
    subprocess.run(["java", "-jar", str(installer), "--installServer"], cwd=SERVER, check=True, timeout=180)
    mods = SERVER / "mods"
    mods.mkdir(exist_ok=True)
    jar = ROOT / "build" / "libs" / f"OkoChatBridge-NeoForge-{PROPERTIES['mod_version']}.jar"
    shutil.copyfile(jar, mods / jar.name)
    (SERVER / "eula.txt").write_text("eula=true\n")
    (SERVER / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25585\nonline-mode=false\n"
        "enforce-secure-profile=false\nview-distance=2\nsimulation-distance=2\n"
        "level-type=minecraft:flat\nspawn-protection=0\nmax-players=2\n"
    )
    (SERVER / "user_jvm_args.txt").write_text("-Xms256M\n-Xmx1G\n")
    args_file = "win_args.txt" if os.name == "nt" else "unix_args.txt"
    args_path = f"libraries/net/neoforged/neoforge/{NEO_VERSION}/{args_file}"
    process = subprocess.Popen(
        ["java", "@user_jvm_args.txt", "@" + args_path, "nogui"], cwd=SERVER,
        stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True, encoding="utf-8", errors="replace",
    )
    deadline = threading.Timer(120, process.kill)
    deadline.start()
    ready = False
    bridge_loaded = False
    try:
        with (WORK / "server-output.log").open("w", encoding="utf-8") as log:
            for line in process.stdout:
                print(line, end="", flush=True)
                log.write(line)
                bridge_loaded |= "OkoChat NeoForge bridge enabled on okochat:messaging" in line
                if "Done (" in line and not ready:
                    verify_status()
                    ready = True
                    process.stdin.write("stop\n")
                    process.stdin.flush()
        code = process.wait(timeout=15)
        assert ready, "Dedicated server did not become ready"
        assert bridge_loaded, "The packaged chat bridge did not initialize"
        assert code == 0, f"Dedicated server exited with code {code}"
        print("Packaged-mod dedicated-server smoke test passed.", flush=True)
    finally:
        deadline.cancel()
        if process.poll() is None:
            process.kill()
            process.wait()


if __name__ == "__main__":
    main()
