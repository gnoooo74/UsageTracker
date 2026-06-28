# Usage Tracker

Android 앱 사용 기록 및 브라우저 방문기록 트래커

## 기능

| 기능 | 설명 |
|------|------|
| 앱 사용 기록 | 앱 이름, 포그라운드 진입시간, 백그라운드/종료시간, 총 사용시간 |
| 브라우저 기록 | URL, 페이지 제목, 방문시각, 방문횟수, 브라우저 출처(크롬/삼성인터넷) |
| 자동 동기화 | 15분마다 WorkManager로 백그라운드 자동 수집 |
| 날짜 선택 | 날짜별 기록 조회 |
| 로컬 저장 | Room DB (기기 내부, 30일 보관) |

## 빌드 방법

### 요구사항
- Android Studio Hedgehog (2023.1.1) 이상
- JDK 17
- Android SDK 34
- Gradle 8.2

### 빌드 단계

```bash
# 1. 클론
git clone https://github.com/YOUR_ID/UsageTracker.git
cd UsageTracker

# 2. Android Studio에서 열기
# File → Open → UsageTracker 폴더 선택

# 3. Gradle Sync
# Android Studio 상단 "Sync Now" 클릭

# 4. 빌드
./gradlew assembleDebug

# 5. 설치
./gradlew installDebug
```

## 권한 설정 (필수)

앱 설치 후 다음 권한을 허용해야 합니다:

1. **앱 사용 통계 권한**
   - 설정 → 디지털 웰빙/앱 사용 시간 → 앱 접근 허용
   - 또는 앱 첫 실행 시 자동으로 설정 화면으로 이동

2. **브라우저 기록 접근**
   - 별도 권한 없음 (ContentProvider 방식)
   - 단, Android 버전/기기에 따라 접근 가능 여부 다름

## 프로젝트 구조

```
app/src/main/java/com/usagetracker/
├── data/
│   ├── db/
│   │   ├── AppUsageDao.kt          # 앱 사용기록 DAO
│   │   ├── BrowserHistoryDao.kt    # 브라우저기록 DAO
│   │   └── TrackerDatabase.kt      # Room Database
│   ├── model/
│   │   ├── AppUsageEntity.kt       # 앱 사용기록 모델
│   │   └── BrowserHistoryEntity.kt # 브라우저기록 모델
│   ├── repository/
│   │   └── TrackerRepository.kt    # 데이터 저장소
│   └── TrackingService.kt          # 포그라운드 서비스
├── ui/
│   └── main/
│       ├── MainActivity.kt         # 메인 화면
│       ├── MainViewModel.kt        # ViewModel
│       ├── AppUsageAdapter.kt      # 앱 기록 RecyclerView
│       └── BrowserHistoryAdapter.kt# 브라우저 기록 RecyclerView
├── util/
│   ├── UsageStatsCollector.kt      # 앱 사용통계 수집
│   ├── BrowserHistoryCollector.kt  # 브라우저 히스토리 수집
│   ├── BootReceiver.kt             # 부팅 시 자동 시작
│   ├── SyncWorker.kt               # WorkManager 워커
│   └── FormatUtil.kt               # 시간 포맷 유틸
└── TrackerApplication.kt           # Application 클래스
```

## 브라우저 기록 수집 제한사항

| 브라우저 | 수집 가능 여부 | 비고 |
|----------|--------------|------|
| 삼성 인터넷 | ✅ 대부분 가능 | ContentProvider 공개 |
| 크롬 | ⚠️ 기기/버전 의존 | Android 6+ 제한적 |
| 네이버 앱 | ❌ 불가 | 샌드박스 |
| 카카오 인앱 | ❌ 불가 | 샌드박스 |

## 기술 스택

- **언어**: Kotlin
- **아키텍처**: MVVM
- **DB**: Room
- **비동기**: Coroutines + LiveData
- **백그라운드**: WorkManager
- **UI**: Material Design 3

## 최소 요구사양

- Android 8.0 (API 26) 이상
- 저장공간: 약 10MB
