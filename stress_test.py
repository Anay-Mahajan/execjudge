import requests
import threading
import time
import statistics
import random

BASE = "http://localhost:8080/api/process"
QID = 1

CLIENTS = 50
SUBMISSIONS_PER_CLIENT = 10

lock = threading.Lock()
latencies = []
failures = 0

payload= {
  "code": "#include <iostream>\nusing namespace std;\n\nint main() {\n    int n;\n    cin >> n;\n    int sum = 0;\n    for (int i = 1; i <= n; i++) {\n        sum += i;\n    }\n    for (int i = 1; i <= n - 1; i++) {\n        int x;\n        cin >> x;\n        sum -= x;\n    }\n    cout << sum << \"\\n\";\n}"
}
def extract_status(data):
    """
    Supports multiple response formats:
    - int
    - { "status": int }
    """
    if isinstance(data, int):
        return data
    if isinstance(data, dict) and "status" in data:
        return data["status"]
    raise ValueError(f"Unexpected response format: {data}")

def client():
    global failures
    for _ in range(SUBMISSIONS_PER_CLIENT):
        try:
            start = time.time()

            # Submit job
            r = requests.post(
                f"{BASE}/{QID}/submit",
                json=payload,
                timeout=3
            )
            if r.status_code != 200:
                raise Exception("Submit failed")

            sid = r.json()

            # Poll result
            while True:
                res = requests.get(f"{BASE}/{sid}/result", timeout=3)
                data = res.json()
                status = extract_status(data)

                # 0 = QUEUED, 1 = RUNNING
                if status not in (0, 1):
                    break
                delay = random.uniform(1,1.5)
                time.sleep(delay)  # avoid DB hammering

            latency = time.time() - start
            with lock:
                latencies.append(latency)

        except Exception as e:
            with lock:
                failures += 1

threads = []
start_time = time.time()

for _ in range(CLIENTS):
    t = threading.Thread(target=client)
    threads.append(t)
    t.start()

for t in threads:
    t.join()

total_time = time.time() - start_time
total_jobs = CLIENTS * SUBMISSIONS_PER_CLIENT

print("\n===== EXECJUDGE STRESS TEST RESULTS =====")
print("Total jobs:", total_jobs)
print("Failures:", failures)
print("Total time:", round(total_time, 2), "sec")
print("Throughput:", round(total_jobs / total_time, 2), "jobs/sec")

if latencies:
    print("Avg latency:", round(statistics.mean(latencies), 3), "sec")
    print("P95 latency:", round(statistics.quantiles(latencies, n=20)[18], 3), "sec")
