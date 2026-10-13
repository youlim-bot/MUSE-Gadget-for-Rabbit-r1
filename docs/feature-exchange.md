# Pet feature exchange: ESP32 and Muse r1

Source review: 2026-10-08; Android dialogue follow-up: 2026-10-09. This
is a source snapshot of an exchange with a separate ESP32 implementation. The
ESP32 sources are outside this public Android tree; C filenames below identify
the reviewed implementation, not files bundled here. The C11 life core was
ported from Android's MIT-licensed life model; its separate provenance and
license records must accompany any redistribution of that core.

At the review checkpoint, the C implementation also had storage/time adapters,
stage sprites, a Korean LVGL page and a basic memory game. The comparison below
is dated intent and source evidence, not a claim of current ESP32 delivery or
physical hardware verification. The glossary in [CONTEXT](../CONTEXT.md) defines
terminology, not delivery status.

## ESP32 -> R1

### Adopted in this change

| Behavior and C source | Android change | JVM regression |
| --- | --- | --- |
| Validate finite needs/traits and bounded times/counters before replacing a save (`pet_core.c:valid`, `pet_encode`, `pet_decode`). | Added [PetRecord.kt](../app/src/main/java/dev/cameronpak/muser1/PetRecord.kt) and routed [PetStore.kt](../app/src/main/java/dev/cameronpak/muser1/PetStore.kt) through it. Needs/warmth are finite 0–100, traits 0–1e9, times/durations 0–100000000000000 ms, bond 0–100, egg care 0–1000, diet uses only six food bits, meal/treat counts are nonnegative signed integers. Encoding checks the complete state before preferences are edited. | `PetRecordTest.nonFiniteNeedsAndTraitsUseDefaultsAndFiniteValuesAreBounded`, `oversizedCountersTimesAndUnknownFormLoadSafelyWithoutCreatingNewLife`, `invalidSaveIsRejectedBeforeReplacingPreviousRecord`, `malformedJsonFailsWithoutInventingReplacementState`. |
| Add at most the remaining incubation before reaching the hatch cap (`pet_core.c:pet_advance`), rather than add an arbitrarily large interval and clamp afterward. | [PetState.kt](../app/src/main/java/dev/cameronpak/muser1/PetState.kt) bounds the old incubation and adds only the remainder. Huge positive absence cannot overflow incubation into a negative number. The egg still waits for the owner's first meeting. | `PetCoreExchangeTest.incubationAddsOnlyRemainingTimeBeforeCapping`; `PetRecordTest.oldReadyEggAboveDurationStillLoadsAndWaitsForExplicitHatch`. |
| Keep persisted quantities within the save validator's domain (`pet_core.c:valid`). | Android extends that principle: trait rewards saturate at 1e9, meals/treats at `Int.MAX_VALUE`, instead of wrapping counters or producing a subsequently unsavable trait. These saturation rules are an Android extension, not a claim that C already saturates its meal counters. | `PetRecordTest.actionAtMaximumCountersAndTraitsRemainsSerializable`. |
| Validate duration inputs before doing derived time arithmetic (`pet_core.c:valid`, `pet_clock.c` timestamp handling). | [PetElapsed.kt](../app/src/main/java/dev/cameronpak/muser1/PetElapsed.kt) subtracts each inferred legacy duration only up to the remaining timestamp. Corrupt large offsets cannot wrap into a plausible positive start; an impossible inference is marked approximate. This is an Android-specific application of the C boundary checks. | `PetCoreExchangeTest.legacyStartInferenceCannotOverflowToPlausiblePositiveStart`. |

