#!/bin/bash
# Renders all decks to slides/out/*.html; with --pdf also to slides/out/*.pdf (needs Chrome/Chromium,
# or point CHROME_PATH to one, e.g. Playwright's).
set -e
cd "$(dirname "$0")"
mkdir -p slides/out
MARP="npx -y @marp-team/marp-cli@4.5.1 --theme-set slides/theme.css --html"
render() {   # render <deck.md> <name>
  $MARP "$1" -o "slides/out/$2.html" < /dev/null
  if [ "$PDF" = 1 ]; then $MARP "$1" --pdf -o "slides/out/$2.pdf" < /dev/null; fi
}
PDF=0; [ "${1:-}" = "--pdf" ] && PDF=1
render slides/workshop.md workshop         # the single 30-minute deck
render slides/00-intro.md 00-intro
for dir in 0[1-8]-*/; do
  render "$dir/slides.md" "${dir%/}"
done
