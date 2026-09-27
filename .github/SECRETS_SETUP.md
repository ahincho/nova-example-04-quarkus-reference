# Configuracion de Secrets para GitHub Actions

Este repositorio usa los siguientes secrets en GitHub Actions para resolver las dependencias publicadas en GitHub Packages (Nova Platform).

## Acceso a la configuracion

`https://github.com/ahincho/nova-example-04-quarkus-reference/settings/secrets/actions`

## Secrets requeridos

### `NOVA_PACKAGES_READ_TOKEN` (REQUIRED para CI)

**Que es:** Personal Access Token (fine-grained recomendado) con scope **`read:packages`** sobre los repos de Nova Platform.

**Por que se necesita:** Este repo consume `pe.edu.nova.java.starters:nova-java-api-standard-quarkus-extension` (publicado en `ahincho/nova-java-10-api-standard-quarkus-extension`) y `pe.edu.nova.java.libs:nova-api-standard` (publicado en `ahincho/nova-java-01-api-standard`). El `GITHUB_TOKEN` automatico NO puede leer packages de OTROS repos, solo del repo actual. Por eso necesitamos un PAT con `read:packages` explicito.

**Como crearlo:**

1. Ir a `https://github.com/settings/personal-access-tokens/new` (fine-grained) o `https://github.com/settings/tokens?type=personal` (classic).
2. **Fine-grained token** (recomendado):
   - **Name:** `Nova Packages Read Token - nova-java-quarkus-example`
   - **Expiration:** 1 year (renovar antes)
   - **Resource owner:** `ahincho`
   - **Repository access:** `Only select repositories` → agregar los 2 repos de Nova:
     - `ahincho/nova-java-10-api-standard-quarkus-extension`
     - `ahincho/nova-java-standard`
   - **Permissions:**
     - **Packages:** Read-only
     - **Metadata:** Read-only (automatico)
3. Click **Generate token**, copiar el valor inmediatamente.

**Como configurarlo en este repo:**

1. `https://github.com/ahincho/nova-example-04-quarkus-reference/settings/secrets/actions/new`
2. **Name:** `NOVA_PACKAGES_READ_TOKEN`
3. **Secret:** pegar el token
4. Click **Add secret**.

## Secrets opcionales

### `NOVA_PACKAGE_VISIBILITY` (OPTIONAL)

Define si los paquetes Maven van como `public` o `private`. Este repo NO publica artefactos, asi que NO aplica. Solo es relevante si en el futuro se quiere publicar este repo como ejemplo "production-ready".

### `NVD_API_KEY` (OPTIONAL, recomendado para velocidad)

API key gratuita del NIST para evitar rate limits en el job OWASP dependency-check.

`https://nvd.nist.gov/developers/request-an-api-key`

## Variables (no secrets)

### `SONAR_TOKEN` (OPTIONAL)

Para analisis SonarCloud. Si no se configura, el job `sonar` se salta silenciosamente.

## Checklist de primer setup

```
[ ] NOVA_PACKAGES_READ_TOKEN configured (con scope read:packages sobre
    ahincho/nova-java-10-api-standard-quarkus-extension Y ahincho/nova-java-01-api-standard)
[ ] (Opcional) NVD_API_KEY configured
[ ] (Opcional) SONAR_TOKEN configured
```

## Por que `NOVA_PACKAGES_READ_TOKEN` y no `GITHUB_TOKEN` o `NOVA_RELEASE_PAT`

| Token | Tiene `read:packages` cross-repo? | Funciona aqui? |
|---|---|---|
| `GITHUB_TOKEN` (automatico) | NO — solo lee packages del repo actual | ❌ Falla con 401 |
| `NOVA_RELEASE_PAT` (con `repo` classic) | NO — classic `repo` no incluye `read:packages` | ❌ Falla con 403 (resource not accessible) |
| `NOVA_PACKAGES_READ_TOKEN` (fine-grained con `Packages: Read`) | SI — explicitamente otorga `read:packages` | ✅ |

> **Trampa comun:** si usas un PAT classic con scope `repo`, falla con
> `Your token has not been granted the required scopes to execute this query. The 'id' field requires one of the following scopes: ['read:packages']`.
> Aunque `repo` parece "completo", NO incluye `read:packages`. Hay que usar fine-grained con el scope explicito.

## Troubleshooting

### Build falla con `Received status code 401 from server: Unauthorized` en el Quarkus IT job

→ Falta `NOVA_PACKAGES_READ_TOKEN` o el PAT configurado no tiene el scope `read:packages`.

### Build falla con `Received status code 403: Resource not accessible by personal access token`

→ El PAT classic con scope `repo` no es suficiente. Re-genera como fine-grained con `Packages: Read`.

### Build falla con `Could not GET .../nova-api-standard-1.0.0.pom` pero la lib existe

→ El repositorio `nova-java-api-standard` no esta en la lista de "Selected repositories" del PAT. Agregalo.