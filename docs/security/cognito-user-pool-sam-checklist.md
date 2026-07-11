# Checklist de Configuracao - AWS Cognito User Pool (Serverless + SAM)

Objetivo: servir como guia de configuracao para infraestrutura em AWS SAM, com foco em autenticacao JWT para APIs serverless.

## 1. Issuer

| Item | O que configurar | Valor esperado | Status |
|---|---|---|---|
| Regiao do User Pool | Regiao unica por ambiente | ex.: us-east-1 | [ ] |
| User Pool ID por ambiente | ID diferente para dev, hml e prd | ex.: us-east-1_XXXXXX | [ ] |
| Issuer completo | Formato fixo do issuer JWT | https://cognito-idp.<region>.amazonaws.com/<userPoolId> | [ ] |
| Validacao por ambiente | API deve validar issuer correto por env | dev/hml/prd sem mistura | [ ] |

Checklist rapido de saida:
- Issuer definido e validado para cada ambiente.
- Issuer documentado no parametro de infraestrutura (SAM Parameters/Globals).

## 2. App Client

| Item | O que configurar | Valor esperado | Status |
|---|---|---|---|
| App Client para APIs | Criar client dedicado para consumo da API | 1 por ambiente (ou 1 por tipo de cliente) | [ ] |
| Gerar secret | Definir se client usa secret | Recomendado sem secret para SPA/mobile, com secret para M2M | [ ] |
| Fluxos habilitados | Definir fluxo OAuth correto | client_credentials para SYSTEM, authorization_code para usuario | [ ] |
| Leitura do client_id | Disponibilizar como parametro no SAM | Parametro por ambiente | [ ] |

Checklist rapido de saida:
- App Client criado com fluxo correto para cada tipo de consumidor.
- client_id e (se aplicavel) client_secret tratados por ambiente.

## 3. Allowed OAuth Scopes

| Item | O que configurar | Valor esperado | Status |
|---|---|---|---|
| Resource Server | Criar namespace de scopes | ex.: feedback-platform | [ ] |
| Escopos minimos | Publicar scopes de dominio | feedback:write, feedback:read, notification:write, notification:read, report:read, admin | [ ] |
| Vinculacao no App Client | Associar scopes permitidos por client | Menor privilegio possivel | [ ] |
| Escopos por perfil | Separar USER e SYSTEM | SYSTEM sem scopes de usuario final desnecessarios | [ ] |

Checklist rapido de saida:
- Scopes padronizados e publicados no Resource Server.
- App Client com apenas os scopes necessarios.

## 4. Regras de expiração de token

| Item | O que configurar | Valor esperado | Status |
|---|---|---|---|
| Access Token validity | Definir duracao curta | 15 a 60 minutos (recomendado: 30) | [ ] |
| ID Token validity | Definir duracao curta/moderada | 15 a 60 minutos (recomendado: 30) | [ ] |
| Refresh Token validity | Definir duracao por risco | 1 a 30 dias (recomendado: 7) | [ ] |
| Diferenciar USER e SYSTEM | Ajustar validade por tipo de cliente | SYSTEM com validade menor quando possivel | [ ] |
| Clock skew de validacao | Definir tolerancia no backend | 60 a 120 segundos | [ ] |

Checklist rapido de saida:
- Validades alinhadas ao nivel de risco e tipo de consumidor.
- Regras documentadas no contrato JWT e no SAM.

## 5. Mapeamento direto para SAM (referencia)

| Tema | Onde refletir no SAM |
|---|---|
| Issuer | Auth Authorizer JwtConfiguration.Issuer |
| Audience (App Client) | Auth Authorizer JwtConfiguration.Audience |
| Allowed OAuth Scopes | Auth Authorizer AuthorizationScopes (por rota) |
| Expiracao de token | Configurado no Cognito App Client, referenciado por parametros/outputs no SAM |

## 6. Gate final antes de deploy

| Verificacao | Criterio de aceite | Status |
|---|---|---|
| Issuer correto por ambiente | Token de dev nao autentica em hml/prd | [ ] |
| Audience correta | Apenas App Client autorizado e aceito | [ ] |
| Scope por rota | Rota protegida sem scope deve falhar | [ ] |
| Sem token | API retorna 401 | [ ] |
| Token invalido/expirado | API retorna 401 | [ ] |
| Token valido sem permissao | API retorna 403 | [ ] |

Resultado esperado: checklist completo, revisado e pronto para guiar a implementacao do arquivo SAM de infraestrutura.
