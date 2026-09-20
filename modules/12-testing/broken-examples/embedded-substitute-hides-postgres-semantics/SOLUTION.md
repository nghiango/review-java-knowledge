# Solution: In-memory repository that hides PostgreSQL semantics

## Annotated code

### `InMemoryAccountRepository.java`

```java
package lab.testing.broken.embeddedsubstitutehidespostgressemantics;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lab.testing.accounts.Account;

/**
 * In-memory stand-in for the account repository used by the account tests.
 *
 * <p>Keeps the accounts in a map so the tests do not need a database, and keys them by a lower-cased
 * email so that {@code ADA@example.com} and {@code ada@example.com} are treated as the same address.
 */
// Maintainability issue: This class is a second implementation of the repository contract, with its
// own persistence semantics to keep in step with AccountRepository. Nothing in the build ties the two
// together, so the map can drift from PostgreSQL and only the map is exercised by the suite. A test
// double for the store belongs at the boundary as a stub of the interface, not as a hand-written
// database with its own rules.
public final class InMemoryAccountRepository {

    private final Map<String, Account> byEmail = new LinkedHashMap<>();

    public Account save(Account account) {
        // Database issue: The map is keyed by a lower-cased email and put() overwrites silently.
        // PostgreSQL has a unique index on "email" and raises a constraint violation instead; it also
        // stores both rows when the addresses differ only by case, because text comparison is
        // case-sensitive. The substitute therefore neither enforces the constraint nor agrees with
        // PostgreSQL about when it applies.
        byEmail.put(account.getEmail().toLowerCase(Locale.ROOT), account);
        return account;
    }

    public Optional<Account> findByEmail(String email) {
        // Testing issue: Case-insensitive matching here is a property of the map's key, not of the
        // query. AccountRepository.findByEmail is a derived query and compares the "email" column
        // case-sensitively, so a lookup the substitute answers can return nothing in production.
        return Optional.ofNullable(byEmail.get(email.toLowerCase(Locale.ROOT)));
    }

    public List<Account> findAll() {
        // Database issue: LinkedHashMap preserves insertion order; SQL has no default row order. The
        // test below reads this as a guarantee, so ordering is only ever checked against the
        // substitute and never against the database that has to serve it.
        return new ArrayList<>(byEmail.values());
    }

    public void deleteAll() {
        byEmail.clear();
    }
}
```

### `AccountService.java`

```java
package lab.testing.broken.embeddedsubstitutehidespostgressemantics;

import java.util.List;
import lab.testing.accounts.Account;

/**
 * Registers accounts and reads their balance on top of {@link InMemoryAccountRepository}.
 *
 * <p>The address is stored and looked up exactly as the caller supplied it; the repository's
 * lower-cased key is what makes two spellings of the same address resolve to one account.
 */
public final class AccountService {

    private final InMemoryAccountRepository repository;

    public AccountService(InMemoryAccountRepository repository) {
        this.repository = repository;
    }

    public Account register(String email, String displayName) {
        // Testing issue: The duplicate check borrows its case-insensitivity from the substitute.
        // Against PostgreSQL, findByEmail("Ada@Example.com") does not match the row saved as
        // "ada@example.com" and the case-sensitive unique index accepts it too, so the second
        // registration succeeds even though the fake rejects it. Case-insensitive uniqueness is never
        // stated here, so it is not a rule the application owns.
        // Reliability issue: Check-then-act with nothing to back it up. The map cannot raise a
        // constraint violation, so two concurrent registrations of the same address both pass the
        // check and one silently overwrites the other; the suite never exercises the race or the
        // violation the database would raise.
        if (repository.findByEmail(email).isPresent()) {
            throw new IllegalStateException("email already registered: " + email);
        }
        return repository.save(new Account(email, displayName, 0L));
    }

    public long balance(String email) {
        // Testing issue: balance depends on the substitute's case-insensitive lookup. On PostgreSQL
        // the same call with a differently-cased address throws "no account for email", so the
        // contract this suite pins down does not exist in production.
        return repository
                .findByEmail(email)
                .map(Account::getBalanceCents)
                .orElseThrow(() -> new IllegalStateException("no account for email: " + email));
    }

    public List<Account> accounts() {
        return repository.findAll();
    }
}
```

### `AccountServiceTest.java`

