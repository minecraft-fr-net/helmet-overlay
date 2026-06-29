#!/usr/bin/env python3
"""
Génère des textures overlay de casque à partir de textures d'armure Minecraft.

La face avant du casque dans une texture d'armure (64x32 pour une résolution 16x16)
se trouve à x=8, y=8 sur une surface de 8x8. Le script calcule automatiquement
le multiplicateur pour les résolutions supérieures (32x32, 64x64, …).

Usage:
  python3 scripts/generate_overlay.py <armor_texture.png> [armor_overlay.png] [options]

Exemples:
  python3 scripts/generate_overlay.py iron_layer_1.png -o iron_helmet_overlay.png
  python3 scripts/generate_overlay.py leather_layer_1.png leather_layer_2.png -o leather_helmet_overlay.png
  python3 scripts/generate_overlay.py diamond_layer_1.png --size 1024 -o diamond_helmet_overlay.png

Dépendances:
  pip install pillow numpy
"""

import sys
import argparse
from typing import Optional
import numpy as np
from PIL import Image, ImageFilter
from pathlib import Path

# ── Dimensions de référence de la texture d'armure Minecraft (résolution 16) ──
BASE_ARMOR_WIDTH  = 64
BASE_ARMOR_HEIGHT = 32
FACE_OFFSET_X = 8   # Position en x de la face avant dans la texture
FACE_OFFSET_Y = 8   # Position en y de la face avant dans la texture
FACE_SIZE     = 8   # Taille de la face (carré 8×8 en résolution 16)

# ── Paramètres d'overlay (calqués sur l'analyse des overlays existants du mod) ──
DEFAULT_OUTPUT_SIZE = 512
MAX_ALPHA        = 217   # ~85 % d'opacité max — identique aux overlays existants
BLUR_RADIUS_PCT  = 0.0   # 0 = pas de flou, pixels nets (mettre ex. 0.004 pour adoucir légèrement)
VIGNETTE_POWER   = 1.8   # Exposant du dégradé radial (plus élevé = bords plus marqués)


def _load_face(path: str, face_x: int, face_y: int, face_size: int) -> Image.Image:
    img = Image.open(path).convert("RGBA")
    return img.crop((face_x, face_y, face_x + face_size, face_y + face_size))


def generate_overlay(
    armor_path: str,
    armor_overlay_path: Optional[str] = None,
    output_path: str = "overlay.png",
    output_size: int = DEFAULT_OUTPUT_SIZE,
) -> None:
    armor = Image.open(armor_path).convert("RGBA")
    w, h = armor.size

    # Calcul du multiplicateur de résolution
    if w % BASE_ARMOR_WIDTH != 0:
        print(
            f"Erreur : la largeur {w} n'est pas un multiple de {BASE_ARMOR_WIDTH}.",
            file=sys.stderr,
        )
        sys.exit(1)

    mult = w // BASE_ARMOR_WIDTH
    expected_h = BASE_ARMOR_HEIGHT * mult

    if h != expected_h:
        print(
            f"Avertissement : hauteur {h} inattendue (attendu {expected_h} "
            f"pour un multiplicateur ×{mult}). Poursuite quand même.",
            file=sys.stderr,
        )

    face_x    = FACE_OFFSET_X * mult
    face_y    = FACE_OFFSET_Y * mult
    face_size = FACE_SIZE * mult

    print(
        f"Résolution détectée : ×{mult} — "
        f"face avant à ({face_x}, {face_y}), taille {face_size}×{face_size}"
    )

    face = _load_face(armor_path, face_x, face_y, face_size)

    # Composition avec la texture overlay de l'armure si fournie
    if armor_overlay_path:
        face_overlay = _load_face(armor_overlay_path, face_x, face_y, face_size)
        face = Image.alpha_composite(face, face_overlay)

    # ── Mise à l'échelle NEAREST — pixels nets, style pixel art ──
    result = face.resize((output_size, output_size), Image.NEAREST)

    # ── Flou gaussien optionnel (désactivé par défaut) ──
    blur_radius = int(output_size * BLUR_RADIUS_PCT)
    if blur_radius > 0:
        result = result.filter(ImageFilter.GaussianBlur(radius=blur_radius))

    # ── Assombrissement : couleur × 0.25 (teinte préservée) ──
    arr = np.array(result, dtype=np.float32)
    arr[:, :, 0] *= 0.25
    arr[:, :, 1] *= 0.25
    arr[:, :, 2] *= 0.25

    # ── Vignette radiale : centre transparent → bords opaques ──
    y_idx, x_idx = np.mgrid[0:output_size, 0:output_size]
    cx, cy = output_size / 2.0, output_size / 2.0
    dist = np.sqrt((x_idx - cx) ** 2 + (y_idx - cy) ** 2)
    max_dist = np.sqrt(cx ** 2 + cy ** 2)
    vignette = np.clip(dist / max_dist, 0.0, 1.0) ** VIGNETTE_POWER

    # ── Plafond d'opacité + vignette ──
    arr[:, :, 3] = np.minimum(arr[:, :, 3] * vignette, MAX_ALPHA)
    arr = arr.astype(np.uint8)

    out = Image.fromarray(arr)
    out.save(output_path, "PNG")

    # Statistiques pour validation
    alpha = arr[:, :, 3]
    nonzero = alpha[alpha > 0]
    coverage = 100 * len(nonzero) / (output_size ** 2)
    print(
        f"Overlay généré : {output_path}\n"
        f"  Taille        : {output_size}×{output_size}\n"
        f"  Flou          : {blur_radius}px\n"
        f"  Alpha max     : {MAX_ALPHA} (~{MAX_ALPHA/255*100:.0f} %)\n"
        f"  Alpha moyen   : {nonzero.mean():.0f}\n"
        f"  Couverture    : {coverage:.1f} % des pixels non transparents"
    )


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Génère un overlay de casque à partir d'une texture d'armure Minecraft",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=__doc__,
    )
    parser.add_argument("armor", help="Texture d'armure principale (ex: iron_layer_1.png)")
    parser.add_argument(
        "armor_overlay",
        nargs="?",
        help="Texture overlay de l'armure, optionnelle (ex: leather_layer_2.png)",
    )
    parser.add_argument(
        "-o", "--output",
        default="overlay.png",
        help="Fichier PNG de sortie (défaut: overlay.png)",
    )
    parser.add_argument(
        "-s", "--size",
        type=int,
        default=DEFAULT_OUTPUT_SIZE,
        help=f"Taille de l'overlay en pixels (défaut: {DEFAULT_OUTPUT_SIZE})",
    )

    args = parser.parse_args()

    if not Path(args.armor).exists():
        print(f"Erreur : fichier introuvable : {args.armor}", file=sys.stderr)
        sys.exit(1)

    if args.armor_overlay and not Path(args.armor_overlay).exists():
        print(f"Erreur : fichier overlay introuvable : {args.armor_overlay}", file=sys.stderr)
        sys.exit(1)

    generate_overlay(args.armor, args.armor_overlay, args.output, args.size)


if __name__ == "__main__":
    main()
