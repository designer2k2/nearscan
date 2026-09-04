# Security Policy

## Supported versions

NearScan is developed on a rolling basis. Only the latest release
([GitHub Releases](https://github.com/designer2k2/nearscan/releases) / Google Play) receives
fixes.

## Reporting a vulnerability

Please **open a [GitHub issue](https://github.com/designer2k2/nearscan/issues)** describing the
*class* of problem — for example "exported component X is reachable without the expected
permission" or "exported file Y leaks Z" — rather than posting a working exploit or a
step-by-step extraction path.

If you'd prefer to share details privately first, say so in the issue (no details needed) and the
maintainer will arrange a private channel, or use
[GitHub's private vulnerability reporting](https://github.com/designer2k2/nearscan/security/advisories/new)
if it's enabled.

## Scope notes

NearScan has no backend server and transmits nothing by default. The most security-relevant
surfaces are:

- The exported `ContentProvider` and `BroadcastReceiver` used for automation
  (`at.designer2k2.nearscan.provider`, the `CMD_*` actions) — guarded by a `normal`-level custom
  permission, which is a namespace guard rather than a hard boundary.
- Files written by the export feature and shared via `FileProvider`.
- Optional MQTT publishing to a user-configured broker.

Findings in any of these are in scope and welcome.
