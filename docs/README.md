# Project documentation

Use this index to load only the context needed for a task.

| Document | Use it for |
| --- | --- |
| [Architecture](ARCHITECTURE.md) | Data flow, module boundaries, state ownership, settings |
| [Karoo data-field contract](KAROO_DATA_FIELD_CONTRACT.md) | `ViewConfig`, adaptive sizing, alignment, cadence, device matrix |
| [Test boundary](TEST_BOUNDARY.md) | Test layers, coverage policy, Android and Karoo boundary |
| [Drawables](DRAWABLES.md) | Mug generation, semantic colors, geometry checks, README artwork |
| [Security](SECURITY.md) | Trust boundaries, prohibited capabilities, audit checklist |
| [Release](RELEASE.md) | Versioning, signing, CI artifacts, release evidence |
| [Troubleshooting](TROUBLESHOOTING.md) | Installation, discovery, rendering, calorie-stream, and build failures |

## Source-of-truth order

1. Executable tests and validation scripts
2. Kotlin implementation and Android resources
3. These documents
4. `README.md` product overview

When behavior changes, update the implementation, focused tests, and the relevant contract document
in the same commit.
