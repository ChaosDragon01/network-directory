# Database Schema Blueprint

## Schema goals

The directory database must represent identities, group membership, roles, permissions, sessions, audit history, and nested organizational units. The schema should protect referential integrity through primary keys, foreign keys, uniqueness constraints, and carefully constrained hierarchy updates.

Identifiers below are described as string-like stable IDs, but the implementation may use UUID, ULID, or database-native generated keys as long as identifiers remain stable across renames.

## Core tables

### `users`

Represents an account that can authenticate or be administered.

Key columns:

- `id`: primary key.
- `username`: unique login name.
- `display_name`: human-readable name.
- `email`: optional unique email address.
- `password_hash`: password verifier output, never the raw password.
- `password_algorithm`: algorithm and version marker for migration support.
- `status`: account lifecycle state such as `ACTIVE`, `LOCKED`, or `DISABLED`.
- `organizational_unit_id`: foreign key to `organizational_units.id`.
- `created_at`, `updated_at`: UTC timestamps.

Relationships:

- Many users belong to one organizational unit.
- Many users can belong to many groups through `group_memberships`.
- Many users can have many roles through `user_roles`.

### `groups`

Represents a named membership collection.

Key columns:

- `id`: primary key.
- `name`: group display name, unique within the intended namespace.
- `description`: administrative description.
- `organizational_unit_id`: optional foreign key to `organizational_units.id`.
- `created_at`, `updated_at`: UTC timestamps.

Relationships:

- Many groups can contain many users through `group_memberships`.
- Many groups can carry many roles through `group_roles`.

### `group_memberships`

Join table between users and groups.

Key columns:

- `group_id`: foreign key to `groups.id`.
- `user_id`: foreign key to `users.id`.
- `created_at`: membership creation timestamp.
- `created_by`: optional foreign key to `users.id` for the administrator who added the member.

Primary key:

- Composite primary key on `group_id` and `user_id`.

This prevents duplicate membership rows and makes membership removal deterministic.

### `roles`

Represents a named set of permissions.

Key columns:

- `id`: primary key.
- `name`: unique role name.
- `description`: administrator-facing explanation.
- `system_role`: boolean indicating whether the role is protected from deletion or unsafe edits.
- `created_at`, `updated_at`: UTC timestamps.

### `permissions`

Represents a specific action grant, such as `USER_READ`, `USER_CREATE`, or `AUDIT_VIEW`.

Key columns:

- `id`: primary key.
- `code`: unique stable permission code.
- `description`: explanation of the capability.
- `resource_type`: protected resource category.
- `action`: action name.

### `role_permissions`

Join table between roles and permissions.

Key columns:

- `role_id`: foreign key to `roles.id`.
- `permission_id`: foreign key to `permissions.id`.

Primary key:

- Composite primary key on `role_id` and `permission_id`.

### `user_roles`

Direct role assignments to users.

Key columns:

- `user_id`: foreign key to `users.id`.
- `role_id`: foreign key to `roles.id`.
- `created_at`: assignment timestamp.
- `created_by`: optional foreign key to `users.id`.

Primary key:

- Composite primary key on `user_id` and `role_id`.

### `group_roles`

Role assignments inherited through group membership.

Key columns:

- `group_id`: foreign key to `groups.id`.
- `role_id`: foreign key to `roles.id`.
- `created_at`: assignment timestamp.
- `created_by`: optional foreign key to `users.id`.

Primary key:

- Composite primary key on `group_id` and `role_id`.

## Organizational unit hierarchy

### `organizational_units`

Represents the nested directory tree used to organize accounts and groups.

Key columns:

- `id`: primary key.
- `parent_id`: nullable foreign key to `organizational_units.id`.
- `name`: OU display name.
- `path`: materialized path or canonical path string.
- `depth`: zero-based depth from the root.
- `created_at`, `updated_at`: UTC timestamps.

Recommended constraints:

- `parent_id` references `organizational_units.id`.
- `parent_id` is nullable only for the root OU.
- Unique constraint on `parent_id` and `name` to prevent duplicate sibling names.
- Check constraint preventing `id = parent_id` where supported.

## Avoiding recursion bugs in OU trees

A self-referential hierarchy can fail in dangerous ways if cycles are allowed. The schema and service layer should work together to prevent them.

### Cycle prevention rules

When creating or moving an OU:

1. Reject a parent that is the same OU.
2. Reject a parent that is any descendant of the OU being moved.
3. Recompute `path` and `depth` for the moved OU and all descendants in a bounded transaction.
4. Enforce a maximum tree depth that prevents unbounded traversal in API responses and UI rendering.
5. Use iterative traversal or database recursive queries with explicit depth limits instead of unbounded recursive Java calls.

### Parent-child traversal strategy

For read operations, the server can build trees by loading a bounded set of rows ordered by `parent_id`, `depth`, and `name`, then assembling nodes in memory using an ID map. This avoids recursive database calls for each OU and prevents stack overflows in the application if malformed data is encountered.

For write operations, moves should be serialized for the affected subtree. The service should lock the moving OU and relevant descendants or use transaction isolation that prevents concurrent moves from producing inconsistent paths.

## Sessions and refresh tokens

### `sessions`

Represents authenticated desktop sessions and refresh-token lifecycle state.

Key columns:

- `id`: primary key.
- `user_id`: foreign key to `users.id`.
- `refresh_token_hash`: hashed refresh token.
- `client_id`: client application identifier.
- `device_label`: workstation label supplied at login.
- `issued_at`, `expires_at`, `revoked_at`: UTC timestamps.
- `last_seen_at`: optional activity timestamp.

Refresh tokens should be stored as hashes so database disclosure does not immediately grant active sessions.

## Audit tables

### `audit_logs`

Records security-relevant and administrative events.

Key columns:

- `id`: primary key.
- `occurred_at`: UTC timestamp.
- `actor_id`: nullable foreign key to `users.id` for system or anonymous events.
- `action`: stable event code.
- `target_type`: resource category.
- `target_id`: target resource identifier.
- `outcome`: `SUCCESS`, `FAILURE`, or `DENIED`.
- `correlation_id`: request correlation identifier.
- `metadata_json`: bounded JSON object for non-sensitive context.

Audit metadata must not include passwords, raw tokens, password hashes, or full secret-bearing request bodies.

## Indexing guidance

Recommended indexes:

- `users(username)` unique.
- `users(email)` unique when email is present.
- `users(organizational_unit_id)` for OU-scoped user queries.
- `groups(organizational_unit_id)` for OU-scoped group queries.
- `group_memberships(user_id)` for reverse membership lookup.
- `user_roles(user_id)` and `group_roles(group_id)` for permission evaluation.
- `role_permissions(role_id)` for role expansion.
- `organizational_units(parent_id)` for tree loading.
- `organizational_units(path)` for subtree queries when using materialized paths.
- `audit_logs(occurred_at)` and `audit_logs(actor_id, occurred_at)` for audit review.
