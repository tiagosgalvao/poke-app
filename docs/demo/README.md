# Demo recordings

The demo, in three narrated chapters. Each one has a thumbnail (`<chapter>.png`, a frame from the video) that the main README links to the video. Balloons explain each step next to the element in use, which is outlined, and a cursor dot shows every click. They are recorded by the Playwright `demo` project in [e2e/demo/](../../e2e/demo) as MP4 (H.264), so they play in QuickTime, any browser and GitHub.

| Chapter | Length | What it shows |
|---|---|---|
| [01-catalog-and-details.mp4](01-catalog-and-details.mp4) | ~1 min | US01 catalog cards and pagination; US02 detail with stats, description and Eevee's branching evolution chain |
| [02-accounts-and-protected-routes.mp4](02-accounts-and-protected-routes.mp4) | ~1 min | Visitor menu, redirect to login and back, wrong password, log in and log out, sign up |
| [03-my-pokedex.mp4](03-my-pokedex.mp4) | ~2 min | US03 sync and add from the detail page; US04 edit with validation, stale-edit conflict, delete |

To record them again, from the repo root:

```bash
docker compose up -d --build
cd e2e && npm run demo     # needs ffmpeg (brew install ffmpeg)
```

After re-recording, refresh the thumbnails (a frame at 12 s, 20 s and 82 s):

```bash
cd docs/demo
ffmpeg -y -ss 12 -i 01-catalog-and-details.mp4 -frames:v 1 -update 1 -vf scale=640:-1 01-catalog-and-details.png
ffmpeg -y -ss 20 -i 02-accounts-and-protected-routes.mp4 -frames:v 1 -update 1 -vf scale=640:-1 02-accounts-and-protected-routes.png
ffmpeg -y -ss 82 -i 03-my-pokedex.mp4 -frames:v 1 -update 1 -vf scale=640:-1 03-my-pokedex.png
```

How the chapters and the narration work is described in [E2E-TEST-PLAN.md](../E2E-TEST-PLAN.md#demo-chapters-and-recordings).
