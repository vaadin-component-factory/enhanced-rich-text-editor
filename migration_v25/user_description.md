# Migrating ERTE to Vaadin 25

## Terminology

| Term | Meaning |
|------|---------|
| **ERTE** | Enhanced Rich Text Editor — this project |
| **RTE** | Vaadin's built-in Rich Text Editor (commercial component) |
| **RTE 1** | The RTE in Vaadin 24 and earlier, based on Quill 1 |
| **RTE 2** | The RTE starting from Vaadin 25, based on Quill 2 |
| **ERTE 1** | The current (unmigrated) ERTE, built on top of RTE 1 |
| **ERTE 2** | The target: migrated ERTE, built on top of RTE 2 |

## Background

ERTE 1 was originally forked from RTE 1. Over time, the codebase gap between the two has grown — new features and fixes added to RTE 1 were never ported back into ERTE 1. Additionally, Quill 1 is now outdated and RTE 2 has moved to Quill 2.

The goal of this document is to describe the migration from ERTE 1 to ERTE 2, using RTE 2 as the new base.

## Migration Steps

The migration involves several workstreams:

1. **Update the project base** to Vaadin 25 (stable 25.0.x release, no pre-releases)
2. **Rebuild ERTE from scratch** on top of the latest RTE 2
3. **Migrate custom features** from ERTE 1 to ERTE 2 that are not already provided by RTE 2
4. **Migrate the table extension** to Quill 2 / Parchment 3

### Feature Migration

Some ERTE 1 features may already be available in RTE 2 — for instance, list indentation is built into Quill 2. Before migrating anything, each ERTE feature must be checked against Quill 2 and RTE 2 to avoid duplicating functionality that already exists.

### Table Extension

The table addon has a complex lineage: it originated as a fork of a Quill 2 addon that was backported to Quill 1, then further modified to work with ERTE 1. Rather than migrating this adapted code directly, the preferred approach is to evaluate whether the original Quill 2 addon can be used as a starting point, with ERTE-specific modifications applied on top. However, feasibility needs to be carefully assessed first.

## Non-Regression

All features currently available in ERTE 1 must remain available in ERTE 2. To ensure this, a complete description of all ERTE use cases must be documented before any migration work begins, so that results can be validated against these requirements.

## Updatability

A key goal is to avoid repeating the mistake of maintaining a full copy of the RTE codebase. Instead of forking, ERTE 2 should **extend** RTE 2, so that future updates, fixes, and new features in RTE 2 are automatically inherited by ERTE 2.

This means ERTE 2 will not copy and modify the RTE source code. Instead, it should hook into the existing codebase at runtime wherever possible.

For example: the RTE toolbar does not support dynamic modification or custom items. ERTE 1 solved this by maintaining a full copy of the toolbar with hardcoded slots — but this means toolbar updates in RTE never reach ERTE. A better approach would be to inject ERTE's toolbar slots at runtime using DOM selectors and JavaScript, rather than duplicating the entire toolbar template.

This runtime-extension approach will be the most challenging part of the migration, as it requires working around the limited extensibility of RTE 2. But it is essential for long-term maintainability.
