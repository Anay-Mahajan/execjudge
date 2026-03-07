<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
</head>

<body>

<h1>🧠 ExecJudge — Online Code Evaluation Backend</h1>

<p>
ExecJudge is a backend system inspired by online judges like <b>LeetCode</b> and <b>Codeforces</b>.
It compiles and executes user-submitted C++ programs in isolated OS processes using an
<b>asynchronous high-throughput execution pipeline</b>.
</p>

<div class="highlight">
Designed to safely run untrusted code while maintaining scalability under heavy concurrent load.
</div>

<hr>

<h2>🚀 Key Features</h2>

<ul>
<li>Asynchronous <b>producer–consumer job pipeline</b></li>
<li>Lock-free <b>MPMC queue</b> for high-throughput scheduling</li>
<li>Multithreaded compilation and execution workers</li>
<li><b>Persistent native worker</b> eliminating per-job process startup overhead</li>
<li>OS-level process isolation using <code>ProcessBuilder</code></li>
<li>Hard execution timeouts to prevent infinite loops</li>
<li>Batch database writes to reduce persistence overhead</li>
<li>Evaluation across multiple test cases per submission</li>
</ul>

<hr>

<h2>🏗 System Architecture</h2>

<pre>
Client
   │
   ▼
REST API (Spring Boot)
   │
   ▼
MPMC Job Queue
   │
   ▼
Worker Threads
   ├── Compile Workers
   └── Execution Workers
           │
           ▼
Persistent Native Runner (C++)
           │
           ▼
User Program Processes
</pre>

<p>
A persistent native runner handles repeated test execution and significantly reduces process startup overhead.
</p>

<hr>

<h2>🛠 Tech Stack</h2>

<ul>
<li><b>Backend:</b> Java 17, Spring Boot, Spring Data JPA</li>
<li><b>Concurrency:</b> Producer–Consumer scheduling, MPMC Queue, Thread Pools</li>
<li><b>Execution Layer:</b> C++ Native Worker, ProcessBuilder, clang++</li>
<li><b>Database:</b> H2 In-Memory Database</li>
<li><b>Infrastructure:</b> Linux</li>
</ul>

<hr>

<h2>📈 Performance Benchmarks</h2>

<p>Stress tested with <b>500 concurrent submissions</b>.</p>

<table>
<tr>
<th>Workload</th>
<th>Throughput</th>
<th>Average Latency</th>
<th>P95 Latency</th>
</tr>

<tr>
<td>Moderate algorithmic problems</td>
<td><b>~13 jobs/sec</b></td>
<td>~4s</td>
<td>&lt; 5s</td>
</tr>

<tr>
<td>CPU-heavy (Tower of Hanoi, n ≤ 16)</td>
<td><b>~11 jobs/sec</b></td>
<td>~4–5s</td>
<td>&lt; 6s</td>
</tr>
</table>

<hr>

<h2>🔒 Execution Isolation</h2>

<ul>
<li>Each submission runs inside a separate OS process</li>
<li>Timeout limits prevent runaway code</li>
<li>JVM remains protected from crashes or infinite loops</li>
</ul>

<hr>

<h2>📬 API</h2>

<h3>Submit Code</h3>

<pre>
POST /submit
</pre>

Response

<pre>
{
  "submissionId": 123
}
</pre>

<h3>Get Result</h3>

<pre>
GET /result/{submissionId}
</pre>

<hr>

<h2>▶️ Running the Project</h2>

<p>Requirements:</p>

<ul>
<li>Java 17+</li>
<li>Maven</li>
<li>clang++</li>
</ul>

Run server:

<pre>
mvn spring-boot:run
</pre>

Server will start at:

<pre>
http://localhost:8080
</pre>

<hr>

<h2>👨‍💻 Author</h2>

<p>
<b>Anay Mahajan</b><br>
<a href="https://github.com/Anay-Mahajan">GitHub</a>
</p>

</body>
</html>