```java
package lab.testing.broken.embeddedsubstitutehidespostgressemantics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import lab.testing.accounts.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AccountService}.
 *
 * <p>Runs without Docker: {@link InMemoryAccountRepository} stands in for the database so the suite
 * is fast and self-contained.
 */
// Testing issue: Every test in this class runs against the substitute, so nothing compares the
// service with the store it will actually use. The suite proves that the service agrees with the map
// — and matching, uniqueness and row order are exactly the things the map implements differently from
// PostgreSQL, so the whole class asserts a contract the database does not offer.
class AccountServiceTest {

    private InMemoryAccountRepository repository;
    private AccountService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryAccountRepository();
        service = new AccountService(repository);
    }

    @Test
    void register_thenBalanceWithDifferentCase_findsTheAccount() {
        service.register("ada@example.com", "Ada");

        // Testing issue: asserts the fake's case-insensitive lookup as if it were the contract.
        // AccountRepository.findByEmail is case-sensitive, so this call throws in production.
        assertEquals(0L, service.balance("ADA@EXAMPLE.COM"));
    }

    @Test
    void register_secondAddressDifferingOnlyByCase_isRejected() {
        service.register("ada@example.com", "Ada");

        // Testing issue: the rejection comes from the map's lower-cased key, not from an application
        // rule. Against PostgreSQL both registrations succeed because the unique index is
        // case-sensitive, so the suite's strongest claim about uniqueness is only true of the
        // substitute.
        assertThrows(
                IllegalStateException.class,
                () -> service.register("Ada@Example.com", "Ada Lovelace"));
    }

    @Test
    void accounts_returnsThemInTheOrderTheyWereRegistered() {
        service.register("zoe@example.com", "Zoe");
        service.register("ada@example.com", "Ada");

        // Database issue: asserts insertion order, which is LinkedHashMap's behaviour. findAll()
        // without a Sort has no defined row order on PostgreSQL, so ordering has to be requested
        // explicitly instead of assumed.
        assertEquals(
                List.of("zoe@example.com", "ada@example.com"),
                service.accounts().stream().map(Account::getEmail).toList());
    }

    @Test
    void save_sameAddressTwice_keepsOnlyOneAccount() {
        // Database issue: documents the substitute's silent overwrite as if it were the contract.
        // PostgreSQL rejects the second insert with a unique-constraint violation, so this assertion
        // describes no real database.
        // Maintainability issue: the test reaches through the service to the repository's internal
        // key, so it asserts the map's implementation rather than any behaviour of AccountService.
        repository.save(new Account("ada@example.com", "Ada", 0L));
        repository.save(new Account("ADA@EXAMPLE.COM", "Ada Lovelace", 500L));

        assertEquals(1, repository.findAll().size());
        assertEquals(
                500L, repository.findByEmail("ada@example.com").orElseThrow().getBalanceCents());
    }
}
```

`Account` (package `lab.testing.accounts`) carries no issue comments: it is the entity the correct
implementation ships. The defect is entirely in the substitute and the suite built on it — which
compiles, and passes, which is why it survived review.

## Issues

| # | Category | Severity | Location | Summary |
|---|---|---|---|---|
| 1 | Testing issue | High | `AccountServiceTest` (whole class), `AccountService.register()` / `balance()` | The suite asserts the in-memory substitute's semantics — case-insensitive matching, insertion order, silent overwrite — none of which PostgreSQL provides |
| 2 | Database issue | High | `InMemoryAccountRepository.save()` / `findByEmail()` / `findAll()` | The unique index and the `findByEmail` collation are assumed to be case-insensitive and to reject duplicates; PostgreSQL compares case-sensitively and the map overwrites silently |
| 3 | Reliability issue | High | `AccountService.register()` | A read-then-write duplicate check with no constraint behind it, tested only against a store that cannot fail, so the race and the real violation are never exercised |
| 4 | Maintainability issue | Medium | `InMemoryAccountRepository` | A hand-written second implementation of the repository that has to be kept in step with `AccountRepository` |

## Issue details

### Substitute diverges from production semantics

**Type:** Testing issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** Spring Data JPA, PostgreSQL, test doubles · **Interview frequency:** High · **Production impact:** High

**Location:** `AccountServiceTest` (whole class), `AccountService.register()` / `balance()`

#### Problem
Every test runs against `InMemoryAccountRepository`, so the suite asserts what the map does rather
than what the database does. The map matches addresses case-insensitively, returns rows in insertion
order and lets a duplicate address overwrite the previous row. PostgreSQL compares `email`
case-sensitively in both the unique index and the `findByEmail` query, and returns rows in no
particular order unless a `Sort` asks. The suite is therefore green against a store the service never
runs on.

