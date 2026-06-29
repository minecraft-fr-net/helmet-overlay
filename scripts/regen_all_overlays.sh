#!/usr/bin/env bash
# Régénère tous les overlays de casque à partir des textures d'armure vanilla.
# Extrait automatiquement les textures du JAR Minecraft installé localement.
#
# Usage:
#   ./scripts/regen_all_overlays.sh
#   ./scripts/regen_all_overlays.sh --mc-version 26.2
#   ./scripts/regen_all_overlays.sh --jar /chemin/vers/minecraft.jar
#
# Dépendances : python3, pillow, numpy, unzip

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OVERLAY_DIR="$REPO_ROOT/src/main/resources/assets/minecraft/textures/misc"
GENERATE_PY="$SCRIPT_DIR/generate_overlay.py"

# ── Version Minecraft lue depuis gradle.properties par défaut ──
DEFAULT_MC_VERSION="$(grep '^minecraft_version=' "$REPO_ROOT/gradle.properties" | cut -d= -f2)"
MC_VERSION="${DEFAULT_MC_VERSION}"
JAR_OVERRIDE=""

# ── Parsing des arguments ──
while [[ $# -gt 0 ]]; do
  case "$1" in
    --mc-version) MC_VERSION="$2"; shift 2 ;;
    --jar)        JAR_OVERRIDE="$2"; shift 2 ;;
    *) echo "Option inconnue : $1" >&2; exit 1 ;;
  esac
done

# ── Localisation du JAR Minecraft ──
# Répertoires d'installation connus (macOS, Linux, Snap)
MC_DIRS=(
  "$HOME/Library/Application Support/minecraft/versions"
  "$HOME/.minecraft/versions"
  "$HOME/snap/minecraft/common/.minecraft/versions"
)

find_jar() {
  # 1. Cherche un dossier dont le nom contient la version exacte
  for base in "${MC_DIRS[@]}"; do
    [[ -d "$base" ]] || continue
    local jar="$base/$1/$1.jar"
    [[ -f "$jar" ]] && echo "$jar" && return 0
  done
  # 2. Fallback : prend le JAR vanilla le plus récent (exclut les dossiers fabric/forge)
  for base in "${MC_DIRS[@]}"; do
    [[ -d "$base" ]] || continue
    # Dossiers de la forme 1.X.Y uniquement
    local jar
    jar=$(find "$base" -maxdepth 2 -name "*.jar" \
      | grep -E '/[0-9]+\.[0-9]+(\.[0-9]+)?/[^/]+\.jar$' \
      | sort -V | tail -1)
    [[ -n "$jar" ]] && echo "$jar" && return 0
  done
  return 1
}

if [[ -n "$JAR_OVERRIDE" ]]; then
  MC_JAR="$JAR_OVERRIDE"
  if [[ ! -f "$MC_JAR" ]]; then
    echo "Erreur : JAR introuvable : $MC_JAR" >&2; exit 1
  fi
else
  echo "Recherche du JAR Minecraft (version mod : $MC_VERSION)..."
  if ! MC_JAR="$(find_jar "$MC_VERSION")"; then
    echo "Erreur : aucun JAR Minecraft trouvé." >&2
    echo "Utilisez --jar /chemin/vers/minecraft.jar pour spécifier manuellement." >&2
    exit 1
  fi
fi

echo "JAR : $MC_JAR"

# ── Extraction des textures d'armure dans un dossier temporaire ──
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

echo "Extraction des textures d'armure..."
# Depuis Minecraft 1.21 : textures/entity/equipment/humanoid/
# Avant : textures/models/armor/
ARMOR_PATH_NEW="assets/minecraft/textures/entity/equipment/humanoid"
ARMOR_PATH_OLD="assets/minecraft/textures/models/armor"

# Tente les deux chemins ; || true pour ne pas sortir avec pipefail
unzip -q "$MC_JAR" "$ARMOR_PATH_NEW/*" -d "$TMP_DIR" 2>/dev/null || true
unzip -q "$MC_JAR" "$ARMOR_PATH_OLD/*" -d "$TMP_DIR" 2>/dev/null || true

if   [[ -f "$TMP_DIR/$ARMOR_PATH_NEW/iron.png" ]]; then
  ARMOR_DIR="$TMP_DIR/$ARMOR_PATH_NEW"
elif [[ -f "$TMP_DIR/$ARMOR_PATH_OLD/iron_layer_1.png" ]]; then
  ARMOR_DIR="$TMP_DIR/$ARMOR_PATH_OLD"
else
  echo "Erreur : textures d'armure absentes du JAR." >&2; exit 1
fi

echo "Textures extraites :"
ls "$ARMOR_DIR"

# ── Vérification des dépendances Python ──
if ! python3 -c "import PIL, numpy" 2>/dev/null; then
  echo "Installation des dépendances Python (pillow, numpy)..."
  pip3 install --quiet pillow numpy
fi

# ── Génération des overlays ──
echo ""
echo "Génération des overlays → $OVERLAY_DIR"
echo "────────────────────────────────────────"

run() {
  local armor_name="$1"      # ex: iron_helmet
  local layer1="$2"          # ex: iron_layer_1.png
  local layer2="${3:-}"      # optionnel
  local output="$OVERLAY_DIR/${armor_name}_overlay.png"

  if [[ ! -f "$ARMOR_DIR/$layer1" ]]; then
    echo "⚠  $layer1 absent du JAR, skipping $armor_name"
    return
  fi

  local args=("$ARMOR_DIR/$layer1")
  if [[ -n "$layer2" && -f "$ARMOR_DIR/$layer2" ]]; then
    args+=("$ARMOR_DIR/$layer2")
  elif [[ -n "$layer2" ]]; then
    echo "⚠  $layer2 absent, génération sans overlay secondaire"
  fi

  python3 "$GENERATE_PY" "${args[@]}" -o "$output"
}

# Noms de fichiers : nouvelle convention (1.21+) ou ancienne (<1.21)
if [[ -f "$ARMOR_DIR/iron.png" ]]; then
  # Minecraft 1.21+ : textures/entity/equipment/humanoid/{material}.png
  run iron_helmet      iron.png
  run diamond_helmet   diamond.png
  run golden_helmet    gold.png
  run netherite_helmet netherite.png
  run chainmail_helmet chainmail.png
  run leather_helmet   leather.png   leather_overlay.png
else
  # Minecraft < 1.21 : textures/models/armor/{material}_layer_1.png
  run iron_helmet      iron_layer_1.png
  run diamond_helmet   diamond_layer_1.png
  run golden_helmet    gold_layer_1.png
  run netherite_helmet netherite_layer_1.png
  run chainmail_helmet chainmail_layer_1.png
  run leather_helmet   leather_layer_1.png   leather_layer_2.png
fi

echo ""
echo "✓ Overlays régénérés dans $OVERLAY_DIR"