Compatibility: the preference file `muse_pet`, key `life_v2`, field names and JSON
representation are unchanged. No binary format, new version or destructive reset
is introduced. Valid saved identity (including both signed seed extremes), needs,
care, growth, form, sleep/death and estimated-start flags round-trip unchanged.
Missing start fields still restore from the latest matching `new_egg` archive,
otherwise from elapsed incubation/age; the estimate survives the next save.
Tests: `PetRecordTest.existingLifeV2RoundTripRetainsAllSavedFieldsAndSignedSeed`
and `missingLegacyStartUsesResetOrEstimateWithoutChangingIdentityAndCare`.
The previous pre-`life_v2` migration/archive and stage mapping remain in PetStore;
its legacy bond is now bounded too.

Deliberate Android compatibility policy: valid JSON with absent or damaged numeric
fields is repaired in memory, using existing defaults for nonfinite needs/traits,
bounds for counters/times, low six bits for diet and UNDECIDED for unknown form.
An old ready egg above its seed-specific duration is capped, retaining its identity
and readiness. Loading an existing JSON record itself performs no write; a later
normal save persists the repaired values. Malformed JSON still raises a parse
failure and does not replace the stored bytes or silently create a new pet.
A friendly corrupt-record recovery UI is outside this change.

### Follow-up dialogue exchange (2026-10-09)

Adopted items 1–3 from the read-only C core's `FEATURE-EXCHANGE.md`:

| ESP32 behavior | Android adoption | JVM regression |
| --- | --- | --- |
| Trim typed-question prompts like voice-note prompts (`pet_dialogue_prompt`). | `PetDialogue.prompt` trims the shared template before appending the JSON-quoted question. The leading-indentation bug reproduced on the JVM before the fix. Persona, state, activity facts and question quoting are unchanged. | `PetDialogueTest.typedQuestionPromptMatchesTrimmedVoiceLayout` checks KO/JA/EN and multiline quoted questions; red before / green after. |
| Reject duplicate reply/action keys and text after an object (`pet_json_object`). | A top-level `JSONTokener` scan rejects duplicate decoded `reply`/`action` names and any non-whitespace suffix, including another object or comment. This scan is needed because Android overwrites duplicate keys while the JVM org.json dependency rejects them. The existing localized retry fallback is retained. | `ambiguousStructuredRepliesUseLocalizedRetry`, `validJsonRepliesKeepEscapesNestedValuesAndWhitespace`. |
| Keep broken Unicode and raw JSON controls out of display/speech (`pet_json.c`). | Raw text and the decoded reply are checked for unpaired UTF-16 surrogates, NUL and other non-layout control characters. Raw controls inside JSON strings are rejected. Valid emoji pairs and normal tab/newline/carriage-return layout in prose or escaped JSON remain readable. Android takes a Kotlin String; this does not claim byte-level UTF-8 validation. | `brokenUnicodeAndControlsNeverReachReplyText`, `rawControlsInsideJsonStringsUseRetry`, `validSurrogatePairsAndProseLayoutRemainReadable`. |

