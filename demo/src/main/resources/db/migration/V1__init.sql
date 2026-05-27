CREATE TABLE scrape_targets (
    id BIGSERIAL PRIMARY KEY,
    url TEXT NOT NULL UNIQUE,
    company VARCHAR(255),
    last_scraped_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE job_listings (
    id BIGSERIAL PRIMARY KEY,
    target_id BIGINT REFERENCES scrape_targets(id),
    title VARCHAR(500) NOT NULL,
    department VARCHAR(255),
    location TEXT,
    url TEXT,
    employment_type VARCHAR(100),
    raw_text TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    scraped_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE scrape_history (
    id BIGSERIAL PRIMARY KEY,
    scraped_at TIMESTAMP,
    company_name VARCHAR(255),
    target_url VARCHAR(255),
    jobs_found INTEGER NOT NULL
);