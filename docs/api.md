# REST API Blueprint

## API conventions

The desktop client communicates with the headless server through JSON REST endpoints. All endpoints should be versioned under `/api/v1` so future protocol changes can be introduced without breaking existing desktop clients.

Unless explicitly public, endpoints require a bearer token in the `Authorization` header:

```http
Authorization: Bearer <access-token>
```

Requests and responses should use `application/json`. Date and time values should be emitted as ISO-8601 strings in UTC. Identifiers should be stable server-generated values rather than display names.

## Standard response fields

Successful object responses should include the requested resource or command result. Collection responses should include pagination metadata so large directories are not loaded in one request.

Common error responses should include:

```json
{
  "errorCode": "ACCESS_DENIED",
  "message": "The authenticated principal is not allowed to perform this action.",
  "correlationId": "01HX7Y4XK7N7F4M9Z8DQ9X7C5A"
}
```

The `message` field should be safe for users. Sensitive implementation details, passwords, token contents, stack traces, and SQL fragments must not appear in API output.

## Authentication endpoints

### `POST /api/v1/auth/login`

Authenticates an administrator or directory user and returns token material for the desktop session.

Request fields:

```json
{
  "username": "alice.admin",
  "password": "user-supplied-secret",
  "clientId": "desktop-console",
  "deviceLabel": "Alice Workstation"
}
```

Response fields:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "opaque-refresh-token",
  "tokenType": "Bearer",
  "expiresAt": "2026-06-02T18:30:00Z",
  "principal": {
    "id": "usr_01HX7Y...",
    "username": "alice.admin",
    "displayName": "Alice Admin",
    "roles": ["DirectoryAdmin"]
  }
}
```

### `POST /api/v1/auth/refresh`

Issues a new access token from a valid refresh token.

Request fields:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Response fields:

```json
{
  "accessToken": "new-jwt-access-token",
  "tokenType": "Bearer",
  "expiresAt": "2026-06-02T19:00:00Z"
}
```

### `POST /api/v1/auth/logout`

Invalidates the current desktop session and refresh token.

Request fields:

```json
{
  "refreshToken": "opaque-refresh-token"
}
```

Response fields:

```json
{
  "loggedOut": true
}
```

## User endpoints

### `GET /api/v1/users`

Queries users with filtering and pagination.

Query parameters:

- `q`: optional search text for username, display name, or email.
- `ouId`: optional organizational unit filter.
- `status`: optional account status filter such as `ACTIVE`, `LOCKED`, or `DISABLED`.
- `page`: zero-based page index.
- `size`: requested page size.

Response fields:

```json
{
  "items": [
    {
      "id": "usr_01HX7Y...",
      "username": "alice.admin",
      "displayName": "Alice Admin",
      "email": "alice@example.org",
      "status": "ACTIVE",
      "organizationalUnitId": "ou_01HX7Y...",
      "roleIds": ["role_admin"]
    }
  ],
  "page": 0,
  "size": 25,
  "totalElements": 1
}
```

### `GET /api/v1/users/{userId}`

Returns one user by identifier, including group and role references needed by the desktop detail panel.

### `POST /api/v1/users`

Creates a user. The server enforces uniqueness, password policy, OU policy, and authorization.

Request fields:

```json
{
  "username": "new.user",
  "displayName": "New User",
  "email": "new.user@example.org",
  "organizationalUnitId": "ou_staff",
  "initialPassword": "temporary-secret",
  "roleIds": ["role_reader"],
  "groupIds": ["grp_helpdesk"]
}
```

### `PATCH /api/v1/users/{userId}`

Updates mutable profile and account fields. Password changes should use a dedicated endpoint so policy and audit semantics remain clear.

### `POST /api/v1/users/{userId}/password-reset`

Resets credentials or creates a temporary password according to password policy.

Request fields:

```json
{
  "temporaryPassword": "new-temporary-secret",
  "mustChangeAtNextLogin": true
}
```

## Group endpoints

### `GET /api/v1/groups`

Lists groups with optional search and OU filtering.

### `POST /api/v1/groups`

Creates a group.

Request fields:

```json
{
  "name": "Helpdesk Operators",
  "description": "Operators allowed to view and assist user accounts.",
  "organizationalUnitId": "ou_it"
}
```

### `PUT /api/v1/groups/{groupId}/members/{userId}`

Adds a user to a group. The server should audit both the actor and the target user.

### `DELETE /api/v1/groups/{groupId}/members/{userId}`

Removes a user from a group.

## Role and permission endpoints

### `GET /api/v1/roles`

Returns assignable roles visible to the authenticated principal.

### `GET /api/v1/permissions`

Returns permission definitions available in the system. This endpoint is useful for role editors but should itself be restricted to privileged administrators.

### `PUT /api/v1/roles/{roleId}/permissions/{permissionId}`

Adds a permission to a role.

### `DELETE /api/v1/roles/{roleId}/permissions/{permissionId}`

Removes a permission from a role.

## Organizational unit endpoints

### `GET /api/v1/organizational-units/tree`

Returns a bounded OU tree for navigation in the desktop client.

Response fields:

```json
{
  "root": {
    "id": "ou_root",
    "name": "Directory Root",
    "children": [
      {
        "id": "ou_it",
        "name": "Information Technology",
        "children": []
      }
    ]
  },
  "maxDepth": 25
}
```

### `POST /api/v1/organizational-units`

Creates an OU under a parent OU. The server must reject parent selections that would create cycles.

### `PATCH /api/v1/organizational-units/{ouId}`

Renames or moves an OU. Move operations must validate that the new parent is not the OU itself or one of its descendants.

## Audit endpoints

### `GET /api/v1/audit/events`

Queries audit events for privileged review.

Query parameters:

- `actorId`: optional user who performed the action.
- `targetId`: optional target resource identifier.
- `action`: optional action type.
- `from`: optional UTC lower bound.
- `to`: optional UTC upper bound.
- `page`: zero-based page index.
- `size`: requested page size.

Response fields:

```json
{
  "items": [
    {
      "id": "audit_01HX7Y...",
      "occurredAt": "2026-06-02T17:10:00Z",
      "actorId": "usr_admin",
      "action": "USER_CREATED",
      "targetType": "USER",
      "targetId": "usr_new",
      "outcome": "SUCCESS",
      "correlationId": "01HX7Y4XK7N7F4M9Z8DQ9X7C5A"
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 1
}
```

## Desktop query behavior

The desktop client should prefer paged list endpoints and targeted detail endpoints rather than loading the full directory into memory. Search boxes should debounce requests, cancel stale calls where possible, and avoid updating UI state with responses from superseded queries.
