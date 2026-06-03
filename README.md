# Java Directory Service (JDS)

**Architecture:** Client-Server / API-Driven Desktop Application  
**Network Protocols:** REST (JSON), LDAP, LDAPS  

A centralized directory management system engineered for identity governance, hierarchical resource organization, and network security simulations. JDS separates all core business and authentication rules into a headless API backend, which is managed over the network via a native desktop application acting as the control plane.

---

## System Architecture

The project is structured as a decoupled, multi-module Maven workspace split along explicit network boundaries:

### 1. The Headless Directory Engine (Server)
A standalone backend service responsible for persistent directory storage, cryptographic operations, access validation, and token lifecycles.
* **Core Stack:** Java 17, Spring Boot, Spring Security, Spring Data JPA.
* **Data Layer:** PostgreSQL (Production configuration) and H2 (In-memory testing runtime).
* **Network Handlers:** Exposes standard JSON endpoints on port 8080 for desktop client communication, alongside an embedded UnboundID LDAP SDK listener on port 10389 for standard directory querying.

### 2. The Administration Console (Desktop Client)
A thick desktop interface serving as the primary control interface for domain administrators.
* **UI Stack:** JavaFX 17 paired with ControlsFX for complex, enterprise-ready UI layout elements.
* **Network Client:** Retrofit and OkHttp for processing asynchronous background network communications.
* **Thread Safety:** Implements strict backgrounding of REST processing to ensure the JavaFX Application Thread never stalls during active network conditions.

---

## Core Operational Features

* **Multi-Protocol Capabilities:** Concurrent handling of client application administrative requests via JSON REST endpoints, alongside native authentication handling over standard LDAP BIND operations.
* **Hierarchical Resource Topologies:** Deeply nestable Organizational Units (OUs) that maintain domain relationships while avoiding circular dependency traps during serialization.
* **Unified Governance Controls:** A security framework merging Role-Based Access Control (RBAC permissions mapping) with Mandatory Access Control (MAC operational bounds).
* **Automated Maintenance Tasks:** Integrated background schedulers handling expired desktop user sessions, policy tracking, and rolling log purges.
* **Immutable Auditing:** Secure tracking configurations capturing actions, target entities, timestamps, and correlation IDs to guarantee compliance trails.

---

## Directory Data Model

The system organizes domain state across the following entities:

* **User:** Accounts containing profile properties, cryptographic salt definitions, security flags, and state contexts.
* **Group:** Custom pooling structures matching identities for collective rights management.
* **Role & Permission:** Granular capability mapping providing system access rights (such as `USER_CREATE` or `AUDIT_VIEW`) across the directory.
* **Organizational Unit (OU):** Tree structural elements facilitating administration boundaries and policy application targets.
* **Audit Log:** Chronological, write-once verification ledger tracking structural variations, authorization denials, and core state edits.

---

## Verified Workspace Structure

The project maps directly to this clean, multi-module structural layout:

```text
jds-root/
├── pom.xml                      # Parent coordination orchestration configuration
├── .gitignore                   # Shared platform clean definitions
│
├── jds-server/                  # Headless REST API & LDAP Backend Engine
│   ├── pom.xml                  # Server dependencies (Spring Boot, Security, JPA)
│   └── src/main/java/com/directoryservice/
│       ├── DirectoryServiceApplication.java
│       ├── api/                 # Handles incoming JSON client routes
│       ├── audit/               # Internal state monitoring
│       ├── cache/               # Data optimizations for high read frequency
│       ├── config/              # Security filter chains and datasources
│       ├── exception/           # Unified server error handling
│       ├── ldap/                # Inbound native protocol processors
│       ├── model/               # Relational database table entities
│       ├── policy/              # Account, password, and group compliance logic
│       ├── repository/          # Native database query abstractions
│       ├── scheduler/           # Automated database optimization tasks
│       ├── security/            # Cryptography handlers and JWT token providers
│       ├── service/             # Master transaction orchestration layers
│       └── util/                # Common internal validation properties
│
├── jds-desktop-client/          # Native Management Interface Console
│   ├── pom.xml                  # Client configurations (JavaFX modules, Retrofit)
│   └── src/main/java/
│       ├── module-info.java     # Strict JPMS access boundaries configuration
│       └── com/directoryclient/
│           ├── ClientApplication.java
│           ├── network/         # Asynchronous server API stubs
│           └── ui/              # JavaFX presentation layout control layers
│
└── docs/                        # Formal architectural layout blueprints
    ├── architecture.md          # Multi-module decoupling patterns
    ├── api.md                   # REST contractual specifications
    ├── database-schema.md       # Entity relation models and recursive safeguards
    └── security-model.md        # Combined RBAC and MAC governance properties
