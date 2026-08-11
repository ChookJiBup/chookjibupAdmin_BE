# 관리자 백엔드 배포 설정

관리자 백엔드 CD는 GitHub Actions의 `production` Environment에 등록한
Secret과 Variable을 사용한다. 실제 인증값은 저장소 파일에 기록하지 않는다.
운영용 `application-secret.yml`은 GitHub Environment Secret에서 AWS Systems
Manager Parameter Store의 SecureString을 거쳐 EC2 외부 설정 파일로 복원한다.

## GitHub Environment 생성

GitHub 저장소의 다음 메뉴에서 `production` Environment를 생성한다.

```text
Settings
└── Environments
    └── New environment
        └── production
```

Environment의 `Deployment branches and tags`는 `main` 브랜치만 허용한다.
운영 배포 전 승인이 필요하면 `Required reviewers`도 함께 설정한다.

## Environment secrets

`production`의 `Environment secrets`에 다음 값을 등록한다.

| 이름 | 필수 | 설명 |
| --- | --- | --- |
| `AWS_ACCESS_KEY_ID` | 필수 | SSM 배포 권한이 있는 AWS IAM Access Key ID |
| `AWS_SECRET_ACCESS_KEY` | 필수 | 위 Access Key의 Secret Access Key |
| `ADMIN_APPLICATION_SECRET_YML` | 필수 | 운영용 `application-secret.yml` 전체 내용 |

Secret 값은 로그, 문서, 이슈 및 커밋 메시지에 남기지 않는다.

AWS IAM 사용자 또는 역할에는 최소한 다음 작업 권한이 필요하다.

- `ssm:SendCommand`
- `ssm:GetCommandInvocation`
- `ssm:PutParameter`

EC2 인스턴스에는 SSM Agent가 실행 중이어야 하며, 인스턴스 역할에
`AmazonSSMManagedInstanceCore`에 해당하는 권한과 애플리케이션 Secret
Parameter에 대한 `ssm:GetParameter` 권한이 필요하다. 고객 관리형 KMS 키를
사용한다면 해당 키의 `kms:Decrypt` 권한도 추가한다.

## Environment variables

`production`의 `Environment variables`에 다음 값을 등록한다.

| 이름 | 필수 | 기본값 또는 설명 |
| --- | --- | --- |
| `ADMIN_EC2_INSTANCE_ID` | 필수 | 배포 대상 EC2 인스턴스 ID |
| `AWS_REGION` | 선택 | `us-east-1` |
| `ADMIN_DEPLOY_PATH` | 선택 | `/home/ec2-user/app/chookjibupAdmin_BE` |
| `ADMIN_DEPLOY_USER` | 선택 | `ec2-user` |
| `ADMIN_DEPLOY_HOME` | 선택 | `/home/ec2-user` |
| `ADMIN_SYSTEMD_SERVICE` | 선택 | `chookjibup-admin-backend` |
| `ADMIN_JAVA_HOME` | 선택 | `/usr/lib/jvm/java-21-amazon-corretto.x86_64` |
| `ADMIN_APPLICATION_SECRET_PARAMETER` | 선택 | `/chookjibup/admin/application-secret-yml` |
| `ADMIN_APPLICATION_SECRET_FILE` | 선택 | `/etc/chookjibup-admin/application-secret.yml` |

인스턴스 ID는 인증 정보는 아니지만 서버 구성을 코드와 분리하기 위해
Environment Variable로 관리한다.

## EC2 사전 조건

배포 전에 EC2에서 다음 조건을 충족해야 한다.

1. `ADMIN_DEPLOY_PATH`에 관리자 백엔드 저장소가 clone되어 있어야 한다.
2. `ADMIN_DEPLOY_USER`가 저장소와 Gradle 빌드 파일을 읽고 쓸 수 있어야 한다.
3. Java 21이 설치되어 `ADMIN_JAVA_HOME`과 일치해야 한다.
4. `ADMIN_SYSTEMD_SERVICE` 이름의 systemd 서비스가 등록되어 있어야 한다.
5. EC2 인스턴스 역할은 `ADMIN_APPLICATION_SECRET_PARAMETER`를 복호화해
   읽을 수 있어야 한다.

배포 작업은 `ADMIN_APPLICATION_SECRET_YML`을 Parameter Store에 SecureString으로
저장하고, EC2에서 `ADMIN_APPLICATION_SECRET_FILE`로 복원한다. 파일 권한은 `600`으로
설정하며 systemd의 `SPRING_CONFIG_IMPORT`를 통해 외부 설정으로 읽는다. 따라서
운영 Secret은 빌드된 JAR에 포함되지 않는다.

`ADMIN_APPLICATION_SECRET_YML`의 Value에는 `application-secret.yml` 내용을
따옴표나 Base64 변환 없이 그대로 붙여넣는다.

현장 스태프 인증을 운영할 때는 `APP_FIELD_STAFF_JWT_SECRET`을
`APP_JWT_SECRET`과 다른 충분히 긴 난수로 설정한다. 미설정 시 호환성을 위해
관리자 JWT secret을 fallback으로 사용하지만 서명키 노출 범위를 분리할 수 없으므로
운영 설정으로 권장하지 않는다.

## 배포 실행

- `main` push의 CI가 성공하면 검증된 커밋을 자동 배포한다.
- Actions의 `Deploy` 워크플로에서 `Run workflow`를 선택하면 `main`을
  수동 배포할 수 있다.
- 최신 `main`보다 오래된 CI 실행은 자동으로 배포를 건너뛴다.
