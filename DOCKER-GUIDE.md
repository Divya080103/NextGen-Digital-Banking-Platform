# Docker Onboarding Guide for Team Members

Welcome to the team! If you've never used Docker before, don't worry. In the NextGen Digital Banking Platform project, **Docker has one job and one job only: running a local PostgreSQL database container on your machine**. You don't need to learn container orchestration, build Dockerfiles, or manage complex networking. Docker simply ensures all 5 team members develop against an identical, clean PostgreSQL 16 database without anyone needing to manually install or configure PostgreSQL server locally.

---

## 1. Installation Links

Download and install **Docker Desktop** for your operating system:
* **Windows**: [Download Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/) (Requires WSL2 enabled).
* **Mac**: [Download Docker Desktop for Mac](https://docs.docker.com/desktop/install/mac-install/) (Choose Apple Silicon or Intel chip installer).

---

## 2. How to Confirm Docker Desktop is Running

Before running any terminal commands, verify Docker Desktop is active:
1. Look at your system tray (bottom-right on Windows, top-right menu bar on Mac). You should see the **Docker whale icon**.
2. Make sure the icon is **steady** (not animated or initializing).
3. Open a terminal (PowerShell, Command Prompt, or Terminal) and run:
   ```bash
   docker info
   ```
   If it outputs server information (Client version, OS/Arch, Storage Driver, etc.), Docker is running and ready!

---

## 3. The Only 4 Commands You Need

You only need to know these four commands for daily development:

| Command | Action | When to use |
|---|---|---|
| `docker compose up -d` | Starts PostgreSQL in the background | Start of your workday |
| `docker ps` | Lists running containers and health status | Verify database is healthy (`(healthy)`) |
| `docker compose logs -f postgres` | Streams live PostgreSQL database logs | Debugging connection or migration issues |
| `docker compose down` | Stops and removes the PostgreSQL container | End of your workday |

---

## 4. Troubleshooting Common Errors

### Error: `unable to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine...`

If you run `docker compose up -d` and see this error:
```text
failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine; check if the path is correct and if the daemon is running
```

**What it means**: This is **not** a code or configuration error. It simply means the **Docker Desktop GUI app is closed or still initializing**.

**How to fix it**:
1. Open **Docker Desktop** from your Windows Start Menu / Mac Applications folder.
2. Wait 15–30 seconds until the Docker whale tray icon stays steady and green ("Engine running").
3. Re-run `docker info` in your terminal to confirm connectivity.
4. Retry `docker compose up -d`.

---

## 5. "You're Ready" Checklist

Before starting your backend Spring Boot module, verify this 4-step checklist:

- [ ] **Docker Desktop is open** (whale icon is steady in system tray).
- [ ] Running `docker info` returns server details without API connection errors.
- [ ] Executing `docker compose up -d` outputs `Container nextgen-postgres Started`.
- [ ] Running `docker ps` shows `nextgen-postgres` with status `Up (healthy)`.

Once all 4 boxes are checked, run `mvn spring-boot:run` in `backend/` and Flyway will automatically apply all database migrations to your local container!
