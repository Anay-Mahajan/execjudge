<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
</head>
<body>

<h1>🧠 ExecJudge — Asynchronous Code Evaluation Backend</h1>

<p>
ExecJudge is a backend system inspired by platforms like LeetCode and Codeforces. 
It compiles and executes C++ submissions in isolated OS processes using an asynchronous, high-throughput job pipeline designed for safe and scalable code evaluation.
</p>

<h2>🚀 Features</h2>
<ul>
  <li>Asynchronous <strong>producer–consumer</strong> submission pipeline</li>
  <li>Lock-free <strong>MPMC queue</strong> for high-throughput job scheduling</li>
  <li>Multithreaded compilation and execution workers</li>
  <li><strong>Persistent native worker</strong> to eliminate per-job process startup overhead</li>
  <li>OS-level process isolation using <code>ProcessBuilder</code></li>
  <li>Hard execution timeouts with forced termination</li>
  <li>Evaluation across multiple test cases per submission</li>
  <li>Batch database writes to reduce persistence overhead</li>
</ul>

<h2>🏗️ Architecture</h2>
<pre>
Client → REST API → MPMC Job Queue → Worker Threads
                                      ├─ Compile Workers
                                      └─ Execution Workers → Persistent Native Runner → User Processes
</pre>

<p>
A long-lived native C++ runner handles repeated test execution, reducing process creation overhead and improving overall throughput.
</p>

<h2>🛠️ Tech Stack</h2>
<ul>
  <li>Java 17, Spring Boot</li>
  <li>Spring Data JPA</li>
  <li>H2 In-Memory Database</li>
  <li>Lock-Free MPMC Queue</li>
  <li>C++ Native Worker + <code>clang++</code></li>
</ul>

<h2>📈 Performance Snapshot</h2>
<p>Stress tested with <strong>500 concurrent submissions</strong> on a single machine.</p>

<table>
<tr><th>Workload</th><th>Throughput</th><th>Avg Latency</th><th>P95 Latency</th></tr>
<tr><td>Moderate algorithmic problems</td><td><strong>~13 jobs/sec</strong></td><td>~3s</td><td>&lt; 5s</td></tr>
<tr><td>CPU-heavy (Tower of Hanoi, n ≤ 16)</td><td><strong>~11 jobs/sec</strong></td><td>~4–5s</td><td>&lt; 6s</td></tr>
</table>

<p>
Latency includes queueing delay, compilation time, and execution across multiple test cases.
</p>

<h2>🔒 Isolation Model</h2>
<ul>
  <li>Each user submission runs in a separate OS process</li>
  <li>JVM protected from crashes and infinite loops</li>
  <li>Timeouts enforced externally</li>
</ul>

<h2>▶️ Running</h2>
<pre>mvn spring-boot:run</pre>
<p>Server: <code>http://localhost:8080</code></p>

<h2>📬 API</h2>
<p><strong>Submit Code</strong></p>
<pre>POST /submit</pre>
<pre>{
  "submissionId": 123
}</pre>

<p><strong>Get Result</strong></p>
<pre>GET /result/{submissionId}</pre>

</body>
</html>
