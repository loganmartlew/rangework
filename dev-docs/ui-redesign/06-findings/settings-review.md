# Rangework - Settings Review

Now I have a thorough picture of the screens. Let me write up the analysis and render the annotated visual.Here's a summary of the key themes coming out of this review.

## Organization

1
Section order doesn't match user priority

Account sits at the very top, but users almost never come to Settings to manage their account — they come to change appearance or units. Deferred, destructive actions (Sign out) shouldn't lead the screen.

→ Reorder: Appearance → Units → Clubs → Account → About
2
Clubs section is huge and buried mid-scroll

30 toggle rows covering woods, hybrids, irons, wedges and putter turn the Settings screen into a long scrolling list. The club bag is a one-time configuration task but occupies the most real estate in the screen.

→ Move to its own screen: a "Manage clubs" entry row that navigates to a dedicated page with category-grouped toggles and an enabled count badge (e.g. "14 of 30")

## Discoverability

3
No summary of the current club selection

The CLUBS section header gives no indication of what's currently configured — the user has to scroll through all 30 rows to understand their own setup.

→ Show a subtitle on the "Manage clubs" row: "12 clubs enabled" or list the category breakdown (e.g. "3 woods · 6 irons · 3 wedges")
4
Dynamic color toggle has no context

The "Dynamic color" switch sits under Theme with no description explaining what it does. Users unfamiliar with Material You will have no idea if this affects their wallpaper, the app palette, or something else.

→ Add a supporting text line: "Adapts colours to your wallpaper (Android 12+)"

## Terminology

5
"CLUBS" is ambiguous — could mean anything

The section header "CLUBS" reads like a navigation destination, not a description of the setting. A golfer's first instinct might be "clubs as in golf clubs I own" — which is exactly right — but the heading offers no indication that this is about filtering which clubs appear in dropdowns.

→ Rename section to "CLUB BAG" to match the spec language and frame it as a bag selection, not a catalogue
6
"APPEARANCE" vs "UNITS" split is slightly fragmented

Both sections are about how the user personalises the app. Separating them with equal-weight headers implies they are as distinct as Account and Clubs, but they're both preference-type settings.

→ Consider a single "PREFERENCES" section header grouping theme and units, or keep split but use a lighter visual weight for subsection labels
7
Speed units feel out of place for a golf app

Ball speed is relevant for launch monitor integration but Rangework doesn't appear to use speed data anywhere in the current feature set. Offering mph/km/h/m/s as a setting without any speed-related fields in the app creates confusion about what it controls.

→ Either remove speed until a feature uses it, or add a short description: "Used when importing launch monitor data"

## Material 3 patterns

8
30 inline toggles violates M3 list density guidance

Material 3 recommends grouping settings with large item counts behind a navigation row. Inline toggle lists work for short item sets (4–8 items). At 30 items — spanning multiple sub-categories with their own non-interactive header rows — the pattern breaks down and makes the screen feel like a form.

→ Use a ListItem with trailing icon (chevron) navigating to a dedicated ClubBag screen — a standard M3 "two-level settings" pattern
9
Section headers use a custom container card rather than M3 preference groups

The current pattern wraps each section in a rounded surface card. M3 Settings uses transparent list groups with a category header row — the card wrapping adds visual weight and compresses horizontal tap targets slightly.

→ Align with standard M3 PreferenceCategory pattern: no card background on setting groups, just a bold label row as the section divider
10
Sign out uses a text-style button inside a card, not a list item

Sign out is currently a tappable text link rather than a standard ListItem row. This deviates from M3 conventions and makes it less obvious as an interactive element — especially for users scanning a dense settings screen.

→ Use a standard M3 list item row with a leading logout icon (ti-logout) and an error-colour label for the destructive action

## Summarised Findings

**The biggest structural problem** is that the Clubs section needs to move off this screen entirely. 30 toggles embedded inline in Settings is the main source of cognitive overload — it makes the screen feel endless and buries everything that comes after it. A single "Manage clubs" row with a count badge ("12 clubs enabled") and a chevron gets you to the same functionality in two taps and makes the main Settings screen scannable at a glance.

**Section ordering** is the second priority. Account and its Sign out action are prominent at the top, but that's the setting users are least likely to need. Reordering to Appearance → Units → Club Bag → Account → About puts the frequently-changed settings first and the destructive one last — standard Android Settings practice.

**Terminology** is fairly solid overall, but "CLUBS" would land better as "CLUB BAG" to immediately communicate that this is about filtering what appears in your bag across the app — rather than reading as a navigation category. The Dynamic color toggle also needs a descriptor line for users who haven't encountered Material You before.

**Speed units** is a minor flag worth discussing. If Rangework doesn't currently surface ball speed anywhere in units or sessions, the setting creates confusion about what it actually controls. Either add a contextual description or hold it back until a feature uses it.
