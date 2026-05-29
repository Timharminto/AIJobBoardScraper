# AI Job Board Scraper (Small SaaS)

This is an internal web application built for scraping career pages. It extracts job listings, stores them in a PostgreSQL database, and visualizes the data using interactive charts on a Vaadin dashboard.

## 🚀 Tech Stack
* **Backend:** Java (17/21), Spring Boot 3.2+
* **Frontend:** Vaadin Flow 24 (Server-Side Rendering)
* **Database:** PostgreSQL 15+
* **Scraping Layer 1:** Jsoup 1.17+
* **Scraping Layer 2:** Scrapling (Python via CLI Bridge)
* **Migrations:** Flyway

---

## ⚙️ Prerequisites
Before running this application, ensure you have the following installed on your machine:
1. **Java Development Kit (JDK):** Version 17 or 21.
2. **PostgreSQL:** Version 15 or higher.
3. **Python 3:** Required for the Layer 2 scraping fallback.
4. **Scrapling (Python Library):** Install it globally via terminal by running:
   ```bash
   pip install scrapling
