# InfoPulse - API Monitoring & Observability Platform

A comprehensive API monitoring and observability platform built for Leap Finance. This platform tracks API requests across multiple microservices, stores performance metrics, analyzes issues, and displays them on a real-time dashboard.

## 🏗️ Architecture Overview

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Sample Service │     │  Other Services │     │   Next.js       │
│  (Port 8081)    │     │  (Using Client) │     │   Dashboard     │
│                 │     │                 │     │   (Port 3000)   │
│ ┌─────────────┐ │     │ ┌─────────────┐ │     │                 │
│ │  Tracking   │ │     │ │  Tracking   │ │     │                 │
│ │   Client    │ │     │ │   Client    │ │     │                 │
│ └─────────────┘ │     │ └─────────────┘ │     │                 │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │  Async HTTP (API Key) │                       │ JWT Auth
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │   Collector Service    │
                    │      (Port 8080)       │
                    │                        │
                    │  • Log Ingestion API   │
                    │  • Dashboard APIs      │
                    │  • Alert Generation    │
                    │  • Incident Management │
                    └───────────┬────────────┘
                                │
                ┌───────────────┴───────────────┐
                │                               │
                ▼                               ▼
    ┌───────────────────┐           ┌───────────────────┐
    │     logs_db       │           │     meta_db       │
    │  (Primary DB)     │           │  (Secondary DB)   │
    │                   │           │                   │
    │  • api_logs       │           │  • users          │
    │  • rate_limit_    │           │  • incidents      │
    │    events         │           │  • alerts         │
    │                   │           │  • rate_limiter_  │
    │                   │           │    configs        │
    │                   │           │  • resolution_    │
    │                   │           │    audit          │
    └───────────────────┘           └───────────────────┘
                    │                       │
                    └───────────┬───────────┘
                                │
                                ▼
                    ┌────────────────────────┐
                    │    MongoDB Atlas       │
                    │   (Single Cluster)     │
                    └────────────────────────┘
```

## 🛠️ Tech Stack

| Component        | Technology                                         |
| ---------------- | -------------------------------------------------- |
| Backend          | Spring Boot 3.2+ (Kotlin), Gradle (Kotlin DSL)     |
| Frontend         | Next.js 14 (App Router), Tailwind CSS, Recharts    |
| Database         | MongoDB (Dual logical databases on single cluster) |
| Auth             | JWT (Dashboard), API Key (Service-to-Service)      |
| Rate Limiting    | Bucket4j                                           |
| Containerization | Docker, Docker Compose                             |

## 📁 Project Structure

```
infopulse/
├── tracking-client/          # Reusable tracking library (JAR)
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/leapfinance/infopulse/tracking/
│           ├── config/           # Configuration properties
│           ├── model/            # Data models (ApiLogEntry, etc.)
│           ├── filter/           # OncePerRequestFilter
│           ├── ratelimit/        # Bucket4j rate limiter
│           ├── collector/        # Async WebClient
│           └── TrackingAutoConfiguration.kt
│
├── collector-service/        # Central collector backend
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/leapfinance/infopulse/collector/
│           ├── config/           # MongoDB, Security configs
│           ├── entity/           # MongoDB entities
│           │   ├── logs/         # logs_db entities
│           │   └── meta/         # meta_db entities
│           ├── repository/       # Dual DB repositories
│           ├── service/          # Business logic
│           ├── controller/       # REST APIs
│           ├── security/         # JWT & API Key auth
│           └── exception/        # Global error handling
│
├── sample-service/           # Demo service using tracking client
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       └── com/leapfinance/infopulse/sample/
│           └── controller/       # Sample endpoints
│
├── dashboard/                # Next.js frontend
│   ├── package.json
│   ├── Dockerfile
│   └── src/
│       ├── app/                  # App Router pages
│       ├── components/           # React components
│       └── lib/                  # API client, auth, utils
│
├── docker/                   # Docker configuration
│   ├── Dockerfile.collector
│   ├── Dockerfile.sample
│   └── mongo-init.js
│
├── docker-compose.yml        # Full stack orchestration
├── build.gradle.kts          # Root Gradle config
└── settings.gradle.kts       # Multi-project settings
```

## 🗄️ Database Schemas

### logs_db (Primary - High Volume)

#### api_logs Collection

```javascript
{
  _id: ObjectId,
  endpoint: String,           // e.g., "/api/v1/orders"
  method: String,             // GET, POST, PUT, DELETE
  requestSize: Long,          // bytes
  responseSize: Long,         // bytes
  statusCode: Int,            // HTTP status code
  timestamp: ISODate,
  latency: Long,              // milliseconds
  serviceName: String,        // e.g., "orders-service"
  traceId: String,            // for distributed tracing
  clientIp: String,
  userAgent: String,
  requestHeaders: Object,     // filtered headers
  errorMessage: String,       // if any
  isSlow: Boolean,            // latency > 500ms
  isBroken: Boolean           // status 5xx
}

