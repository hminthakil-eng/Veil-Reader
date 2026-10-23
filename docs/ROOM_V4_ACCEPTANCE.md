# Room V4 implementation checklist

- [x] Additive migration 3 -> 4: no existing tables altered.
- [x] Local outbox, tombstones, known devices, per-deletion acks, conflicts, explicit capture preference.
- [x] Annotation & bookmark events committed in the same Room transaction as the source edit.
- [x] Progress events captured after durable Room progress, with transaction in existing queue.
- [x] Sync capture OFF by default; no new network permissions, service, UI, or user account.
- [x] Restoration clears stale sync state atomically and disables capture.
- [ ] Android KSP compile & generate verified schema 4.json.
- [ ] MigrationTestHelper instrumented test on Android emulator/physical device.
- [ ] Device performance/latency profiling when capture enabled.
- [ ] Authenticated server transport and user opt-in UI.
- [ ] Cloud conflict resolution, remote ACK assignment, secure remote apply.

Do NOT merge until open gates pass. Device/Android CI can be run on the draft PR.
