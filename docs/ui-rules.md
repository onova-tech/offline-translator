# UI rules

Every screen of PolyTools follows these rules, so the app looks and works like one thing. They are the owner's
decisions, most of them made on the phone, collected here on 2026-11-08 from `CLAUDE.md`, the code's comments, the
reviews and the specs. They are binding for every new screen and every change to an existing one.

Each rule says where it came from, which shared component does it for you, and which test fails the build when a
screen breaks it. A rule with no test is checked by review and by the owner's phone pass.

## How to use this file

1. **Before building or changing a screen**, read the sections that apply. When a rule names a component, use it: never
   rebuild it in a feature module. The Date calculator drifted from the Financial calculator because the field-and-toggle
   layout lived in one tool's private code, so the next tool rebuilt it differently (2026-11-08).
2. **A new decision** goes here in the same pull request that applies it: the rule, who decided and when, the component
   and the test. Apply it to every screen it touches, not only the one at hand.
3. **A screen that can't follow a rule** goes under [Exceptions](#exceptions) with the owner's reason, and into the
   test's allow-list with the same reason.
4. **The plan of every spec** checks its screens against this file (`.specify/templates/plan-template.md`, "UI Rules
   Check").

The tests that read these rules walk every tool listed in `LayoutFitTest.TOOL_IDS`, so a new tool joins them by being
added there.

## 1. Layout of a tool screen

- **L1. Three regions.** Top, under the title: selectors and search. Middle: the content, the only part that scrolls.
  Bottom, fixed to the bottom edge: every other control. *Owner, 2026-10-23.* Use `ToolLayout(top, bottom, content)` or
  `AdaptiveToolLayout`.
- **L2. The top holds only the tool's modes or categories** (the converter's Length/Area/Volume, Random's
  Numbers/Dice/Coin/Words, Stopwatch/Timer, BMI's Metric/Imperial) and search fields (Notepad, Add city). A long list of
  categories stays one row that scrolls sideways: wrapped, it took too much of the screen. *Owner, 2026-10-23; owner's
  phone pass (converter).*
- **L3. The bottom holds everything else:** action buttons (New note, Add city, Draw, Start), input fields (Random's
  Minimum/Maximum, the converter's value and unit, a timer's name) and the calculator's keypad. They stay put while the
  content changes. A screen with a keypad (Calculator, the new-timer form) sizes its keys from the space left, between
  the touch target and `keypadKeyMaxHeight`, and scrolls as a whole only when even the smallest keys don't fit; keys
  get smaller, never wider than the screen. *Owner, 2026-10-23.*
- **L4. The kind of thing you type is chosen with the form.** A choice that only changes which fields you fill in for
  the same tool, such as the kind of QR code (Text, Link, Wi-Fi, Contact) or of NFC message (Text, Link, Phone, Email),
  is a `SegmentedChoice` at the bottom, right above the fields it changes. The top keeps the tool's own modes (Scan,
  Create, History; Read, Write). The choice starts at the same place whichever kind is chosen, in every window: its
  fields keep the room of the tallest kind's even in a small window such as a Samsung pop-up, where the bottom area
  scrolls instead. *Owner, 2026-11-08, twice: the second on the phone, in a pop-up window.* Use `KindForm`. Checked by
  `ModeStabilityTest` (phone size) and `KindFormWindowTest` (a 350x590 pop-up).
- **L5. Full-screen dialogs follow the same rules** (add timer, add city, note editor, reminder editor): content in the
  middle, confirm and cancel at the bottom. A dialog with a text field is always full screen. *Owner, 2026-10-23.*
- **L6. Empty states are text only;** the action that fills them carries the icon, like the "+" on New note. An icon in
  the empty state looked like a button. *Owner's phone pass.*
- **L7. Fit, then scroll.** A tool works at every screen size mostly without scrolling. Only lists and long text scroll
  by design. Everything else shares the height with `FitColumn`: a graph, a dial or a camera view gets
  `Modifier.flex(min, max)` (`PocketSizes.graphMinHeight/graphMaxHeight`, `dialMin/dialMax`, `previewMinHeight`)
  rather than a fixed height. Put the content in `ToolLayout(scrollContent = false)` or
  `AdaptiveToolLayout(scrollContent = false)` and give `FitColumn` `Modifier.weight(1f)`; a dial tool uses
  `DialToolLayout`. *Owner, 2026-10-31 and 2026-11-01.* Checked by `LayoutFitTest`, which renders every tool and
  surface at 22 combinations of window, text size and language: rule FITS (a 411x850 phone and tablets in both
  orientations, default text size) allows scrolling only in `SCROLLS_BY_DESIGN`, each with its reason. Its tables go to
  `androidApp/build/layout-fit/`, and `docs/layout-fit/` keeps a copy to compare. A new tool joins `TOOL_IDS`; a dialog
  or sheet joins `surfaces()`.
