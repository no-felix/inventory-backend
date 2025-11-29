# k6 Load Tests

Performance and load testing suite using [Grafana k6](https://k6.io/).

## Quick Start

```bash
# Install k6
choco install k6          # Windows (Chocolatey)
winget install k6         # Windows (winget)
brew install k6           # macOS

# Start the backend
./mvnw spring-boot:run

# Run smoke test
k6 run k6/smoke-test.js
```

## Available Tests

| Test | Command | Duration | Use Case |
|------|---------|----------|----------|
| **Smoke** | `k6 run k6/smoke-test.js` | 30s | Quick sanity check after deployment |
| **Load** | `k6 run k6/load-test.js` | 10min | Normal load testing (10-20 VUs) |
| **Spike** | `k6 run k6/spike-test.js` | 5min | Sudden traffic surge (5→100 VUs) |
| **Soak** | `k6 run k6/soak-test.js` | 1hr | Memory leak detection |
| **Breakpoint** | `k6 run k6/breakpoint-test.js` | 18min | Find system limits (up to 400 VUs) |

## Configuration

Override the base URL for different environments:

```bash
k6 run -e BASE_URL=https://staging.example.com k6/smoke-test.js
```

Default: `http://localhost:8080`

## What Gets Tested

All tests authenticate via `/api/v1/auth/setup` (auto-creates admin on first run) and cover:

- **Authentication** - Login, token refresh, setup status
- **Products** - CRUD operations
- **Purchase Orders** - Create, list, receive
- **Stock Movements** - List with filters
- **Metrics** - All 6 metric endpoints

## Pass/Fail Thresholds

| Metric | Threshold |
|--------|-----------|
| Response time (p95) | < 500ms |
| Request failure rate | < 1% |
| Custom error rate | < 5% |

## Output Options

```bash
# JSON output for processing
k6 run --out json=results.json k6/smoke-test.js

# HTML report (requires xk6-dashboard)
k6 run --out web-dashboard k6/load-test.js
```
