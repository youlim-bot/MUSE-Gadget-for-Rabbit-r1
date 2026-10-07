# Pet special activities

Six activities are available in the pet room under **Special activities** (KO/JA/EN).
The normal Feed/Play/Care/Sleep row stays in the room; it is not duplicated here.

- **Solo exploration:** a healthy, awake, well-fed hatched companion can visit the
  forest (30 minutes), beach (1 hour), or hill (2 hours). The saved trip uses wall
  time across app restarts. Needs continue to decay normally. Return to claim a
  personality-influenced souvenir and a fictional expedition story. Early recall
  gives no reward. Claims are atomic/idempotent. Eggs cannot depart.
- **Muse's diary:** local first-person daily entries reflect recorded care, meals,
  play, conversations, hatching and special activities. No activity is invented
  for days without records. Select a day to review a prompt before discussing it
  with Muse. The latest 400 events and 30 diary days are displayed/stored as noted
  in the UI implementation; this is a bounded local journal, not cloud backup.
- **Memory chest:** first recorded meeting, hatching, trips, missions, promises and
  explicitly reviewed/saved conversation excerpts (up to 1200 characters). No photo
  bytes or credentials are stored in this feature. Recalling with Muse opens the
  normal reviewable composer; it does not silently upload a memory.
- **Decorate room:** place an earned souvenir on the room shelf. Six procedural
  decorations retain the existing avatar art. Ownership and selected decoration
  survive restarts. The avatar reacts; an optional question asks Muse about it.
- **Real-world missions:** choose green object, round object, or flower. The existing
  camera preview/review/send flow is used. Verification uses a fixed target and
  a strict structured Muse result; malformed, false, stale, or duplicate responses
  grant nothing. One reward per local calendar day per companion. No live camera
  upload is triggered by opening the menu. Internet/Muse is required for judgement.
- **Our promises:** up to six daily R1 notifications for Korean/Japanese/English
  conversation or a daily reflection. Scroll hour/minute wheels, then confirm.
  Idle-capable inexact AlarmManager reminders may be delayed by battery saving.
  They recur across app closure, reboot and timezone changes; deletion cancels
  the notification and scheduled alarm. Tap to review/start in the pet room.
  Starting practice keeps its language during the pet conversation; Room/Stop
  voice ends that practice context. Notifications do not auto-send messages or
  start microphone capture. Android notification permission must be enabled.

Storage is isolated by the existing companion seed in `pet_activities`; no growth
counters, hatch deadlines, or evolution formulas are displayed. Welcoming a new
companion retains old local records but cancels its predecessor's promises.
Existing pairing, encrypted API credentials, audio settings and care state are
not reset. Decorations/trips/diary work offline; Muse discussion/photo judgement
requires the existing paired connection.

## Validation

`PetAdventureTest` covers departure guards, timing/clock rollback, deterministic
personality outcomes, daily/DST reminder times, strict image verification, and
scoped activity/practice prompts. `PetActivitiesCheck` runs only on an unpaired
emulator: persistence, exactly-once rewards, recall, per-life isolation, inventory,
mission nonces and daily cap, photo-reply correlation, recurring reminder
schedule/cancel, and native 480x640 pages in three languages. It sends no live
Muse requests. Actual camera-model accuracy is not established by these fixtures.