- **L8. Reduced windows keep everything reachable.** Each text role has a maximum (today's size, from phone size up)
  and a minimum for the smallest windows; `PocketTheme` follows the window between them through `windowFraction`: 1
  from 360x600 up (or 360 tall side by side), 0 at 240 wide or 320 tall, so a raised Display size counts as a small
  window. The phone's text setting always multiplies on top: never override `LocalDensity`. `PocketSpacing` and the
  `PocketSizes` with a getter follow the window too, so read them only in composition. `touchTarget` is 48 dp from
  phone size up and with a larger text setting, and goes down to 36 dp only in a reduced window (reached halfway to the
  smallest); anything touched uses it, or `buttonHeight` and `keypadKeyMinHeight`, which stop there. Use
  `isSmallWindow` sparingly. A control under a `Row` that may get very narrow uses `WholeWordText`. *Owner, 2026-11-05
  and 2026-11-06 (spec 015, Principle X).* Checked by `LayoutFitTest`, rule REACHABLE (every other combination): it
  allows scrolling but fails on anything clipped, text cut off or broken, a scroller under a touch target, overlapping
  touch areas, a crash or a hang; only 240x360 at 200 % just keeps its table (`docs/decisions-pending.md`).
  `DesignSystemOnlyTest` refuses a density override.
- **L9. Landscape is opt-in** through `AdaptiveToolLayout`: controls beside the content when `isTwoPane(w, h)` (wider
  than tall and at least `PocketSizes.twoPaneMinWidth`, 560 dp), so a 412x440 split window keeps the portrait layout.
  Plain `ToolLayout` never switches by itself. `endContent` puts content above the controls in the right pane (after
  the content in portrait). For a custom layout call `isTwoPane(maxWidth, maxHeight)` rather than comparing sizes.
  *Owner, 2026-11-01.*
- **L10. The content is capped** at `PocketSizes.contentMaxWidth`, and `ToolLayout` always leaves
  `PocketSizes.contentMinHeight` to it: tall bottom controls scroll (up to 60 % of the window) instead of being cut off.

## 2. Nothing jumps

- **M1. Modes keep the layout.** What stays on screen between a tool's modes keeps its place and size. What only one
  mode shows keeps its room with `ReservedSlot`; what a mode adds goes above the action at the bottom edge. The QR
  creator keeps the room of a contact's three fields for every kind, so its kinds don't move when tapped. *Owner,
  2026-11-05.* Checked by `ModeStabilityTest` (exceptions in its `ALLOWED`, each with its reason).
- **M2. Reserve room for results and messages,** so typing or a new result never moves the controls: `ReservedLine`
  for a message, `ReservedSlot` for a block. A hint stays shown and only changes color, as in BMI. *Owner,
  2026-10-23.*
- **M3. What needs a result isn't drawn until there is one;** its room stays reserved (`ReservedSlot`), so nothing
  moves when it appears and no disabled button clutters the screen. *Spec 018, owner, 2026-11-06.*
- **M4. A reading keeps one size and one baseline.** Give `LiveReadout` or `MainReading` a `widestText` when readings
  differ a lot in length (the tuner's notes): a character from a fallback font, such as ♯, no longer moves the line.
  *Owner's phone pass, 2026-11-05.* Checked by `RealFontsTest` (on the emulator).
- **M5. No check marks** on chips or segments: the check mark's width moved the chips after it. The background and the
  bold label show the choice. *Owner, 2026-11-05.*

## 3. Readings and results

- **R1. One main reading.** A tool's answer is a `MainReading`: the value large, its unit smaller beside it, what it is
  below, all centered. With no reading yet it shows `NO_READING` and says why in the caption, so a sentence never takes
  the reading's place. *Spec 018, owner, 2026-11-06.*
- **R2. Statistics are a table:** `ValueRow`s, name on the left and value on the right (Battery, Speedometer, GPS
  info). No value yet shows `NO_READING`. *Spec 018, owner, 2026-11-06.*
- **R3. One short line qualifies a result:** `ShortNote`, with the rest in Help. Heart rate's medical notice stays on
  screen, whole. *Owner, 2026-11-07.* The note comes with the result it qualifies, never before it. *Owner, 2026-11-05.*
- **R4. Safety and accuracy texts may live in Help** (Tone generator, Sound meter, Speedometer, Drag timer, Distance),
  so the controls keep their room. *Owner, 2026-11-01.*
- **R5. A one-line reading** uses `ReservedLine(compact = true)`; the default reserves a touch target's height, for a
  message that may wrap. A text row that is not a control (the BMI scale) uses `PocketSizes.textRow`.
- **R6. A result over time is a `SeriesGraph`:** under the result and its note, zero at the bottom and the
  largest value at the top, labelled. A scenario kept to compare with is a dashed line in `tertiary` beside the solid
  `primary` one of now, on the same axes, with a legend that uses the comparison's words, so the two read apart
  without their colours. Up to four marks divide the period evenly, each a tick on the x axis and a dot on each line,
  and under it, ending at its tick, the month or year it is and each line's value there in the line's colour, now's
  first (*owner, 2026-11-13*). It takes the room left (L7): with too little for the marks it shows its lines and top
  value alone, with too little even for those it isn't drawn at all, and folding the values (A7) gives it the room
  (*owner, 2026-11-13*). It stays empty until there is a result (M3); a
  screen reader hears where each line starts and ends, then each mark. *Owner, 2026-11-12 (spec 071, the Financial
  calculator's balance month by month).*

## 4. Actions

- **A1. One main action per screen.** `BottomAction` is the main action by default: filled with the strong color, and
  never more than one on a screen. Every other action beside or above it is `BottomActionStyle.Outlined` (or `Text`
  for a dialog's Cancel). *Spec 018, owner, 2026-11-06.* Checked by `UiRulesTest` (A1).
- **A2. The main action is last, on the right.** Two or three short actions share one row in a `BottomActionRow`
  (full-width stacked buttons covered the Bubble level), each `compact = true` with `Modifier.weight(1f)`; a label that
  wraps makes its neighbour as tall; the outlined ones come first and the filled one
  last: Reset | Start, Copy | Generate, Cancel | Import. *Owner, 2026-10-31 (one row); 2026-11-08 (the main action's
  side, where most rows already had it; confirmed by the owner).* Checked by `UiRulesTest` (A2).
- **A3. The actions are the bottom edge.** Top to bottom, the bottom area holds: what the mode adds and the inputs,
  then "More options", then the actions. No field, toggle, slider, selector or "More options" goes below the actions,
  so opening "More options" or changing a mode never moves them. *Owner, 2026-11-05 and 2026-11-08.* Checked by
  `UiRulesTest` (A3). A "More options" under a list stays at the bottom when the list is empty: the room the list
  leaves goes above it, not below (the Intervals tab; *owner, 2026-11-09*).
- **A4. Rarely used controls go behind "More options"** (`MoreControls`): smoothing, resetting a calibration, a
  compass's declination. What the tool is for keeps its room. *Owner, 2026-11-01.* **Only two or more:** a button that
  opens a single control only adds a tap, so a single option shows from the start (the snooze length, the tone's big
  steps, Count the end day, Avoid look-alikes, Lock the tag). `MoreControls` takes its options
  as a list and draws one alone without the button. A control that is only sometimes of use stays in the list,
  disabled (Reset calibration in Plumb, Delete all with no results), so the button doesn't come and go. *Owner,
  2026-11-08.* Built into `MoreControls`; checked by `MoreControlsTest`. **Never what changes what runs:** the interval
  editor's "First step runs once" and "Last step runs once" show with the steps, and headings in the list say where each
  step runs (before the rounds, in each round, after them). *Owner, 2026-11-09.*
- **A5. Repeated action words** follow `docs/glossary.md`.
- **A6. A row's actions are behind ⋮ on its right** (`RowMenu`), never buttons under each row: the Notepad's layout,
  for every list and history. A history row is a `MenuRow`: what it is, then each detail on one line cut with "…"
  and when, with an optional mark on the left (a color's square). The same actions read the same everywhere: Copy and
  Share send the text the screen shows, formatted (an NFC tag's binary data as hex), Delete or Remove come last.
  *Owner, 2026-11-09:* the QR history like the Notepad, an NFC history and a color history the same way, Copy and Share
  in the World clock's menu, Copy in GPS places'. Checked by `MenuRowTest`; `LayoutFitTest` lets a detail be cut
  (`UiRole.Summary`), never anything else.
- **A7. The values may fold away.** On a screen whose inputs take much of its height (the Financial
  calculator), a `FoldLine` on top of them, a thin line with a round ∨ / ∧ button in its middle, folds the inputs
  and More options away and back, animated (`FoldAway`), so the results and the graph get the room; the actions stay
  at the bottom edge (A3). The line is pinned: it is `ToolLayout`'s `bottomHeader`, which never scrolls with the
  values, so when a small screen makes them scroll the button stays where it is, and it doesn't count in the
  bottom's share of the window (L10) (*owner, 2026-11-13*). Beside the values in landscape (L9) there is nothing to
  fold and no line. The fold is kept while the tool is open. *Owner, 2026-11-12 (spec 071).* Checked by
  `FinanceScreenTest`.

## 5. Fields and toggles

- **T1. A toggle beside a field sits on the field's right, exactly as tall as the field's outline.** The field takes
  the rest of the width. When a label wouldn't fit beside the toggle (a narrow window, 200 % text), field and toggle
  each take the whole width, the toggle below. *Owner's phone pass, 2026-11-03 and 2026-11-05; restated 2026-11-08.*
  Use `FieldToggleColumn` with one `FieldWithToggle` per row. Checked by `UiRulesTest` (T1).
- **T2. A toggle keeps one width.** A `ToggleChoice` is as wide as its longest word whichever is shown, so a tap
  changes only the word and nothing around it moves. *Owner, 2026-11-05 (fields don't change width); 2026-11-08.*
  Built into `ToggleChoice`. Checked by `ModeStabilityTest`, which taps every toggle through its words.
- **T3. The toggles beside fields on one screen share one width:** the longest word of every toggle in every mode, so
  the fields line up and keep their width between the modes too. *Owner, 2026-11-05.* Give `FieldToggleColumn` every
  word (`toggleWords`) and every field label (`fieldLabels`) of the screen. Checked by `UiRulesTest` (T3).
- **T4. A two-way choice beside a field is a one-word `ToggleChoice`,** never two segments in half the width, which cut
  their words (per year/per month, deposit/withdrawal, Add/Subtract). *Owner's phone pass, 2026-11-03.*
- **T5. A toggle that chooses something no field types** gets a `ToggleCaption` in the field's place, so the row keeps
  the shape of the others (the Financial calculator's "Amortization system", PRICE/SAC). *Owner's phone pass,
  2026-11-05.*
- **T6. A choice instead of a minus sign:** a phone's number keyboard may have no minus key, so a sign is a toggle
  (deposit/withdrawal). *Owner's phone pass, 2026-11-03.*
- **T7. Text fields are `PocketTextField`:** Material's own keeps 56 dp however small the text gets. *Owner,
  2026-11-06.* Checked by `DesignSystemOnlyTest`.
- **T8. A value changed in steps** is a `StepperRow` (− value +), or a field between the two buttons when the value is
  also typed (Metronome's BPM). A value set with a slider takes its − and + on the slider's line (Ruler's
  calibration, Tone's Hz: owner, 2026-11-11), with its name: a `StepSlider`, the name beside the slider in one width
  for every slider of the screen, so they line up, or above it where there is no room (the Tone generator's Frequency
  and Volume, the Password generator's length: owner, 2026-11-13). A count picked from a short list is a `PickerField`
  instead (S4).
- **T9. Small option buttons wear the hint colour.** The button that shows the current choice, a `ToggleChoice`
  (GPS info's DD, per year | per month) or a `MenuChoice` (Area & volume's unit), has its word and outline in the
  theme's `primary`, light and dark, so it reads as something to tap without looking like the main action, which is
  filled. A `MenuChoice` shows a small ▾ after its word, **( mm ▾ )**, and a tap opens its list, the chosen option
  bold (S4); like a toggle it keeps one width (T2). Beside a top choice, the button is exactly as tall as the
  choice, top and bottom aligned: `ChoiceWithOption`. *Owner, 2026-11-12 (spec 070).* Checked by
  `OptionButtonsTest`.

## 6. Selectors

- **S1. A choice of one at the top is a `SegmentedChoice`:** one line per label, equal heights, and equal widths while
  the widest label fits in an equal share. Otherwise each segment is as wide as its own label, with an equal part of the
  room left; when even that doesn't fit, the row scrolls sideways, opening on the chosen one. A label never shrinks or
  breaks to fit, and a segment is never narrower than a touch target. Every tool's modes and categories look like this (L2): the
  Converter's categories, Random's modes, Home's categories. *Owner, 2026-11-09.* A
  `FilterChipRow` stays for choices inside a screen (smoothing, subdivision). When the choices are too many for one
  row, the last segment can open a list (`ChoiceMenu`): the Tuner's **( Cromático | Cordas ▾ )**, with the ▾ of a
  `MenuChoice`, reading the option chosen from it, **( Cromático | Violino ▾ )**, and keeping one width whichever it
  reads. *Owner, 2026-11-13 (spec 073).*
- **S2. The choice shows by its background and bold label,** never a check mark (M5).
- **S3. A touch and hold makes a favourite.** Holding a result pins it to the top of its list; holding a category keeps
  it first in its row; holding again undoes it. Favourites come first, in the order they were marked (Area held before
  Volume stays before it; *owner, 2026-11-10*), and carry a small star (`FavouriteMark`); a hold never does what a tap does, and a screen reader finds it as a named action ("Keep Cooking
  first"). Use `Modifier.onLongPress` and `SegmentedChoice`'s `marked`/`onLongPress`. *Owner, 2026-11-08 (spec 039).* The first
  time the Converter opens, a tip with an (i) says so (`FirstTimeHint`, `PocketSnackbarHost`). *Owner, 2026-11-09.*
- **S4. A number or a size from a fixed list is a `PickerField`:** how many dice, how many sides, how many teams. It
  takes one field's room, shows the chosen value under its label, and a tap opens the list below it; the chosen option
  is bold. Not rows of chips, which took too much room, and not a stepper, whose taps felt slow. Beside a toggle it is
  the field of a `FieldWithToggle` (Random's "Split into 3 teams"). *Owner's phone pass, 2026-11-08 (spec 040).*
- **S5. A color is chosen in a `ColorField`:** it looks like a `PickerField`, with a small square of the color, and a
  tap opens a sheet with the person's kept colors and then a `ColorGrid` of every other one (12 hues in 6 shades and the
  grays). Each square is a touch target, labelled with its name and HEX, and the chosen one wears a ring. *Owner,
  2026-11-17 (spec 078), the Color picker's Contrast.*
- **S6. A list too long for a menu is chosen on its own page:** the menu shows what the person chose, and its last
  entry, after a divider, opens a full-screen page of every option (`MenuChoice`'s `last`, a `MenuEntry`). The page has
  back, its title and a `SearchField` on top (S1), the only part that scrolls in the middle, and nothing at the bottom,
  since a change applies at once. When a least number must stay checked, those rows are disabled and one `ShortNote`
  in kept room says why. *Owner, 2026-11-19 (spec 080).* The Translator's is **Manage languages** (owner,
  2026-11-20): "Manage languages…" ends every list and Settings → Offline languages opens it; ⓘ on its top bar opens
  its help (`HelpSheet`). "Downloaded languages" (pack on the phone, English built in) come first, "Other languages"
  after; a language stays in its section until the page opens again, so it isn't lost while the person gets its
  parts. Each language is an `ExpandableCard`: its name and own name, a `StatusMark` (a check in a circle: all there,
  primary when enabled, gray when not; a warning triangle: something the phone can still get is missing, amber when
  enabled, gray when not; none until its pack is on the phone) and ⌄; opened, under a divider, "Enable language" (a
  `CheckRow`, checked when its pack is downloaded) and its translation pack, speech recognition and voice, each with
  its actions behind ⋮ (`MenuSettingsRow`, A6) or its state alone (`InfoRow`). Guarded
  by `ManageLanguagesPageTest`, `OfflineLanguagesSectionTest` and `CheckRowAndMenuEntryTest`.

## 7. Text

- **X1. No hard-coded text.** Every string is a Compose resource in `values`, `values-pt-rBR` and `values-es`. Write
  apostrophes plainly (`can't`). Checked by `DesignSystemOnlyTest` and `TranslationCompletenessTest`.
- **X2. Words never break.** Labels in tiles, cards and buttons use `WholeWordText`, which shrinks the text (down to
  60 %) so its longest word fits instead of hyphenating it. *Owner's phone pass ("Calculador-a").*
- **X3. Help reads in blocks:** `HelpContent() = ToolHelp(Res.string.<tool>_help)`, one string per tool; `\n` starts a
  paragraph and `\n- ` a list item; every language has the same blocks, none over 180 characters. *Owner's phone pass,
  2026-11-05.* Checked by `HelpTextsTest`.
- **X4. Numbers, dates and times** follow the app's language and Settings: `DecimalFormatter` and `DecimalInput` with
  `rememberNumberSymbols()`, and `rememberDisplayFormats()` for dates and times. Never `toString()` a number or a date.
- **X5. Privacy is said once:** in the first-launch notice, the privacy policy and the Play listing. A tool's
  explanation or help says what the tool does ("uses your position once", "the app never opens links"), never that
  nothing is sent, stored or kept off the phone. *Owner, 2026-11-17: "repeating this over and over in every tool that
  needs permission seems more like a threat."* Checked by `PrivacySaidOnceTest`.

## 8. Visual values

- **V1. Visual values come only from `core/designsystem`:** no dp or sp literals, `Color(0x…)`, `FontWeight(…)`,
  shapes or tweens in a feature. Use `PocketSpacing`, `PocketSizes`, `PocketShapes`, `PocketTypography` and
  `MaterialTheme`; a new need is added to the design system first. *Constitution, Principle VII.* Checked by
  `DesignSystemOnlyTest`.
- **V2. A Material icon button** takes `PocketSizes.iconButtonSize`, or it stays 40 dp in a reduced window.
- **V3. A message at the bottom of a screen wears the theme's colors:** every snackbar is drawn by
  `PocketSnackbarHost`, on the secondary container with its action in the primary color, so it is dark in the dark
  theme and follows the phone's colors. Never Material's `SnackbarHost`, whose inverse colors drew a white bar on a
  dark screen. *Owner, 2026-11-10.* Checked by `DesignSystemOnlyTest` and `SnackbarColorsTest`.

## 9. The frame around a tool

- **F1. The host draws the frame:** back, title, the add-to-home icon (left of the star), the favorite star and Help. A
  tool provides only `Content()` and `HelpContent()`. The title shrinks to two lines beside the actions, or takes its
  own row. *Owner, 2026-11-06.*
- **F2. Permissions.** A tool that can't work without one shows `PermissionPane`: the explanation in the content, the
  action and Not now at the bottom; Not now leaves the tool. `PermissionGate` is only the in-card offer for a screen
  that works without it. *Owner, 2026-11-01.*
- **F3. A notice the person acknowledges is a `NoticeScreen`:** full screen like `ConfirmScreen`, its title and text in
  the middle, one main action last at the bottom (A3) and an optional link beside it; Back doesn't close it. The
  first-launch privacy notice is the first (spec 078). Settings' sections run Appearance, Language, Units & formats, Your
  data, Privacy, Remove ads, About (owner, 2026-11-17: Remove ads below Privacy). The Translator adds Offline languages
  after Your data (spec 012).

## 10. Two people at one phone

- **C1. Conversation: two facing halves,** each with its turns, then its own language (a `MenuChoice`) beside its large
  microphone (`MicrophoneButton`) at the speaker's own edge; nothing else inside the halves (`FacingHalves`). A window
  too short for both halves scrolls them (`PocketSizes.facingHalfLeastHeight`). The far half is turned to face the
  person across the phone, and More options turns it upright for someone beside you; the turn is only drawn, so screen
  readers read both halves in the normal order. What the phone's owner sets up (a language pack, the phone's speech or a
  voice, for either language) shows upright in the near half, one thing at a time: the translation pack, then speech
  the phone can get (the near half's language first), then a voice, last what can't be had; when one is done, the next
  comes, and a language the phone downloads moves it on without leaving the tool (`SetupNeeds`). A speaker's own
  messages (nothing heard, the microphone busy) stay in their half. While a half listens and nothing is heard yet, its
  turn reads "Listening." "Listening.." "Listening..." (`WaitingText`), and a smaller round square button beside its
  microphone stops and drops what was being heard (`StopButton`, in a `ReservedSlot`, so nothing moves). A turned
  half's language list is turned too and opens under its chip as the person across sees it (`LocalHalfTurned`,
  `MenuChoice`). *Spec 012, owner's decisions 2026-11-17; setup in the near half, owner 2026-11-19: "the popup being
  flipped seems strange since the download popup goes to the correct position"; the waiting dots, the square button
  and the turned list, owner 2026-11-19.* Guarded by `ConversationScreenTest` and `SetupNeedsTest`.
- **C2. A speech notice can be ignored and leads to Manage languages:** each of the Conversation's speech notices
  (speech to get, a language the phone didn't install, a voice, a language the phone can't recognize) has "Manage
  languages", which opens that page at the half's language, open, and a gray "Ignore" text button before it
  (`Notice`'s `onClose`; owner, 2026-11-20: a × beside the text squeezed the buttons, and at the row's start it felt
  strange); a notice taller than the half's room scrolls inside instead of squeezing its button; the translation
  pack's offer has none, since nothing translates without it. A closed notice gives way to the next one and stays
  closed while the Translator is open; it comes back when the person leaves the tool and returns, or when its half's
  language is changed and chosen again. Type or speak's speech messages have OK and "Manage languages". *Owner,
  2026-11-20: "Arabic didn't have a STT in samsung … add an small x button"; then "The x button is too big …
  The popup can let the user go to that screen".* Guarded by `SetupNeedsTest`, `ConversationViewModelTest` and
  `ConversationScreenTest`.
- **C3. A voice comes from the voice app's own list:** on Manage languages, "Get a voice" opens the phone's voice
  engine's download list (Samsung's and Google's both have one), the phone's voice settings only when the engine has
  none; an app can't download a voice itself. A recognition language the phone downloads and never installs reads
  "The phone didn't install it", with "Update" for the phone's speech service in Google Play where the phone has one,
  instead of offering the same download again. *Owner's phone pass, 2026-11-20.*

## Exceptions

| Screen | Rule | What it does instead, and why |
|---|---|---|
| Ruler | L2 | Calibrate sits beside the cm/in switch on top, so the scale keeps the screen's full length (`docs/decisions-pending.md`, spec 004 row 14); since spec 064, all but the bottom row's History, Add stretch and Save. |
| Calculator | L2, T1 | The mode and the angle unit are one-word chips in the row above the keys, so the scientific keys fit without scrolling (owner's choice). Scientific mode adds key rows, so the keys change size (`ModeStabilityTest`). |
| Counter | A1–A3 | The large Add one is the tool itself, a tally kept by tapping a large button (spec 001), with Reset and Subtract one below it, in that order (owner, 2026-11-08). |
| Flashlight | A3 | Torch's "More options" sits where Strobe's rate goes, so Brightness keeps its place in every mode (M1 first). |
| Heart rate | R3 | The medical notice stays on screen, whole (owner, 2026-11-07). |
| Metronome, Pitch tuner | M1 | The beats and the strings share the row's width, and their count follows the signature or the instrument (specs 025, 026). So do the Tuner's tunings (spec 045): `ModeStabilityTest` compares a segment only among the same choices. |
| Ruler | M1 | The end marker starts on a round mark of each unit: 5 cm and 2 in aren't the same length. |
| Financial calculator | L7 | Its graph (spec 071) has no least height (`PocketSizes.graphLeastHeight`): on a shorter phone with the values open, or with many results, it isn't drawn rather than make the screen scroll; folding the values shows it (owner, 2026-11-13: "Hide until folded"). |
| Area & volume | M1 | Each shape's lengths come from the top, as many as it has: a Height is where its shape puts it (spec 068). The reading and the rows above keep their place between Area and Volume. |
| Translator | M1 | The two languages sit side by side above the text in Type or speak, and each in its own half, beside its microphone, in Conversation (C1): that mode is two people's, so each gets their own (spec 012). `ModeStabilityTest` allows the language menus only. |
| Translator, Conversation | A1, A3 | No filled main action at the bottom: Copy, Share and Clear are all outlined, because the main actions are the two microphones inside the halves (C1; approved by the owner 2026-11-17, spec 012). |

## Where each rule lives

| Component (`core/designsystem`, `core/tool-api`) | Rules |
|---|---|
| `ToolLayout`, `AdaptiveToolLayout`, `DialToolLayout`, `FitColumn` | L1–L3, L7–L10 |
| `BottomAction`, `BottomActionRow`, `BottomActionStyle` | A1, A2 |
| `MoreControls` (its options as a list) | A3, A4 |
| `FoldLine`, `FoldAway` | A3, A7 |
| `MenuRow`, `RowMenu` (⋮, or its own icon: the Notepad's Aa), `ReorderableRow` (its `actions`) | A6 |
| `FieldToggleColumn`, `FieldWithToggle`, `ToggleCaption`, `ToggleChoice` | T1–T6, T9 |
| `MenuChoice`, `ChoiceWithOption` | T2, T9 |
| `FacingHalves`, `MicrophoneButton` | C1 |
| `Notice` (`onClose`) | C2 |
| `ExpandableCard`, `StatusMark`, `HelpSheet` | S6 |
| `CheckRow`, `MenuEntry` (`MenuChoice`'s `last`), `MenuSettingsRow` | S6 |
| `PocketTextField`, `StepperRow`, `StepSlider` | T7, T8 |
| `KindForm` | L4, M1 |
| `SegmentedChoice` (scrolls when the labels don't fit; a `ChoiceMenu` segment), `FilterChipRow` | L2, S1, S2, S3, M5 |
| `FirstTimeHint`, `PocketSnackbarHost` | S3, V3 |
| `PickerField` | S4, T1 |
| `ColorField`, `ColorGrid`, `Swatch` | S5 |
| `NoticeScreen`, `ConfirmScreen` | F3, L5 |
| `Modifier.onLongPress`, `FavouriteMark` | S3 |
| `ReservedSlot`, `ReservedLine` | M1–M3, R5 |
| `MainReading`, `LiveReadout`, `ValueRow`, `ShortNote` | R1–R5, M4 |
| `SeriesGraph`, `GraphLine` | R6 |
| `WholeWordText`, `CountsLine` (counts at one size, each number in its room), `ToolHelp` | X2, X3 |
| `PermissionPane`, `PermissionGate` | F2 |
| `UiRole` (semantics for the tests) | A1–A3, T1–T3 |

| Test | Rules |
|---|---|
| `UiRulesTest` (androidApp) | T1, T3, A1, A2, A3 |
| `ModeStabilityTest` (androidApp) | M1, T2, L4 |
| `KindFormWindowTest` (androidApp) | L4 in a small window |
| `MoreControlsTest` (androidApp) | A4 |
| `MenuRowTest` (androidApp) | A6 |
| `LayoutFitTest` (androidApp) | L7, L8 |
| `RealFontsTest` (androidApp, emulator) | M4 |
| `DesignSystemOnlyTest`, `HelpTextsTest`, `TranslationCompletenessTest` (tooling/architecture-tests) | V1, V3, T7, X1, X3, L8 |
| `SnackbarColorsTest` (androidApp) | V3 |
| `OptionButtonsTest` (androidApp) | T9 |
| `PrivacySaidOnceTest` (tooling/architecture-tests) | X5 |
| `PrivacyNoticeTest`, `SettingsScreenTest` (androidApp) | F3 |

## Changes

- **2026-11-08:** the rulebook, gathered from `CLAUDE.md`, the code and the reviews. The field-and-toggle layout moved
  from the Financial calculator into the design system (`FieldToggleColumn`, `FieldWithToggle`, `ToggleCaption`), and
  `ToggleChoice` keeps one width; the Date calculator and the QR creator's Wi-Fi password now use them. Later the same
  day, from the owner's phone pass in a pop-up window: `KindForm` holds the QR creator's and the NFC writer's kinds in
  place in small windows too. A2, A3 and L4
  are new; `UiRulesTest` and the toggle walk of `ModeStabilityTest` guard them. Screens brought in line: Date
  calculator (T1, T2), QR creator (L4, T1), GPS info (T2), Bubble level and Protractor (A2, A3), G-meter, Vibration meter
  and the Monthly table (A2), Metal detector (A3). From the owner's phone pass of Random: S4 and `PickerField`, for the
  dice and the new Teams mode. Then A4's second half, "More options" only for two or more: the Date calculator,
  Reminders, Password generator, Tone generator, Metronome and NFC writer show their one option from the start. The
  Bubble level's signal (spec 042) is a third option in its "More options": three short choices, so a
  `SegmentedChoice` under a label, as Reminders' snooze (S1). The Speedometer's alert (spec 043) is a `PickerField`
  (S4) above Reset and Windshield, and `MainReading` takes an alert color, always with a line that says why (R1). Then
  A6, from the owner's look at the Notepad (2026-11-09): the QR scanner's history moves its buttons behind ⋮ in a
  `MenuRow`, and the NFC tool and the color picker get histories in the same rows; the World clock's menu gains Copy
  and Share, GPS places' Copy. Then S1 again (spec 045): every top selector is a `SegmentedChoice` that scrolls
  sideways when its labels don't fit, the Converter's favourite categories included, and the Converter shows its
  touch-and-hold tip the first time. The Drag timer's results become `MenuRow`s (A6, spec 046). A3 again (spec 047):
  with no sequences, the Intervals tab's "More options" stays at the bottom, just above Notifications. The World clock gets
  two modes, Clocks | Meeting (S1, spec 048); the planner's day and hour are `StepperRow`s above Copy | Share (A1–A3),
  and `HourStrip` shades a day's hours by lightness, silent beside the words that say the same (R5). The G-meter's
  axes are a second `SegmentedChoice` under its unit, as the Tuner's tunings (S1, spec 049).
- **2026-11-10:** from the owner's look at the Converter on the phone (spec 055). V3 is new: every snackbar goes through
  `PocketSnackbarHost` and wears the theme's colors; the Calculator, Counter, Notepad, Reminders and the frame's own
  messages were Material's. S3 changes: favourites keep the order they were marked in, no longer the catalog's.
  The Sound meter's **Start log** is its main action, beside Reset statistics (A1–A2, spec 056); the log's time takes
  the microphone's line (M2, R5); and a stopped log's question, Save .csv or Discard, stays until one is chosen, so a
  touch outside never loses the readings.
  The Tone generator's five modes are one `SegmentedChoice` at the top (S1, spec 057), and Noise's kinds take the
  wave's row under it. Which of two frequencies the slider changes (First | Second, From | To) is a
  `SegmentedChoice` right above that slider, as a `KindForm`'s kinds sit above their fields (L4); the sweep's seconds
  and its Once | Repeat are a `FieldWithToggle` (T1), in the place of the steps of 10 Hz, which a sweep doesn't
  offer: a row more would take the controls past their share of a phone's screen (L7). Each block that only some
  modes show keeps its room in its own `ReservedSlot`, so the volume and Start never move between the five (M1), and
  each mode's reading is sized for its widest text (M4). *Owner, 2026-11-11:* the frequency has no field: − and + of
  1 Hz sit on the slider's line, as in the Ruler's calibration (T8); and a `WaveShape` between the reading and the
  controls shows a fixed example of the wave, the noise or the mode, never drawn to the frequency, taking the height
  the reading leaves (L7).
  The Vibration meter's **Strength | Frequency** is its top `SegmentedChoice` (S1, spec 058); each view has its one
  `MainReading` (R1), and the two hints share a box where each keeps its `ReservedSlot`, so the graph and the spectrum
  start at the same place (M1).
  The BMI tool's top is its pages, **BMI | Body | History** (S1, spec 059), so Metric | Imperial moves to the bottom
  as a `KindForm` above the fields it changes (L4). The estimates have their own page so the BMI's scale stays on a
  phone's screen (L7): the body fat is its `MainReading`, the rest `ValueRow`s (R2), and the age a `FieldWithToggle`
  with Female | Male (T1). Save in History is the BMI page's one filled action, and History's rows are `MenuRow`s with
  Delete behind ⋮ (A6), under the weight's graph, which scrolls with them.
  The Financial calculator's inflation and a loan's extra payment are behind More options (A4, spec 060), the extra
  payment disabled in Compound interest; **Keep to compare** is outlined beside Monthly table (A1–A2), and a kept
  scenario's values sit beside the new ones under Kept and Now, `WholeWordText`s that shrink rather than break (L8).
  Reminders show **All and each list** at the top once a list is in use (S1, spec 061), the screen's filter; a row's
  steps sit under it with their ticks, ticked off right there; the editor adds the list (with the lists in use as
  chips), "Also remind before" for a one-off reminder, and the steps, each with its tick and Remove. A one-off
  reminder's line says its day once ("Travel · 11/20/2026 · 09:00 · also 3 days before").
  A note's ⋮ menu adds **Save as .txt** beside Share, Delete still last (A6, spec 062), with `PocketIcons.Save`.
  **Exception, the note widget** (spec 063): the launcher draws it from `RemoteViews`, which can't read the design
  system, so `androidApp/src/main/res` copies its surface, text colours, corner and type sizes by hand; keep them in
  step with `PocketColorSchemes` and `PocketTypography`. It follows the app's theme like every screen
  (`NoteWidgetPalette`): Light, Dark or the phone's, with the wallpaper's colours when "Dynamic color" is on.
  The Ruler gets a bottom row, **History · Add stretch · Save** (A1–A3, spec 064), so its scale is a bottom row
  shorter; Calibrate stays on top (L2). The reading sits a little below the middle, so the end marker's starting place
  (5 cm, 2 in) stays clear of it on a phone, at both text sizes. The stretch line under it keeps its room
  (`ReservedSlot`, M1); under phone size only its − shows, in the reading's card (L9);
  History is a page over the tool like the heart rate's, its rows `MenuRow`s (A6), and Save asks for an optional name
  in a full-screen dialog (L5).
  The Protractor's top is **Plane · Elevation · Camera** (S1, spec 065). Camera is a `ToolLayout` under the same
  choice, behind `CameraPermissionPane`: its `MainReading` above the picture, three points with 48 dp targets and
  Move up, down, left and right for screen readers, and **Reset points · Hold** at the bottom, Hold where it is in the
  other modes (M1).
  **Sun & moon** (spec 066): **Sun | Moon** on top (S1); each page one `MainReading` and `ValueRow`s (R1, R2), "—"
  with "Waiting for your position" until there is a place; the place (`PickerField`) and the day (‹ day ›, the day
  going back to today) are the bottom's inputs, with no action; the moon's drawing takes the room left (L7).
  **The Notepad counts and formats** (spec 069, *owner, 2026-11-12*, merging in the Text counter of spec 067, which
  goes): under the note's text, or its list, a footnote that is always there (M1) in `labelSmall` and
  `onSurfaceVariant`, right under the text, grouped at the end of the line as
  **Characters: 123 | Words: 20 | Sentences: 8**, a `CountsLine`: each number has the room of 99,999, so a count
  growing never moves the others (*owner, 2026-11-13*, spec 072); at large text the three shrink together and keep one
  size, and go one under the other only past the smallest (X2); the title never counts. On the Title
  field's right, **Aa** is a `RowMenu` with its own icon (`PocketIcons.TextCase`) and the four cases as items without
  icons, each written the way it makes the text; disabled while the text is empty. A change of case offers Undo in the
  editor's `PocketSnackbarHost` (V3).
  **Area & volume** (spec 068): **Area | Volume** on top (S1); the shape is a `KindForm` with its lengths at the
  bottom (L4), the unit a `PickerField` above it; the shape's picture takes the room left above one `MainReading` and
  its other units as `ValueRow`s (R1, R2, L7); no action: the result follows the typing.
- **2026-11-12:** from the owner's phone pass of the 068 build (spec 070). T9 is new: every `ToggleChoice` wears the
  hint colour, the Date calculator's, Random's, BMI's, the Financial calculator's, the QR creator's and the Tone
  generator's with GPS info's. GPS info's format button is as tall as **Position | Places** and aligned with it
  (`ChoiceWithOption`), and Area & volume's unit leaves the bottom for a `MenuChoice` on the right of
  **Area | Volume**, so the picture and the result get the room the `PickerField` took.
  Then the Financial calculator (spec 071): R6 and A7 are new. Under the approximation note, a `SeriesGraph` of the
  balance month by month, Kept dashed beside Now while comparing; above the values, a `FoldLine` that folds them
  and More options away, animated, the actions staying put. The middle is now a `FitColumn` (L7) whose graph takes
  the room left. The amortization system's caption ends in a colon ("Sistema de amortização:").
- **2026-11-13:** from the owner's phone pass of the 071 build (spec 072). The Notepad's counts move under the
  text, grouped at the end with thin lines between them, each number in the room of 99,999 (`CountsLine`, in
  place of `SpreadLine`). The Financial calculator's fold line is pinned on top of the values (`bottomHeader`,
  A7); its graph marks up to four points with their month or year and values (R6) and isn't drawn without the
  room; `ToggleCaption` stays on one line, shrinking.
  Then the Tuner (spec 073): its top row is **( Cromático | Cordas ▾ )**, a `SegmentedChoice` with a `ChoiceMenu`
  segment that lists the stringed instruments and reads the one chosen (S1); its string cells keep a sharp's height,
  so the dial is the same size for every instrument.
  Then the owner's phone pass of the 073 build (spec 074): the Financial calculator's marks read short ("7 a. 6 m.");
  `WholeWordText` draws new words again even when they keep the old room, which steppers' middles do (the World
  clock's meeting and the Tip kept their old words); `StepSlider` brings − and + to the Password generator's length
  and the Tone generator's volume, both of the Tone's sliders with their names (T8); Reminders shows the permission's
  own notice alone; the Tone generator's reading and wave sit closer.
  Then Home's categories (spec 075): ten instead of six, with Location & navigation (the GPS tools and the Compass),
  Calculation, Time and Health; the chips row scrolls sideways as S1 says.
- **2026-11-17 (spec 078):** privacy is said once (X5): a first-launch `NoticeScreen` (F3) says no tool sends your
  data and some keep what you create on the phone, and the tools' explanations and help no longer repeat it. Remove ads
  moved below Privacy in Settings. The Color picker opens on Picture, then Camera, Contrast and History, and Contrast
  picks each color in a `ColorField` with the kept colors and a `ColorGrid` (S5).
- **2026-11-18 (spec 012):** the Translator. Its Conversation is two facing halves (C1, `FacingHalves`,
  `MicrophoneButton`), with the A3 exception: the microphones are its main actions. A `MenuChoice` can fill its width,
  and an option's detail (a language pack's size) shows in its list only. Settings → Offline languages uses
  `MenuSettingsRow`, a Settings row with its ⋮ (A6).
- **2026-11-19 (spec 012, owner's phone pass):** the Conversation's setup notices show upright in the near half, one
  thing at a time, and move on by themselves when the phone has downloaded a language (C1). Later that day: waiting
  dots before the first word (`WaitingText`, Type or speak's field too), a square stop-and-drop button beside a
  listening microphone (`StopButton`), and a turned half's language list turned with it (`LocalHalfTurned`).
- **2026-11-19 (spec 080):** S6, a list too long for a menu on its own page: the Translator's "Other languages…"
  (`MenuChoice`'s last `MenuEntry`) opens a page of check rows (`CheckRow`), at least two checked.
- **2026-11-20 (spec 080, owner's phone pass):** C2, the Conversation's speech notices close with a small × until the
  tool reopens or the half's language is chosen again (`Notice`'s `onClose`); C3, "Get a voice" opens the voice app's
  own download list, and a language the phone didn't install offers the speech service's update. Later that day: S6
  and C2 rewritten for **Manage languages**, one page to turn languages on or off and get or remove their pack,
  speech and voice; the speech notices lead to it, with a smaller ×; Settings opens it. That evening: "Downloaded
  languages", each language an `ExpandableCard` with a `StatusMark` and "Enable language" inside, the sections kept
  while the page is open, a help; the notice's × in its buttons' row, and the notice scrolls rather than squeeze. Then
  a gray "Ignore" text button instead of the ×.
