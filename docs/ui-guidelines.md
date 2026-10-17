# Wear OS UI guidelines

Use these rules when adding or revising screens. They capture the design decisions from the Auto Count and counter reviews.

## Layout and actions

- Center sparse content on round displays. Center button labels so they remain balanced inside wide pill controls.
- Leave about 10dp between a page heading and its first action. Keep related title and helper text visually grouped.
- Keep primary touch targets at least 48dp and inside the round display's safe area. Give icon-only controls meaningful TalkBack descriptions.
- On entry and setup screens, keep the next action and bottom Back arrow visible together at default text size on a 192dp round display. Let secondary details scroll when needed.
- Give peer options equal size, spacing, color, and visual weight. Do not make one option look recommended unless the product intentionally recommends it. A destructive action may use a warning color.
- Use the gold accent for a single clear primary action or key information, not as an arbitrary selected state. Keep secondary and alternative options visually legible.
- Use one clear action per step. When a control changes state, update it in place where possible (for example, microphone to stop square) instead of adding a redundant confirmation screen.
- Keep text inside the round display's safe width. Let short helper text wrap instead of clipping at the curved edge.
- Use familiar, direct action labels such as “Play” and “Record.” Keep simple adjustments inline and show their level with a compact progress bar.
- Keep Back as a centered bottom arrow. If it also means “Not now” or “Cancel,” make that meaning clear to users and expose an accurate accessibility label.

## Language and feedback

- Keep instructions short, concrete, and understandable to a child who can read the app's language. State the next action and the result of tapping it.
- Use familiar action words such as “Start,” “Stop,” and “Next chant.” Avoid implementation terms such as “trial” when the user is simply chanting.
- Do not imply that audio is captured on the pace-only path. Explain that it only measures time and saves no sound.
- For microphone use, say what is recorded, why it is used, where it stays, and how it can be deleted before asking Android for permission.
- Distinguish active recording visibly, and make the stop action immediately available. Preserve a clear way to decline permission and continue using the rest of the app.

## Wearable checks

- Check 192dp and 227dp round layouts at default and enlarged system font sizes.
- Verify labels do not clip at the curved edge, and check touch targets with Compose semantics as well as on a physical watch.
- Test actual crown input, haptics, ambient mode, and icon-only controls on hardware where applicable.
