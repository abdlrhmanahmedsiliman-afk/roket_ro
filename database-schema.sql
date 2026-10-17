-- =====================================================================
-- roket_ro Sports Analytics Platform - Complete Database Schema DDL
-- Compatible with SQLite (Room) & PostgreSQL (Backend Microservices)
-- =====================================================================

-- 1. Sports & Categories
CREATE TABLE IF NOT EXISTS sports (
    id VARCHAR(32) PRIMARY KEY,
    name_ar VARCHAR(64) NOT NULL,
    name_en VARCHAR(64) NOT NULL,
    icon_name VARCHAR(64) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    display_order INT DEFAULT 0
);

-- 2. Leagues & Competitions (50+ Global Competitions across 4 Tiers)
CREATE TABLE IF NOT EXISTS leagues (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    country VARCHAR(64) NOT NULL,
    sport_id VARCHAR(32) NOT NULL,
    tier VARCHAR(32) NOT NULL CHECK(tier IN ('TIER_1_ELITE', 'TIER_2_TOP', 'TIER_3_MAJOR', 'TIER_4_ALL_SPORTS')),
    season VARCHAR(32) DEFAULT '2024/2025',
    logo_url TEXT,
    active_teams_count INT DEFAULT 20,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sport_id) REFERENCES sports(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_leagues_tier ON leagues(tier);
CREATE INDEX IF NOT EXISTS idx_leagues_sport ON leagues(sport_id);

-- 3. Teams Table
CREATE TABLE IF NOT EXISTS teams (
    id VARCHAR(64) PRIMARY KEY,
    name_ar VARCHAR(128) NOT NULL,
    name_en VARCHAR(128) NOT NULL,
    short_code VARCHAR(10),
    country VARCHAR(64) NOT NULL,
    city VARCHAR(64),
    stadium_name VARCHAR(128),
    logo_url TEXT,
    founded_year INT,
    coach_name VARCHAR(128),
    default_formation VARCHAR(16) DEFAULT '4-3-3',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Matches & Fixtures Table
CREATE TABLE IF NOT EXISTS matches (
    id VARCHAR(64) PRIMARY KEY,
    sport_id VARCHAR(32) NOT NULL,
    league_id VARCHAR(64) NOT NULL,
    home_team_id VARCHAR(64) NOT NULL,
    away_team_id VARCHAR(64) NOT NULL,
    home_score INT DEFAULT 0,
    away_score INT DEFAULT 0,
    match_status VARCHAR(16) NOT NULL CHECK(match_status IN ('NOT_STARTED', 'LIVE', 'HALF_TIME', 'FINISHED', 'EXTRA_TIME', 'PENALTIES')),
    current_minute INT DEFAULT 0,
    kickoff_timestamp BIGINT NOT NULL,
    venue VARCHAR(128),
    referee VARCHAR(128),
    home_win_probability DOUBLE PRECISION DEFAULT 45.0,
    draw_probability DOUBLE PRECISION DEFAULT 25.0,
    away_win_probability DOUBLE PRECISION DEFAULT 30.0,
    ai_preview TEXT,
    ai_live_tactical TEXT,
    ai_post_report TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (league_id) REFERENCES leagues(id),
    FOREIGN KEY (home_team_id) REFERENCES teams(id),
    FOREIGN KEY (away_team_id) REFERENCES teams(id)
);

CREATE INDEX IF NOT EXISTS idx_matches_status ON matches(match_status);
CREATE INDEX IF NOT EXISTS idx_matches_kickoff ON matches(kickoff_timestamp);
CREATE INDEX IF NOT EXISTS idx_matches_league ON matches(league_id);

-- 5. Match Statistics Table (Opta & Stats Perform Telemetry)
CREATE TABLE IF NOT EXISTS match_stats (
    match_id VARCHAR(64) PRIMARY KEY,
    possession_home INT DEFAULT 50,
    possession_away INT DEFAULT 50,
    xg_home REAL DEFAULT 0.0,
    xg_away REAL DEFAULT 0.0,
    xa_home REAL DEFAULT 0.0,
    xa_away REAL DEFAULT 0.0,
    ppda_home REAL DEFAULT 10.0,
    ppda_away REAL DEFAULT 10.0,
    field_tilt_home INT DEFAULT 50,
    field_tilt_away INT DEFAULT 50,
    shots_home INT DEFAULT 0,
    shots_away INT DEFAULT 0,
    shots_on_target_home INT DEFAULT 0,
    shots_on_target_away INT DEFAULT 0,
    big_chances_home INT DEFAULT 0,
    big_chances_away INT DEFAULT 0,
    pass_accuracy_home INT DEFAULT 80,
    pass_accuracy_away INT DEFAULT 80,
    corners_home INT DEFAULT 0,
    corners_away INT DEFAULT 0,
    fouls_home INT DEFAULT 0,
    fouls_away INT DEFAULT 0,
    yellow_cards_home INT DEFAULT 0,
    yellow_cards_away INT DEFAULT 0,
    red_cards_home INT DEFAULT 0,
    red_cards_away INT DEFAULT 0,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

-- 6. Match Timeline Events
CREATE TABLE IF NOT EXISTS match_events (
    id VARCHAR(64) PRIMARY KEY,
    match_id VARCHAR(64) NOT NULL,
    minute INT NOT NULL,
    event_type VARCHAR(32) NOT NULL CHECK(event_type IN ('GOAL', 'YELLOW_CARD', 'RED_CARD', 'SUBSTITUTION', 'VAR', 'CORNER')),
    team_is_home BOOLEAN NOT NULL,
    player_name VARCHAR(128) NOT NULL,
    detail TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_events_match ON match_events(match_id, minute);

-- 7. Player Condition Index (PCI) Telemetry Table
CREATE TABLE IF NOT EXISTS player_condition_index (
    id VARCHAR(64) PRIMARY KEY,
    player_id VARCHAR(64) NOT NULL,
    match_id VARCHAR(64) NOT NULL,
    team_id VARCHAR(64) NOT NULL,
    player_name VARCHAR(128) NOT NULL,
    player_number INT NOT NULL,
    position VARCHAR(32) NOT NULL,
    overall_pci_score INT NOT NULL CHECK(overall_pci_score BETWEEN 0 AND 100),
    pci_status VARCHAR(32) NOT NULL CHECK(pci_status IN ('PEAK', 'OPTIMAL', 'MODERATE', 'HIGH_RISK')),
    -- The 7 Factors (0-100 values)
    physical_fatigue REAL NOT NULL,
    mental_psychological REAL NOT NULL,
    recent_form REAL NOT NULL,
    injury_risk REAL NOT NULL,
    travel_schedule_load REAL NOT NULL,
    tactical_fit REAL NOT NULL,
    weather_pitch_adaptation REAL NOT NULL,
    ai_condition_summary TEXT,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pci_match_player ON player_condition_index(match_id, player_id);

-- 8. PCI Minute-by-Minute Timeline Points
CREATE TABLE IF NOT EXISTS pci_timeline_points (
    id VARCHAR(64) PRIMARY KEY,
    pci_id VARCHAR(64) NOT NULL,
    minute INT NOT NULL,
    score INT NOT NULL,
    note VARCHAR(255),
    FOREIGN KEY (pci_id) REFERENCES player_condition_index(id) ON DELETE CASCADE
);

-- 9. Official & Legal Broadcasters Guide Table
CREATE TABLE IF NOT EXISTS match_broadcasters (
    id VARCHAR(64) PRIMARY KEY,
    match_id VARCHAR(64) NOT NULL,
    channel_name VARCHAR(128) NOT NULL,
    channel_number_or_package VARCHAR(128),
    region VARCHAR(32) NOT NULL CHECK(region IN ('ALL', 'MENA', 'UK', 'USA', 'SPAIN', 'GERMANY', 'FRANCE', 'INTERNATIONAL')),
    stream_format VARCHAR(32) NOT NULL CHECK(stream_format IN ('UHD_4K', 'FULL_HD', 'OTT_STREAM', 'FREE_TO_AIR')),
    commentators TEXT, -- Comma-separated
    audio_language VARCHAR(64) DEFAULT 'العربية',
    official_platform_url TEXT NOT NULL,
    app_package_name VARCHAR(128),
    is_official_license_verified BOOLEAN DEFAULT TRUE,
    FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_broadcasters_match ON match_broadcasters(match_id, region);

-- 10. Standings & League Tables
CREATE TABLE IF NOT EXISTS league_standings (
    league_id VARCHAR(64) NOT NULL,
    team_name VARCHAR(128) NOT NULL,
    rank INT NOT NULL,
    played INT DEFAULT 0,
    won INT DEFAULT 0,
    drawn INT DEFAULT 0,
    lost INT DEFAULT 0,
    goals_for INT DEFAULT 0,
    goals_against INT DEFAULT 0,
    goal_diff INT DEFAULT 0,
    points INT DEFAULT 0,
    recent_form VARCHAR(32) DEFAULT 'W,D,W,L,W',
    PRIMARY KEY (league_id, rank),
    FOREIGN KEY (league_id) REFERENCES leagues(id) ON DELETE CASCADE
);

-- 11. Top Scorers Table
CREATE TABLE IF NOT EXISTS top_scorers (
    id VARCHAR(64) PRIMARY KEY,
    league_id VARCHAR(64) NOT NULL,
    rank INT NOT NULL,
    player_name VARCHAR(128) NOT NULL,
    team_name VARCHAR(128) NOT NULL,
    goals INT DEFAULT 0,
    assists INT DEFAULT 0,
    FOREIGN KEY (league_id) REFERENCES leagues(id) ON DELETE CASCADE
);

-- 12. User Device Local Preferences & Favorites
CREATE TABLE IF NOT EXISTS user_favorites (
    user_device_id VARCHAR(128) NOT NULL,
    entity_type VARCHAR(16) NOT NULL CHECK(entity_type IN ('TEAM', 'LEAGUE', 'MATCH', 'PLAYER')),
    entity_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_device_id, entity_type, entity_id)
);
