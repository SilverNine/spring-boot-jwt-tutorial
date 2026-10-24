# Spring Boot JWT Tutorial

인프런 강의 [Spring Boot JWT Tutorial](https://www.inflearn.com/course/%EC%8A%A4%ED%94%84%EB%A7%81%EB%B6%80%ED%8A%B8-jwt)의 예제 코드입니다.
Spring Boot 4, Spring Security 7, Java 25 기준으로 다시 정리했습니다.

- Kotlin 버전: [spring-boot-jwt-tutorial-kotlin](https://github.com/SilverNine/spring-boot-jwt-tutorial-kotlin)
- 강의 영상과 똑같은 코드(Spring Boot 2.4.1, Java 8): [`lecture-video` 태그](https://github.com/SilverNine/spring-boot-jwt-tutorial/tree/lecture-video)

> 강의 영상은 2021년에 촬영했습니다. 지금 `master`의 코드는 영상과 다릅니다.
> 영상을 따라 치면서 보려면 `lecture-video` 태그를, 실제 프로젝트에 가져다 쓰려면 `master`를 보세요.
> 무엇이 바뀌었는지는 아래 [강의 영상과 달라진 점](#강의-영상과-달라진-점)에 정리했습니다.

## 실행

JDK 25가 필요합니다. Gradle은 래퍼가 들어 있어 따로 설치하지 않아도 됩니다.

```bash
./gradlew bootRun     # http://localhost:8080
./gradlew test        # 테스트 20개
```

H2 메모리 DB를 쓰고, 시작할 때 `data.sql`이 샘플 계정 두 개를 넣습니다.

| 아이디 | 비밀번호 | 권한 |
|---|---|---|
| `admin` | `admin` | ROLE_USER, ROLE_ADMIN |
| `user` | `password` | ROLE_USER |

H2 콘솔은 http://localhost:8080/h2-console 입니다(JDBC URL `jdbc:h2:mem:testdb`).

## API

| 메서드 | 경로 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/hello` | 없음 | 동작 확인 |
| POST | `/api/signup` | 없음 | 회원가입 (ROLE_USER) |
| POST | `/api/authenticate` | 없음 | 로그인. Access Token과 Refresh Token 발급 |
| POST | `/api/token/refresh` | 없음 | Refresh Token으로 토큰 재발급 |
| POST | `/api/logout` | 없음 | Refresh Token 폐기 |
| GET | `/api/user` | USER 또는 ADMIN | 내 정보 |
| GET | `/api/user/{username}` | ADMIN | 다른 사용자 정보 |

### 따라 해 보기

```bash
# 1. 로그인
curl -s -X POST localhost:8080/api/authenticate \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin"}'
# {"token":"eyJhbGciOiJIUzUxMiJ9...","refreshToken":"q3k...Zc"}

# 2. Access Token으로 호출
curl -s localhost:8080/api/user -H "Authorization: Bearer <token>"

# 3. Access Token이 만료되면 Refresh Token으로 재발급
curl -s -X POST localhost:8080/api/token/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'

# 4. 로그아웃
curl -s -X POST localhost:8080/api/logout \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken>"}'
```

회원가입 본문은 `{"username":"...","password":"...","nickname":"..."}`이고 세 값 모두 3자 이상입니다.

### 토큰 정책

| 항목 | 값 |
|---|---|
| Access Token | JWT, HS512 서명, 기본 30분 |
| Refresh Token | 256비트 난수 문자열(JWT 아님), 기본 14일 |
| Refresh Token 저장 | `refresh_token` 테이블에 SHA-256 해시만 저장 |
| 재발급 | 쓸 때마다 새 Refresh Token으로 교체. 한 번 쓴 토큰은 다시 못 씀 |
| 로그아웃 | Refresh Token 행 삭제. 이미 발급한 Access Token은 만료될 때까지 유효 |

Access Token은 서버에 저장하지 않으므로 수명을 짧게 둡니다.
Refresh Token은 DB에 해시로만 남겨서, DB가 새어도 원래 토큰을 알 수 없습니다.

### 오류 응답

모든 오류는 [RFC 9457](https://www.rfc-editor.org/rfc/rfc9457) Problem Details 형식(`application/problem+json`)으로 돌려줍니다.
필터에서 막힌 401, 403도 같은 형식입니다.

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "아이디 또는 비밀번호가 올바르지 않습니다.",
  "instance": "/api/authenticate"
}
```

| 상황 | 상태 코드 |
|---|---|
| 입력값 검증 실패 (`fieldErrors` 포함) | 400 |
| 토큰 없음, 잘못된 토큰, 만료, 비밀번호 불일치, 잘못된 Refresh Token | 401 |
| 권한 부족 | 403 |
| 없는 사용자 조회 | 404 |
| 이미 가입한 아이디 | 409 |

## 설정

`application.yml`의 값은 환경변수로 덮어쓸 수 있습니다.

| 환경변수 | 기본값 | 설명 |
|---|---|---|
| `JWT_SECRET` | 예제용 값 | Base64로 인코딩한 서명 키. HS512는 64바이트 이상 |
| `JWT_ACCESS_TOKEN_VALIDITY` | `30m` | Access Token 수명 (`Duration` 형식) |
| `JWT_REFRESH_TOKEN_VALIDITY` | `14d` | Refresh Token 수명 |

`application.yml`에 들어 있는 키는 예제용이라 공개돼 있습니다. 실제로 배포할 때는 반드시 새로 만들어 환경변수로 넣으세요.

```bash
export JWT_SECRET="$(openssl rand -base64 64 | tr -d '\n')"
```

## 강의 영상과 달라진 점

| 항목 | 강의 영상 | 지금 코드 |
|---|---|---|
| 버전 | Spring Boot 2.4.1, Java 8, jjwt 0.11.2 | Spring Boot 4.1.2, Java 25, jjwt 0.13.0 |
| 패키지 | `javax.*` | `jakarta.*` |
| 보안 설정 | `WebSecurityConfigurerAdapter` 상속 | `SecurityFilterChain` 빈 + 람다 DSL |
| JWT 필터 등록 | `JwtSecurityConfig`(`SecurityConfigurerAdapter`) | `addFilterBefore` 한 줄 |
| JWT 필터 | `GenericFilterBean` | `OncePerRequestFilter` |
| 로그인 처리 | `AuthenticationManagerBuilder.getObject()` | `AuthenticationManager` 빈(`ProviderManager` + `DaoAuthenticationProvider`) |
| 메서드 보안 | `@EnableGlobalMethodSecurity` | `@EnableMethodSecurity` |
| CORS | `CorsFilter` 직접 등록 | `CorsConfigurationSource` 빈 + `.cors(withDefaults())` |
| JWT 설정값 | `@Value` | `@ConfigurationProperties` 레코드(`JwtProperties`) |
| JWT API | `parserBuilder()`, `signWith(key, SignatureAlgorithm)` | `parser().verifyWith()`, `signWith(key, Jwts.SIG.HS512)` |
| 권한 클레임 | 쉼표로 이은 문자열 | JSON 배열 |
| Refresh Token | 없음 | 발급, 교체, 폐기 |
| 오류 응답 | Spring 기본 오류 응답 (영상 이후 `ErrorDto` 추가) | 표준 `ProblemDetail` |
| 없는 사용자 | 영상 이후 추가분에서 403 | 404 |
| DTO | Lombok 클래스 | `record` (`TokenDto`, `LoginDto` 등) |
| 테스트 | `contextLoads` 하나 | MockMvc 통합 테스트, 서비스 테스트, `TokenProvider` 단위 테스트 20개 |

로그인 응답의 `token` 필드 이름은 강의와 같게 두었습니다. `refreshToken` 필드가 하나 늘었습니다.

## 참고

- **Jackson 2와 3이 함께 들어 있습니다.** Spring Boot 4는 Jackson 3(`tools.jackson`)을 쓰고, `jjwt-jackson`은 아직 Jackson 2(`com.fasterxml.jackson`)를 씁니다. 패키지 이름이 달라 충돌하지 않습니다.
- **Spring Security에도 JWT 기능이 있습니다.** 이 예제는 원리를 보이려고 필터를 직접 만들지만, Spring Security의 [OAuth 2.0 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html)로도 같은 일을 할 수 있습니다. 대칭키라면 `NimbusJwtDecoder.withSecretKey(...)`를 쓰면 됩니다.
- 버전과 설정은 2026년 10월 기준 Spring Boot 4.1.2, Spring Security 7.1.2 공식 문서를 따랐습니다.

## 버전

| 구성 요소 | 버전 |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.2 |
| Spring Security | 7.1.2 |
| jjwt | 0.13.0 |
| Gradle | 9.8.2 |
| DB | H2 (메모리) |

## Author

**SilverNine**

* https://silvernine.me
* https://portfolio.silvernine.me
* https://github.com/silvernine

## Copyright and license

The code is released under the [MIT license](LICENSE?raw=true).
