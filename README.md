# Tinder Microservices

![Java](https://img.shields.io/badge/Java-21-orange.svg?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5.x-brightgreen.svg?style=flat-square&logo=springboot)
![Kafka](https://img.shields.io/badge/Kafka-3.7.0-231F20.svg?style=flat-square&logo=apachekafka)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue.svg?style=flat-square&logo=postgresql)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-47A248.svg?style=flat-square&logo=mongodb)
![Redis](https://img.shields.io/badge/Redis-7-DC3828.svg?style=flat-square&logo=redis)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED.svg?style=flat-square&logo=docker)

A distributed microservices architecture for a dating application similar to Tinder, built with Spring Boot, Java 21, and event-driven architecture using Kafka.

## 🏗️ Architecture Overview

This project implements a **microservices architecture** following the **database-per-service pattern**, where each service has a specific responsibility and its own database. Services communicate through **Kafka events** for asynchronous operations and **REST APIs** via an API Gateway for synchronous operations. Inter-service communication is hybrid: asynchronous (Kafka) for most business flows, and synchronous (internal REST/RPC) where real-time consistent data is required (e.g., Feed Service fetching profile data for feed generation).

### Architectural Principles

1. **Single Responsibility**: Each microservice handles one specific business domain
2. **Loose Coupling**: Services communicate through well-defined APIs and events
3. **High Cohesion**: Related functionality is grouped within a single service
4. **Autonomous Deployment**: Each service can be deployed independently
5. **Database Per Service**: Each service owns its data schema
6. **Event-Driven**: Asynchronous communication for eventual consistency
7. **API Gateway Pattern**: Single entry point for external clients

### System Architecture

For detailed architecture diagrams, data flows, and deployment architecture, see [ARCHITECTURE.md](./ARCHITECTURE.md).

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│  API Gateway    │ ← JWT Validation, Routing
│  (Port 8080)    │
└──────┬──────────┘
       │
       ├─────────────────────────────────────────────────────────────┐
       │                                                             │
       ▼                                                             ▼
┌──────────────────┐              ┌──────────────────┐              ┌──────────────────┐
│   Auth Service   │              │  Profile Service │              │  Swipe Service   │
│   (Port 8081)    │              │   (Port 8082)    │              │   (Port 8083)    │
│   PostgreSQL     │              │   MongoDB        │              │   PostgreSQL     │
└──────────────────┘              └──────────────────┘              └──────────────────┘
       │                                                             │
       │                                                             │
       ▼                                                             ▼
┌──────────────────┐              ┌──────────────────┐              ┌──────────────────┐
│   Chat Service   │              │   Feed Service   │              │  Notification    │
│   (Port 8084)    │              │   (Port 8086)    │              │   Service        │
│   PostgreSQL     │              │   Redis Cache    │              │   (Port 8085)    │
│   WebSocket      │              │                  │              │   PostgreSQL     │
└──────────────────┘              └──────────────────┘              └──────────────────┘
       │                                                             │
       └─────────────────────────────────────────────────────────────┘
                              │
                              ▼
                    ┌─────────────────┐
                    │  Kafka (Events) │
                    │  Redis (Cache)  │
                    │  MinIO (Media)  │
                    └─────────────────┘
```

## 🚀 Services

### 1. API Gateway (Port 8080)
- **Responsibility**: Single entry point, JWT validation, request routing, rate limiting
- **Tech Stack**: Spring Cloud Gateway, Spring WebFlux (reactive), Redis
- **Key Features**:
  - Centralized authentication via JWT validation
  - Dynamic request routing to microservices based on paths
  - Rate limiting using Redis (prevents abuse)
  - Request/response logging and monitoring
- **Configuration**: Route definitions for each microservice, JWT public key validation
- **Security**: Validates JWT tokens before forwarding requests to downstream services

### 2. Auth Service (Port 8081)
- **Responsibility**: User authentication, authorization, identity management
- **Database**: PostgreSQL 15 with Liquibase migrations
- **Tech Stack**: Spring Security, JWT (jjwt), Spring Kafka, Spring Mail, Thymeleaf
- **Key Features**:
  - JWT token generation (access + refresh tokens)
  - Google OAuth 2.0 integration
  - Email verification with token-based workflow
  - Password reset via email with secure tokens
  - BCrypt password hashing
  - Token refresh mechanism
  - User registration with validation
- **Events Published**:
  - `UserCreated` - When a new user registers
  - `UserVerified` - When email verification completes
  - `UserDeleted` - When account is deleted
- **Security**: Password strength validation, email verification required, secure token storage

### 3. Profile Service (Port 8082)
- **Responsibility**: User profile management, photo handling, preferences
- **Database**: MongoDB 7.0 (flexible schema for evolving profile data)
- **Tech Stack**: Spring Data MongoDB, MinIO, Spring Kafka, MapStruct
- **Key Features**:
  - Profile CRUD operations with validation
  - Photo upload/management via MinIO
  - Profile preferences (age range, distance, interests, gender)
  - Geo-location support with GeoJSON
  - Profile visibility settings
  - Photo moderation ready architecture
  - Interest-based matching tags
- **Events**:
  - Publishes: `ProfileCreated`, `ProfileUpdated`, `ProfileDeleted`
  - Consumes: `UserCreated` (creates initial profile)
- **Data Model**: Flexible MongoDB schema with embedded arrays for photos/interests
- **Security**: Profile ownership validation, photo upload limits, content type validation

### 4. Swipe Service (Port 8083)
- **Responsibility**: Swipe mechanics, match detection, statistics
- **Database**: PostgreSQL 15 with Liquibase migrations
- **Tech Stack**: Spring Data JPA, ShedLock, Spring Kafka, Hypersistence Utils
- **Key Features**:
  - Like/Dislike (Swipe) functionality
  - Mutual match detection algorithm
  - Distributed locking with ShedLock (prevents duplicate matches)
  - **Product Rate Limiting**: SwipeRateLimiterService enforces swipe limits per user (business logic for premium monetization)
  - Swipe statistics and analytics
  - Undo functionality (time-limited)
  - Swipe history tracking
- **Events**:
  - Publishes: `SwipeRecorded`, `UserMatched`, `MatchRevoked`
  - Consumes: `UserDeleted` (cleans up swipe data)
- **Concurrency Control**: ShedLock ensures only one instance processes match detection
- **Performance**: Batch processing for match detection, database indexing on user pairs

### 5. Chat Service (Port 8084)
- **Responsibility**: Real-time messaging, media handling, conversation management
- **Database**: PostgreSQL 15 with Liquibase migrations
- **Tech Stack**: Spring WebSocket, STOMP messaging, MinIO, Spring Kafka, Redis
- **Key Features**:
  - WebSocket for real-time bidirectional communication
  - Message persistence with read receipts
  - Media file upload (images, videos) via MinIO
  - Kafka events for new message notifications
  - **Cursor-based (Keyset) Pagination** for message history - no performance degradation on large datasets, ideal for infinite scroll
  - **CQRS Pattern**: Clear separation between QueryService (e.g., ChatListQueryService for reading) and UseCase (e.g., SendMessageUseCase for commands)
  - Typing indicators
  - Online/offline status tracking
  - Conversation room management
- **Events**:
  - Consumes: `UserMatched` (creates conversation room)
  - Publishes: `MessageSent`, `MessageRead`
- **Performance**: Redis for caching active sessions, message batching
- **Security**: JWT validation for WebSocket connections, file type validation

### 6. Notification Service (Port 8085)
- **Responsibility**: Push notifications, notification preferences
- **Database**: PostgreSQL 15 with Liquibase migrations
- **Tech Stack**: Firebase Admin SDK, Spring Kafka, MapStruct
- **Key Features**:
  - Firebase Cloud Messaging (FCM) integration
  - Push notifications for matches, messages, likes
  - Notification preferences per user
  - Notification history and analytics
  - Batch notification sending
  - Retry logic for failed notifications
  - Device token management
- **Events**:
  - Consumes: `UserMatched`, `MessageSent`, `SwipeRecorded`
  - Publishes: `NotificationSent`, `NotificationFailed`
- **Reliability**: Retry mechanism with exponential backoff, dead letter queue

### 7. Feed Service (Port 8086)
- **Responsibility**: Personalized feed generation, user recommendations
- **Database**: Redis (no persistent storage - ephemeral cache)
- **Tech Stack**: Spring Data Redis, Spring Kafka, REST Client (ProfileRestAdapter)
- **Key Features**:
  - Generates personalized user feed based on preferences
  - Redis caching for high-performance feed generation
  - Geographic filtering (distance-based)
  - Age and preference matching
  - Consumes profile updates from Kafka for cache invalidation
  - Pagination support for large feeds
- **Caching Strategy**:
  - Feed results cached in Redis with TTL
  - Cache invalidated on profile updates via Kafka events
  - User-specific cache keys
- **Performance**: Sub-second feed generation, Redis pipelining

## 🛠️ Technology Stack

### Backend Framework
- **Java 21** - Modern Java with virtual threads, records, pattern matching
- **Spring Boot 3.5.x** - Microservices framework with auto-configuration
- **Spring Cloud Gateway** - Reactive API Gateway with WebFlux
- **Spring Security 6.x** - Comprehensive security framework
- **Spring Kafka 3.x** - Apache Kafka integration for event streaming
- **Spring Data JPA** - JPA/Hibernate for relational databases
- **Spring Data MongoDB** - MongoDB integration with reactive support
- **Spring Data Redis** - Redis integration with reactive and imperative APIs
- **Spring WebSocket** - WebSocket support with STOMP messaging
- **Spring Mail** - Email sending with Thymeleaf templates
- **Spring Validation** - Bean validation (JSR-380)

### Databases & Data Stores
- **PostgreSQL 15** - Relational databases (auth, chat, swipe, notification)
  - ACID compliance, JSONB support, full-text search
  - Connection pooling via HikariCP
  - Database migrations via Liquibase
- **MongoDB 7.0** - Document database (profiles)
  - Flexible schema for evolving profile data
  - GeoJSON support for location-based queries
  - Index optimization for read performance
- **Redis 7** - In-memory data store
  - Caching layer for feed and session data
  - Pub/Sub for real-time notifications
  - Key-space notifications for cache invalidation

### Message Broker & Event Streaming
- **Apache Kafka 3.7.0** - Distributed event streaming platform
  - Event-driven communication between services
  - Exactly-once semantics where possible
  - Topic partitioning for scalability
  - Consumer groups for load balancing
  - Kafka UI for monitoring and management

### Object Storage
- **MinIO 9.0** - S3-compatible object storage
  - Media file storage (images, videos)
  - Bucket-based organization
  - **Event-Driven Storage**: MinIO acts as Event Producer, automatically publishing events to Kafka on file uploads via MinioMediaEventKafkaAdapter
  - Asynchronous media processing pipeline: Upload → MinIO Event → Kafka → Chat Service consumes → Attach to message
  - MinIO Client (mc) for management operations

### Security & Authentication
- **JWT (jjwt 0.12.6)** - JSON Web Token implementation
  - Access tokens (short-lived, 15 minutes)
  - Refresh tokens (long-lived, 7 days)
  - RS256 signing algorithm
- **Spring Security** - Authentication and authorization
  - BCrypt password hashing
  - Role-based access control (RBAC)
  - Method-level security with @PreAuthorize
- **Google OAuth 2.0** - Third-party authentication
  - Google API Client integration
  - OAuth 2.0 authorization code flow

### Distributed Systems & Resilience
- **ShedLock 6.3.0** - Distributed locking
  - Prevents duplicate scheduled task execution
  - JDBC-based lock storage
  - Lock expiration handling
  - Bulkhead patterns
- **Spring Retry** - Declarative retry support
- **Hypersistence Utils 3.9.4** - Hibernate utilities
  - JSON type mapping for PostgreSQL
  - Custom types for database columns

### Integration & Communication
- **Spring Kafka** - Kafka producer/consumer integration
  - @KafkaListener for event consumption
  - KafkaTemplate for event publishing
  - Error handling with dead letter queues
- **Spring WebClient** - Reactive HTTP client
  - Non-blocking HTTP calls
  - Connection pooling
- **WebSocket & STOMP** - Real-time bidirectional communication
  - Message broker with STOMP protocol
  - User destination prefixes
  - Message conversion

### Build & Quality Tools
- **Gradle 8.x** - Build automation and dependency management
- **Lombok** - Reduce boilerplate code
  - @Data, @Builder, @Slf4j annotations
  - Compile-time code generation
- **MapStruct 1.6.3** - Type-safe bean mapping
  - Compile-time mapping code generation
  - Performance-optimized mappers
- **Spotless 6.25.0** - Code formatting
  - Eclipse Java formatter
  - Automatic import organization
  - Pre-commit hooks ready
- **Lombok MapStruct Binding** - Integration between Lombok and MapStruct

### Testing
- **JUnit 5** - Modern testing framework
- **Spring Boot Test** - Spring integration testing
- **Spring Security Test** - Security testing support
- **Testcontainers** - Integration testing with real containers
  - PostgreSQL containers
  - MongoDB containers
  - Kafka containers
  - MinIO containers
  - Redis containers
- **Spring Kafka Test** - Kafka testing with embedded broker
- **WireMock** - HTTP service mocking
- **Awaitility** - Asynchronous testing support
- **Jacoco** - Code coverage reporting

### Monitoring & Observability
- **Spring Boot Actuator** - Production-ready endpoints
  - Health checks
  - Metrics (Micrometer)
  - Info endpoints
- **Micrometer Tracing** - Distributed tracing
  - Automatic trace propagation across services
  - Integration with Zipkin for trace visualization
  - Span timing and performance metrics
  - Uses Brave tracer implementation
  - Used in: all services
- **Zipkin** - Distributed tracing system
  - Collects and visualizes trace data
  - Service dependency mapping
  - Performance analysis and bottleneck identification
  - Available at http://localhost:9411
- **Micrometer** - Metrics collection
  - Prometheus metrics format
  - Custom metrics support
- **Kafka UI** - Kafka topic and consumer monitoring
- **MinIO Console** - Object storage management UI

### Development Tools
- **Git** - Version control
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **IntelliJ IDEA** - IDE (recommended)

## 🎨 Design Patterns & Principles

### Architectural Patterns

1. **Hexagonal Architecture (Ports and Adapters)**
   - Clean separation between domain logic and external concerns
   - Application layer organized into ports (in/out) for input/output
   - Infrastructure adapters implement ports (REST, Kafka, Database)
   - Domain layer remains isolated from frameworks and databases
   - **Applied to Chat Service**: Chosen for Chat Service due to complex I/O operations (WebSocket, Kafka, MinIO, PostgreSQL) and horizontal scaling requirements via Redis Pub/Sub. Other services have simpler I/O patterns and don't require this architectural overhead
   - Example structure in chat-service: `application/port/in`, `application/port/out`, `infrastructure/adapter`, `domain/model`

2. **Domain-Driven Design (DDD)**
   - Rich domain models with business logic encapsulation
   - Bounded contexts per microservice (Chat, Swipe, Profile, etc.)
   - Domain events for cross-context communication
   - Aggregates and value objects for complex domain modeling

3. **Microservices Architecture**
   - Each service is independently deployable
   - Services communicate via well-defined APIs
   - Database-per-service pattern for data autonomy
   - Horizontal scaling per service based on load

4. **API Gateway Pattern**
   - Single entry point for all client requests
   - Centralized authentication and authorization
   - Request routing and load balancing
   - Rate limiting

5. **Event-Driven Architecture**
   - Asynchronous communication via Kafka
   - Loose coupling between services
   - Event sourcing for audit trails
   - Eventually consistent data across services
   - Distributed Saga for account deletion (GDPR compliance)

6. **CQRS (Command Query Responsibility Segregation)**
   - Separate read and write models in some services
   - Optimized read models for feed generation
   - Write models for data consistency

### Design Patterns

1. **Repository Pattern**
   - Abstract data access logic
   - Clean separation between business logic and data access
   - Easy to swap database implementations

2. **Service Layer Pattern**
   - Business logic encapsulation
   - Transaction management
   - Reusable business operations

3. **DTO Pattern (Data Transfer Object)**
   - Separate API models from domain models
   - Control data exposure to clients
   - Validation at API boundaries

4. **Factory Pattern**
   - Object creation logic encapsulation
   - Used in event publishers and message builders

5. **Strategy Pattern**
   - Interchangeable algorithms
   - Used in notification strategies (push, email, etc.)

6. **Observer Pattern**
   - Kafka event consumers observe events
   - Reactive to state changes in other services

7. **Builder Pattern**
   - Complex object construction
   - Used with Lombok @Builder for DTOs and entities

8. **Singleton Pattern**
   - Single instance per JVM
   - Spring beans are singletons by default
   - Configuration classes

### SOLID Principles

1. **Single Responsibility Principle**
   - Each service has one business responsibility
   - Each class/method has one reason to change
   - Clear separation of concerns

2. **Open/Closed Principle**
   - Open for extension, closed for modification
   - Event-driven architecture allows adding new consumers
   - Strategy pattern for new notification types

3. **Liskov Substitution Principle**
   - Proper inheritance hierarchies
   - Repository interfaces with multiple implementations

4. **Interface Segregation Principle**
   - Focused interfaces
   - Separate read/write repositories where needed

5. **Dependency Inversion Principle**
   - Depend on abstractions, not concretions
   - Spring dependency injection
   - Interface-based service contracts

### Other Principles

- **DRY (Don't Repeat Yourself)**: Shared libraries, common utilities
- **KISS (Keep It Simple, Stupid)**: Simple solutions over complex ones
- **YAGNI (You Aren't Gonna Need It)**: Only implement what's needed
- **Fail Fast**: Early validation and error handling
- **Defensive Programming**: Null checks, validation, exception handling

### Distributed Systems Patterns

#### Transactional Outbox Pattern
- **Purpose**: Ensures guaranteed event delivery to Kafka
- **Implementation**: Events are stored in `outbox_events` table in the same transaction as business entity
- **Relay Worker**: `OutboxRelayWorker` with ShedLock publishes events to Kafka asynchronously
- **Benefits**: Solves Dual Write problem, ensures atomicity of database write and event publishing
- **At-Least-Once Delivery**: Guaranteed event delivery with retry mechanism

#### Inbox Pattern (Idempotency)
- **Purpose**: Prevents duplicate event processing
- **Implementation**: `inbox_events` table tracks processed events with `InboxDedupService`
- **Deduplication**: Events are deduplicated by eventId before processing
- **Benefits**: Handles duplicate Kafka deliveries, ensures exactly-once processing semantics
- **Idempotent Consumers**: Services can safely process duplicate events

## 💾 Caching Strategy

### Multi-Level Caching

1. **Application-Level Caching (Spring Cache)**
   - Method-level caching with @Cacheable
   - Cache eviction with @CacheEvict
   - Cache updating with @CachePut
   - Used for frequently accessed data

2. **Distributed Caching (Redis)**
   - Shared cache across service instances
   - Session storage for API Gateway
   - Feed caching in Feed Service
   - Rate limiting counters

3. **Database-Level Caching**
   - PostgreSQL query cache
   - MongoDB query optimization
   - Connection pooling (HikariCP)

### Cache Implementation Details

#### API Gateway
- **JWT Token Cache**: Stores validated tokens to reduce validation overhead
- **Rate Limiting**: Redis-based sliding window rate limiting
- **Route Cache**: Cached route configurations

#### Auth Service
- **User Session Cache**: Active user sessions in Redis
- **Token Blacklist**: Revoked tokens stored in Redis with TTL

#### Feed Service
- **Feed Cache**: Pre-generated feeds cached in Redis
  - Key: `feed:{userId}`
  - TTL: 5 minutes
  - Invalidated on profile updates via Kafka
- **User Preferences Cache**: User matching preferences
- **Geo-spatial Cache**: Location-based user proximity

#### Chat Service
- **Active Sessions**: WebSocket session tracking
- **Online Status**: User online/offline status
- **Message Cache**: Recent messages for quick access

#### Profile Service
- **Profile Cache**: User profiles cached in Redis
  - Key: `profile:{userId}`
  - TTL: 10 minutes
  - Invalidated on profile updates

### Cache Invalidation Strategies

1. **Time-Based Expiration (TTL)**
   - Automatic expiration after set time
   - Used for non-critical data (feeds, profiles)

2. **Event-Based Invalidation**
   - Kafka events trigger cache invalidation
   - Profile updates invalidate feed cache
   - User updates invalidate profile cache

3. **Write-Through Cache**
   - Cache updated on database write
   - Ensures cache consistency

4. **Cache-Aside Pattern**
   - Application manages cache
   - Cache miss triggers database load
   - Most common pattern in this project

### Cache Performance Considerations

- **Cache Hit Ratio Monitoring**: Track cache effectiveness
- **Cache Warming**: Pre-load critical data on startup
- **Cache Partitioning**: Separate cache instances for different data types
- **Serialization**: Efficient JSON serialization for Redis
- **Memory Management**: Redis max memory with eviction policies

## 🔄 Event-Driven Architecture

### Kafka Topics

The system uses Apache Kafka for asynchronous event-driven communication between services:

| Topic | Publisher | Consumer | Purpose |
|-------|-----------|----------|---------|
| `user-events` | Auth Service | Profile Service, Notification Service | User lifecycle events |
| `profile-events` | Profile Service | Feed Service, Swipe Service | Profile updates |
| `swipe-events` | Swipe Service | Notification Service, Feed Service | Swipe actions |
| `match-events` | Swipe Service | Chat Service, Notification Service | Match notifications |
| `chat-events` | Chat Service | Notification Service | Message events |
| `notification-events` | Notification Service | - | Notification delivery status |
| `minio-chat-media-events` | MinIO | Chat Service | Media upload notifications |

### Event Flow Examples

#### User Registration Flow
```
1. User registers via Auth Service
   ↓
2. Auth Service saves user to PostgreSQL
   ↓
3. Auth Service publishes UserCreated event to Kafka
   ↓
4. Profile Service consumes UserCreated
   ↓
5. Profile Service creates initial profile in MongoDB
   ↓
6. Profile Service publishes ProfileCreated event
   ↓
7. Feed Service consumes ProfileCreated
   ↓
8. Feed Service adds user to recommendation pool
```

#### Match Flow
```
1. User A swipes right on User B
   ↓
2. Swipe Service records swipe in PostgreSQL
   ↓
3. Swipe Service publishes SwipeRecorded event
   ↓
4. User B swipes right on User A
   ↓
5. Swipe Service detects mutual match
   ↓
6. Swipe Service publishes UserMatched event
   ↓
7. Chat Service consumes UserMatched
   ↓
8. Chat Service creates conversation room
   ↓
9. Notification Service consumes UserMatched
   ↓
10. Notification Service sends push to both users
```

### Event Design

#### Event Structure
```json
{
  "eventId": "uuid",
  "eventType": "UserCreated",
  "timestamp": "2024-01-01T00:00:00Z",
  "version": "1.0",
  "payload": {
    "userId": "user-123",
    "email": "user@example.com",
    "name": "John Doe"
  }
}
```

#### Event Versioning
- Events include version field for backward compatibility
- Consumers handle multiple event versions
- Schema evolution support

### Consumer Patterns

1. **Competing Consumers**
   - Multiple instances of same service
   - Kafka consumer group for load balancing
   - Automatic partition assignment

2. **Fan-Out**
   - Single event consumed by multiple services
   - Each service in its own consumer group
   - Independent processing

3. **Dead Letter Queue (DLQ)**
   - Failed events sent to DLQ topic
   - Manual inspection and retry
   - Error analysis and monitoring

### Reliability Guarantees

- **At-Least-Once Delivery**: Kafka guarantees
- **Idempotent Consumers**: Handle duplicate events
- **Event Replay**: Kafka log retention allows replay
- **Transaction Support**: Atomic event publishing with database updates

## 🗄️ Data Storage Strategy

### Database Selection Rationale

#### PostgreSQL (Auth, Chat, Swipe, Notification)
- **ACID Compliance**: Strong consistency for critical data
- **Relational Model**: Structured data with relationships
- **JSONB Support**: Flexible JSON storage within relational tables
- **Full-Text Search**: Built-in search capabilities
- **Mature Ecosystem**: Proven reliability and tooling

#### MongoDB (Profile Service)
- **Flexible Schema**: Evolving profile structure
- **Document Model**: Nested data (photos, interests)
- **GeoJSON**: Native geospatial queries
- **Horizontal Scaling**: Easy sharding for growth
- **Schema Validation**: Document validation rules

#### Redis (Caching, Sessions)
- **In-Memory**: Sub-millisecond access
- **Data Structures**: Rich data types (strings, lists, sets, hashes)
- **Persistence**: Optional disk persistence
- **Pub/Sub**: Real-time messaging
- **Expiration**: Built-in TTL support

### Database Schema Design

#### Auth Service (PostgreSQL)
```sql
users (id, email, password_hash, name, created_at, updated_at, verified)
refresh_tokens (id, user_id, token, expires_at, revoked)
verification_tokens (id, user_id, token, expires_at, used)
```

#### Chat Service (PostgreSQL)
```sql
conversations (id, participant1_id, participant2_id, created_at)
messages (id, conversation_id, sender_id, content, media_url, created_at)
message_read_status (id, message_id, user_id, read_at)
```

#### Profile Service (MongoDB)
```javascript
profiles: {
  _id: ObjectId,
  userId: String,
  name: String,
  age: Number,
  bio: String,
  photos: [{ url: String, order: Number }],
  interests: [String],
  location: { type: "Point", coordinates: [Number, Number] },
  preferences: {
    ageRange: { min: Number, max: Number },
    maxDistance: Number,
    gender: String
  },
  createdAt: Date,
  updatedAt: Date
}
```

#### Swipe Service (PostgreSQL)
```sql
swipes (id, swiper_id, target_id, direction, created_at)
matches (id, user1_id, user2_id, created_at)
shedlock (name, lock_until, locked_at, locked_by)
```

#### Notification Service (PostgreSQL)
```sql
notifications (id, user_id, type, title, body, data, created_at, read)
device_tokens (id, user_id, token, platform, created_at)
```

### Data Consistency

- **Strong Consistency**: Within a single service (ACID)
- **Eventual Consistency**: Across services (via Kafka events)
- **Compensating Transactions**: Rollback via events
- **Saga Pattern**: Multi-service transaction coordination

### Database Migrations

- **Liquibase**: Version-controlled database migrations
- **Change Logs**: XML, YAML, or SQL formats
- **Rollback Support**: Downgrade scripts
- **Environment-Specific**: Dev, test, prod migrations

### Backup & Recovery

- **Volume Mounts**: Docker volumes for data persistence
- **Database Dumps**: Regular backup schedules
- **Point-in-Time Recovery**: WAL archives (PostgreSQL)
- **MongoDB Backups**: mongodump for snapshots

## 🔒 Security Architecture

### Authentication & Authorization

#### JWT Token Strategy
- **Access Tokens**: Short-lived (15 minutes)
- **Refresh Tokens**: Long-lived (7 days)
- **Token Storage**: HttpOnly cookies for refresh tokens
- **Token Rotation**: Refresh token rotation on use

#### Authentication Flow
```
1. User submits credentials
   ↓
2. Auth Service validates credentials
   ↓
3. Auth Service generates JWT access token
   ↓
4. Auth Service generates refresh token
   ↓
5. Tokens returned to client
   ↓
6. Client includes access token in requests
   ↓
7. API Gateway validates JWT signature
   ↓
8. Request forwarded to service
```

#### Authorization
- **Role-Based Access Control (RBAC)**: USER, ADMIN roles
- **Method-Level Security**: @PreAuthorize annotations
- **Resource Ownership**: Users can only access their own data
- **Service-to-Service**: Internal network security

### Security Measures

#### Password Security
- **BCrypt Hashing**: Strong password hashing
- **Salt**: Automatic salt generation
- **Strength Validation**: Minimum length, complexity requirements
- **Reset Tokens**: Secure, time-limited reset tokens

#### API Security
- **Rate Limiting**: Prevent brute force attacks
- **Circuit Breakers**: Prevent cascading failures
- **Input Validation**: Bean validation (JSR-380)
- **SQL Injection Prevention**: Parameterized queries
- **XSS Prevention**: Input sanitization

#### Data Security
- **Encryption at Rest**: Database encryption
- **Encryption in Transit**: TLS/HTTPS
- **Sensitive Data**: Environment variables for secrets
- **PII Protection**: GDPR compliance considerations

#### Communication Security
- **Internal Network**: Docker network isolation
- **Service Discovery**: Internal service names
- **mTLS**: Mutual TLS (future enhancement)

### OAuth 2.0 Integration

#### Google OAuth Flow
```
1. User clicks "Sign in with Google"
   ↓
2. Redirect to Google OAuth consent screen
   ↓
3. User grants permission
   ↓
4. Google redirects with authorization code
   ↓
5. Auth Service exchanges code for tokens
   ↓
6. Auth Service retrieves user info from Google
   ↓
7. Auth Service creates/links user account
   ↓
8. Auth Service generates JWT tokens
   ↓
9. Tokens returned to client
```

### Security Headers
- **X-Content-Type-Options**: nosniff
- **X-Frame-Options**: DENY
- **X-XSS-Protection**: 1; mode=block
- **Strict-Transport-Security**: HSTS
- **Content-Security-Policy**: CSP headers

## 📊 Monitoring & Observability

### Health Checks

#### Spring Boot Actuator Endpoints
- **/actuator/health**: Service health status
- **/actuator/health/readiness**: Readiness probe
- **/actuator/health/liveness**: Liveness probe
- **/actuator/metrics**: Application metrics
- **/actuator/info**: Application information

#### Health Indicators
- **Database Health**: Connection pool status
- **Redis Health**: Connection status
- **Kafka Health**: Broker connection status
- **Disk Space**: Available disk space

### Metrics Collection

#### Micrometer Metrics
- **HTTP Metrics**: Request count, response time, error rate
- **JVM Metrics**: Memory, GC, thread count
- **Database Metrics**: Connection pool usage, query time
- **Kafka Metrics**: Producer/consumer lag, message rates
- **Custom Metrics**: Business-specific metrics

#### Key Metrics Tracked
- **Request Latency**: p50, p95, p99 response times
- **Error Rate**: HTTP 5xx errors percentage
- **Throughput**: Requests per second
- **Cache Hit Ratio**: Cache effectiveness
- **Database Connection Pool**: Active/idle connections
- **Kafka Consumer Lag**: Event processing delay

### Logging Strategy

#### Structured Logging
- **JSON Format**: Structured log entries
- **Correlation IDs**: Trace requests across services
- **Log Levels**: ERROR, WARN, INFO, DEBUG
- **Sensitive Data**: Redaction of PII

#### Log Aggregation (Future)
- **ELK Stack**: Elasticsearch, Logstash, Kibana
- **Loki**: Grafana Loki for log aggregation
- **Cloud Logging**: Cloud-based solutions

### Alerting (Future)

#### Alert Conditions
- **High Error Rate**: > 5% error rate
- **High Latency**: p99 > 1 second
- **Service Down**: Health check failures
- **Database Connection Pool Exhaustion**: > 90% usage
- **Kafka Consumer Lag**: > 1000 messages

#### Notification Channels
- **Email**: On-call engineer notifications
- **Slack**: Team notifications
- **PagerDuty**: Critical alerts

## �️ Domain Architecture

* **Users & Authentication**: Role-based access control with JWT tokens, Google OAuth integration, and email verification. Refresh tokens for secure session management.
* **Profiles**: Rich user profiles with flexible MongoDB schema supporting photos, bio, interests, and GeoJSON location data for proximity-based matching.
* **Swipes & Matches**: Swipe history tracking with mutual match detection algorithm. Distributed locking prevents duplicate match processing.
* **Conversations**: Real-time chat rooms with message persistence, read receipts, and media file support via MinIO.
* **Feed**: Personalized user recommendations based on preferences, location, and swipe history with Redis caching for performance.
* **Notifications**: Push notifications via Firebase Cloud Messaging for matches, messages, and system events with user preferences.

## 💻 API Usage Example

##### Here is a quick example of how to register a new user:

###### Request: POST /api/auth/register

```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "name": "John Doe"
}
```

###### Response 201 CREATED

```json
{
  "id": "usr_123abc",
  "email": "user@example.com",
  "name": "John Doe",
  "verified": false,
  "createdAt": "2024-01-15T10:30:00Z"
}
```

##### After registration, create a profile:

###### Request: POST /api/profiles
Authorization: Bearer <jwt-token>

```json
{
  "name": "John Doe",
  "age": 25,
  "bio": "Software developer who loves hiking and coffee",
  "interests": ["coding", "music", "hiking"],
  "location": {
    "lat": 40.7128,
    "lng": -74.0060
  },
  "preferences": {
    "ageRange": {
      "min": 22,
      "max": 30
    },
    "maxDistance": 50,
    "gender": "FEMALE"
  }
}
```

###### Response 201 CREATED

```json
{
  "id": "prof_456def",
  "userId": "usr_123abc",
  "name": "John Doe",
  "age": 25,
  "bio": "Software developer who loves hiking and coffee",
  "interests": ["coding", "music", "hiking"],
  "location": {
    "type": "Point",
    "coordinates": [-74.0060, 40.7128]
  },
  "preferences": {
    "ageRange": {
      "min": 22,
      "max": 30
    },
    "maxDistance": 50,
    "gender": "FEMALE"
  },
  "createdAt": "2024-01-15T10:35:00Z"
}
```

## 📋 Prerequisites

- **Java 21** or higher
- **Docker** and **Docker Compose**
- **Gradle** (or use provided gradlew scripts)
- **RAM**: 8 GB minimum, 12 GB recommended (7 services + PostgreSQL ×4 + MongoDB + Redis + Kafka + MinIO run concurrently)

## 🌟 Key Engineering Highlights

This project was built focusing on distributed systems patterns and production readiness:

* **🔄 Transactional Outbox Pattern**: Implemented `OutboxRelayWorker` with ShedLock to solve the Dual Write problem. Events are stored in `outbox_events` table in the same transaction as business entities, then asynchronously published to Kafka with guaranteed delivery (at-least-once semantics).
* **🛡️ Inbox Pattern (Idempotency)**: Implemented `InboxDedupService` with `inbox_events` table to prevent duplicate event processing. Ensures exactly-once processing semantics even with duplicate Kafka deliveries.
* **📡 Redis Pub/Sub for WebSocket Scaling**: Implemented `RedisChatSubscriberAdapter` and `RedisPresenceSubscriberAdapter` to enable horizontal scaling of Chat Service. WebSocket messages and online status are synchronized across multiple service instances via Redis Pub/Sub, preventing stateful monolith behavior.
* **🔒 Distributed Locking**: Utilized ShedLock with JDBC-based lock storage to prevent duplicate scheduled task execution across multiple service instances (e.g., match detection in Swipe Service).
* **🎯 Hybrid Communication Pattern**: Implemented hybrid inter-service communication - asynchronous (Kafka) for most business flows with eventual consistency, and synchronous (REST/RPC) where real-time consistent data is required (e.g., Feed Service fetching profile data via `ProfileRestAdapter`).
* **💾 Multi-Level Caching Strategy**: Implemented Cache-Aside pattern with Redis at multiple levels - profile caching in Profile Service (`RedisProfileCacheServiceImpl`), feed caching in Feed Service, and session caching in API Gateway.
* **🗄️ Polyglot Persistence**: Selected appropriate databases per service - PostgreSQL for relational data with ACID guarantees (auth, chat, swipe, notification), MongoDB for flexible document storage (profiles with GeoJSON), and Redis for high-performance caching.
* **🚀 Event-Driven Choreography**: Implemented Pub/Sub choreography pattern where services act as independent publishers and subscribers. Events fan out to multiple consumers (e.g., `UserCreated` consumed by Profile Service and Notification Service independently).

## 🚀 Quick Start

### Docker Compose Files

This repository includes two Docker Compose files:

- **docker-compose.yml** - Starts infrastructure only (databases, Kafka, Redis, MinIO) for convenient development in IDE without running services in containers
- **docker-compose.full.yml** - Deploys the entire microservices ecosystem in containers (all services + infrastructure) for quick demo or evaluation

### Option 1: Quick Demo (One Command)

For a quick demo or evaluation without installing Java/Gradle locally, use the full Docker Compose setup:

```bash
docker-compose -f docker-compose.full.yml up -d
```

This will build and start:
- **All 7 microservices** (api-gateway, auth, chat, feed, profile, swipe, notification)
- **All infrastructure** (PostgreSQL, MongoDB, Redis, Kafka, MinIO)
- **Kafka UI** for monitoring events

**Note**: First run may take 5-10 minutes to build all service images.

Access the application:
- **API Gateway**: http://localhost:8080
- **Kafka UI**: http://localhost:8090
- **MinIO Console**: http://localhost:9001 (admin/password123)

To stop all services:
```bash
docker-compose -f docker-compose.full.yml down
```

### Option 2: Development Mode (Recommended for Developers)

If you want to modify the code or debug services, run infrastructure in Docker and services locally:

#### 1. Start Infrastructure

```bash
docker-compose up -d
```

This starts databases, Kafka, Redis, and MinIO only.

#### 2. Start Services Locally

Each service can be started independently. Navigate to the service directory and run:

```bash
# Example for auth-service
cd auth-service
./gradlew bootRun
```

Or start all services from separate terminals:

```bash
# Terminal 1 - API Gateway
cd api-gateway && ./gradlew bootRun

# Terminal 2 - Auth Service
cd auth-service && ./gradlew bootRun

# Terminal 3 - Chat Service
cd chat-service && ./gradlew bootRun

# Terminal 4 - Feed Service
cd feed-service && ./gradlew bootRun

# Terminal 5 - Profile Service
cd profile-service && ./gradlew bootRun

# Terminal 6 - Swipe Service
cd swipe-service && ./gradlew bootRun

# Terminal 7 - Notification Service
cd notification-service && ./gradlew bootRun
```

#### 3. Access Services

- **API Gateway**: http://localhost:8080
- **Kafka UI**: http://localhost:8090
- **MinIO Console**: http://localhost:9001 (admin/password123)

## 📡 API Endpoints

### Authentication (via Gateway → Auth Service)

```bash
# Register
POST /api/auth/register
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "John Doe"
}

# Login
POST /api/auth/login
Content-Type: application/json
{
  "email": "user@example.com",
  "password": "password123"
}

# Google OAuth
GET /api/auth/google
```

### Profiles (via Gateway → Profile Service)

```bash
# Create Profile
POST /api/profiles
Authorization: Bearer <jwt-token>
Content-Type: application/json
{
  "name": "John Doe",
  "age": 25,
  "bio": "Software developer",
  "interests": ["coding", "music"],
  "location": {"lat": 40.7128, "lng": -74.0060}
}

# Get Profile
GET /api/profiles/{userId}
Authorization: Bearer <jwt-token>

# Upload Photo
POST /api/profiles/{userId}/photos
Authorization: Bearer <jwt-token>
Content-Type: multipart/form-data
```

### Swipes (via Gateway → Swipe Service)

```bash
# Swipe Right (Like)
POST /api/swipes
Authorization: Bearer <jwt-token>
Content-Type: application/json
{
  "targetUserId": "target-user-id",
  "direction": "RIGHT"
}

# Swipe Left (Dislike)
POST /api/swipes
Authorization: Bearer <jwt-token>
Content-Type: application/json
{
  "targetUserId": "target-user-id",
  "direction": "LEFT"
}

# Get Matches
GET /api/swipes/matches
Authorization: Bearer <jwt-token>
```

### Chat (via Gateway → Chat Service)

```bash
# WebSocket Connection
WS /api/chat/ws?token=<jwt-token>

# Send Message (via WebSocket)
{
  "recipientId": "user-id",
  "content": "Hello!",
  "type": "TEXT"
}

# Get Chat History
GET /api/chat/{userId}/messages
Authorization: Bearer <jwt-token>
```

### Feed (via Gateway → Feed Service)

```bash
# Get Feed
GET /api/feed
Authorization: Bearer <jwt-token>
Query Parameters: ?page=0&size=10
```

## 🧪 Testing

Each service includes integration tests using Testcontainers:

```bash
# Run tests for a specific service
cd auth-service
./gradlew test

# Run tests with coverage
./gradlew test jacocoTestReport
```

Testcontainers spin up real database and Kafka instances for testing, ensuring tests reflect production behavior.

### Test Coverage

| Service | Coverage |
|---------|----------|
| Auth Service | 96% |
| Notification Service | 88% |
| Profile Service | 84% |
| API Gateway | 80% |
| Feed Service | 81% |
| Swipe Service | 71% |
| Chat Service | 65% |
| **Average** | **~81%** |

Coverage measured via JaCoCo. Critical business logic and domain layer maintain 100% coverage. Infrastructure adapters (Redis, Kafka, WebSocket) account for the majority of uncovered code in Chat Service.

## 🔐 Security

- **JWT Tokens**: Stateless authentication with access and refresh tokens
- **API Gateway**: Centralized security layer
- **Service-to-Service**: Internal communication secured via network policies
- **Password Hashing**: BCrypt encryption
- **OAuth 2.0**: Google OAuth integration

## 📊 Database Schema

### Auth Service (PostgreSQL)
- `users` - User accounts
- `refresh_tokens` - Token management
- `verification_tokens` - Email verification

### Chat Service (PostgreSQL)
- `conversations` - Chat rooms
- `messages` - Message history
- `message_read_status` - Read receipts

### Profile Service (MongoDB)
- `profiles` - User profiles with flexible schema
- Embedded arrays for photos, interests

### Swipe Service (PostgreSQL)
- `swipes` - Swipe history
- `matches` - Mutual matches
- `shedlock` - Distributed lock table

### Notification Service (PostgreSQL)
- `notifications` - Notification history
- `device_tokens` - FCM device tokens

## 🎯 Key Features & Challenges

### Implemented Solutions

1. **Distributed Transactions**: Event-driven architecture with Kafka for eventual consistency
2. **Real-time Communication**: WebSocket for instant messaging
3. **Scalability**: Each service can be scaled independently
4. **Data Consistency**: Database-per-service pattern with event synchronization
5. **Media Storage**: MinIO for S3-compatible file storage
6. **Distributed Locking**: ShedLock prevents duplicate match processing
8. **Caching Strategy**: Redis for frequently accessed data (feed, sessions)

### Performance Optimizations

- Redis caching for feed generation
- Database connection pooling
- Asynchronous event processing
- Lazy loading for chat history
- CDN-ready media storage structure

## 🐛 Troubleshooting

### Services won't start
- Ensure Docker containers are running: `docker-compose ps`
- Check port conflicts (services use ports 8080-8086)
- Verify Java 21 is installed: `java -version`

### Kafka connection issues
- Check Kafka UI: http://localhost:8090
- Verify Kafka container is healthy: `docker logs shared_kafka`

### Database connection errors
- Ensure PostgreSQL containers are running
- Check database credentials in docker-compose.yml
- Verify database migrations ran successfully

## 📝 Development Notes

### Code Style
- Project uses Spotless for code formatting
- Run `./gradlew spotlessCheck` to verify formatting
- Run `./gradlew spotlessApply` to fix formatting issues

### Adding New Services
1. Create new service directory
2. Add Spring Boot dependencies
3. Configure database in docker-compose.yml
4. Add Kafka topics if needed
5. Update API Gateway routing rules

## 🚧 Future Improvements

### Short-term
- [ ] Add monitoring with Prometheus and Grafana
- [ ] Add end-to-end tests with Playwright
- [ ] Implement A/B testing framework for feed algorithms

### Long-term
- [ ] Add Elasticsearch for advanced search
- [ ] Implement GraphQL API gateway
- [ ] Implement service mesh with Istio

## 📄 License

This project is for portfolio and educational purposes.

## 🧠 What I Learned

Building this distributed microservices architecture was a significant step forward in my software engineering journey. Key takeaways include:

1. **Distributed Systems Challenges**: Moving beyond monolithic applications and understanding the complexity of distributed systems - dealing with network partitions, eventual consistency, and the CAP theorem in practice.
2. **Event-Driven Architecture**: Learning that choreography-based event-driven systems require careful design of event schemas, versioning, and idempotent consumers to handle duplicate deliveries gracefully.
3. **Dual Write Problem**: Realizing that saving to a database and publishing to an event broker in the same transaction is fundamentally impossible without the Outbox pattern, and implementing it with ShedLock for reliable delivery.
4. **WebSocket Scaling**: Understanding that WebSocket connections are inherently stateful, and implementing Redis Pub/Sub to enable horizontal scaling of real-time chat services across multiple instances.
5. **Polyglot Persistence**: Learning to select the right database for the right job - PostgreSQL for ACID guarantees, MongoDB for flexible document schemas with GeoJSON, and Redis for high-performance caching.
6. **Distributed Locking**: Implementing ShedLock to prevent duplicate scheduled task execution across multiple service instances, which is critical for match detection logic.
7. **Hybrid Communication**: Recognizing that not all inter-service communication should be asynchronous - some operations require synchronous REST calls for real-time data consistency (e.g., feed generation).
8. **Container Orchestration**: Gaining hands-on experience with Docker Compose for multi-service orchestration, understanding service dependencies, health checks, and network isolation.

## 👤 Author

Built as a portfolio project demonstrating microservices architecture, event-driven design, and modern Spring Boot practices.

*Developed by **Artem Hrytsenko***
