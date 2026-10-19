PR Review Triage Agent
An AI agent that reads a GitHub pull request and tells a reviewer, in plain terms, "is this safe to merge, and what should I look at first?" — with every issue ranked by severity and pointed straight at the exact line of code.

Built for the micro1 Agentic Workflows Hackathon.

🚀 Try it live (no setup needed)
The whole thing is deployed. You can test the agent right from your browser:

Open https://hackathon.rishavchoudhary.com
Log in with the demo account:
Username: micro1
Password: micro1@123
Paste any public GitHub pull request URL, for example: https://github.com/psf/requests/pull/6555
Click Review this PR and wait ~20–40 seconds.
You will see:
A verdict (approve / request changes / comment)
A list of findings, ranked from critical → high → medium → low, each with the exact code snippet
A side-by-side comparison with a simple baseline reviewer
The agent's steps (how it reached the answer)
The full diff, with flagged lines highlighted — click any finding to jump to it
In a hurry? Click "See an example" to explore a saved result instantly, without waiting. Tip: pick a PR with only a few changed files for the fastest review.

1. The Problem
Who has this problem? Anyone who reviews code — team leads, senior engineers, open-source maintainers. Every time someone opens a pull request (PR), a human has to read the changes and decide whether it is safe to merge.

Why is it hard today? Reading code changes line by line is slow and inconsistent:

Two reviewers looking at the same PR often catch different things.
The serious stuff — a security hole, a change that breaks other code, missing tests — gets buried under small style comments ("add a space here", "rename this variable").
If a reviewer is busy or tired, important problems slip through.
There is no reliable, repeatable way to answer the one question that matters most: "What are the real risks in this change, and where exactly are they?"

Why solving it is worth it. If a reviewer could instantly see the real risks first — each tied to a specific file and line — they could spend their limited attention where it actually matters and merge with more confidence. That saves time and stops bad code from shipping.

2. The Solution
We built an agent (not just one AI prompt) that reviews a pull request the way a careful senior engineer would, in clear steps:

Understand the change — reads the PR title, description, and the actual code diff, and writes a short summary of what the PR is trying to do.
Hunt for real risk — looks specifically for security problems, logic/correctness bugs, and changes that could break other code. It is told to ignore pure style nitpicks.
Check the tests — asks "are these changes actually covered by tests?"
Double-check itself — a separate "verifier" step re-reads every issue it found and throws away anything it cannot actually point to in the code. This removes made-up (hallucinated) issues.
Write the verdict — ranks everything from critical → high → medium → low, gives each issue the exact file:line, and makes a recommendation.
A human always makes the final merge decision. The agent advises; it never merges anything.

Why an agent instead of a single AI prompt?
A single prompt ("review this PR") gives you one pass with no self-checking. Our agent adds deliberate steps, and each one has a clear job:

Step	What it adds	Why it helps
Context Gatherer	A clean summary of the PR	Later steps reason about a tidy brief instead of raw, noisy diff text
Risk Analyzer	Focused security / correctness / breaking-change hunting	Puts attention on what actually matters, not style
Test Coverage	A check for missing tests	Untested changes are a common real-world risk
Verifier	Removes findings it can't prove	Fewer false alarms — the reviewer can trust the output
Synthesizer + guardrail	Ranked verdict; never "approve" if a critical/high issue exists	A safe, calibrated final answer
The Risk Analyzer and Test Coverage steps run in parallel to keep it fast.

To make the improvement measurable, we also built a baseline (a single-prompt reviewer). The agent and the baseline get the exact same input, so any difference in quality comes from the agent's design — not from extra data. The UI shows them side by side.

