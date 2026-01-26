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
  "code": "#include <iostream>\n#include <vector>\nusing namespace std;\n\nint main() {\n    int n, m;\n    cin >> n >> m;\n\n    vector<string> grid(n);\n    for (int i = 0; i < n; i++) {\n        cin >> grid[i];\n    }\n\n    for (int y = 0; y < n; y++) {\n        for (int x = 0; x < m; x++) {\n            for (int c = 'A'; c <= 'D'; c++) {\n                bool fail = false;\n                if (grid[y][x] == c) fail = true;\n                if (y > 0 && grid[y - 1][x] == c) fail = true;\n                if (x > 0 && grid[y][x - 1] == c) fail = true;\n                if (!fail) {\n                    grid[y][x] = c;\n                    break;\n                }\n            }\n            cout << grid[y][x];\n        }\n        cout << \"\\n\";\n    }\n}\n",
  "input":"3 4\nAAAA\nBBBB\nCCDD"
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
                f"{BASE}/{QID}/run",
                json=payload,
                timeout=100
            )
            if r.status_code != 200:
                raise Exception("Submit failed")

            output = r.json()
            print(output)
            latency = time.time() - start
            with lock:
                latencies.append(latency)

        except Exception as e:
            with lock:
               print("Exception:", type(e).__name__, e)
               if 'r' in locals():
                print("Status:", r.status_code)
                print("Raw response:")
                print(r.text)

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
