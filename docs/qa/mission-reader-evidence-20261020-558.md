# Exact reader evidence inspection — PR558

Input source e182b46cdf982383c96bbe26dfc0dc521263c08c.
Owned artifacts: source verification12303183743 from39502326161, digest e1152dfdc0657fb0a0af92ed70289358212c69459e90d4766e3c072702fe5b59; device/captures12301927946 from39502326165, digest4de538f7c89f991cc74ed7d68f3142bbe246cb0d66b2cb7d15eb35422803f622.

Read-only mission replaces the completed dispatch control. No checkout or execution of artifact/source code. Verify GitHub metadata head/run, bounded ZIP size and SHA256, then parse bounded JUnit/lint XML and list only series-capture names. Do not treat inventory as visual inspection or physical/TalkBack acceptance. Assert zero failures/errors/skips and16 capture filenames; logs are returned evidence.

Permissions contents/actions read; no write-capable token or other secrets. Timeout5minutes. Retire workflow definition after logs/results are preserved; retain diagnostic branch only for QA handoff value. Product branch remains unchanged.