#### Why it happens
Replacing the repository is the fastest way to keep a service test Docker-free, and a map is the
smallest thing that satisfies the methods the service calls. The substitute is written to make the
service testable, not to reproduce the store, so the differences are invisible until the two are
compared.

#### Production impact
```text
register("ada@example.com") then register("Ada@Example.com")
→ the fake rejects the second call, so the suite proves uniqueness works
→ PostgreSQL accepts it: findByEmail is case-sensitive and the unique index is too
→ two accounts exist for one address; the user's balance is split across them
→ the suite never noticed, because it never spoke to PostgreSQL
```

#### Broken implementation
```java
assertEquals(0L, service.balance("ADA@EXAMPLE.COM"));       // map's lower-cased key
assertThrows(IllegalStateException.class,
        () -> service.register("Ada@Example.com", "Ada Lovelace"));   // map's lower-cased key
```

#### Correct implementation
```java
// src/integrationTest/java/lab/testing/accounts/AccountRepositoryIT.java
@Test
void register_emailDifferingOnlyByCase_isRejected() {
    service.register("ada@example.com", "Ada");

    assertThatThrownBy(() -> service.register("  Ada@Example.com  ", "Ada Lovelace"))
            .isInstanceOf(AccountService.DuplicateEmailException.class);
}
```

#### Why the solution works
`AccountService` normalises the address (trim + lower-case) before it stores or queries it, so the
rejection is an application rule the service states, not a property of whichever store is plugged in.
The integration test runs that rule against a real PostgreSQL container, so the case-sensitive index
and query are part of what is exercised.

#### Trade-offs
A Docker-backed test is slower than a map and needs the container infrastructure. Keep the fast unit
tests for pure logic, but test persistence semantics — matching, constraints, ordering, transaction
boundaries — against the real engine, because that is where the semantics live.

#### How to detect it
For every hand-written repository double, list the guarantees of the real repository (types,
collation, constraints, ordering, isolation) and ask which of them the double reproduces. Any
guarantee the double supplies "for free" is a candidate for this defect.

#### Interview follow-up
> Which behaviours must a repository test double reproduce to be honest, and when is a real database
> the only acceptable option?

#### Related
- Test doubles · `@DataJpaTest` · Testcontainers · Test pyramid

### Constraint and collation assumptions are unverified

**Type:** Database issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** PostgreSQL, Spring Data JPA · **Interview frequency:** High · **Production impact:** High

**Location:** `InMemoryAccountRepository.save()` / `findByEmail()` / `findAll()`

#### Problem
The substitute assumes three things about the database that PostgreSQL does not do: that a unique
index on `email` is case-insensitive, that a duplicate insert fails, and that `findAll()` returns rows
in insertion order. None of the three is verified anywhere, and all three are load-bearing for the
service's uniqueness rule.

#### Why it happens
An in-memory store has no schema, so there is nothing to enforce a constraint and no collation to
disagree with. `LinkedHashMap` ordering and `put()` overwriting look like incidental implementation
details rather than database semantics being quietly redefined.

#### Production impact
```text
the unique index is case-sensitive (PostgreSQL default, no citext, no lower(email) index)
→ "Ada@Example.com" inserts a second row for an existing customer
→ a duplicate-email cleanup migration is needed, and until then lookups return one of two rows
```

#### Broken implementation
```java
byEmail.put(account.getEmail().toLowerCase(Locale.ROOT), account);   // no constraint, silent overwrite
return new ArrayList<>(byEmail.values());                            // insertion order, not SQL
```

#### Correct implementation
```java
@Entity
@Table(name = "accounts",
        uniqueConstraints = @UniqueConstraint(name = "uk_accounts_email", columnNames = "email"))
public class Account { /* ... */ }

// and ordering is requested, never assumed:
repository.findAll(Sort.by(Sort.Direction.ASC, "email"));
```

#### Why the solution works
The unique constraint is declared on the mapping and created by the schema, so the database — not the
application's read-then-write check — is what finally rejects a duplicate. The integration test saves
two addresses that differ only by case and asserts both persist, proving the index is case-sensitive
and that normalisation, not the index, is what provides case-insensitive uniqueness. Ordering is
asserted only where a `Sort` was requested.

#### Trade-offs
A case-insensitive rule enforced by normalising in the application is portable but must be applied at
every entry point. The alternatives — a `citext` column or a `lower(email)` functional index — push
the rule into the database, which is stronger but PostgreSQL-specific and needs a migration.

