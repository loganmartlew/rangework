# Tags are planning-only; analytics classification deferred

Tags classify Practice Units and Practice Sessions for organisation, filtering, and AI discovery, but they are **not** captured in a Range Session's Snapshot. The Snapshot is the system's immutable execution record; freezing tags into it would commit us — before any analytics feature exists — to guessing which tags (unit vs session) and what form (code vs name) a future metrics model needs.

We considered snapshotting tags now to support eventual "record metrics against a Range Session" analytics, and rejected it: deferring is cheap and additive (analytics only needs classification captured *from the point it ships forward*, since historical sessions carry no metrics anyway), whereas snapshotting now bakes an unvalidated guess into the one record designed never to change.

When metrics-against-snapshots is built, that feature owns the decision of how to capture classification at Range Session start. **Default Tags** (stable, shared, coded, and immune to user rename/delete) are the intended dimension to snapshot then; **Custom Tags** are private organisational labels and a poor analytics axis (a user deleting one would orphan its history).
