jds-root/
├── jds-server/                  # Headless REST API & LDAP Backend
│   ├── pom.xml
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── directoryservice/
│   │   │   │           ├── DirectoryServiceApplication.java
│   │   │   │           ├── api/              # Rest Endpoints for the Desktop Client
│   │   │   │           ├── audit/            # Server-side event tracking
│   │   │   │           ├── cache/            # Server-side data caching
│   │   │   │           ├── config/           # Database and Security Configs
│   │   │   │           ├── exception/        # Global error handling
│   │   │   │           ├── ldap/             # Embedded LDAP Server Engine
│   │   │   │           ├── model/            # Database Entities (User, OU, etc.)
│   │   │   │           ├── policy/           # Core business logic rules
│   │   │   │           ├── repository/       # Database access layer
│   │   │   │           ├── scheduler/        # Background maintenance tasks
│   │   │   │           ├── security/         # Cryptography & JWT Engines
│   │   │   │           ├── service/          # Core transaction processing
│   │   │   │           └── util/
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── schema.sql
│   │   │       ├── data.sql
│   │   │       └── logback.xml
│   │   └── test/                             # Server Backend Unit/Integration Tests
│   
├── jds-desktop-client/          # Native Administration Console (UI)
│   ├── build.gradle.kts (or pom.xml)
│   └── src/
│       ├── main/
│       │   ├── java/ (or kotlin/)
│       │   │   └── com/
│       │   │       └── directoryclient/
│       │   │           ├── ClientApplication.java   # UI Entry Point
│       │   │           ├── ui/                      # View Controllers (JavaFX/Compose)
│       │   │           │   ├── MainDashboard.java
│       │   │           │   ├── TreeViewController.java
│       │   │           │   └── UserTableController.java
│       │   │           ├── network/                 # API Client (Retrofit/OkHttp stubs)
│       │   │           │   ├── ApiClient.java
│       │   │           │   └── DirectoryApiService.java
│       │   │           ├── state/                   # Client-side state management & caching
│       │   │           └── model/                   # Local UI-mapped data objects
│       │   └── resources/
│       │       ├── ui/                               # FXML layouts or Compose assets
│       │       │   ├── main_dashboard.fxml
│       │       │   └── styles.css
│       │       └── images/
│       └── test/                                    # UI and Network mock tests
│
├── docs/                        # Project Documentation
│   ├── architecture.md
│   └── api.md
└── scripts/                     # Deployment and backup automation tools
    ├── backup.sh
    └── create-admin.sh