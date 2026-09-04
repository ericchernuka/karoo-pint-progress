---
status: accepted
---

# Remove the Pint Mug field

Pint Progress exposes only Pints Fill and Pints Count because they cover the graphical and native
numeric presentations with less field-specific code. We removed the released `pint-progress` type
without a compatibility alias, so existing Pint Mug tiles require manual replacement, while the
shared mug icon and beer model remain part of the product.
