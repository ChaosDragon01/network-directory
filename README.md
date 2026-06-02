# Java Directory Service

A Java-based directory management system inspired by core ideas from Active Directory, built for learning and internal network management.

This project is designed to manage users, groups, roles, permissions, organizational units, and authentication in a centralized way.

## Overview

The goal of this project is to provide a lightweight directory service that can:

- store and manage user accounts
- organize users into groups and organizational units
- control access through roles and permissions
- support login and session handling
- expose directory operations through a Java backend

This is not a full replacement for Active Directory. It is a custom Java implementation focused on core directory concepts.

## Core Features

- User management
- Group management
- Role based access control
- Organizational unit structure
- Authentication and authorization
- Password hashing and account security
- Audit logging
- REST API for directory operations
- Database-backed persistence

## Suggested Tech Stack

- Java
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL or MySQL
- Maven or Gradle
- Lombok
- JWT for authentication if needed

## Basic Directory Model

The system can be organized around the following entities:

### User
Represents an individual account in the directory.

Fields may include:
- id
- username
- passwordHash
- firstName
- lastName
- email
- status
- createdAt
- updatedAt

### Group
Represents a collection of users.

Fields may include:
- id
- groupName
- description
- members
- createdAt

### Role
Represents a set of privileges.

Fields may include:
- id
- roleName
- permissions

### Permission
Represents a specific action a user or group is allowed to perform.

Examples:
- CREATE_USER
- DELETE_USER
- UPDATE_GROUP
- VIEW_AUDIT_LOGS

### Organizational Unit
Represents a hierarchy for organizing users and groups.

Fields may include:
- id
- unitName
- parentUnit
- children
- users
- groups

### Audit Log
Tracks important security and administrative actions.

Examples:
- login attempts
- password changes
- user creation
- group membership changes
- permission updates

## Suggested Project Structure

```txt
src/
 ├── main/
 │   ├── java/
 │   │   └── com/
 │   │       └── yourname/
 │   │           └── directoryservice/
 │   │               ├── config/
 │   │               ├── controller/
 │   │               ├── dto/
 │   │               ├── exception/
 │   │               ├── model/
 │   │               ├── repository/
 │   │               ├── service/
 │   │               ├── security/
 │   │               └── DirectoryServiceApplication.java
 │   └── resources/
 │       ├── application.properties
 │       └── data.sql
 └── test/
     └── java/