import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics for analyzing Kubernetes load balancing and performance
export const errorRate = new Rate('error_rate');
export const requestLatency = new Trend('request_latency_ms');
export const distinctPods = new Counter('distinct_pod_observations');

// Workload configuration: 5 virtual users for 30 seconds by default, overridable via CLI
export const options = {
    vus: __ENV.VUS ? parseInt(__ENV.VUS) : 5,
    duration: __ENV.DURATION || '30s',
    thresholds: {
        'http_req_duration': ['p(95)<500'], // 95% of requests should be below 500ms
        'error_rate': ['rate<0.01'],        // Error rate should be below 1%
    },
};

const BASE_URL = __ENV.TARGET_URL || 'http://localhost:8081/api/instance';

// In-memory set to count distinct Kubernetes pod names observed in this VU
const seenPods = {};

export default function () {
    const params = {
        headers: {
            'Accept': 'application/json',
            'X-Correlation-Id': `k6-run-${__VU}-${__ITER}-${Date.now()}`
        },
        timeout: '5s'
    };

    const res = http.get(BASE_URL, params);

    // Verify response status and structure
    const success = check(res, {
        'status is 200': (r) => r.status === 200,
        'has service name': (r) => {
            try {
                return JSON.parse(r.body).service !== undefined;
            } catch (e) {
                return false;
            }
        },
        'has hostname': (r) => {
            try {
                return JSON.parse(r.body).hostname !== undefined;
            } catch (e) {
                return false;
            }
        }
    });

    errorRate.add(!success);
    requestLatency.add(res.timings.duration);

    if (res.status === 200) {
        try {
            const body = JSON.parse(res.body);
            const podName = body.hostname || 'unknown';
            if (!seenPods[podName]) {
                seenPods[podName] = true;
                distinctPods.add(1);
                console.log(`[k6 VU ${__VU}] Observed new pod replica: ${podName} (Service: ${body.service})`);
            }
        } catch (e) {
            // ignore json parse error
        }
    }

    // Pacing between requests (50ms - 200ms)
    sleep(0.1);
}

export function handleSummary(data) {
    const p95 = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['p(95)'].toFixed(2) : 'N/A';
    const avg = data.metrics.http_req_duration ? data.metrics.http_req_duration.values['avg'].toFixed(2) : 'N/A';
    const rps = data.metrics.http_reqs ? data.metrics.http_reqs.values['rate'].toFixed(2) : 'N/A';
    const errors = data.metrics.error_rate ? (data.metrics.error_rate.values['rate'] * 100).toFixed(2) : '0';

    console.log('\n======================================================');
    console.log('             LOAD TEST EXPERIMENT SUMMARY             ');
    console.log('======================================================');
    console.log(`  Requests / Second (RPS):  ${rps}`);
    console.log(`  Average Latency:          ${avg} ms`);
    console.log(`  p95 Latency:              ${p95} ms`);
    console.log(`  Error Percentage:         ${errors} %`);
    console.log('======================================================\n');

    return {
        'stdout': '' // let default k6 terminal summary print
    };
}
