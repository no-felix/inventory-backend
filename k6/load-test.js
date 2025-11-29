import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const authDuration = new Trend('auth_duration');
const productDuration = new Trend('product_duration');
const orderDuration = new Trend('order_duration');
const metricsDuration = new Trend('metrics_duration');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test options - configure based on your needs
export const options = {
    scenarios: {
        // Smoke test - verify system works under minimal load
        smoke: {
            executor: 'constant-vus',
            vus: 1,
            duration: '30s',
            startTime: '0s',
            tags: { test_type: 'smoke' },
        },
        // Load test - normal expected load
        load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 10 },   // Ramp up to 10 users
                { duration: '3m', target: 10 },   // Stay at 10 users
                { duration: '1m', target: 20 },   // Ramp up to 20 users
                { duration: '3m', target: 20 },   // Stay at 20 users
                { duration: '1m', target: 0 },    // Ramp down
            ],
            startTime: '30s',
            tags: { test_type: 'load' },
        },
        // Stress test - beyond normal capacity
        stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 50 },   // Ramp up to 50 users
                { duration: '5m', target: 50 },   // Stay at 50 users
                { duration: '2m', target: 100 },  // Ramp up to 100 users
                { duration: '5m', target: 100 },  // Stay at 100 users
                { duration: '2m', target: 0 },    // Ramp down
            ],
            startTime: '10m',
            tags: { test_type: 'stress' },
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],  // 95% under 500ms, 99% under 1s
        http_req_failed: ['rate<0.01'],                   // Less than 1% errors
        errors: ['rate<0.05'],                            // Custom error rate under 5%
        auth_duration: ['p(95)<300'],
        product_duration: ['p(95)<400'],
        order_duration: ['p(95)<500'],
        metrics_duration: ['p(95)<600'],
    },
};

// Shared state
let adminToken = null;
let userToken = null;
let testProductId = null;

