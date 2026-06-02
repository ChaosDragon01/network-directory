# Java Directory Service Architecture

## System purpose

The Java Directory Service is organized as a decoupled, multi-module Maven workspace. The backend module (`jds-server`) is the headless identity and directory engine. The desktop module (`jds-desktop-client`) is the JavaFX administrative console. The root project only coordinates module builds; it does not own application source code.

This separation keeps directory policy, authentication, authorization, persistence, LDAP parsing, and audit handling out of the desktop application. The client presents administrative workflows and sends authenticated network requests. The server owns every state-changing decision and is the only process that talks to the database.

## Maven module boundaries

The root `pom.xml` lists two modules:

- `jds-server`: Spring-based server-side directory service.
- `jds-desktop-client`: JavaFX desktop administration client.

No Java source should live at the repository root. A root-level `src/` directory would blur module ownership, make package scanning ambiguous, and risk accidental deployment of classes outside the intended application boundary.

## Backend responsibility model

The backend source tree is rooted at `jds-server/src/main/java/com/directoryservice/`. Its packages are intentionally layered:

- `api`: REST controllers that translate HTTP requests into service-layer commands and queries.
- `service`: Application-level workflows such as user administration, role assignment, authentication, policy checks, and audit dispatch.
- `repository`: Persistence-facing interfaces responsible for accessing users, groups, roles, permissions, organizational units, and audit records.
- `model`: Domain state definitions for directory objects and security records.
- `security`: Password verification, JWT issuing and validation, session lifecycle coordination, and permission evaluation.
- `policy`: Password, account, and group policy definitions.
- `ldap`: LDAP protocol entry points, request parsing, and request handling.
- `audit`: Audit event representation and audit logging boundaries.
- `scheduler`: Scheduled retention and cleanup tasks.
- `cache`: Read-through or invalidation-aware cache boundaries for high-volume directory lookups.
- `config`: Security, database, and cache configuration.
- `exception`: Application exception and global exception handling boundaries.
- `util`: Shared low-level helpers and constants.

The server is headless. It should not depend on JavaFX, desktop state, local user interface models, or workstation filesystem paths. Requests arrive over REST or LDAP-oriented entry points, are authenticated, authorized, validated, processed through services, persisted through repositories, and recorded through audit boundaries.

## Desktop client responsibility model

The desktop source tree is rooted at `jds-desktop-client/src/main/java/com/directoryclient/`. It is organized around presentation and network boundaries:

- `ClientApplication`: JavaFX application entry boundary.
- `ui`: Controllers for dashboards, tree navigation, and tabular user management screens.
- `network`: Retrofit client setup and API service declarations.

The desktop module should not implement directory policy or trust local decisions for privileged operations. It may validate obvious form errors to improve usability, but the server must repeat validation and authorization for every request.

## Client-server communication

The desktop client communicates with the server over HTTP using Retrofit and OkHttp. Retrofit gives the client a typed API boundary while keeping HTTP details out of UI controllers. OkHttp owns transport-level concerns such as connection reuse, request interception, timeout configuration, and authorization header attachment.

A healthy interaction path is:

1. A JavaFX controller reacts to a user action on the JavaFX Application Thread.
2. The controller delegates to a client-facing service or Retrofit API abstraction.
3. Retrofit executes the network request asynchronously so the UI thread is not blocked.
4. The completion callback or future continuation transforms response metadata into UI-safe state.
5. UI updates are scheduled back onto the JavaFX Application Thread.

This pattern prevents slow network requests from freezing menus, tables, directory trees, login dialogs, or modal windows.

## JavaFX event loop and thread ownership

JavaFX has a single UI event loop. Scene graph reads and writes must occur on the JavaFX Application Thread. Network calls, JSON decoding, file IO, and long-running data preparation should not run directly on that thread.

Controllers in `com.directoryclient.ui` should therefore remain thin. They should collect user intent, trigger asynchronous work, and render the result. They should not perform REST calls synchronously, sleep, poll server state, or calculate authorization decisions. When a background operation completes, UI mutation should be marshalled back to the JavaFX thread.

## Headless backend request processing

The backend receives REST requests through the `api` package. A controller should perform request mapping, basic request shape handling, and response conversion. It should not contain business rules. Service interfaces in the `service` package represent the application workflows that will eventually implement the requested behavior.

A typical server-side request path is:

1. The API layer accepts a request and extracts authentication context.
2. Security components validate JWTs or session state.
3. Services validate requested actions and coordinate policy checks.
4. Permission evaluation combines RBAC grants with MAC restrictions.
5. Repositories load and persist domain data in a transaction-safe way.
6. Audit components capture the actor, target, action, outcome, and correlation data.
7. Controllers return consistent HTTP status codes and JSON responses.

LDAP request handling follows the same ownership model: parse the protocol request, map it into directory service operations, enforce security and policy, query repositories, audit the result, and return protocol-appropriate responses.

## Cache placement

Caching belongs behind service-level or repository-facing boundaries, never in UI controllers. User and group caches should be scoped to read-heavy directory lookups and must be invalidated when identity membership, roles, permissions, account status, or organizational unit placement changes. Cache entries must not bypass authorization checks unless the cache key includes the security context and the data is explicitly safe for that context.

## Audit and observability

Audit logging is a first-class backend concern. Administrative actions, authentication outcomes, policy violations, permission denials, LDAP binds, and retention jobs should produce audit events. Audit records should include stable identifiers rather than only display names so renamed users, groups, or organizational units can still be traced historically.

## Architectural guardrails

- Keep source code inside the owning Maven module.
- Keep UI controllers free of server-side policy decisions.
- Keep controllers free of persistence details.
- Keep repositories free of presentation concerns.
- Keep password material and tokens out of logs.
- Make asynchronous network boundaries explicit in the desktop module.
- Route every privileged operation through server-side authorization and audit checks.
