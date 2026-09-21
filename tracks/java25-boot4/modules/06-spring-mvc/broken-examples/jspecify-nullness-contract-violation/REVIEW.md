# Code Review Target: JSpecify Nullness Contract Violation

## Overview
A customer profile microservice adopts JSpecify null-safety across its package with `@NullMarked` in `package-info.java`. `CustomerProfileController` handles customer queries and returns `CustomerProfileResponse`.

## Code Under Review
Review `package-info.java`, `CustomerProfileResponse.java`, and `CustomerProfileController.java`.

## Questions for the Reviewer
1. In a `@NullMarked` package, what is the nullness contract of an unannotated `String` field like `phoneNumber`?
2. What happens if `@RequestParam(required = false)` resolves to `null` and is passed directly to the record constructor?
3. How should JSpecify null-safety contracts be declared on optional request parameters and optional DTO response fields?
