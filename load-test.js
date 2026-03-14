import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate, Counter } from 'k6/metrics';

// ─── Custom Metrics ──────────────────────────────────────────────────────────
const productListDuration = new Trend('product_list_duration');
const loginFailRate       = new Rate('login_fail_rate');
const totalOrders         = new Counter('total_orders_created');

// ─── Test Configuration ───────────────────────────────────────────────────────
// This defines the "shape" of the test – how users ramp up and down over time.
export const options = {
  stages: [
    { duration: '30s', target: 10  }, // 🔼 Warm-up: ramp from 0 → 10 users over 30s
    { duration: '1m',  target: 20  }, // ➡️  Steady state: hold at 20 users for 1 minute
    { duration: '30s', target: 100 }, // 🚀 SPIKE: jump to 100 concurrent users (stress test!)
    { duration: '30s', target: 0   }, // 🔽 Cool-down: ramp back to 0 users
  ],
  thresholds: {
    // ✅ Pass/Fail criteria - the CI pipeline or test suite will FAIL if these are breached
    http_req_duration:      ['p(95)<2000'], // 95% of requests must complete under 2 seconds
    http_req_failed:        ['rate<0.10'],  // Error rate must stay below 10%
    product_list_duration:  ['p(90)<1500'], // Product listing must be fast
  },
};

// When running via Docker use the service name; for local k6 installs use localhost
const BASE_URL = __ENV.BASE_URL || 'http://orderly-app:8080/api';

// ─── Setup (runs ONCE before all VUs start) ───────────────────────────────────
// This registers a shared test user and returns the token for all virtual users.
export function setup() {
  const username = `k6user_${Math.random().toString(36).slice(2, 8)}`;
  const email    = `${username}@loadtest.com`;
  const password = 'Pass@12345';

  // 1️⃣ Register a new user
  const regRes = http.post(`${BASE_URL}/auth/register`,
    JSON.stringify({ username, email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  check(regRes, { 'register: status 201': (r) => r.status === 201 });

  // 2️⃣ Login and extract the JWT token
  const loginRes = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ usernameOrEmail: username, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const success = check(loginRes, { 'login: status 200': (r) => r.status === 200 });
  loginFailRate.add(!success);

  let token = '';
  try { token = loginRes.json('token'); } catch (_) {}

  // 3️⃣ Pre-seed a product for all VUs to query
  if (token) {
    http.post(`${BASE_URL}/products`,
      JSON.stringify({ sku: 'K6-TEST-SKU', name: 'Load Test Widget', price: 9.99, stockQuantity: 9999 }),
      { headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` } }
    );
  }

  return { token };
}

// ─── Default Function (runs repeatedly for every VU) ─────────────────────────
export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${data.token}`,
  };

  // ── Scenario A: List Products (most common operation) ──
  const listRes = http.get(`${BASE_URL}/products?page=0&size=10`, { headers });
  check(listRes, {
    'list products: status 200': (r) => r.status === 200,
    'list products: has content': (r) => {
      try { return JSON.parse(r.body).content !== undefined; } catch (_) { return false; }
    },
  });
  productListDuration.add(listRes.timings.duration); // track custom metric
  sleep(1);

  // ── Scenario B: Search by name (filtered query) ──
  const searchRes = http.get(`${BASE_URL}/products?name=Widget&page=0&size=10`, { headers });
  check(searchRes, { 'search products: status 200': (r) => r.status === 200 });
  sleep(0.5);

  // ── Scenario C: Create a product (write operation) ──
  const sku = `SKU-${Date.now()}-${__VU}`;
  const createRes = http.post(`${BASE_URL}/products`,
    JSON.stringify({ sku, name: `VU-${__VU} Product`, description: 'k6 generated', price: 19.99, stockQuantity: 50 }),
    { headers }
  );
  check(createRes, { 'create product: status 201': (r) => r.status === 201 });
  sleep(1);

  // ── Scenario D: Get a specific product by ID ──
  const productId = Math.floor(Math.random() * 10) + 1;
  const getRes = http.get(`${BASE_URL}/products/${productId}`, { headers });
  check(getRes, { 'get product: not 500': (r) => r.status !== 500 });
  sleep(0.5);

  // ── Scenario E: List Orders ──
  const ordersRes = http.get(`${BASE_URL}/orders?page=0&size=10`, { headers });
  check(ordersRes, { 'list orders: not 500': (r) => r.status !== 500 });
  totalOrders.add(ordersRes.status === 200 ? 1 : 0);
  sleep(1);
}