// Setup function - runs once before all VUs start
export function setup() {
    console.log(`Starting load test against ${BASE_URL}`);
    
    // Check if setup is required and create admin if needed
    const setupStatusRes = http.get(`${BASE_URL}/api/v1/auth/setup/status`);
    if (setupStatusRes.status === 200) {
        const setupStatus = JSON.parse(setupStatusRes.body);
        if (setupStatus.setupRequired) {
            console.log('Creating initial admin account...');
            const adminSetupRes = http.post(
                `${BASE_URL}/api/v1/auth/setup`,
                JSON.stringify({
                    username: 'k6admin',
                    email: 'k6admin@test.com',
                    password: 'k6TestPassword123',
                }),
                { headers: { 'Content-Type': 'application/json' } }
            );
            check(adminSetupRes, {
                'admin setup successful': (r) => r.status === 201,
            });
        }
    }
    
    // Login as admin to get token
    const loginRes = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            username: 'k6admin',
            password: 'k6TestPassword123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    
    if (loginRes.status === 200) {
        const authData = JSON.parse(loginRes.body);
        adminToken = authData.accessToken;
        console.log('Admin login successful');
    } else {
        console.error(`Admin login failed: ${loginRes.status} - ${loginRes.body}`);
    }
    
    // Register a test user
    const registerRes = http.post(
        `${BASE_URL}/api/v1/auth/register`,
        JSON.stringify({
            username: `k6user_${Date.now()}`,
            email: `k6user_${Date.now()}@test.com`,
            password: 'testPassword123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    
    let testUsername = null;
    if (registerRes.status === 201) {
        const userData = JSON.parse(registerRes.body);
        testUsername = userData.username;
        
        // Login as test user
        const userLoginRes = http.post(
            `${BASE_URL}/api/v1/auth/login`,
            JSON.stringify({
                username: testUsername,
                password: 'testPassword123',
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        
        if (userLoginRes.status === 200) {
            userToken = JSON.parse(userLoginRes.body).accessToken;
        }
    }
    
    return { adminToken, userToken };
}

// Main test function - runs for each VU
export default function(data) {
    const token = data.adminToken || data.userToken;
    const authHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
    };
    
    // Test Authentication Endpoints
    group('Authentication', () => {
        // Test login
        const start = Date.now();
        const loginRes = http.post(
            `${BASE_URL}/api/v1/auth/login`,
            JSON.stringify({
                username: 'k6admin',
                password: 'k6TestPassword123',
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );
        authDuration.add(Date.now() - start);
        
        const loginSuccess = check(loginRes, {
            'login status is 200': (r) => r.status === 200,
            'login returns access token': (r) => {
                const body = JSON.parse(r.body);
                return body.accessToken !== undefined;
            },
        });
        errorRate.add(!loginSuccess);
        
        if (loginRes.status === 200) {
            const authData = JSON.parse(loginRes.body);
            
            // Test token refresh
            const refreshRes = http.post(
                `${BASE_URL}/api/v1/auth/refresh`,
                JSON.stringify({ refreshToken: authData.refreshToken }),
                { headers: { 'Content-Type': 'application/json' } }
            );
            
            check(refreshRes, {
                'refresh status is 200': (r) => r.status === 200,
            });
        }
        
        // Test setup status (public endpoint)
        const setupStatusRes = http.get(`${BASE_URL}/api/v1/auth/setup/status`);
        check(setupStatusRes, {
            'setup status is 200': (r) => r.status === 200,
        });
    });
    
    sleep(0.5);
    
    // Test Product Endpoints
    group('Products', () => {
        // List products
        let start = Date.now();
        const listRes = http.get(`${BASE_URL}/api/v1/products`, {
            headers: authHeaders,
        });
        productDuration.add(Date.now() - start);
        
        const listSuccess = check(listRes, {
            'list products status is 200': (r) => r.status === 200,
            'list products returns array': (r) => Array.isArray(JSON.parse(r.body)),
        });
        errorRate.add(!listSuccess);
        
        // Create a product
        const sku = `K6-${__VU}-${__ITER}-${Date.now()}`;
        start = Date.now();
        const createRes = http.post(
            `${BASE_URL}/api/v1/products`,
            JSON.stringify({
                sku: sku,
                name: `K6 Test Product ${__VU}`,
                description: 'Product created by k6 load test',
                quantityOnHand: Math.floor(Math.random() * 100) + 10,
                unitPrice: Math.random() * 100 + 10,
            }),
            { headers: authHeaders }
        );
        productDuration.add(Date.now() - start);
        
        const createSuccess = check(createRes, {
            'create product status is 201': (r) => r.status === 201,
        });
        errorRate.add(!createSuccess);
        
        if (createRes.status === 201) {
            const product = JSON.parse(createRes.body);
            
            // Get product by ID
            start = Date.now();
            const getRes = http.get(`${BASE_URL}/api/v1/products/${product.id}`, {
                headers: authHeaders,
            });
            productDuration.add(Date.now() - start);
            
            check(getRes, {
                'get product status is 200': (r) => r.status === 200,
                'get product returns correct id': (r) => JSON.parse(r.body).id === product.id,
            });
            
            // Update product
            start = Date.now();
            const updateRes = http.put(
                `${BASE_URL}/api/v1/products/${product.id}`,
                JSON.stringify({
                    sku: product.sku,
                    name: `${product.name} - Updated`,
                    description: product.description,
                    quantityOnHand: product.quantityOnHand + 5,
                    unitPrice: product.unitPrice,
                }),
                { headers: authHeaders }
            );
            productDuration.add(Date.now() - start);
            
            check(updateRes, {
                'update product status is 200': (r) => r.status === 200,
            });
            
            // Delete product (cleanup)
            start = Date.now();
            const deleteRes = http.del(`${BASE_URL}/api/v1/products/${product.id}`, null, {
                headers: authHeaders,
            });
            productDuration.add(Date.now() - start);
            
            check(deleteRes, {
                'delete product status is 204': (r) => r.status === 204,
            });
        }
    });
    
    sleep(0.5);
    
    // Test Purchase Order Endpoints
    group('Purchase Orders', () => {
        // First, create a product for the order
        const sku = `K6-PO-${__VU}-${__ITER}-${Date.now()}`;
        const productRes = http.post(
            `${BASE_URL}/api/v1/products`,
            JSON.stringify({
                sku: sku,
                name: `K6 PO Test Product ${__VU}`,
                description: 'Product for PO test',
                quantityOnHand: 50,
                unitPrice: 25.00,
            }),
            { headers: authHeaders }
        );
        
        if (productRes.status === 201) {
            const product = JSON.parse(productRes.body);
            
            // List purchase orders
            let start = Date.now();
            const listRes = http.get(`${BASE_URL}/api/v1/purchase-orders`, {
                headers: authHeaders,
            });
            orderDuration.add(Date.now() - start);
            
            check(listRes, {
                'list orders status is 200': (r) => r.status === 200,
            });
            
            // Create a purchase order
            start = Date.now();
            const createOrderRes = http.post(
                `${BASE_URL}/api/v1/purchase-orders`,
                JSON.stringify({
                    supplierName: `K6 Test Supplier ${__VU}`,
                    lines: [
                        {
                            productId: product.id,
                            quantity: 10,
                            unitPrice: 20.00,
                        },
                    ],
                    received: false,
                }),
                { headers: authHeaders }
            );
            orderDuration.add(Date.now() - start);
            
            const orderCreateSuccess = check(createOrderRes, {
                'create order status is 201': (r) => r.status === 201,
            });
            errorRate.add(!orderCreateSuccess);
            
            if (createOrderRes.status === 201) {
                const order = JSON.parse(createOrderRes.body);
                
                // Get order by ID
                start = Date.now();
                const getOrderRes = http.get(`${BASE_URL}/api/v1/purchase-orders/${order.id}`, {
                    headers: authHeaders,
                });
                orderDuration.add(Date.now() - start);
                
                check(getOrderRes, {
                    'get order status is 200': (r) => r.status === 200,
                });
                
                // Receive the order
                start = Date.now();
                const receiveRes = http.post(
                    `${BASE_URL}/api/v1/purchase-orders/${order.id}/receive`,
                    null,
                    { headers: authHeaders }
                );
                orderDuration.add(Date.now() - start);
                
                check(receiveRes, {
                    'receive order status is 200': (r) => r.status === 200,
                    'order status is RECEIVED': (r) => JSON.parse(r.body).status === 'RECEIVED',
                });
            }
            
            // Cleanup: delete the test product
            http.del(`${BASE_URL}/api/v1/products/${product.id}`, null, {
                headers: authHeaders,
            });
        }
    });
    
    sleep(0.5);
    
    // Test Stock Movement Endpoints
    group('Stock Movements', () => {
        let start = Date.now();
        const listRes = http.get(`${BASE_URL}/api/v1/stock-movements`, {
            headers: authHeaders,
        });
        metricsDuration.add(Date.now() - start);
        
        check(listRes, {
            'list stock movements status is 200': (r) => r.status === 200,
        });
        
        // Filter by reason
        start = Date.now();
        const filteredRes = http.get(`${BASE_URL}/api/v1/stock-movements?reason=PO_RECEIPT`, {
            headers: authHeaders,
        });
        metricsDuration.add(Date.now() - start);
        
        check(filteredRes, {
            'filtered stock movements status is 200': (r) => r.status === 200,
        });
    });
    
    sleep(0.5);
    
    // Test Metrics Endpoints
    group('Metrics', () => {
        // Inventory summary
        let start = Date.now();
        const summaryRes = http.get(`${BASE_URL}/api/v1/metrics/inventory-summary`, {
            headers: authHeaders,
        });
        metricsDuration.add(Date.now() - start);
        
        const summarySuccess = check(summaryRes, {
            'inventory summary status is 200': (r) => r.status === 200,
            'summary has productCount': (r) => JSON.parse(r.body).productCount !== undefined,
        });
        errorRate.add(!summarySuccess);
        
        // Stock levels
        start = Date.now();
        const stockLevelsRes = http.get(`${BASE_URL}/api/v1/metrics/stock-levels`, {
            headers: authHeaders,
        });
        metricsDuration.add(Date.now() - start);
        
        check(stockLevelsRes, {
            'stock levels status is 200': (r) => r.status === 200,
        });
        
        // Low stock alerts
        start = Date.now();
        const lowStockRes = http.get(`${BASE_URL}/api/v1/metrics/low-stock-alerts`, {
            headers: authHeaders,
        });
        metricsDuration.add(Date.now() - start);
        
        check(lowStockRes, {
            'low stock alerts status is 200': (r) => r.status === 200,
        });
        
        // Slow moving items
        start = Date.now();
        const slowMovingRes = http.get(`${BASE_URL}/api/v1/metrics/slow-moving-items?days=30`, {
            headers: authHeaders,
        });
        metricsDuration.add(Date.now() - start);
        
        check(slowMovingRes, {
            'slow moving items status is 200': (r) => r.status === 200,
        });
        
        // Valuation by price range
        start = Date.now();
        const valuationRes = http.get(`${BASE_URL}/api/v1/metrics/valuation-by-price-range`, {
            headers: authHeaders,
        });
        metricsDuration.add(Date.now() - start);
        
        check(valuationRes, {
            'valuation status is 200': (r) => r.status === 200,
        });
        
        // Receipts time series
        const today = new Date();
        const fromDate = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);
        const from = fromDate.toISOString().split('T')[0];
        const to = today.toISOString().split('T')[0];
        
        start = Date.now();
        const receiptsRes = http.get(
            `${BASE_URL}/api/v1/metrics/receipts?from=${from}&to=${to}`,
            { headers: authHeaders }
        );
        metricsDuration.add(Date.now() - start);
        
        check(receiptsRes, {
            'receipts time series status is 200': (r) => r.status === 200,
        });
    });
    
    sleep(1);
}

// Teardown function - runs once after all VUs finish
export function teardown(data) {
    console.log('Load test completed');
}
