# Security Model Blueprint

## Security goals

The directory service protects account data, group membership, role assignments, audit history, and administrative workflows. The desktop client is treated as an untrusted presentation layer: it may guide users through tasks, but the backend must authenticate, authorize, validate, and audit every privileged operation.

The planned security model combines Role-Based Access Control (RBAC) with Mandatory Access Control (MAC):

- RBAC determines whether a principal has a named permission through direct roles or group-inherited roles.
- MAC applies non-bypassable contextual restrictions such as organizational unit scope, account sensitivity, system-role immutability, and protected audit access.

A request is allowed only when both RBAC and MAC checks pass.

## RBAC layout

RBAC concepts map to the backend packages as follows:

- `model.Role`: role identity and metadata.
- `model.Permission`: stable permission definition.
- `repository.RoleRepository`: role lookup and assignment persistence boundary.
- `repository.PermissionRepository`: permission lookup boundary.
- `service.RoleService`: role administration workflow boundary.
- `security.PermissionEvaluator`: authorization decision boundary.

Expected RBAC resolution flow:

1. Identify the authenticated user from the validated token or session.
2. Load direct role assignments for the user.
3. Load group memberships and group role assignments.
4. Expand roles into permission codes.
5. Check whether the requested action and resource type are covered by those permissions.

Direct user roles and group-inherited roles should be distinguishable in audit output so administrators can explain why a request was allowed.

## MAC layout

MAC restrictions should be enforced after RBAC grants are known and before data is changed or disclosed. MAC rules can include:

- OU scoping: a principal may only manage users inside allowed organizational units.
- Protected principals: built-in break-glass or service accounts may not be modified by ordinary administrators.
- System roles: roles marked as system-owned may not be deleted or stripped of required permissions.
- Audit confidentiality: audit logs may require a separate high-trust permission and additional OU or tenant filtering.
- Session safety: users should not be allowed to revoke or weaken mandatory controls on their own active privileged session without explicit policy support.

MAC checks belong in backend policy and security boundaries, not in JavaFX controllers. The desktop can hide controls for convenience, but hidden controls are not a security mechanism.

## JWT lifecycle

The `security.JwtProvider` boundary is responsible for issuing and validating access tokens. Access tokens should be short-lived and signed with a strong key. Refresh tokens should be long enough to support normal desktop usage but revocable through server-side session state.

Recommended lifecycle:

1. User submits credentials to `/api/v1/auth/login`.
2. Server verifies the password with `PasswordHasher`.
3. Server creates a session record through `SessionManager`.
4. Server returns a short-lived JWT access token and a refresh token.
5. Desktop client sends the JWT as a bearer token for API calls.
6. When the JWT nears expiration, the desktop client calls the refresh endpoint.
7. Logout, account disablement, password reset, or administrative revocation invalidates the refresh token and session.

JWT claims should include only the minimum required identity and authorization context. Sensitive attributes, password state, and raw permission internals should not be embedded if they could become stale or disclose unnecessary information. A token identifier claim is useful for audit correlation and revocation analysis.

## Password cryptography requirements

The `security.PasswordHasher` boundary should use a password hashing algorithm designed for credential storage, such as Argon2id, bcrypt, scrypt, or PBKDF2 with parameters approved for the deployment environment. Passwords must never be stored, logged, cached, or returned in API responses.

Requirements:

- Store only password hashes and algorithm metadata.
- Use a unique salt per password.
- Support algorithm versioning so hashes can be upgraded after login.
- Compare password verifiers in a timing-safe manner where supported.
- Enforce password policy through backend services, not only through the UI.
- Treat temporary passwords and reset flows as auditable security events.

## Session management

The `security.SessionManager` boundary coordinates session records, refresh-token hashing, revocation, and activity tracking. Refresh tokens should be stored hashed in the database. Access tokens can remain stateless for normal validation, but sensitive operations should still consult current account and session status when stale authorization would be dangerous.

Session invalidation triggers should include:

- Explicit logout.
- Password reset.
- Account disablement or lockout.
- Role or permission changes that materially reduce access.
- Administrator-initiated session revocation.

## Authorization decision auditing

Denied requests are security-relevant events. The audit layer should capture permission denials, MAC denials, authentication failures, session revocations, password resets, and role or group assignment changes.

Audit events should include:

- Actor identifier when known.
- Target resource type and identifier.
- Action code.
- Outcome.
- Correlation identifier.
- Safe contextual metadata.

Audit records should avoid secret-bearing data. Passwords, raw tokens, password hashes, and full request bodies containing credentials must not be logged.

## Desktop client security responsibilities

The JavaFX client should:

- Store tokens only as long as needed for the active session.
- Attach bearer tokens through the network layer rather than duplicating header logic in UI controllers.
- Avoid logging tokens, credentials, or full authentication responses.
- Treat server errors as authoritative.
- Avoid presenting stale permission state after token refresh or authorization failures.

The JavaFX client should not:

- Decide whether an administrator is allowed to perform a sensitive action.
- Cache password material.
- Implement fallback local authentication.
- Bypass server-side policy because a UI control is visible.

## Failure handling

Security failures should be explicit and safe. Authentication failures should not disclose whether a username exists. Authorization failures should return a consistent denial response. Server-side logs can carry diagnostic context, but user-facing responses should avoid implementation details.
