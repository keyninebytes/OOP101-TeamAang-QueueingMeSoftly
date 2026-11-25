# Queueing Me Softly: logo

**Concept (based on Benzel's sketch).** A front-view TODA tricycle: the sidecar cabin with a green-striped windshield, green grille and green roof on the left, the motorcycle with handlebar and a green "go" headlight on the right. Motion lines trail on the left for smooth dispatching. The pin holds the queue: a row of three slots, green for the unit boarding now, then blue for next, then faded blue for the one after.

## Files
| File | Use |
|---|---|
| `qms-icon.svg` | App icon / favicon master (1024 grid, rounded tile) |
| `qms-lockup.svg` | Horizontal lockup for light backgrounds |
| `qms-lockup-dark.svg` | Horizontal lockup for dark backgrounds |
| `png/qms-icon-{1024,512,180,64,32,16}.png` | Raster icon sizes (180 = Apple touch icon) |
| `qms-favicon.svg`, `png/qms-favicon-{32,16}.png` | Simplified tricycle-only icon for browser tabs, where the pin and motion lines are too small to read |
| `png/qms-lockup@2x.png`, `png/qms-lockup-dark@2x.png` | 1200×320 transparent lockups |
| `qms-preview.png` | Overview sheet |

The lockup SVGs use live text with the app's own font stack (`-apple-system, "SF Pro Display", Inter, …`), so the wordmark matches the UI on each device. The PNGs are rendered in Inter SemiBold.

## Palette
| Name | Hex | Role |
|---|---|---|
| Dispatch Blue | `#0066D6` | Icon tile. Same hue as the app's `--color-primary` (`#0A84FF`), one step deeper so white reaches 5.4:1 contrast (vs 3.6:1 on `#0A84FF`). |
| Go Green | `#30D158` | Head-of-queue roof. Same as the app's `--color-accent`. |
| Ink | `#1D1D1F` | Wordmark on light |
| Secondary ink | `#6E6E73` | Tagline on light |
| Canvas | `#F5F5F7` | Background |

## Small sizes
Use the favicon files at 16–32px. The full icon is meant for 64px and up.
