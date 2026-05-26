
# System Architecture Diagrams (Mermaid)

## High-Level Architecture

```mermaid
flowchart TB
    subgraph Client["CLIENT LAYER"]
        Mobile["Mobile App<br/>(React Native)"]
        Web["Web App<br/>(React)"]
        Admin["Admin Panel<br/>(React)"]
        Third["Third Party<br/>(Google)"]
    end

    Gateway["Spring Cloud Gateway<br/>Port 8080<br/><br/>JWT Validate<br/>Rate Limit (Redis)<br/>Route Config<br/>Circuit Breaker"]

    Mobile --> Gateway
    Web --> Gateway
    Admin --> Gateway
    Third --> Gateway

    Auth["AUTH SERVICE<br/>Port 8081<br/><br/>Spring Security<br/>JWT (jjwt)<br/>Google OAuth"]
    Profile["PROFILE SERVICE<br/>Port 8084<br/><br/>MongoDB<br/>Redis Cache<br/>MinIO Client"]

    Chat["CHAT SERVICE<br/>Port 8082<br/><br/>WebSocket STOMP<br/>Redis Pub/Sub"]
    Swipe["SWIPE SERVICE<br/>Port 8085<br/><br/>ShedLock<br/>Kafka Events"]

    Feed["FEED SERVICE<br/>Port 8083<br/><br/>Redis Cache<br/>Kafka Consumer"]
    Notification["NOTIFICATION SERVICE<br/>Port 8086<br/><br/>Firebase FCM<br/>Kafka Consumer"]

    Gateway --> Auth
    Gateway --> Profile
    Gateway --> Chat
    Gateway --> Swipe
    Gateway --> Feed
    Gateway --> Notification

    Auth --> AuthDB[(PostgreSQL auth_db)]
    Profile --> ProfileDB[(MongoDB profile_db)]
    Chat --> ChatDB[(PostgreSQL chat_db)]
    Swipe --> SwipeDB[(PostgreSQL swipe_db)]
    Notification --> NotificationDB[(PostgreSQL notification_db)]

    Kafka[(Apache Kafka)]
    Redis[(Redis)]
    MinIO[(MinIO)]

    Auth <--> Kafka
    Profile <--> Kafka
    Chat <--> Kafka
    Swipe <--> Kafka
    Feed <--> Kafka
    Notification <--> Kafka

    Chat <--> MinIO
    Profile <--> MinIO

    Feed <--> Redis
    Profile <--> Redis
    Gateway <--> Redis
```

## Event-Driven Communication

```mermaid
flowchart LR
    Auth["Auth Service"]
    Profile["Profile Service"]
    Swipe["Swipe Service"]
    Chat["Chat Service"]
    Feed["Feed Service"]
    Notif["Notification Service"]

    Kafka[(Apache Kafka)]

    Auth -- "user-events" --> Kafka
    Profile -- "profile-events" --> Kafka
    Swipe -- "swipe-events / match-events" --> Kafka
    Chat -- "chat-events" --> Kafka

    Kafka --> Profile
    Kafka --> Feed
    Kafka --> Chat
    Kafka --> Notif
    Kafka --> Notif
```

## User Registration Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Auth as Auth Service
    participant Kafka
    participant Profile as Profile Service
    participant Feed as Feed Service

    Client->>Gateway: POST /api/auth/register
    Gateway->>Auth: Route request

    Auth->>Auth: Validate input
    Auth->>Auth: Hash password
    Auth->>Auth: Save user
    Auth->>Auth: Generate token
    Auth->>Auth: Send verification email

    Auth->>Kafka: Publish UserCreated
    Kafka->>Profile: Consume event
    Kafka->>Notif: Consume event

    Profile->>Profile: Create initial profile
    Profile->>Kafka: Publish ProfileCreated

    Kafka->>Feed: Consume event
    Feed->>Feed: Add user to recommendation pool

    Notif->>Notif: Send welcome notification
```

## Match Flow

```mermaid
sequenceDiagram
    participant A as User A
    participant Swipe as Swipe Service
    participant Kafka
    participant Chat as Chat Service
    participant Notif as Notification Service
    participant B as User B

    A->>Swipe: Swipe Right
    Swipe->>Swipe: Record swipe
    Swipe->>Swipe: Check match (none)

    B->>Swipe: Swipe Right
    Swipe->>Swipe: Record swipe
    Swipe->>Swipe: MATCH!

    Swipe->>Kafka: Publish UserMatched

    Kafka->>Chat: Create conversation
    Kafka->>Notif: Send push notifications

    Notif->>A: Push notification
    Notif->>B: Push notification