// Indexes
{ serviceName: 1, timestamp: -1 }
{ endpoint: 1, timestamp: -1 }
{ statusCode: 1, timestamp: -1 }
{ latency: -1 }
```

#### rate_limit_events Collection

```javascript
{
  _id: ObjectId,
  serviceName: String,
  endpoint: String,
  method: String,
  timestamp: ISODate,
  configuredLimit: Int,       // e.g., 100
  eventType: "rate-limit-hit",
  clientIp: String
}

// Indexes
{ serviceName: 1, timestamp: -1 }
```

### meta_db (Secondary - Metadata)

#### users Collection

```javascript
{
  _id: ObjectId,
  username: String,           // unique
  email: String,              // unique
  password: String,           // BCrypt hashed
  role: "ADMIN" | "DEVELOPER" | "VIEWER",
  createdAt: ISODate,
  updatedAt: ISODate
}
```

#### incidents Collection

```javascript
{
  _id: ObjectId,
  serviceName: String,
  endpoint: String,
  method: String,
  incidentType: "SLOW_API" | "BROKEN_API" | "RATE_LIMIT_HIT",
  status: "OPEN" | "ACKNOWLEDGED" | "RESOLVED",
  avgLatency: Long,
  errorRate: Double,
  sampleErrorMessage: String,
  occurrenceCount: Int,
  firstSeenAt: ISODate,
  lastSeenAt: ISODate,
  createdAt: ISODate,
  updatedAt: ISODate,
  resolvedBy: String,
  resolvedAt: ISODate,
  resolutionNotes: String,
  version: Long               // For optimistic locking
}
```

#### alerts Collection

```javascript
{
  _id: ObjectId,
  alertType: "SLOW_API" | "ERROR_SPIKE" | "RATE_LIMIT_EXCEEDED",
  serviceName: String,
  endpoint: String,
  method: String,
  message: String,
  timestamp: ISODate,
  acknowledged: Boolean,
  acknowledgedBy: String,
  acknowledgedAt: ISODate,
  incidentId: String,
  metadata: Object
}
```

## 🔐 Authentication

### Service-to-Service (API Key)

Used by tracking clients to send logs to the collector.

```yaml
# In your service's application.yaml
monitoring:
  collector:
    api-key: ${SERVICE_API_KEY:your-api-key}
```

Header: `X-Service-Api-Key: your-api-key`

### Dashboard (JWT)

Used by the Next.js dashboard for user authentication.

**Demo Credentials:**

- Username: `demo`
- Password: `demo123`

## ⚡ Rate Limiter

The rate limiter uses **Bucket4j** with a **soft limit** approach:

- Default: 100 requests/second per service
- When exceeded: Request **continues normally** but a `rate-limit-hit` event is logged
- Configurable per service

### How It Works

```
Request → Rate Limiter Check → Request Proceeds
                 │
                 ▼
         Limit Exceeded?
                 │
        ┌────────┴────────┐
        │ Yes             │ No
        ▼                 ▼
  Log Event Async    Continue
  (Non-blocking)     Normally
```

### Configuration

```yaml
# application.yaml
monitoring:
  rate-limit:
    enabled: true
    limit: 100 # requests per window
    window-seconds: 1 # time window
