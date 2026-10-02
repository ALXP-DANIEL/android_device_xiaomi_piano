# Keyboard key and hall validation

The DND accessibility service is enabled only while the MCU reports a connected
cover. It filters F23 (scan 193), mapped to BUTTON_2, from vendor 15d9/product 00a3, toggles Priority/All
once on initial key down, and consumes its matching key release/repeats. Other
input devices and other enabled accessibility services are preserved. It does
not retrieve window contents. Privileged permissions are allowlisted.

On attach/detach, input-device gating is refreshed immediately; registered HID
devices must not remain enabled after the MCU reports detach.

Hall tablet-mode transitions are logged alongside the angle. Because piano
polarity is unverified, an assertion only gains gating authority after a known
folded angle agrees. That state remains protective if gravity becomes unknown;
a hall release, a working angle or detach clears it. This does not implement
an independent lid listener; cover sleep remains the framework lid behavior.

Required proof before marking hardware support complete:

1. Compile PianoParts with the final sources and build its permission XMLs.
2. Boot enforcing without privapp-permission exceptions.
3. Confirm F23 arrives as BUTTON_2 from product 00a3 on the real cover.
4. Tap: one DND toggle. Hold/release: no further toggles. Check other keyboard
   and gamepad BUTTON_2 input, including other BUTTON_2 scan codes on the cover,
   is unaffected.
5. Attach/detach with another accessibility service enabled; its settings and
   behavior must remain intact. Detach must disable all cover input devices.
6. Exercise working, closed and folded-back angles. Compare hall transitions
   against physical state before enabling any uncorroborated hall gating.
7. Sleep/wake while folded, then unfold; keys and touchpad must recover.
   Include the first working-angle sample after a sensor reset, when the angle
   helper may report no working-state transition.

8. Delay an authentication reply, detach and attach another cover (or request
   re-authentication), then deliver the old result. It must be logged as obsolete
   and must neither authorize automatic updating nor reject the current cover.

XML and transition checks pass. The final keyboard Java sources compile with
JDK 21 against the generated Android framework and existing app classes, using
freshly generated MiDevAuth AIDL and R classes. aapt2 compiled and linked the
strings and key-service XML in an isolated resource package. Full PianoParts
packaging, boot permission validation and real-cover testing remain pending.