3. Architecture
Everything runs on AWS. The browser talks to a cloud API, which runs the agent on Amazon Bedrock (Claude Sonnet) and saves the results.

                 ┌─────────────────────────────────────────────────────────┐
                 │                     Your browser                         │
                 │        https://hackathon.rishavchoudhary.com             │
                 │            (React app: login + review UI)                │
                 └───────────────────────────┬─────────────────────────────┘
                                              │  HTTPS
                 ┌────────────────────────────▼─────────────────────────────┐
                 │  CloudFront (CDN)  ←—  S3 bucket (hosts the React files)  │
                 │  Route53 maps the subdomain • ACM provides the HTTPS cert │
                 └───────────────────────────┬─────────────────────────────┘
                                              │  API calls (/login, /jobs)
                 ┌────────────────────────────▼─────────────────────────────┐
                 │                 API Gateway (HTTP API)                    │
                 └───────────────────────────┬─────────────────────────────┘
                                              │
                 ┌────────────────────────────▼─────────────────────────────┐
                 │                 AWS Lambda (Python)                       │
                 │  • /login  → checks user in DynamoDB, returns a token     │
                 │  • /jobs   → starts a review, returns a job id            │
                 │  • runs the 5-step agent; self-invokes for long jobs      │
                 └───────┬───────────────────────────┬─────────────────┬─────┘
                         │                           │                 │
           ┌─────────────▼──────┐   ┌────────────────▼──────┐  ┌───────▼───────────┐
           │  Amazon Bedrock    │   │  GitHub public API    │  │  DynamoDB + S3    │
           │  Claude Sonnet 4.5 │   │  (fetches the PR diff) │  │  users, results,  │
           │  (the agent's LLM) │   │                        │  │  job status       │
           └────────────────────┘   └────────────────────────┘  └───────────────────┘
How a request flows:

You log in on the website. The Lambda checks your username/password in DynamoDB and returns a token.
You paste a PR URL and click review. The frontend calls /jobs, which starts the work and immediately returns a job id (so the request never times out).
The Lambda fetches the PR diff from GitHub, then runs the 5-step agent using Bedrock.
The result (verdict + findings + diff) is saved, and the browser polls until it is ready.
The page shows the ranked findings and the diff.
How it's built
Backend (backend/, Python)

The agent's 5 stages live in app/agent.py; the single-prompt baseline in app/baseline.py.
app/bedrock_client.py is the only place that talks to the model, so it could be moved to a different LLM by changing one file.
app/api.py is a FastAPI app running in Lambda (via Mangum). A multi-step review can take longer than API Gateway's 29-second limit, so the API starts the job and the Lambda invokes itself asynchronously to finish, writing status to DynamoDB while the browser polls.
Login (app/auth.py): passwords are stored as salted PBKDF2-SHA256 hashes (never plaintext); login returns a signed token that the review endpoints require.
Frontend (frontend/, React + Vite)

A first-time "how it works" guide, a login screen, live progress, a severity-sorted findings list (critical first), an inline code snippet on each finding, and a full diff viewer where clicking a finding jumps to the exact line.
Infrastructure (infra/, AWS CDK in TypeScript)

All infrastructure is code, and every resource name starts with hackathon- so the whole project can be torn down cleanly.
Backend stack: Lambda + API Gateway + DynamoDB + S3 + least-privilege IAM (Bedrock invoke only).
Frontend stack: S3 + CloudFront + Route53 subdomain + ACM certificate (in us-east-1, as CloudFront requires).
Model: Amazon Bedrock, Claude Sonnet 4.5 (eu.anthropic.claude-sonnet-4-5-20250929-v1:0), region eu-west-1.

Repository layout
.
├── backend/     Agent + baseline + API (Python, calls Bedrock Claude Sonnet)
├── frontend/    React UI (login, paste a PR URL, see the review + diff)
├── infra/       AWS CDK app — all resources prefixed "hackathon-"
└── eval/        Baseline-vs-agent evaluation harness + synthetic test cases
Run it locally (for developers)
You need: Python 3.12, Node 20+, and AWS credentials with Bedrock access.

# 1) Backend API
cd backend
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
export AWS_REGION=eu-west-1               # a region where you have Bedrock/Sonnet access
export AUTH_TOKEN_SECRET="any-random-string"
uvicorn app.api:app --port 8000

# 2) Frontend (in another terminal)
cd frontend
npm install
VITE_API_TARGET=http://localhost:8000 npm run dev
# open http://localhost:5173
Review a single PR straight from the command line:

cd backend && source .venv/bin/activate
python -m app.cli review https://github.com/<owner>/<repo>/pull/<number>
Deploy to the cloud (CDK)
Provide your own values (nothing sensitive is committed):

cd infra && npm install

export API_URL="https://<your-api-id>.execute-api.eu-west-1.amazonaws.com"
export FRONTEND_DOMAIN="app.yourdomain.com"
export ROOT_DOMAIN="yourdomain.com"          # an existing Route53 hosted zone
export SEED_USERNAME="micro1"
export SEED_PASSWORD_HASH="<pbkdf2 hash from backend/app/auth.hash_password>"

npx cdk deploy --all       # deploy everything
npx cdk destroy --all      # tear it all down afterwards
A note on security / credentials
No real credentials live in this repository. Domain, API URL, login seed, and the token secret are all supplied at deploy time through environment variables. The demo login above (micro1 / micro1@123) is only a gate for judges to try the hosted demo and controls nothing sensitive.
