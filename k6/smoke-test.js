import http from 'k6/http';
import { check, sleep } from 'k6';

// Smoke test configuration - minimal load to verify system works
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const options = {
    vus: 1,
    duration: '30s',
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.01'],
    },
};

export function setup() {
    // Check and setup admin if needed
    const setupStatusRes = http.get(`${BASE_URL}/api/v1/auth/setup/status`);
    if (setupStatusRes.status === 200) {
        const status = JSON.parse(setupStatusRes.body);
        if (status.setupRequired) {
            http.post(
                `${BASE_URL}/api/v1/auth/setup`,
                JSON.stringify({
                    username: 'smokeadmin',
                    email: 'smokeadmin@test.com',
                    password: 'smokeTestPass123',
                }),
                { headers: { 'Content-Type': 'application/json' } }
            );
        }
    }
    
    // Login
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            username: 'smokeadmin',
            password: 'smokeTestPass123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    
    if (loginRes.status === 200) {
        return { token: JSON.parse(loginRes.body).accessToken };
    }
    
    console.error('Failed to login for smoke test');
    return { token: null };
}

export default function(data) {
    const headers = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${data.token}`,
    };
    
    // Test all critical endpoints
    
    // Auth - Setup Status (public)
    const setupRes = http.get(`${BASE_URL}/api/v1/auth/setup/status`);
    check(setupRes, { 'setup status OK': (r) => r.status === 200 });
    
    // Products - List
    const productsRes = http.get(`${BASE_URL}/api/v1/products`, { headers });
    check(productsRes, { 'list products OK': (r) => r.status === 200 });
    
    // Purchase Orders - List
    const ordersRes = http.get(`${BASE_URL}/api/v1/purchase-orders`, { headers });
    check(ordersRes, { 'list orders OK': (r) => r.status === 200 });
    
    // Stock Movements - List
    const movementsRes = http.get(`${BASE_URL}/api/v1/stock-movements`, { headers });
    check(movementsRes, { 'list movements OK': (r) => r.status === 200 });
    
    // Metrics - Summary
    const summaryRes = http.get(`${BASE_URL}/api/v1/metrics/inventory-summary`, { headers });
    check(summaryRes, { 'inventory summary OK': (r) => r.status === 200 });
    
    // Metrics - Stock Levels
    const stockRes = http.get(`${BASE_URL}/api/v1/metrics/stock-levels`, { headers });
    check(stockRes, { 'stock levels OK': (r) => r.status === 200 });
    
    // Metrics - Low Stock
    const lowStockRes = http.get(`${BASE_URL}/api/v1/metrics/low-stock-alerts`, { headers });
    check(lowStockRes, { 'low stock alerts OK': (r) => r.status === 200 });
    
    sleep(1);
}
