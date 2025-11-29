import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Breakpoint test - find the breaking point of the system
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');
const requestCount = new Counter('requests');

export const options = {
    stages: [
        { duration: '2m', target: 50 },    // Start moderate
        { duration: '2m', target: 100 },   // Increase
        { duration: '2m', target: 150 },   // More pressure
        { duration: '2m', target: 200 },   // Heavy load
        { duration: '2m', target: 250 },   // Very heavy
        { duration: '2m', target: 300 },   // Extreme
        { duration: '2m', target: 350 },   // Breaking point?
        { duration: '2m', target: 400 },   // Beyond limits
        { duration: '2m', target: 0 },     // Recovery
    ],
    thresholds: {
        // Relaxed thresholds - we're trying to break the system
        http_req_duration: ['p(50)<1000'],  // At least 50% should be under 1s
        http_req_failed: ['rate<0.5'],       // Allow up to 50% failures
    },
};

export function setup() {
    const setupStatusRes = http.get(`${BASE_URL}/api/v1/auth/setup/status`);
    if (setupStatusRes.status === 200) {
        const status = JSON.parse(setupStatusRes.body);
        if (status.setupRequired) {
            http.post(
                `${BASE_URL}/api/v1/auth/setup`,
                JSON.stringify({
                    username: 'breakadmin',
                    email: 'breakadmin@test.com',
                    password: 'breakTestPass123',
                }),
                { headers: { 'Content-Type': 'application/json' } }
            );
        }
    }
    
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            username: 'breakadmin',
            password: 'breakTestPass123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    
    if (loginRes.status === 200) {
        return { token: JSON.parse(loginRes.body).accessToken };
    }
    return { token: null };
}

export default function(data) {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.token}`,
    };
    
    // Heavy read load
    let start = Date.now();
    const productsRes = http.get(`${BASE_URL}/api/v1/products`, { headers });
    responseTime.add(Date.now() - start);
    requestCount.add(1);
    errorRate.add(!check(productsRes, { 'products OK': (r) => r.status === 200 }));
    
    start = Date.now();
    const summaryRes = http.get(`${BASE_URL}/api/v1/metrics/inventory-summary`, { headers });
    responseTime.add(Date.now() - start);
    requestCount.add(1);
    errorRate.add(!check(summaryRes, { 'summary OK': (r) => r.status === 200 }));
    
    start = Date.now();
    const stockRes = http.get(`${BASE_URL}/api/v1/metrics/stock-levels`, { headers });
    responseTime.add(Date.now() - start);
    requestCount.add(1);
    errorRate.add(!check(stockRes, { 'stock OK': (r) => r.status === 200 }));
    
    // Some write load
    if (Math.random() < 0.2) {
        const sku = `BREAK-${__VU}-${Date.now()}`;
        start = Date.now();
        const createRes = http.post(
            `${BASE_URL}/api/v1/products`,
            JSON.stringify({
                sku: sku,
                name: `Break Test ${__VU}`,
                description: 'Breakpoint test product',
                quantityOnHand: 50,
                unitPrice: 25.00,
            }),
            { headers }
        );
        responseTime.add(Date.now() - start);
        requestCount.add(1);
        
        if (createRes.status === 201) {
            const product = JSON.parse(createRes.body);
            http.del(`${BASE_URL}/api/v1/products/${product.id}`, null, { headers });
            requestCount.add(1);
        }
    }
}

export function handleSummary(data) {
    // Custom summary to identify breaking point
    const errorThreshold = 0.1;  // 10% error rate indicates breaking point
    const p95Threshold = 2000;   // 2s p95 indicates breaking point
    
    let breakingPoint = 'Not reached';
    
    // Analyze results to find breaking point
    if (data.metrics.errors && data.metrics.errors.values.rate > errorThreshold) {
        breakingPoint = `Error rate exceeded ${errorThreshold * 100}%`;
    }
    if (data.metrics.http_req_duration && data.metrics.http_req_duration.values['p(95)'] > p95Threshold) {
        breakingPoint = `P95 response time exceeded ${p95Threshold}ms`;
    }
    
    return {
        'stdout': `
========================================
BREAKPOINT TEST RESULTS
========================================
Total Requests: ${data.metrics.requests ? data.metrics.requests.values.count : 'N/A'}
Error Rate: ${data.metrics.errors ? (data.metrics.errors.values.rate * 100).toFixed(2) : 'N/A'}%
P50 Response Time: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(50)'].toFixed(2) : 'N/A'}ms
P95 Response Time: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 'N/A'}ms
P99 Response Time: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(99)'].toFixed(2) : 'N/A'}ms
Max Response Time: ${data.metrics.http_req_duration ? data.metrics.http_req_duration.values.max.toFixed(2) : 'N/A'}ms
Breaking Point Indicator: ${breakingPoint}
========================================
`,
        'breakpoint-results.json': JSON.stringify(data, null, 2),
    };
}
