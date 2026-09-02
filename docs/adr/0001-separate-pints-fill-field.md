---
status: superseded by ADR-0002
---

# Give Pints Fill its own Karoo data type

Pints Fill is a separate graphical field that shares pint progress and the global calorie target with Pint Mug and Pints Count. Karoo does not provide a per-tile appearance setting for extension fields, so a separate data type lets each tile select its presentation independently. We accept a third picker entry and a durable type identity instead of a global mode that would change every Pint Mug tile together.
