# 🗄️ مخطط قاعدة بيانات منصة roket_ro (Database Architecture)

منصة **roket_ro** تعتمد بنية بيانات هجينة تجمع بين:
1. **الذاكرة الفائقة (Memory LRU Cache):** للوصول الفوري في أقل من 1ms بدون I/O.
2. **التخزين المحلي الآمن (Offline-First Room SQLite):** يضمن عمل التطبيق بنسبة 100% بدون إنترنت.
3. **قاعدة البيانات السحابية (PostgreSQL + Redis):** لمعالجة أحداث المباريات اللحظية وتدفق بيانات الـ WebSocket والـ APIs العالمية.

---

## 📊 هيكل الجداول والكيانات (Entities & Tables)

### 1. جدول الدوريات والبطولات (`leagues`)
يضم أكثر من 50 دوري وبطولة عالمية مقسمة لـ 4 مستويات:
- `id` (PK): معرّف الدوري (مثل `ucl`, `pl`, `laliga`, `spl`, `nba`).
- `name`: اسم الدوري الرسمي.
- `country`: الدولة أو القارة التابع لها.
- `sport_id` (FK): الرياضة (كرة قدم، سلة، تنس، F1، إلخ).
- `tier`: تصنيف البطولة (`TIER_1_ELITE`, `TIER_2_TOP`, `TIER_3_MAJOR`, `TIER_4_ALL_SPORTS`).
- `season`: الموسم الحالي (مثل 2024/2025).
- `active_teams_count`: عدد الفرق المنافسة.

### 2. جدول المباريات والنتائج (`matches`)
- `id` (PK): معرّف المباراة.
- `sport_id` & `league_id` (FKs): ارتباطات الدوري والرياضة.
- `home_team_id` & `away_team_id`: الفريق المضيف والضيف.
- `home_score` & `away_score`: الأهداف/النقاط الحالية.
- `match_status`: حالة المباراة (`NOT_STARTED`, `LIVE`, `HALF_TIME`, `FINISHED`, `EXTRA_TIME`, `PENALTIES`).
- `current_minute`: الدقيقة الحالية (تُحدّث كل ثانية في المباريات المباشرة).
- `home_win_probability`, `draw_probability`, `away_win_probability`: احتمالات الفوز الذكية بدون أي مراهنات.
- `ai_preview`, `ai_live_tactical`, `ai_post_report`: تقارير الذكاء الاصطناعي التكتيكية.

### 3. جدول إحصائيات المباراة المتقدمة (`match_stats`)
مستخرجة مباشرة من موفري Opta و Stats Perform:
- `possession_home` / `away`: نسبة الاستحواذ %.
- `xg_home` / `away`: الأهداف المتوقعة (Expected Goals).
- `xa_home` / `away`: صناعة الأهداف المتوقعة (Expected Assists).
- `ppda_home` / `away`: معدل التمريرات المسموح بها لكل تدخل دفاعي (Passes Allowed Per Defensive Action).
- `field_tilt_home` / `away`: نسبة ميل الملعب الهجومي ومحاصرة منطقة الثلث الأخير.
- `big_chances_home` / `away`: الفرص المحققة المؤكدة.

### 4. جدول مؤشر جاهزية اللاعبين الذكي (`player_condition_index`)
يقيس جاهزية كل لاعب من 0 إلى 100 عبر 7 عوامل مرجحة بدقة:
1. `physical_fatigue`: مؤشر الإجهاد البدني (وزن 22%).
2. `recent_form`: مستوى اللاعب في آخر 5 مباريات والـ xG/xA (وزن 20%).
3. `injury_risk`: مخاطر الإصابة العضلية والمجهود التراكمي (وزن 18%).
4. `tactical_fit`: التوافق مع نظام لعب المدرب والخطة (وزن 14%).
5. `mental_psychological`: العامل الذهني والثقة تحت الضغط (وزن 12%).
6. `travel_schedule_load`: كثافة السفر والتنقل الدولي (وزن 8%).
7. `weather_pitch_adaptation`: درجة التأقلم مع أرضية الملعب والطقس (وزن 6%).
- `overall_pci_score`: النتيجة المركبة المجمعة (Composite Score).
- `pci_status`: التصنيف اللوني والوصفي (`PEAK >= 88`, `OPTIMAL >= 75`, `MODERATE >= 60`, `HIGH_RISK < 60`).

### 5. جدول تطور الجاهزية اللحظي (`pci_timeline_points`)
- يرصد انخفاض أو صعود طاقة اللاعب كل ربع ساعة أو عند التدخلات البدنية.

### 6. جدول القنوات الناقلة الرسمية (`match_broadcasters`)
دليل شرعي 100% خالٍ من القرصنة أو المراهنات:
- `channel_name`: اسم القناة (مثل beIN SPORTS 1 HD, SSC 1 HD, Sky Sports, DAZN, ESPN+).
- `region`: المنطقة الجغرافية (`MENA`, `UK`, `USA`, `SPAIN`, `GERMANY`, `FRANCE`, `INTERNATIONAL`).
- `stream_format`: جودة ونوع البث (`UHD_4K`, `FULL_HD`, `OTT_STREAM`, `FREE_TO_AIR`).
- `commentators`: أسماء المعلقين المعتمدين لكل قناة.
- `official_platform_url`: رابط التحويل المباشر لتطبيق القناة الرسمي أو منصة البث المرخصة.

---

## ⚡ استراتيجيات الفهارس والأداء (Indexing & Caching Strategy)

1. `CREATE INDEX idx_matches_status ON matches(match_status);` — استعلام المباريات المباشرة في أقل من 2ms.
2. `CREATE INDEX idx_leagues_tier ON leagues(tier);` — فلترة سريعة فورية للـ 50+ دوري.
3. `CREATE INDEX idx_pci_match_player ON player_condition_index(match_id, player_id);` — تحميل بطاقة اللاعب والرادار السباعي بسرعة فائقة.
4. `CREATE INDEX idx_broadcasters_match ON match_broadcasters(match_id, region);` — فلترة القنوات حسب الدولة دون تأخير.