```

## Chat Flow

```mermaid
sequenceDiagram
    participant A as User A
    participant Chat as Chat Service
    participant Kafka
    participant Notif as Notification Service
    participant B as User B

    A->>Chat: Send message
    Chat->>Chat: Save to PostgreSQL
    Chat->>Chat: Upload media to MinIO

    Chat->>Kafka: Publish MessageSent
    Kafka->>Notif: Consume event

    Notif->>B: Send push notification
```

## Security Flow

```mermaid
sequenceDiagram
    participant Client
    participant Gateway as API Gateway
    participant Auth as Auth Service
    participant Profile as Profile Service

    Client->>Gateway: POST /api/auth/login
    Gateway->>Auth: Route request

    Auth->>Auth: Validate credentials
    Auth->>Auth: Verify password hash
    Auth->>Auth: Generate JWT + Refresh token

    Auth-->>Client: Return tokens

    Client->>Gateway: GET /api/profiles/me
    Gateway->>Gateway: Validate JWT
    Gateway->>Profile: Forward authenticated request

    Profile->>Profile: Fetch profile
    Profile-->>Client: Return profile
```

## Caching Flow

```mermaid
flowchart TD
    Client --> Gateway["API Gateway"]
    Gateway --> Feed["Feed Service"]

    Feed --> RedisCheck{"Cache HIT?"}

    RedisCheck -->|YES| Cached["Return cached feed"]
    RedisCheck -->|NO| Generate["Generate feed"]

    Generate --> Query["Query preferences"]
    Query --> Filter["Filter by age/location/interests"]
    Filter --> Algo["Apply algorithm"]
    Algo --> Cache["Cache in Redis (TTL 5m)"]

    Cache --> Result["Return feed"]
    Cached --> Result

    Profile["Profile Service"] --> Kafka[(Kafka)]
    Kafka --> Invalidate["Invalidate Redis cache"]
```

## Deployment Architecture

```mermaid
flowchart TB
    LB["Load Balancer<br/>Nginx / HAProxy"]

    subgraph K8S["Kubernetes Cluster"]
        Gateway["API Gateway<br/>3 pods"]
        Auth["Auth Service<br/>3 pods"]
        Chat["Chat Service<br/>5 pods"]
        Profile["Profile Service<br/>3 pods"]
        Feed["Feed Service<br/>3 pods"]
        Swipe["Swipe Service<br/>3 pods"]
        Notif["Notification Service<br/>2 pods"]
    end

    subgraph Managed["Managed Services"]
        PG[(PostgreSQL RDS)]
        Mongo[(MongoDB Atlas)]
        Redis[(Redis ElastiCache)]
        Kafka[(Kafka MSK)]
        S3[(S3 / MinIO)]
        Firebase[(Firebase FCM)]
    end

    LB --> K8S

    Gateway --> PG
    Auth --> PG
    Chat --> PG
    Profile --> Mongo
    Feed --> Redis
    Swipe --> PG
    Notif --> Firebase

    K8S <--> Kafka
    Chat <--> S3
    Profile <--> S3
```

## Service Dependencies

```mermaid
flowchart LR
    Auth["Auth Service"]
    Profile["Profile Service"]
    Feed["Feed Service"]
    Swipe["Swipe Service"]
    Chat["Chat Service"]
    Notif["Notification Service"]

    Auth -->|"UserCreated"| Profile
    Auth -->|"UserCreated"| Notif

    Profile -->|"ProfileCreated"| Feed
    Profile -->|"ProfileUpdated"| Feed

    Swipe -->|"UserMatched"| Chat
    Swipe -->|"UserMatched"| Notif

    Chat -->|"MessageSent"| Notif
```

## WebSocket Horizontal Scaling

```mermaid
flowchart LR
    subgraph Instance1["Chat Service Instance 1"]
        A["User A WebSocket Session"]
        Save["Save to PostgreSQL"]
        Publish["Publish to Redis"]
    end

    Redis[(Redis Pub/Sub)]

    subgraph Instance2["Chat Service Instance 2"]
        Receive["Receive Redis event"]
        B["User B WebSocket Session"]
    end

    A --> Save
    Save --> Publish
    Publish --> Redis
    Redis --> Receive
    Receive --> B
```

## Port Mapping

```mermaid
flowchart TD
    Gateway["8080 - API Gateway"]
    Auth["8081 - Auth Service"]
    Chat["8082 - Chat Service"]
    Feed["8083 - Feed Service"]
    Profile["8084 - Profile Service"]
    Swipe["8085 - Swipe Service"]
    Notif["8086 - Notification Service"]

    Gateway --> Auth
    Gateway --> Chat
    Gateway --> Feed
    Gateway --> Profile
    Gateway --> Swipe
    Gateway --> Notif
```
