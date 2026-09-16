## System Design Assignment

For each problem statement, you are expected to work on the system from **design to implementation**.

### What you have to do

For every problem statement:

1. **Understand the requirements**

   * Define functional and non-functional requirements.
   * State your assumptions and constraints.

2. **HLD (High-Level Design)**

   * Design the overall system architecture.
   * Identify services/components.
   * Explain databases, caching, queues, APIs, etc., wherever required.
   * Include an architecture diagram.

3. **LLD (Low-Level Design)**

   * Design the internal structure of the system.
   * Include class diagrams, database schemas, API definitions, design patterns, sequence diagrams, etc., wherever relevant.

4. **Implementation**

   * Build a working implementation based on your design.
   * You may use **C, C++, Java, JavaScript/TypeScript, Python**, etc.
   * UI is optional. Focus primarily on the system and its architecture.

### Getting Started

1. **Fork** this repo: click **Fork** at the top right of [AI-Brewery/design-driven-problems](https://github.com/AI-Brewery/design-driven-problems). This makes a copy under your GitHub account.

2. **Clone your fork** (not the original repo):

   ```bash
   git clone https://github.com/<your-username>/design-driven-problems.git
   cd design-driven-problems
   ```

3. **Create a branch** for the problem you're working on:

   ```bash
   git checkout -b <your-name>/url-shortner
   ```

4. **Do your work** inside `<problem-folder>/<Your Name>/` (see [Submission](#submission)).

5. **Commit and push to your fork**:

   ```bash
   git add .
   git commit -m "url-shortner: <your name> submission"
   git push origin <your-name>/url-shortner
   ```

6. **Open a Pull Request**: go to your fork on GitHub, click **Compare & pull request**, and make sure the target is `AI-Brewery/design-driven-problems` → `main`.

To get new problem statements later, sync your fork with the original repo:

```bash
git remote add upstream https://github.com/AI-Brewery/design-driven-problems.git
git fetch upstream
git merge upstream/main
```

### Submission

Each problem statement has its own folder with a `readme.md` describing the problem. Put your complete work in a folder with **your name** **inside that problem's folder**, then create a **Pull Request** against the repository.

```text
url-shortner/
├── readme.md              ← problem statement
├── <Your Name>/
│   ├── README.md
│   ├── HLD/
│   ├── LLD/
│   ├── src/
│   └── ...
└── <Another Name>/
    └── ...
```

Only add or change files inside your own folder.

The PR should contain:

* HLD
* LLD
* Architecture/design diagrams
* Source code
* Setup instructions
* Design decisions and assumptions
* Any limitations or future improvements

### Timeline

Each problem statement will have a specific deadline, typically **1–2 weeks**.

The Pull Request must be submitted before the specified deadline.

### Review

The PR will be reviewed based on:

* Understanding of the problem
* Quality of HLD
* Quality of LLD
* Code quality
* Consistency between design and implementation
* Handling of edge cases and failures
* Reasoning behind architectural decisions

If the design and implementation are satisfactory, we will have a **Google Meet discussion** where you will explain your approach, design decisions, and implementation.

### Important

Do not simply generate the entire solution using AI and submit it.

You are expected to **actually understand and implement the system yourself**. During the review, you should be able to explain every major design and implementation decision you made.
