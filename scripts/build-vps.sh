#!/usr/bin/env bash
# Compile Survivor: End of the World sur un serveur Linux (VPS Ubuntu / Debian).
#
#   ./scripts/build-vps.sh setup   # une seule fois : JDK 17 et SDK Android (sudo pour les paquets système)
#   ./scripts/build-vps.sh apk     # APK à installer sur un téléphone
#   ./scripts/build-vps.sh aab     # .aab à envoyer sur Google Play
#   ./scripts/build-vps.sh all     # les deux
#
# Les fichiers produits vont dans ~/builds/survive/.
# La clé de signature est créée au premier build dans ~/.survive/ : À SAUVEGARDER, elle sert pour toutes les mises à jour.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
KEYS_DIR="${KEYS_DIR:-$HOME/.survive}"
OUT_DIR="${OUT_DIR:-$HOME/builds/survive}"

# Versions demandées par app/build.gradle.kts (compileSdk 36, exigé par Google Play depuis août 2026).
CMDLINE_TOOLS_ZIP="commandlinetools-linux-13114758_latest.zip"
SDK_PACKAGES=("platform-tools" "platforms;android-36" "build-tools;36.0.0")

say() { printf '\n\033[1;33m▶ %s\033[0m\n' "$*"; }
die() { printf '\033[1;31m✖ %s\033[0m\n' "$*" >&2; exit 1; }
as_root() { if [ "$(id -u)" -eq 0 ]; then "$@"; else sudo "$@"; fi; }

setup() {
  command -v apt-get >/dev/null || die "Ce script installe les paquets avec apt (Ubuntu / Debian)."

  say "Paquets système (JDK 17, git, unzip)"
  as_root apt-get update -y
  as_root apt-get install -y openjdk-17-jdk-headless git unzip curl ca-certificates

  if [ ! -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
    say "Outils en ligne de commande Android"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    tmp="$(mktemp -d)"
    curl -fsSL -o "$tmp/tools.zip" "https://dl.google.com/android/repository/$CMDLINE_TOOLS_ZIP"
    unzip -q "$tmp/tools.zip" -d "$tmp"
    mv "$tmp/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
  fi

  say "SDK Android (plateforme 36, build-tools) : environ 1 Go"
  yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || true
  "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "${SDK_PACKAGES[@]}" >/dev/null

  # Gradle + R8 ont besoin d'environ 3 Go de mémoire (RAM + swap).
  mem_kb="$(awk '/MemTotal|SwapTotal/ { s += $2 } END { print s }' /proc/meminfo)"
  if [ "$mem_kb" -lt 2800000 ]; then
    printf '\033[1;31m⚠ Seulement %s Mo de mémoire (RAM + swap). Ajoutez du swap avant de compiler, par exemple :\033[0m\n' "$((mem_kb / 1024))"
    echo "    sudo fallocate -l 4G /swapfile && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile"
  fi
  say "Installation terminée. Lancez : ./scripts/build-vps.sh apk"
}

# Crée la clé de signature au premier build ; les mots de passe restent sur le serveur, jamais dans le dépôt.
signing() {
  mkdir -p "$KEYS_DIR" && chmod 700 "$KEYS_DIR"
  if [ ! -f "$KEYS_DIR/signing.env" ]; then
    say "Création de la clé de signature dans $KEYS_DIR"
    pass="$(head -c 24 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 24)"
    keytool -genkeypair -v -storetype JKS -keystore "$KEYS_DIR/survive.jks" -alias survive \
      -keyalg RSA -keysize 2048 -validity 10000 -storepass "$pass" -keypass "$pass" \
      -dname "CN=Survivor End of the World, O=Aymen, C=FR" >/dev/null 2>&1
    printf 'STORE_PASSWORD=%s\nKEY_ALIAS=survive\nKEY_PASSWORD=%s\n' "$pass" "$pass" > "$KEYS_DIR/signing.env"
    chmod 600 "$KEYS_DIR/signing.env" "$KEYS_DIR/survive.jks"
    printf '\033[1;31m⚠ Sauvegardez %s (les deux fichiers) : sans cette clé, plus de mise à jour possible.\033[0m\n' "$KEYS_DIR"
  fi
  # shellcheck disable=SC1091
  . "$KEYS_DIR/signing.env"
  SIGNING=(
    "-Pandroid.injected.signing.store.file=$KEYS_DIR/survive.jks"
    "-Pandroid.injected.signing.store.password=$STORE_PASSWORD"
    "-Pandroid.injected.signing.key.alias=$KEY_ALIAS"
    "-Pandroid.injected.signing.key.password=$KEY_PASSWORD"
  )
}

build() {
  [ -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ] || die "SDK Android introuvable dans $ANDROID_HOME. Lancez d'abord : ./scripts/build-vps.sh setup"
  cd "$ROOT"
  echo "sdk.dir=$ANDROID_HOME" > local.properties
  version="$(sed -n 's/.*versionName *= *"\(.*\)".*/\1/p' app/build.gradle.kts)"
  code="$(sed -n 's/.*versionCode *= *\([0-9]*\).*/\1/p' app/build.gradle.kts)"
  name="survivor-end-of-the-world-$version-$code"

  # Vérifie le contenu (fiches FR/EN, sources, outils, politique de confidentialité) avant de compiler.
  say "Vérification du contenu"
  ./gradlew testDebugUnitTest --no-daemon --tests '*ContentRepositoryTest*' --tests '*MorseTest*' \
    --tests '*MapCatalogTest*' --tests '*PrivacyPolicyTest*' --tests '*LocalizationTest*'

  signing
  mkdir -p "$OUT_DIR"
  for target in "$@"; do
    if [ "$target" = apk ]; then
      say "Compilation de l'APK"
      ./gradlew assembleRelease --no-daemon "${SIGNING[@]}"
      cp app/build/outputs/apk/release/app-release.apk "$OUT_DIR/$name.apk"
      say "APK : $OUT_DIR/$name.apk"
    else
      say "Compilation du .aab pour Google Play"
      ./gradlew bundleRelease --no-daemon "${SIGNING[@]}"
      cp app/build/outputs/bundle/release/app-release.aab "$OUT_DIR/$name.aab"
      say ".aab : $OUT_DIR/$name.aab"
      # Code réduit par R8 : Google Play a besoin de ce fichier pour lire les rapports de plantage.
      if [ -f app/build/outputs/mapping/release/mapping.txt ]; then
        cp app/build/outputs/mapping/release/mapping.txt "$OUT_DIR/$name-mapping.txt"
        say "Fichier de désobscurcissement : $OUT_DIR/$name-mapping.txt (à envoyer avec le .aab)"
      fi
    fi
  done
}

case "${1:-}" in
  setup) setup ;;
  apk) build apk ;;
  aab) build aab ;;
  all) build apk aab ;;
  *) sed -n '2,10p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; exit 1 ;;
esac
