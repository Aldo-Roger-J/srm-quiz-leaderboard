# SRM Quiz Leaderboard System

## 📌 Overview
This project implements a backend integration system that:
- Polls quiz event data from an API
- Deduplicates repeated events
- Aggregates participant scores
- Generates a leaderboard
- Submits results back to the server

## ⚙️ Tech Stack
- Java 17
- java.net.http.HttpClient
- Gson
- java.time

## 🔁 Workflow
1. Poll API 10 times (poll=0 to 9)
2. Maintain 5-second delay between requests
3. Deduplicate using (roundId + participant)
4. Aggregate scores
5. Sort leaderboard (descending)
6. Compute total score
7. Submit results once

## ▶️ How to Run

### Compile
javac -cp gson-2.10.1.jar SRMLeaderboard.java

### Run
java -cp .;gson-2.10.1.jar SRMLeaderboard


## ⚠️ Notes
- API may return 503 due to load
- Retry logic is implemented
- Program skips invalid responses safely

## ✅ Output
- Final leaderboard printed
- API response verification (`isCorrect: true`)