#### How to detect it
Compare every assumption a test double makes about matching, ordering and constraints with the schema
and the query. `EXPLAIN` the real query, inspect the index definition, and assert the constraint by
attempting the conflicting write against the real engine.

#### Interview follow-up
> How would you make email uniqueness case-insensitive in PostgreSQL, and what are the trade-offs of
> each option?

#### Related
- Unique constraints · Collation · `citext` · Functional indexes · Testcontainers

### Defects reach production undetected

**Type:** Reliability issue · **Severity:** High · **Difficulty:** Intermediate
**Technology:** JUnit 5, Spring Data JPA, PostgreSQL · **Interview frequency:** High · **Production impact:** High

**Location:** `AccountService.register()`

#### Problem
`register` reads for an existing address and then writes, with nothing but the substitute's map to
back the check. The map cannot raise a constraint violation, so the suite never executes the failure
the database will produce, and never runs the two registrations concurrently. A duplicate check that
is only ever tested against a store that cannot fail is not verified at all.

#### Why it happens
The check-then-act shape reads naturally and passes every test the fake can run. Because the fake
always answers the lookup, the branch where the database rejects the write is unreachable, so the code
that has to handle it (and the constraint that has to exist) is never needed.

#### Production impact
```text
two requests register the same address at the same time
→ both findByEmail calls return empty, both save
→ without a unique index both rows commit; with one, the loser gets a
  DataIntegrityViolationException that nothing maps to a 409
→ the caller sees a 500, and the duplicate exists in the meantime
```

#### Broken implementation
```java
if (repository.findByEmail(email).isPresent()) {        // racy read
    throw new IllegalStateException("email already registered: " + email);
}
return repository.save(new Account(email, displayName, 0L));   // nothing enforces the rule
```

#### Correct implementation
```java
// the unique index is the arbiter; the check is an optimisation that produces a clean error
@Transactional
public Account register(String email, String displayName) {
    String normalisedEmail = normalise(email);
    if (repository.findByEmail(normalisedEmail).isPresent()) {
        throw new DuplicateEmailException(normalisedEmail);
    }
    return repository.save(new Account(normalisedEmail, displayName, 0L));
}
```

#### Why the solution works
With the unique constraint on the column, the losing writer of a concurrent registration fails at
commit with a `DataIntegrityViolationException` instead of creating a second row. The integration test
exercises the constraint directly, so the failure path is covered by a test rather than left to
production. The pre-check stays only to turn the common case into a domain exception.

#### Trade-offs
Relying on the constraint means the application must translate a persistence exception into its own
error contract (a 409), which couples the service to the exception type. That is cheaper than a
correct distributed lock, and the constraint has to exist regardless.

#### How to detect it
Look for read-then-write uniqueness checks whose only test double cannot fail. Then check that the
column actually carries a unique constraint and that a test attempts the conflicting write.

#### Interview follow-up
> Why is a database constraint the right place for a uniqueness rule, and how would you surface its
> violation as a proper API error?

#### Related
- Unique constraints · Check-then-act races · `DataIntegrityViolationException` · Idempotency

### The fake duplicates repository logic

**Type:** Maintainability issue · **Severity:** Medium · **Difficulty:** Intermediate
**Technology:** Spring Data JPA test doubles · **Interview frequency:** Medium · **Production impact:** Medium

**Location:** `InMemoryAccountRepository`

#### Problem
`InMemoryAccountRepository` is a second implementation of the repository contract. It has to be kept in
step with `AccountRepository` by hand: add a query, change a matching rule or a constraint, and the map
is now wrong in a way only the suite can see — and the suite is the thing that reads the map. Nothing
in the build links the two.

#### Why it happens
The map is written next to the test that needs it, so it looks like test scaffolding rather than a
parallel implementation of persistence. Because it compiles and the tests pass, there is no signal that
it is a maintenance liability.

#### Production impact
```text
AccountRepository gains findByEmailIgnoreCase or a new derived query
→ the fake does not, and the tests written against it still pass
→ developers reason about persistence from the fake and ship queries that do not exist
→ the fake becomes the de facto specification of the repository, and it is wrong
```

#### Broken implementation
```java
public final class InMemoryAccountRepository {   // a hand-written database, owned by no one
    private final Map<String, Account> byEmail = new LinkedHashMap<>();
    // save / findByEmail / findAll re-implemented with different semantics
}
```

