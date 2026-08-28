# Pint Progress

Pint Progress translates ride energy into completed pints and progress toward the next pint.

## Language

**Completed pints**:
The number of whole calorie targets reached during the ride.
_Avoid_: Total progress, pint value

**Completion transition**:
The brief full-foam and draining sequence between completing one pint and starting next-pint progress.
_Avoid_: Reset, rollover

**Next-pint progress**:
The partial progress from the latest completed pint toward the next pint.
_Avoid_: Pint count, total pints

**Field variation**:
An independently selectable presentation of pint progress. Each tile selects one variation without changing the other tiles.
_Avoid_: Mode, style

**Pints Fill**:
A field variation in which next-pint progress fills the field while completed pints remain visible over it.
_Avoid_: Fill variation, fill mode, full-tile mode

**Beer texture**:
Static bubbles, highlights, and an irregular foam boundary that make Pints Fill read as beer while keeping completed pints legible.
_Avoid_: Realistic fill, beer effect

**Foam cap**:
The thin foam layer that follows the top of the beer fill. It becomes deeper during the final fifth of next-pint progress.
_Avoid_: Foam state, completion foam

**Unavailable progress**:
The state in which usable ride energy is not available. It is distinct from zero completed pints.
_Avoid_: Zero progress
