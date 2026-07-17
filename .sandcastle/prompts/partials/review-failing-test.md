**Does each fix address the bug the spec describes, or does it just make the test
pass?** That is the failure mode this stage exists to catch. The suite being green
tells you nothing on its own — the fixer was graded on green, so green is what you
would get either way.

A fix that special-cases the test's inputs, weakens an assertion, short-circuits
the code path, or narrows a guard to exactly the tested value is **test-gaming** —
report it even though the suite passes.

Read the test the fix satisfies, then ask: what is the *smallest* change to the
production code that would still pass this test, and is that what happened? If the
fix and the test were written to fit each other rather than to fit the bug, say so.