```

## 🔄 Dual MongoDB Setup

The collector connects to a **single MongoDB cluster** (Atlas or local) but uses **two logical databases**:

### Why Two Databases?

1. **Data Isolation**: High-volume logs don't impact metadata operations
2. **Independent Scaling**: Each database can have different read/write patterns
3. **Backup Flexibility**: Different backup strategies for each
4. **Query Performance**: Metadata queries aren't affected by log volume

### Implementation

```kotlin
@Configuration
class MongoConfig {
    // Single shared MongoClient
    @Bean
    fun mongoClient(): MongoClient { ... }

    // Primary: logs_db
    @Bean @Primary
    @Qualifier("logsMongoTemplate")
    fun logsMongoTemplate(): MongoTemplate { ... }

    // Secondary: meta_db
    @Bean
    @Qualifier("metaMongoTemplate")
    fun metaMongoTemplate(): MongoTemplate { ... }
}
```

### Usage in Repositories

```kotlin
@Repository
class ApiLogRepository(
    @Qualifier("logsMongoTemplate")
    private val mongoTemplate: MongoTemplate
)

@Repository
class IncidentRepository(
    @Qualifier("metaMongoTemplate")
    private val mongoTemplate: MongoTemplate
)
```

## 🔒 Concurrency Safety (Optimistic Locking)

When multiple developers try to resolve the same incident:

```kotlin
@Document(collection = "incidents")
data class IncidentEntity(
    // ... other fields
    @Version
    val version: Long? = null  // Increments on each update
)
```

### Resolution Flow

1. Frontend fetches incident with current `version`
2. Developer clicks "Resolve" → sends `version` in request
3. Backend checks if `version` matches → Updates atomically with `version + 1`
4. If mismatch → Returns 409 Conflict → Frontend shows "refresh" message

## 🚀 Getting Started

### Prerequisites

- Docker & Docker Compose
- (Optional) JDK 17+ for local development
- (Optional) Node.js 20+ for local frontend development

### Quick Start with Docker

```bash
# Clone the repository
cd "Leap Finance"

# Start all services
docker-compose up -d

# View logs
docker-compose logs -f

# Access the dashboard
open http://localhost:3000
```

### Environment Variables

```bash
# MongoDB (for Atlas, use your connection string)
MONGODB_URI=mongodb://admin:admin123@mongodb:27017

# Security
JWT_SECRET=your-jwt-secret
SERVICE_API_KEY=your-service-api-key

# Dashboard
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### Using MongoDB Atlas

1. Create a MongoDB Atlas cluster
2. Get your connection string
3. Update `docker-compose.yml`:

```yaml
collector-service:
  environment:
    MONGODB_URI: mongodb+srv://username:password@cluster.mongodb.net
```

## 📊 Dashboard Features

### Overview Page

- Total requests, slow APIs, broken APIs counts
- Rate limit violations
- Average latency
- Open incidents count
- Top 5 slowest endpoints chart
- Error rate over time graph

### API Logs

- Filterable table with all API requests
- Filter by: Service, Endpoint, Method, Status, Slow/Broken
- Real-time updates (30s polling)

### Incidents

- List of detected issues (slow APIs, broken APIs)
- Status: Open, Acknowledged, Resolved
- Resolve with notes
- Optimistic locking prevents conflicts

### Alerts

- Real-time alert feed
- Types: Slow API, Error Spike, Rate Limit Exceeded
- Acknowledge functionality

## 🧪 Testing the Platform

### Generate Test Traffic

```bash
# Hit various endpoints on sample-service
curl http://localhost:8081/api/v1/orders
curl http://localhost:8081/api/v1/products
curl http://localhost:8081/api/v1/users

# Generate slow API alerts
curl http://localhost:8081/api/v1/slow-endpoint

# Generate error alerts
curl http://localhost:8081/api/v1/error-endpoint

# Load test (hit rate limiter)
for i in {1..200}; do
  curl http://localhost:8081/api/v1/load-test &
done
```

## 📝 API Documentation

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

### Key Endpoints

| Endpoint                         | Method | Description       |
| -------------------------------- | ------ | ----------------- |
| `/api/v1/auth/login`             | POST   | User login        |
| `/api/v1/logs/batch`             | POST   | Ingest log batch  |
| `/api/v1/dashboard/summary`      | GET    | Dashboard metrics |
| `/api/v1/dashboard/logs`         | GET    | Filtered logs     |
| `/api/v1/incidents`              | GET    | List incidents    |
| `/api/v1/incidents/{id}/resolve` | POST   | Resolve incident  |
| `/api/v1/alerts`                 | GET    | List alerts       |

