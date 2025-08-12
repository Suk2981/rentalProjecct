 (cd "$(git rev-parse --show-toplevel)" && git apply --3way <<'EOF' 
diff --git a/README.md b/README.md
index 8e049ed87be024e28c947504965244358fd9db1f..3135bc4dc2c14cdd868415d1401fb29fa64c11d4 100644
--- a/README.md
+++ b/README.md
@@ -1 +1,57 @@
-# rentalProjecct
+# rentalProjecct
+
+## 프로젝트 개요
+RentalProjecct는 장비 대여 관리를 위한 JavaFX 기반 데스크톱 애플리케이션입니다. 사용자와 관리자가 장비를 조회하고 대여하거나 반납할 수 있으며, 대여 기록과 연체 현황도 확인할 수 있습니다.
+
+## 주요 기능
+- 회원가입 및 로그인
+- 장비 목록 조회와 상세 정보 확인
+- 장비 대여, 반납, 대여/연체 기록 조회
+- 개인 정보 확인 및 수정
+- 관리자용 장비 등록/수정과 대여 내역 관리
+
+## 기술 스택
+- **Language:** Java (JDK 11+)
+- **UI:** JavaFX, FXML
+- **Database:** Oracle Database (Oracle JDBC 드라이버)
+
+## 실행 및 빌드 방법
+1. **환경 준비**
+   - JDK 11 이상 설치
+   - JavaFX SDK 설치 후 `PATH_TO_FX` 환경 변수 설정
+   - Oracle Database 준비 (기본 연결 정보는 `DBUtil.java` 참고)
+     - URL: `jdbc:oracle:thin:@localhost:1521/xepdb1`
+     - 사용자/비밀번호: `hr/hr`
+
+2. **빌드** (예시)
+   ```bash
+   javac --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml \
+     -d out $(find project_test/src -name "*.java")
+   ```
+
+3. **실행**
+   ```bash
+   java --module-path $PATH_TO_FX --add-modules javafx.controls,javafx.fxml \
+     -cp out main.MainApp
+   ```
+   또는 IDE에서 `main.MainApp` 클래스를 실행합니다.
+
+## 프로젝트 구조
+```
+project_test/
+ └─ src/
+    ├─ main/        # 애플리케이션 시작점
+    ├─ controller/  # FXML 컨트롤러
+    ├─ dao/         # DB 접근 로직
+    ├─ model/       # DTO 및 도메인 모델
+    ├─ view/        # FXML 뷰 파일
+    ├─ resources/   # CSS, 이미지 등 정적 자원
+    ├─ utils/       # DB 연결 등 유틸리티
+    └─ uploads/     # 업로드된 파일 저장소
+```
+
+## 필요 환경
+- Oracle Database (1521 포트, 서비스: `xepdb1`)
+- Oracle JDBC 드라이버
+- JavaFX SDK
+
 
EOF
)
