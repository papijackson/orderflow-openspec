# AI Context Strategy

## Purpose
This document explains how to keep the SDD kit rich without sending every rule to the model on every interaction.

## Context Layers

### Layer 1 — Stable Global Context
Keep short:
- `.specify/memory/constitution.md`
- `.github/copilot-instructions.md`

These define non-negotiable principles, stack, workflow, and routing.

Target: roughly 3–5k tokens combined.

### Layer 2 — Specialized Context
Use `.github/instructions/*.instructions.md` according to the files and concerns involved.

Examples:
- Entity/repository -> architecture + Java + Spring Boot + JPA/PostgreSQL + testing
- SQS consumer -> architecture + Java + Spring Boot + messaging + AWS + testing
- Cucumber -> testing + cucumber
- performance-sensitive DB change -> JPA/PostgreSQL + performance + observability

Do not reproduce unrelated instructions into the active prompt.

### Layer 3 — Feature Context
The current:
- specification;
- plan;
- tasks;
- relevant performance analysis.

These are the most important contextual artifacts for the current change.

## Token Guidance
File size in Git is not equal to tokens consumed in every interaction.

Recommended targets:
- constitution: <= 2.5k tokens;
- global Copilot instructions: <= 2.5k;
- specialized instructions: typically 2–4.5k each;
- active feature context: ideally < 25–30k tokens where practical.

The full repository may contain substantially more tokens than the active context.

## Why Detailed Instructions Are Still Valuable
Specialized Markdown serves several purposes:
- AI guidance;
- team documentation;
- architecture contract;
- review reference;
- skill input;
- checklist basis.

Do not remove important rules merely to reduce repository size.

## Executable Governance
Use Markdown to explain rules and executable controls to enforce them:
- ArchUnit;
- Maven build;
- JUnit;
- Testcontainers;
- Flyway validation;
- SonarQube;
- security/dependency scanning;
- GitHub Actions;
- production gates.

This reduces dependence on the model remembering every rule.

## Design Principle
Prefer:
`rich knowledge base + selective context + executable gates`

over:
`one huge prompt containing the entire engineering policy`.