## 🔧 Development

### Local Backend Development

```bash
# Build all modules
./gradlew build

# Run collector service
./gradlew :collector-service:bootRun

# Run sample service
./gradlew :sample-service:bootRun
```

### Local Frontend Development

```bash
cd dashboard
npm install
npm run dev
```

## 🚀 Cloud Deployment

### Option 1: Railway (Recommended)

Railway supports Docker and provides easy deployment from GitHub.

#### Step 1: Set up MongoDB Atlas (Free Tier)

1. Go to [MongoDB Atlas](https://www.mongodb.com/atlas)
2. Create a free cluster (M0)
3. Create a database user
4. Add `0.0.0.0/0` to IP whitelist (for Railway)
5. Get your connection string

#### Step 2: Deploy to Railway

1. Push your code to GitHub
2. Go to [Railway](https://railway.app)
3. Click "New Project" → "Deploy from GitHub Repo"
4. Select your repository

#### Step 3: Deploy Collector Service

1. In Railway, add a new service
2. Point to your repo, set root to `/`
3. Set Dockerfile path: `docker/Dockerfile.collector`
4. Add environment variables:
   ```
   MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/?retryWrites=true&w=majority
   JWT_SECRET=your-super-secret-jwt-key-min-32-characters
   SERVICE_API_KEY=your-service-api-key
   CORS_ALLOWED_ORIGINS=https://your-dashboard.railway.app
   ```
5. Deploy and note the generated URL

#### Step 4: Deploy Dashboard

1. Add another service in Railway
2. Point to your repo, set root to `/dashboard`
3. Set Dockerfile path: `Dockerfile`
4. Add environment variables:
   ```
   NEXT_PUBLIC_API_URL=https://your-collector-url.railway.app
   ```
5. Deploy!

### Option 2: Render.com

#### Backend (Collector Service)

1. Create a "Web Service"
2. Connect your GitHub repo
3. Set Dockerfile path: `docker/Dockerfile.collector`
4. Add environment variables (same as Railway)

#### Frontend (Dashboard)

1. Create another "Web Service"
2. Root directory: `dashboard`
3. Dockerfile path: `Dockerfile`
4. Add `NEXT_PUBLIC_API_URL` environment variable

### Option 3: Vercel + Render (Hybrid)

Best performance for Next.js:

- **Dashboard**: Deploy to Vercel (zero config, best for Next.js)
- **Collector**: Deploy to Render or Railway
- **MongoDB**: Use MongoDB Atlas

### Environment Variables Reference

| Variable               | Service           | Description                          |
| ---------------------- | ----------------- | ------------------------------------ |
| `MONGODB_URI`          | Collector         | MongoDB connection string            |
| `JWT_SECRET`           | Collector         | Secret for JWT tokens (min 32 chars) |
| `SERVICE_API_KEY`      | Collector, Sample | API key for service auth             |
| `CORS_ALLOWED_ORIGINS` | Collector         | Comma-separated frontend URLs        |
| `NEXT_PUBLIC_API_URL`  | Dashboard         | Backend API URL                      |
| `COLLECTOR_URL`        | Sample            | Internal collector URL               |

### Production Checklist

- [ ] MongoDB Atlas cluster created
- [ ] Strong JWT_SECRET generated (`openssl rand -base64 64`)
- [ ] CORS origins configured for production domains
- [ ] Environment variables set in deployment platform
- [ ] Health checks passing
- [ ] SSL/HTTPS enabled (automatic on Railway/Render)

## 📦 Deployment Considerations

1. **MongoDB Atlas**: Use dedicated cluster for production
2. **Secrets Management**: Use Kubernetes secrets or Vault
3. **Monitoring**: Add Prometheus/Grafana for infrastructure monitoring
4. **Log Retention**: Implement TTL indexes for old logs
5. **Horizontal Scaling**: Collector service is stateless, can scale horizontally

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

MIT License - © 2024 Leap Finance
