# Lovelyy design principles

## Palette

| Role | Color | Hex |
|---|---|---|
| Primary | Peach | `#FFB38A` |
| Secondary | Mint | `#9ED9C8` |
| Text | Warm brown | `#4A3B35` |
| Background | Cream | `#FFF8F2` |

Supporting tones (derived): Peach deep `#E8895C` (emphasis), Peach soft `#FFE3D3`,
Mint soft `#DDF2EC`, Brown soft `#8A7A72` (secondary text), Outline `#EADBD0`.

**Font:** Quicksand (rounded, friendly). Bold for titles, Medium for body.

## Principles

1. **Soft and rounded.** Corners 16–24dp (pills for buttons). No sharp edges, no harsh shadows.
2. **The cat is the center.** Home always opens on the cat; everything else is around it.
3. **"Us", not "me".** Both partners are visible together (paired avatars, "Ulaş & Deniz").
4. **One main action per screen.** One big peach button. Everything else is quieter.
5. **Alive, gently.** Micro-animations (breathing, beating heart, press squish) and haptic ticks on taps.
6. **One-hand use.** Main actions live in the lower half of the screen, within thumb reach.
7. **Material 3 underneath.** Standard components, styled with our tokens.

## Rules for code

- Screens use only `MaterialTheme.colorScheme` / `typography` / `shapes`, never raw colors.
  This keeps dark mode a one-file change later (`ui/theme/Theme.kt`).
- Shared building blocks live in `ui/components` (`PrimaryButton`, `SecondaryButton`,
  `LovelyyTextField`, `InitialAvatar`, `BeatingHeart`).
- v1 is light theme only.