#### Correct implementation
```java
// No repository double at all: the persistence layer is exercised against PostgreSQL.
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = Replace.NONE)
@Import({TestingJpaConfiguration.class, AccountService.class})
class AccountRepositoryIT { /* ... */ }
```

#### Why the solution works
There is only one implementation of the repository contract — the Spring Data interface — and it is
tested against the engine that will run it. There is nothing to keep in step, and the semantics the
tests assert are the store's own.

#### Trade-offs
Giving up the map means the tests need a container and are slower. Reserve test doubles for
collaborators where the interaction, not the semantics, is the subject; where the store's behaviour is
the subject, test the store.

#### How to detect it
Search test sources for classes whose name mirrors a repository (`InMemory…Repository`, `Fake…Repository`,
`Stub…Repository`) and whose body re-implements queries. Each one is a contract with two
implementations and one test.

#### Interview follow-up
> When is an in-memory repository double acceptable, and how would you keep it from becoming a second
> implementation of the contract?

#### Related
- Test doubles · Contract tests · Testcontainers · Test data builders

## Why H2 hides the same failure

H2 is the other popular substitute for PostgreSQL, and it hides the *same* class of defect as the
map: a different engine's semantics are silently substituted for the ones the service ships against.
H2's `MODE=PostgreSQL` compatibility mode translates syntax, not semantics — the engine still has its
own collation, its own type coercion, its own constraint and locking behaviour and its own row
ordering. A suite that passes against H2 therefore proves that the service works against H2.

Concretely, the failure this example documents reappears under H2:

- **Matching.** H2's default comparison rules, its case handling and its `MODE=PostgreSQL` emulation
  are not PostgreSQL's. A query or a unique index that behaves one way on H2 can behave differently
  on the real server, so case-sensitivity has to be asserted against PostgreSQL.
- **Constraints.** H2 accepts schema and statements PostgreSQL rejects, and vice versa, so a
  constraint that the tests "prove" exists may not be created the same way (or at all) on PostgreSQL.
- **Ordering.** H2 frequently returns rows in insertion order, which is exactly the accident the map
  in this example relies on; SQL guarantees no such thing, so an ordering test written against H2
  passes for the wrong reason.
- **Types and isolation.** `VARCHAR` length enforcement, `timestamptz`, `jsonb`, sequences and MVCC
  visibility all differ, so defects in those areas are invisible to an H2 suite.

This repository bans H2 for persistence behaviour (see `AGENTS.md` §1): an H2 test is a substitute
with the same defect as `InMemoryAccountRepository`, only further from the code. Infrastructure
semantics are tested against PostgreSQL through Testcontainers
([`SharedPostgresContainer`](../../../test-support/src/main/java/lab/testsupport/SharedPostgresContainer.java)),
and the JPA slice is wired with `@AutoConfigureTestDatabase(replace = Replace.NONE)` so Boot cannot
swap the container-backed DataSource for an embedded one.

## Correct implementation

The production-ready counterpart lives in `lab.testing.accounts`:

- [`Account.java`](../../src/main/java/lab/testing/accounts/Account.java) — the JPA entity, with a
  unique constraint on `email` so the database, not a read-then-write check, is the arbiter of
  uniqueness.
- [`AccountRepository.java`](../../src/main/java/lab/testing/accounts/AccountRepository.java) — the
  Spring Data interface; `findByEmail` is a derived query and therefore case-sensitive, which is
  documented as a contract rather than papered over.
- [`AccountService.java`](../../src/main/java/lab/testing/accounts/AccountService.java) — normalises
  the email (trim + lower-case) before storing or querying it, so case-insensitive uniqueness is an
  explicit application rule, and rejects a duplicate with a typed `DuplicateEmailException`.
- [`AccountRepositoryIT.java`](../../src/integrationTest/java/lab/testing/accounts/AccountRepositoryIT.java)
  — the persistence test against a real PostgreSQL container: it asserts that addresses differing only
  by case both persist (the index is case-sensitive), that `register` normalises and rejects the
  duplicate, that `findByEmail` is case-sensitive, that ordering must be requested with `Sort`, and
  that `balance` reflects persisted state across a flush/clear boundary.

The container is shared through
[`SharedPostgresContainer`](../../../test-support/src/main/java/lab/testsupport/SharedPostgresContainer.java)
and published as a `@ServiceConnection` bean from `TestingJpaConfiguration`, so the DataSource is
derived from the running container.

Walkthrough and trade-offs: [Testing — solutions](../../../../docs/topics/testing/solutions.md).
