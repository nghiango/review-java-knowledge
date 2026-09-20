# Code Review: Weak Password Hashing & Insecure Verification

## Context
A user registration and authentication controller manages user credential storage and login validation.

## Target Files
- [`UserAccount.java`](UserAccount.java)
- [`InsecureRegistrationController.java`](InsecureRegistrationController.java)

## Task
Review `InsecureRegistrationController.java`. Identify cryptographic weaknesses, unsalted digests, lack of adaptive work factor, timing attack risks, and missing input validation.
