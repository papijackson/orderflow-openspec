# Branch + SDD Workflow

For an existing Git repository:

```text
main/base
   |
   +-- feature/<name>
          |
          +-- spec.md
          |
          +-- plan.md
          |
          +-- tasks.md
          |
          +-- implementation
          |
          +-- convergence
          |
          +-- Pull Request
          |
          +-- human approval
          |
          +-- merge/deploy
```

The branch is the traceability container for the feature.

Recommended commit discipline:
- specification/plan artifacts;
- implementation;
- tests/quality gates;
- convergence fixes.

The exact commit strategy may follow the organization's Git policy.