Six dialogue regressions cover prompt layout, duplicate keys/trailing text,
valid escapes, broken Unicode/control characters and valid emoji/layout. Items
4–7 from the separate exchange proposal (stream/input limits, cue validation and
timezone injection) remain deferred. Expression guards, learned cues, pairing
and transport code are unchanged by this dialogue follow-up. See the update
caveat in [Development](development.md#physical-r1-update-caveat) before an
owner-authorized installation. This public-tree port performs no installation
or live Muse turn.

### Already present; verified rather than reimplemented

- Backward/equal `advance` timestamps do nothing, dead pets stop at death, and
  foreground ticks match offline catch-up. Five-minute residual time remains in
  `updatedAt` across JSON save/load. New regression:
  `PetCoreExchangeTest.fractionalStepSurvivesSaveLoadAndRollbackWithoutReplay`;
  existing `PetStateTest.foregroundTicksAndOfflineCatchupAgreeExactly` and
  `rollbackCannotReplayTimeAndHugeAbsenceTerminates`.
- An unopened egg never becomes a neglected newborn while the app is closed.
  `hatch` is a separate model operation; the visible R1 room calls it after a
  2.6-second first-meeting animation (immediately with reduced motion), not after
  a separate hatch button. Existing `PetLifecycleTest.readyEggWaitsForFirstMeetingBeforeNewbornNeedsStart`.
- Care advances first, then checks the new needs, sleep, illness and death.
  A rejected action must still return/save any elapsed simulation changes.
  New `PetCoreExchangeTest.guardsUseAdvancedNeedsRatherThanStaleSnapshot` tests
  food crossing the fullness threshold, energy becoming too low, emerging illness
  and automatic waking. `failedCareStillReturnsElapsedChangesAndCannotResurrectDeath`
  covers all care entry points at death and persistence of that elapsed result.

### Deliberately not adopted

- `MUSPET2`/272-byte CRC records or 296-byte board checkpoints: incompatible with
  existing Android JSON/preferences and its start-estimate migration. Android's
  save boundary uses field validation; no new checksum is claimed.
- Rejecting every noncanonical numeric JSON field as a whole record: Android's
  older records already use defaults/clamps. Repairing recoverable fields keeps
  those pets loadable; failed parsing does not overwrite the original record.
- ESP32 uptime/SNTP rebasing and NVS-specific writes: the Android app already uses
  epoch wall time and Android preferences. Changing the time basis would require
  rebasing all care timestamps, archives and per-companion activities together.
- Plain-text, Korean-only prompts: R1 already uses selected-language JSON replies
  with a closed animation set and streamed-turn buffering; downgrading would lose
  its expression and learned-cue contract.
- LVGL menu/restart behavior: R1 has native Android controls and archives the old
  companion on a confirmed restart. Replacing that with the board's single-life
  reset would discard history. No transport/pairing/license files were changed.
- New clock tamper protection: both cores ignore backward life advancement but
  remain offline games using caller-supplied time. They do not prove secure time,
  prevent forward clock edits or uniformly reject every backward care call.

## R1 -> ESP32

### Feeding rule update (2026-10-13; Android only until the C owner ports it)

`PetState.feedResult` advances life first, then reports `DEAD`, `EGG`,
`SLEEPING`, or `FULL` in that priority order. `FULL_THRESHOLD` is **95.0**:
an awake, living, hatched pet can eat whenever its advanced food is below 95,
even just after another meal. The old food >85 guard and 30-minute feeding
cooldown are removed. `fedAt` remains the last accepted meal timestamp for
save compatibility; comparing it cannot detect two accepted meals at the same
timestamp. Use the explicit accepted/refusal result for UI, audio and care events.
Rejected meals retain elapsed changes and grant no meal/diet/trait/item reward.

Food still caps at 100. All six foods add at least 12, so at most eight meals
can be accepted from empty at one timestamp (cake: 0 -> 12 -> ... -> 96).
Cake retains -5 health, +12 mood and its treat counter on each accepted meal;
meal/treat counters and traits retain saturation. Wardrobe `bow` unlocks once,
never overwrites its first timestamp and never equips itself. `care(FEED)` uses
the same porridge feeding rule for hatched pets; egg care keeps its existing
warm/nurture semantics, rather than giving an egg a meal.

R1 exchange vectors: `PetCoreExchangeTest.feedingVectorsUseFullnessRatherThanLastMealTime`
uses cake at food 70 with `fedAt=now`: 82 -> 94 -> 100 -> FULL at the same time.
It also covers sleeping/egg/dead reasons, automatic wake, and rejected care
preserving the advanced timestamp. The advanced-needs threshold vector is now
95.1 -> 94.85 in one five-minute awake step, which accepts fish. Repeated-cake,
care-path, 94.999/95 boundary and save/reload tests are in PetStateTest and
PetRecordTest. No `esp32/` files are changed by this R1 task.

Priority is implementation order, not a claim these items have been ported.
P0 establishes dialogue/language/time contracts, P1 adds offline local activities,
P2 needs larger storage or platform/network services. Persist each activity by the
signed life seed, separate from credentials and the life record. A new egg gets a
new namespace; old records are retained. Daily gates use the selected device's
local calendar date; elapsed rounds use monotonic time. The following rules are
implementation specifications derived from the named Kotlin functions.

### P0 — JSON expression protocol and exactly-once turn reactions

Sources: [PetDialogue.kt](../app/src/main/java/dev/cameronpak/muser1/PetDialogue.kt)
(`prompt`, `parse`, `allowed`, `PetDialogueTurn.receive`),
[PetRoom.kt](../app/src/main/java/dev/cameronpak/muser1/PetRoom.kt)
(`activityContext`, `answerExpression`). ESP32 currently builds plain-text prompts.

1. Ask for one JSON object with string fields `reply` and `action`; actions are
   `none`, `wave`, `dance`, `jump`, `nod`, `shake`, `cheer`, `think`. Select at most
   one visual reaction; the output can never feed, heal, reset or perform an
   external action. Include actual qualitative needs, personality, sleep/illness,
   expedition and activity facts; prohibit invented care and growth/hatch formulas.
2. Buffer streaming chunks by reply ID. Do not show or speak JSON fragments.
   On completion, use nonempty completion text as the final message, otherwise
   the accumulated chunks. Ignore repeated completion for the same ID.
3. Strip surrounding JSON code fences; accept nonblank string `reply`, strip `**`,
   and case-insensitively match the action enum. Unknown actions become `none`.
   Ordinary nonstructured prose may pass through with `none`; malformed structured
   output uses a localized retry message and must not be read aloud as JSON.
4. Across all reply IDs in a turn, allow only the first non-NONE expression.
   A learned exact cue overrides the model-selected action, then apply state
   guards: dead/asleep -> none; egg -> nod for any non-none; ill/energy below 20
   -> nod instead of dance/jump/cheer. Preserve other gentle reactions.
   Reset buffers/deduplication at the next turn, never change life through parsing.

### P0 — KO/JA/EN display selection

Sources: [UiText.kt](../app/src/main/java/dev/cameronpak/muser1/UiText.kt)
(`DisplayLanguage`, `language`, `set`, `text`),
[PetWorldText.kt](../app/src/main/java/dev/cameronpak/muser1/PetWorldText.kt),
[PetActivityText.kt](../app/src/main/java/dev/cameronpak/muser1/PetActivityText.kt),
`PetDialogue.prompt`. ESP32's current LVGL pet labels are Korean literals.

Persist one selection, KO/JA/EN, default KO; unknown stored values fall back to KO.
Select every label, prompt and retry message from a three-language table. Pet
conversation defaults to the selected language, allowing an explicit user request
to override that turn. This display setting is independent of interpreter source/
target languages. Bundle readable Korean and Japanese font coverage and localize
new activity screens too; switching languages must leave life/activity records intact.

### P0 — Calendar elapsed display and start migration

Sources: [PetElapsed.kt](../app/src/main/java/dev/cameronpak/muser1/PetElapsed.kt)
(`between`, `restoreStart`, `text`), `PetRecord.decode`, `PetRoom.render`.
ESP32 currently shows whole days/hours from `updated - started`, omitting residual
step time, calendar months/years and an estimated-start marker.

Persist the start of this life across every care, hatch, growth and restart load.
Prefer a positive stored start, then the latest reset archive at/before updated,
then bounded `updated - incubation - age`. Infer an approximate start for a
hatched/capped egg or an impossible subtraction; preserve that flag on resave.
For display, use wall-clock now while alive, `diedAt` while dead. If start is
unknown or now precedes it, show zero. In the local timezone, consume whole calendar
years, then months (end-of-month and leap-day clamping), then calendar days, then
whole elapsed hours; omit minutes. Thus Jan 31 -> Feb 29 is one month in a leap
year and a DST day is one local day, not necessarily 24 hours. Render all four
units and an approximate marker (`약 ` / `約` / `~`). Never replace this row with
hatching countdowns, growth gates or action counts.

### P1 — Three-planter garden with atomic collection

Sources: [PetWorld.kt](../app/src/main/java/dev/cameronpak/muser1/PetWorld.kt)
(`GardenPlant`), [PetWorldStore.kt](../app/src/main/java/dev/cameronpak/muser1/PetWorldStore.kt)
(`garden`, `plant`, `water`, `collect`, `clearWilted`),
[PetWorldActivities.kt](../app/src/main/java/dev/cameronpak/muser1/PetWorldActivities.kt)
(`garden`). No corresponding garden exists in the reviewed C port.

Each companion has exactly three nullable slots and a unique collection of flower
IDs 0–3. Plant one of three kinds into an empty slot, with a random plant seed,
planted/observed/watered timestamps, age 0, health 85 and today's local day in a
set of care days. Advance only forward: add elapsed wall time to age, cap at
30 days; after 24 hours without water subtract 22 health per dry day (fractional
intervals count), floor at zero. Compute only the newly accrued dry interval so
frequent reads agree with one catch-up. Water only after six hours since the last
water, never under a backward timestamp or after wilting; add 14 health capped
at 100, reset watered time and add today's day once. Growth stages: initial,
sprout at six hours, growing at one day, blooming after three days AND three
separate care dates; health zero overrides all stages as wilted. Flower ID is
floor-modulo `(plant_seed + kind + sum(first three sorted care dates))` by four.
Collect a blooming plant by atomically clearing its slot and adding its flower to
the unique collection. Repeat collection must return no reward, including after
restart. Wilted slots require explicit confirmation to clear. Garden operations
do not directly change pet needs; the R1 garden UI disables mutation for dead pets.

### P1 — Learned tricks and exact custom cues

Sources: `PetWorld.kt` (`LearnedSkill`, `PetSkills`), `PetWorldStore.kt`
(`practice`, `setPhrase`, `context`), `PetWorldActivities.kt` (`training`),
`PetDialogueTurn.receive`. No corresponding skills exist in the reviewed C port.

Keep three skill records: 0 wave, 1 dance, 2 jump. A practice records at most one
local date per skill, refuses time before that skill's last practice, and becomes
learned after three distinct dates. Require a hatched, awake, living, healthy pet;
no expedition may be active. R1's practice UI additionally requires energy >=10.
A cue is trimmed, at most 60 UTF-16 code units on R1; blank means the three defaults:
wave `인사해` / `あいさつして` / `say hello`; dance `춤춰` / `踊って` / `dance`;
jump `점프해` / `ジャンプして` / `jump`. Normalize by trimming, locale-independent
lowercase, removing trailing `. ! ? 。 ！ ？`, then trimming again. Reject a
nonblank cue if it equals any active cue of another skill after normalization.
Require normalized equality of the entire question, not substring recognition.
Expose only learned cues in the per-turn context; render via the expression guards
above. Do not promise ASR accuracy from this local matching rule.

### P1 — Seven daily branching story chapters

Sources: `PetWorld.kt` (`PetStory`, `StoryChoice`), `PetWorldStore.kt`
(`story`, `choose`), `PetWorldActivities.kt` (`story`), `PetWorldText.kt`
(`chapter`, `choice`, `outcome`, `ending`). No story records exist in the C port.

Store an ordered list `{chapter, choice, at}` with chapter 0–6 and choice 0/1.
The first choice is immediately available; subsequent choices require both
`now >= last.at` and a later local calendar date. Reject invalid choice values and
any eighth chapter. Append `chapter = current count` and save atomically before
rendering the outcome. Route is zero when zero-choices are at least as numerous
as one-choices (including ties/empty history), otherwise one. Display the next
chapter's localized route-specific text, two options, then its chosen outcome;
after chapter seven show the ending. Keep a reread history. Dialogue receives
only saved fictional choices; no model output advances the story. The R1 UI
blocks choices for dead companions and hides numeric growth/learning formulas.
Copy the actual KO/JA/EN content table rather than inventing an identical story.

### P1 — Playable star/rhythm games and wheel pond fishing

Sources: `PetRoom.kt` (`startGame`, `catchStar`, `updateGame`, `finishGame`,
`petKey`), `PetWorld.kt` (`WheelFishing`), `PetWorldActivities.kt` (`games`,
`fishing`), `PetActivities.kt` (`key`). The C core has all three reward enums,
but its reviewed LVGL page plays only a basic deterministic memory game.

Use three selectable lanes, wheel movement and a released side-button tap to
select. A side-button hold plus wheel claims the press for volume and cannot
also select/send voice on release. Use monotonic round time; completed wins AND
losses invoke one guarded play reward, cancellation invokes none.

- Star catch: 30-second round; a star changes to another random lane every
  1.5 seconds and immediately after a catch. Five catches win, five incorrect
  selections lose; timeout loses.
- Memory: choose a random three-lane sequence, show each lane for 800 ms of a
  1.2-second slot; after the 3.6-second demo accept three selections in order.
  First wrong selection loses; all three correct win; timeout at 30 seconds.
- Rhythm: require middle lane during phase 650–1100 ms of each 1.6-second cycle,
  and at least 650 ms since an accepted tap; five catches win, five misses lose,
  timeout at 30 seconds.
- Pond fishing: initial lane 1; wrap wheel movement modulo three. Fish lane is
  floor `(elapsed / 2400) mod 3`; biting interval is phase 900–1750 ms inclusive.
  Correct lane plus bite increments catches at most once per beat. A wrong cast
  increments misses; repeat casts in an already caught beat do nothing.
  Five catches win, five misses or 30 seconds end a loss. Ignore pre-start casts.
  Closing, losing page focus or screen-off cancels the round without reward.
  R1 routes the completed pond result through the STARS play-reward type.

### P1 — Growth BGM, effects and short local character voice

Sources: [PetSoundtrack.kt](../app/src/main/java/dev/cameronpak/muser1/PetSoundtrack.kt),
[PetAudio.kt](../app/src/main/java/dev/cameronpak/muser1/PetAudio.kt),
`PetRoom.tick`, [growth asset notes](pet-growth-assets.md).
The reviewed ESP32 pet page has sprites but no equivalent pet soundtrack policy.

Persist music mode separately from life: auto, forest, musicbox, garden, lullaby,
sprout, sunny, meadow, starlight, off. In auto choose garden while asleep;
otherwise levels 0/1 lullaby, 2 sprout, 3 sunny, 4 meadow, 5 starlight. Manual mode
overrides growth selection; off disables BGM only. Keep separate effects and
character-voice switches, both default on. Loop BGM at nominal gain .20; effects
at .35; short character clips at .65 and at least six seconds apart. Bundle
hello/thanks/sleep/praise in KO/JA/EN; these local prototype clips are separate
from Muse reply speech. Use hatch/level/win and plant/water/bloom/learn/catch events
for corresponding clips/effects. Stop pet audio for death, recording, pending Muse
requests/reply speech, loss of window/audio focus and room closure; release
resources on closure. Effects/voice can work with BGM off when focus is available.
Actual board speaker/I2S mixing and acoustic quality require separate validation.

### P2 — Solo exploration, diary, memory chest and decorations

Sources: [PetAdventure.kt](../app/src/main/java/dev/cameronpak/muser1/PetAdventure.kt),
[PetActivitiesStore.kt](../app/src/main/java/dev/cameronpak/muser1/PetActivitiesStore.kt)
(`begin`, `claim`, `event`, `memories`, `decorate`),
[PetActivities.kt](../app/src/main/java/dev/cameronpak/muser1/PetActivities.kt)
(`explore`, `diary`, `memories`, `decorate`), `PetRoom.refreshActivities`.
No corresponding activity state exists in the reviewed C port.

A trip requires hatched/alive/awake/not ill, food and energy >=40, no existing
trip. Forest/beach/hill take 30/60/120 wall-clock minutes; persist a unique trip
ID, place, start, due and departure temperament. Needs continue during absence;
block room care while away. A return needs `now >= start` and `now >= due`.
Compute souvenir with nonnegative floor-modulo: base `floorMod(seed XOR start,3)`;
affinity CURIOUS=2, PLAYFUL=1, AFFECTIONATE=3, otherwise 0; item
`(place*2 + base + affinity) mod 6`. A dead return awards no item. Claim must
atomically clear the trip, add a return event and add the unique item. Recall
early clears the trip without reward. Decorate only with an owned item or -1
(clear decoration).

Keep at most 400 local meaningful events per companion, values at most 1200
UTF-16 code units. Suppress a duplicate last kind/value within 0–60 seconds.
Diary groups by local date and shows the latest 30 days. Memory chest filters
welcome/trip/memory/mission/promise/hatch, newest first, displays up to 80 entries.
Saving the latest conversation requires a content review and explicit save;
discussing a selected memory explicitly sends that text to Muse. Do not silently
export diary/memory content or put it into firmware logs.

### P2 — Private photo discovery album

Sources: `PetWorldStore.kt` (`addPhoto`, `photoReply`, `deletePhoto`),
`PetWorldActivities.kt` (`discoveries`, `discovery`),
[MainActivity.kt](../app/src/main/java/dev/cameronpak/muser1/MainActivity.kt)
(`petDiscoveryCamera`, `petDiscoveryQuestion`, `sendPendingPhoto`, `receiveReply`),
[PhotoQuestion.kt](../app/src/main/java/dev/cameronpak/muser1/PhotoQuestion.kt),
[CameraActivity.kt](../app/src/main/java/dev/cameronpak/muser1/CameraActivity.kt).
The ESP32 core has no album or capture/review storage flow; availability of a
board camera elsewhere does not implement this feature.

Support three discovery categories and at most 24 photos per companion. Every
record has UUID, kind 0–2, capture/save time, reviewed question and eventual reply.
Review photo/question before the user explicitly sends; save the private album
entry at that send boundary before network upload. A failed upload may therefore
leave a photo with a pending explanation. No automatic retry. Store a JPEG only
if size is 4–180000 bytes and SOI/EOI markers match; restrict filename to a 36-char
lowercase hex/hyphen ID. Write a temporary file then rename before committing
metadata, and remove the new file if metadata saving fails. Duplicate ID succeeds
idempotently without consuming another slot; question is bounded to 1000 UTF-16
units, first nonblank correlated reply to 2400 with `**` removed. Check the active
life seed and submitted turn/token correlation; stale/duplicate replies cannot
attach to a different photo or a new life. Deleting requires confirmation,
removes metadata then the private JPEG; a late reply must not recreate it.
A follow-up uses the selected saved photo after explicit user review. Label
Muse descriptions as model answers requiring owner judgement; no recognition
accuracy is implied by storage tests.

### P2 — Photo missions and recurring promises

Sources: [PetMission.kt](../app/src/main/java/dev/cameronpak/muser1/PetMission.kt),
[PetPromises.kt](../app/src/main/java/dev/cameronpak/muser1/PetPromises.kt),
`PetActivitiesStore.completeMission`, `PetActivities.mission`/`promises`,
`MainActivity.receiveReply`. These need camera/transport or platform scheduling,
not just the standalone life core.

Missions select green object / round object / flower, attach a unique active
mission token and reviewed photo. Request a boolean `matched` and nonblank string
`reply`; only one complete JSON object is accepted (quoted braces are text),
input <=16000 UTF-16 units. Ambiguous visual evidence or invalid verdict earns
nothing. Positive completion needs the same active token/life and no reward on
that local date; atomically consume the token, clear target, record date/event
and grant item `target+3`. The explanation alone cannot grant a reward.

Promises store UUID, life seed, kind, local hour/minute, next due and pending flag.
Compute the next strictly future occurrence, repeating next day at the same local
clock time. On fire, reject another life/dead pet, mark pending and schedule the
next occurrence; restoring after restart/time/timezone change reschedules from
stored records. Android uses inexact idle-allowed alarms and a private notification
that opens that specific promise. A board port needs its own equivalent scheduler
and owner-visible prompt; it cannot claim Android notification behavior. Do not
let a Muse reply create or complete either activity without local controls.

## Local verification for this Android change

Build and JVM verification for this public-tree port use Java 17 and an Android
SDK configured through standard environment variables:

```sh
./gradlew :app:assembleDebug :app:assembleDemo :app:assembleDebugAndroidTest \
  :app:testDebugUnitTest
```

On 2026-10-09 this command succeeded in the public tree: debug and demo APKs,
the AndroidTest APK, and **183 JVM tests across 34 suites**, with zero failures,
errors or skips. Instrumentation was assembled only, not executed. The demo
build type has no separate JVM unit-test task in this Gradle configuration.

The ready-egg Android instrumentation fixture uses the seed-specific hatch
duration so it meets the save validator. AndroidTest assembly compiles/packages
the fixture without executing it. Local builds and JVM tests do not establish
physical installation, microphone/camera operation, live model compliance,
acoustic quality or clean Android lint.

## R1 -> ESP32: wardrobe

The local `life_v2` record adds an optional `wardrobe` object: `{version:1,
unlocked:{item_id:epoch_ms}, equipped:{slot:item_id}}`. Missing wardrobe data means
empty collections; retain life identity, needs, dates and all existing care fields.
Preserve unknown future IDs/slots on round-trip and render only known, unlocked
items in their matching slot. Slots are `HEAD`, `NECK`, `BODY`, `FACE`; each holds
at most one item, with removal deleting only that slot. Eggs and memorials do not
wear items. All rewards are local and idempotent; never let agent text grant them.

IDs/rules: `beanie` on hatching, `flowers` as a child, `sweater` as a teen,
`sunglasses` as an adult; `bow` after an accepted first meal, `scarf` after an
accepted bath, `glasses` after a memory-game win, `raincoat` after an accepted
star/rhythm/fishing win. Play and bath cooldowns apply; feeding uses the fullness rule above. Loss, cancellation and rejected
care grant no game/item reward. Legacy migration can infer meal/bath/growth
rewards from saved facts, but never infer a win from `playedAt` or traits.
Unlock timestamps are written once; new rewards never equip themselves.

Art remains separate from `pet_*.png`, including the original adult PNG. The
R1 has six shared head/neck/face overlays and ten body PNGs (two garments × five
current growth shapes). Body overlay canvases use the alpha-cropped stage's
aspect ratio and its full normalized bounds; draw them with the same 12×12 mesh,
body transform and sway as the base. Shared face overlays use the two transformed
`PetArt` eye centers; head/neck use per-stage fractions of the cropped sprite.
Body rectangles (left,top,right,bottom): baby (.16,.62,.84,.965), child
(.12,.62,.88,.965), teen (.12,.48,.88,.965), adult (.12,.47,.88,.965), elder
(.18,.53,.82,.965). Neck Y: .67/.68/.55/.55/.59. Draw body, neck, head, face after
blink masking, so transparent spectacle lenses reveal the animated eyes. ESP32
can render equivalent transparent layers with the same anchors; removing all
slots must reveal the unchanged base. R1 captures are emulator evidence, not
ESP32 implementation or hardware verification.
