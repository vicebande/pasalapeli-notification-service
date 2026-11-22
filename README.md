# pasalapeli-notification-service (Notificaciones in-app + EKS)

Microservicio que consume eventos de SQS, aplica reglas de distribución (fan-out) y almacena notificaciones in-app en MySQL. Despliega sobre **EKS** como Deployment + Service `ClusterIP` (puerto 8080).

## ☁️ Despliegue CI/CD (GitHub Actions → Terraform → EKS)

La infraestructura compartida es propietaria de [`pasalapeli-database`](./../pasalapeli-database) y debe desplegarse primero.

Workflow `.github/workflows/deploy.yml` en cada `push` a `main`:
1. Quality Gate: SonarCloud.
2. Terraform Init / Format / Validate / Plan / Apply (backend S3 `pasalapeli-tfstate`, clave `pasalapeli-notification-service/terraform.tfstate`).
3. `aws eks update-kubeconfig --name pasalapeli-cluster --region us-east-1`
4. `kubectl apply -f k8s-manifests/`

### Kubernetes manifests (`k8s-manifests/`)
- `deployment.yaml`: Deployment `pasalapeli-notification-service` (1 réplica, puerto 8080).
- `service.yaml`: Service `pasalapeli-notification-service` tipo `ClusterIP` (8080 → 8080).

### GitHub Secrets requeridos
| Secret | Descripción |
|---|---|
| `AWS_ACCESS_KEY_ID` | Credencial AWS |
| `AWS_SECRET_ACCESS_KEY` | Secreto asociado |
| `AWS_SESSION_TOKEN` | Token de sesión (credenciales temporales STS) |
| `SONAR_TOKEN` | Token de SonarCloud |
| `SONAR_HOST_URL` | `https://sonarcloud.io` |

### Variables de entorno (terraform)
- `TF_VAR_environment_name` = `dev`
- `TF_VAR_domain_name` = `pasalapeli-notification-service`

### Imagen ECR
Contenedor extraído de `pasalapeli-images` como `notification-service:latest`.

## ⚙️ Configuración de la aplicación
Véase `src/main/resources/application.yml`. Variables clave: `SPRING_DATASOURCE_*`, `AWS_REGION`, `SQS_QUEUE_URL`, `SQS_DLQ_URL`, `AZURE_*`, `CORS_ALLOWED_ORIGINS`.
