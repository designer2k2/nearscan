# Play Store Promo Video — Gemini/Veo Prompts

Image-to-video prompts for generating a Play Store promo video clip by clip. For each one,
attach the named image as the reference/first frame in Gemini (Veo) and paste the prompt.

Play Store promo videos are hosted on YouTube and linked from Play Console — Veo's output
doesn't upload directly, so generate the clips, stitch them (see "Assembling the full video"
below), upload the result to YouTube (unlisted is fine), and paste that URL into the listing.

Veo-generated text is unreliable. If titles/labels come out garbled or blurry, regenerate with
any on-screen text description removed from the prompt (just describe the motion) and overlay
the real text afterward with a simple video editor, rather than fighting the model to render
text correctly.

---

## 1. Feature graphic intro (`docs/store-assets/feature_graphic.png`)

> Animate this app promo graphic into an 8-second looping video for a Google Play Store listing.
> Keep the exact navy (#0D1B2A) background, cyan (#00E5FF) radar icon, and white "NearScan"
> title/tagline text unchanged in style and position — do not redesign or restyle them.
>
> Motion: the radar circle's cyan sweep arm rotates smoothly one full clockwise revolution,
> sweeping continuously like an active radar display. As the sweep passes each concentric ring,
> a small white dot briefly blips into existence at a random point along that ring (like a
> detected signal), fades in, then fades out after about a second. The faint background radar
> rings pulse very subtly outward from the icon, like a slow radar ping — barely visible, not
> distracting. The "NearScan" title and tagline stay static and fully legible the whole time; do
> not let motion blur or distort the text.
>
> Camera: locked-off, no pans or zooms. Lighting: keep it flat and clean, matching the original
> graphic — no added lens flares, particles, or extra decoration beyond the radar sweep and
> signal blips.
>
> Style: minimal, modern flat UI motion graphic, not photorealistic. Output at 1920x1080 (16:9),
> seamless loop.

---

## 2. Live scanning (`storefiles/nearscan-en/01_live_scanning.png`)

> Animate this app screenshot into a 6-second video. Keep every UI element — text, layout,
> colors, the red STOP button — pixel-identical; do not redesign anything.
>
> Motion: the three counter numbers (WiFi, BT, Cell) count upward smoothly and continuously,
> like live numbers ticking as new devices are detected — WiFi and BT incrementing slowly, Cell
> incrementing faster. The "Total records" number ticks upward in sync. The session timer digits
> advance in real time (00:00:05 → 00:00:11). The red STOP button pulses with a very subtle soft
> glow, like an active recording indicator. Nothing else moves — no camera motion, no icon
> changes, status bar stays static.
>
> Style: flat mobile UI screen recording look, not a rendered 3D scene. Output 1080x2340 portrait
> (phone aspect ratio), or crop to whatever the platform requires.

---

## 3. Scan types settings (`storefiles/nearscan-en/03_scan_types.png`)

> Animate this settings screenshot into a 6-second video, keeping all text, labels, and layout
> exactly as shown — no redesign.
>
> Motion: one at a time, each interval slider's small dot handle nudges slightly left then back
> (as if being dragged), and its "1s" label briefly flickers to a different number before
> returning — suggesting live adjustment. The "Capture BLE advertising data" and "Cell" switches
> each toggle off then back on once, smoothly, with the switch thumb sliding and the track color
> fading between grey and cyan. Do this for one switch/slider at a time, in sequence, not all
> simultaneously. No camera movement, no scrolling.
>
> Style: clean flat mobile UI motion graphic, matching a real Android settings screen recording.

---

## 4. Export & MQTT (`storefiles/nearscan-en/04_export_mqtt.png`)

> Animate this settings screenshot into a 6-second video, keeping all text and layout exactly as
> shown.
>
> Motion: the "Export Now" button is pressed (brief scale-down/scale-up tap animation) and its
> label briefly swaps to a small circular progress spinner, then back to "Export Now" — implying
> a quick export completing. Independently, the MQTT toggle's track pulses with a soft cyan glow
> every couple of seconds, like an active live connection heartbeat. The Broker and Topic text
> fields stay static. No scrolling, no camera movement.
>
> Style: flat mobile UI motion graphic, matching a real screen recording.

---

## 5. Set Location dialog (`storefiles/nearscan-en/05_set_location.png`)

> Animate this dialog screenshot into a 5-second video, keeping the dialog layout and text
> exactly as shown.
>
> Motion: the "Get GPS Fix" button is tapped (brief press animation), then the Latitude,
> Longitude, and Altitude number fields count up digit-by-digit as if a GPS fix is being
> acquired, settling on their final values. An "Accuracy: 20 m" line fades in below the button
> right as the numbers settle. No other motion — background stays static, no camera movement.
>
> Style: flat mobile UI motion graphic, matching a real screen recording.

---

## Assembling the full video

Suggested cut order, ~30 seconds total (the sweet spot for a Play Store promo video):

1. Feature graphic intro (hook)
2. Live scanning (proof it works)
3. Scan types (configurability)
4. Export & MQTT (power feature)
5. Set Location (ease of setup)
6. Back to feature graphic, or a static end card with the Play Store icon

Upload the stitched result to YouTube (unlisted is fine) and link it from Play Console →
Store listing → Promo video.
