# Growth visuals, elapsed time and local sound pack

The pet home displays calendar time since the current life began: years, months,
days and whole hours. The value follows the display language (KO/JA/EN) and includes
time while the app is closed. This is time together, not a hatch countdown.
An archived new-egg timestamp is used to migrate an existing life exactly. If an
older file only permits an inferred start, the UI marks it as approximate. Life
state, seed, care, pairing and API credentials are not reset. New eggs explicitly
record their start timestamp; death freezes the shared-time display at that event.

Six transparent sprites cover the egg, baby, child, growing teenager, adult and
old friend. Every character stage has one continuous rounded lower body, without
feet or legs. The baby is round with a large face, the child is a compact bell
shape, the teenager has a taller tapered body, and the old friend has a broader
body and gentle eyelids. The adult remains the unmodified original Muse image.
The child, teenager and old friend were regenerated from that original after
the owner rejected all footed variants; earlier footed drafts are not app assets.
Blink landmarks match each silhouette; child/teen/elder chest
accents stay away from their faces. Adult form emblems float beside the original
body. Growth still follows existing life rules. Adult care
outcomes retain their color/emblem accents. Body deformation, movement, blink
landmarks and egg cracking remain procedural animations. Only the current stage
is decoded, at a reduced size, to limit memory use.

Five new original 24-second loops accompany growth: Little lullaby, Sprout stroll,
Sunny playtime, Meadow afternoon and Starlight friend. They are selectable beside
the existing forest/music-box/night-garden choices; Follow growth selects the
stage track and sleep selects Night garden. The generator is
`scripts/make-pet-growth-audio.py` (Python + NumPy). Six new effects cover hatching,
planting, watering, blooming, learning and catching fish. No song or recording
samples were used to compose these mathematical-tone assets.

Twelve locally synthesized prototype voice clips cover greeting, thanks, sleep
and praise in Korean, Japanese and English. They are macOS speech synthesis
outputs, not recordings or clones of an official Muse voice. They are separately
identified from the project-composed music/effects. The full-screen pet music panel offers a scrollable track list and fixed
effect/short-voice switches. Local voice prototypes can be recreated with
`scripts/make-pet-prototype-voices.py` on a Mac with the installed system voices
and ffmpeg. The existing reply mute button cancels a clip;
conversation playback, microphone capture, screen/window loss and audio focus loss
stop game audio. A six-second clip cooldown prevents repeated chatter. Existing
ElevenLabs settings for normal conversation are unchanged.

The pack and APK are local build artifacts. The owner authorized a preservation-safe
Rabbit r1 update after reviewing the revised stage art. Build and emulator results
are recorded in the development log; device installation is reported separately.
That earlier installation predates the owner's rejection of all footed characters.
The current child/teen/elder replacement was verified locally and on an unpaired
emulator. The private, same-package build was then installed on the owner's
explicit request with adb install -r. Signing identity and installed APK hash
matched; 26 protected user-data files were unchanged across installation, and
credentials were unchanged after launch. Muse reached ready and the enabled
side-button service remained bound. No life-state code, pairing code or audio
assets changed. No GitHub commit/push or release publication was requested for
either revision.
