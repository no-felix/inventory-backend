import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Spike test - sudden surge in traffic
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const errorRate = new Rate('errors');
const responseTime = new Trend('response_time');

export const options = {
    stages: [
        { duration: '10s', target: 5 },    // Warm up
        { duration: '1m', target: 5 },     // Normal load
        { duration: '10s', target: 100 },  // Spike to 100 users
        { duration: '3m', target: 100 },   // Stay at 100
        { duration: '10s', target: 5 },    // Scale back down
        { duration: '1m', target: 5 },     // Recovery period
        { duration: '10s', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'],  // Allow higher latency during spike
        http_req_failed: ['rate<0.1'],       // Allow up to 10% errors during spike
        errors: ['rate<0.15'],
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
                    username: 'spikeadmin',
                    email: 'spikeadmin@test.com',
                    password: 'spikeTestPass123',
                }),
                { headers: { 'Content-Type': 'application/json' } }
            );
        }
    }
    
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            username: 'spikeadmin',
            password: 'spikeTestPass123',
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
    
    // Simulate realistic user behavior during spike
    
    // Read-heavy operations (most common during traffic spikes)
    let start = Date.now();
    const productsRes = http.get(`${BASE_URL}/api/v1/products`, { headers });
    responseTime.add(Date.now() - start);
    const productsOk = check(productsRes, {
        'products list OK': (r) => r.status === 200,
    });
    errorRate.add(!productsOk);
    
    start = Date.now();
    const summaryRes = http.get(`${BASE_URL}/api/v1/metrics/inventory-summary`, { headers });
    responseTime.add(Date.now() - start);
    const summaryOk = check(summaryRes, {
        'summary OK': (r) => r.status === 200,
    });
    errorRate.add(!summaryOk);
    
    start = Date.now();
    const stockRes = http.get(`${BASE_URL}/api/v1/metrics/stock-levels`, { headers });
    responseTime.add(Date.now() - start);
    const stockOk = check(stockRes, {
        'stock levels OK': (r) => r.status === 200,
    });
    errorRate.add(!stockOk);
    
    // Occasional write operations (10% of requests)
    if (Math.random() < 0.1) {
        const sku = `SPIKE-${__VU}-${Date.now()}`;
        start = Date.now();
        const createRes = http.post(
            `${BASE_URL}/api/v1/products`,
            JSON.stringify({
                sku: sku,
                name: `Spike Test ${__VU}`,
                description: 'Created during spike test',
                quantityOnHand: 100,
                unitPrice: 50.00,
            }),
            { headers }
        );
        responseTime.add(Date.now() - start);
        
        if (createRes.status === 201) {
            const product = JSON.parse(createRes.body);
            // Cleanup
            http.del(`${BASE_URL}/api/v1/products/${product.id}`, null, { headers });
        }
    }
    
    sleep(0.5);
}
