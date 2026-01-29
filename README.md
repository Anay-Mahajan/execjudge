<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>

<h1>🧠 ExecJudge — Asynchronous Code Execution Engine</h1>

<p>ExecJudge is a backend system inspired by platforms like LeetCode. It compiles and executes C++ submissions in isolated OS processes using an asynchronous job pipeline.</p>

<h2>🚀 Features</h2>
<ul>
  <li>Asynchronous Producer–Consumer submission pipeline</li>
  <li>Multithreaded compile and execution workers</li>
  <li>OS-level process isolation using <code>ProcessBuilder</code></li>
  <li>Hard timeouts with forced process termination</li>
  <li>Evaluation across multiple test cases</li>
</ul>

<h2>🏗️ Architecture</h2>
<pre>
Client → REST API → Submission Queue → Worker Threads
                                      ├─ Compile Workers
                                      └─ Execution Workers → OS Processes
</pre>

<h2>🛠️ Tech Stack</h2>
<ul>
  <li>Java 17</li>
  <li>Spring Boot</li>
  <li>Spring Data JPA</li>
  <li>H2 Database</li>
  <li>clang++ for C++ execution</li>
</ul>

<h2>📈 Stress Test Snapshot</h2>
<table>
<tr><th>Workload</th><th>Throughput</th><th>Avg Latency</th><th>P95 Latency</th></tr>
<tr><td>Moderate Problems</td><td>~2.7 jobs/sec</td><td>~17s</td><td>~19s</td></tr>
<tr><td>CPU-heavy (Hanoi n≤16)</td><td>~2.0 jobs/sec</td><td>~24s</td><td>~35s</td></tr>
</table>

<h2>▶️ Running</h2>
<pre>mvn spring-boot:run</pre>
<p>Server: <code>http://localhost:8080</code></p>

<h2>📬 API</h2>
<p><strong>Submit:</strong></p>
<pre>POST /submit</pre>
<pre>{
  "submissionId": 123
}</pre>

<p><strong>Result:</strong></p>
<pre>GET /result/{submissionId}</pre>

</body>
</html>
