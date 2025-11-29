import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// Soak test - extended duration to find memory leaks and degradation
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const errorRate = new Rate('errors');

export const options = {
    stages: [
        { duration: '2m', target: 20 },   // Ramp up
        { duration: '56m', target: 20 },  // Stay at 20 users for ~1 hour
        { duration: '2m', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.01'],
        errors: ['rate<0.02'],
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
                    username: 'soakadmin',
                    email: 'soakadmin@test.com',
                    password: 'soakTestPass123',
                }),
                { headers: { 'Content-Type': 'application/json' } }
            );
        }
    }
    
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            username: 'soakadmin',
            password: 'soakTestPass123',
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
    
    // Mix of operations simulating realistic usage patterns
    
    // 40% - List products
    if (Math.random() < 0.4) {
        const res = http.get(`${BASE_URL}/api/v1/products`, { headers });
        errorRate.add(!check(res, { 'list products OK': (r) => r.status === 200 }));
    }
    
    // 20% - View metrics
    if (Math.random() < 0.2) {
        const res = http.get(`${BASE_URL}/api/v1/metrics/inventory-summary`, { headers });
        errorRate.add(!check(res, { 'summary OK': (r) => r.status === 200 }));
        
        const stockRes = http.get(`${BASE_URL}/api/v1/metrics/stock-levels`, { headers });
        errorRate.add(!check(stockRes, { 'stock OK': (r) => r.status === 200 }));
    }
    
    // 15% - View purchase orders
    if (Math.random() < 0.15) {
        const res = http.get(`${BASE_URL}/api/v1/purchase-orders`, { headers });
        errorRate.add(!check(res, { 'orders OK': (r) => r.status === 200 }));
    }
    
    // 10% - View stock movements
    if (Math.random() < 0.1) {
        const res = http.get(`${BASE_URL}/api/v1/stock-movements`, { headers });
        errorRate.add(!check(res, { 'movements OK': (r) => r.status === 200 }));
    }
    
    // 5% - Create and delete product (write operation)
    if (Math.random() < 0.05) {
        const sku = `SOAK-${__VU}-${__ITER}-${Date.now()}`;
        const createRes = http.post(
            `${BASE_URL}/api/v1/products`,
            JSON.stringify({
                sku: sku,
                name: `Soak Test Product ${__VU}`,
                description: 'Product for soak testing',
                quantityOnHand: Math.floor(Math.random() * 100),
                unitPrice: Math.random() * 100,
            }),
            { headers }
        );
        
        if (createRes.status === 201) {
            const product = JSON.parse(createRes.body);
            sleep(0.2);
            http.del(`${BASE_URL}/api/v1/products/${product.id}`, null, { headers });
        }
    }
    
    // 5% - Create purchase order
    if (Math.random() < 0.05) {
        // First create a product
        const sku = `SOAK-PO-${__VU}-${Date.now()}`;
        const productRes = http.post(
            `${BASE_URL}/api/v1/products`,
            JSON.stringify({
                sku: sku,
                name: `Soak PO Product`,
                description: 'Product for PO soak test',
                quantityOnHand: 10,
                unitPrice: 25.00,
            }),
            { headers }
        );
        
        if (productRes.status === 201) {
            const product = JSON.parse(productRes.body);
            
            const orderRes = http.post(
                `${BASE_URL}/api/v1/purchase-orders`,
                JSON.stringify({
                    supplierName: `Soak Supplier ${__VU}`,
                    lines: [{ productId: product.id, quantity: 5, unitPrice: 20.00 }],
                    received: true,
                }),
                { headers }
            );
            errorRate.add(!check(orderRes, { 'create order OK': (r) => r.status === 201 }));
            
            // Cleanup
            sleep(0.2);
            http.del(`${BASE_URL}/api/v1/products/${product.id}`, null, { headers });
        }
    }
    
    // 5% - Refresh token
    if (Math.random() < 0.05) {
        const loginRes = http.post(
            `${BASE_URL}/api/v1/auth/login`,
            JSON.stringify({
                username: 'soakadmin',
                password: 'soakTestPass123',
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        
        if (loginRes.status === 200) {
            const authData = JSON.parse(loginRes.body);
            const refreshRes = http.post(
                `${BASE_URL}/api/v1/auth/refresh`,
                JSON.stringify({ refreshToken: authData.refreshToken }),
                { headers: { 'Content-Type': 'application/json' } }
            );
            errorRate.add(!check(refreshRes, { 'refresh OK': (r) => r.status === 200 }));
        }
    }
    
    sleep(1 + Math.random());  // Variable sleep to simulate realistic patterns
}
