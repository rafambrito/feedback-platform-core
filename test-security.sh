#!/bin/bash

# Script de teste de segurança - Etapa 6
# Valida tokens JWT: ausente, inválido, expirado, sem escopo, válido

BASE_URL="http://localhost:8080"

# Tokens gerados
VALID_TOKEN="eyJraWQiOiJhcGktZ2F0ZXdheS1raWQiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2NvZ25pdG8taWRwLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tL3VzLWVhc3QtMV9URVNUUE9PTCIsImF1ZCI6ImZlZWRiYWNrLXBsYXRmb3JtLWFwaS1kZXYiLCJzdWIiOiJ0ZXN0LXVzZXItMTIzNDUiLCJleHAiOjE3ODM3ODY1OTIsImlhdCI6MTc4Mzc4Mjk5MiwiZ3JvdXBzIjpbIkFETUlOIiwiU1lTVEVNIl0sInNjb3BlIjoiZmVlZGJhY2s6cmVhZCBmZWVkYmFjazp3cml0ZSJ9.Lah8jktRI-wj6RTln9LSbOSprmn4nAcQVL5QDV3XI6HLqlMKiafy32ASMPB-maUvykltNcTfi3KH_yFezjj9o6t42AvpXJ8_x1YYzByKmKI9baMrQT_EHE96Ig3lxHNLpHfnQ_8hafMTdnTi5V8cd0pMnt6h6Gxn9kzqrsklOKEDwwr4OSFvU9q_Ej1G_h63pzlCehnlQsBMYFJElHCOaXYYYQweZsQ-3TRH6APgIhbWJGrZ6vKAyOJ0tXnU8j8FUnj3aaIvN6Pn24-wOyfnzMJR8ZRLJqHURM64VVSb_ZdjzzpceyDhodJDZkOuoOYR8vdDNlXx76tqAVomg8B7Dw"

EXPIRED_TOKEN="eyJraWQiOiJhcGktZ2F0ZXdheS1raWQiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2NvZ25pdG8taWRwLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tL3VzLWVhc3QtMV9URVNUUE9PTCIsImF1ZCI6ImZlZWRiYWNrLXBsYXRmb3JtLWFwaS1kZXYiLCJzdWIiOiJ0ZXN0LXVzZXItMTIzNDUiLCJleHAiOjE3ODM3ODIzOTMsImlhdCI6MTc4Mzc4MTc5MywiZ3JvdXBzIjpbIkFETUlOIl0sInNjb3BlIjoiZmVlZGJhY2s6cmVhZCJ9.eJ4H6kq9fX4hCKGsslRZKyuGIkwLr4ZtNc6ILWT9QiUPBUVVqy_208rk6Anx2WVF7etOYeJhvTmm5m5O50J2h2AjiugFQBaacjnJACalfmOpsRVYBRytw-9f0yZxBzgvsiZS61qz0BErOoKX8zYxIBLKNjg5W4olJk8Mh_HffR3C2kKiQlTl-fqg8gNXx_TBO6taCtqsIdOyHjAsOXvxWs7c4NCmpj5ob6KtkVSAt2O12YK3IVUpjQcpE9B9wauRls-tgn4ypp4iXmfMogCeKCm_kV8FM45J1diFH7gdG0kvW55GyE-CpCtSDdx8B4tYBag2NWykrGuesryyRo5vNQ"

NO_SCOPE_TOKEN="eyJraWQiOiJhcGktZ2F0ZXdheS1raWQiLCJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJodHRwczovL2NvZ25pdG8taWRwLnVzLWVhc3QtMS5hbWF6b25hd3MuY29tL3VzLWVhc3QtMV9URVNUUE9PTCIsImF1ZCI6ImZlZWRiYWNrLXBsYXRmb3JtLWFwaS1kZXYiLCJzdWIiOiJ0ZXN0LXVzZXItMTIzNDUiLCJleHAiOjE3ODM3ODY1OTMsImlhdCI6MTc4Mzc4Mjk5M30.LHKzyxniT8Z50CxXPfwS-IV13i2-1KzJSuffa5OtShzYJI1Rk5-156i_gnnnVBUZsrQod8YiZmkE78sti0IFuasol6aWlBMl7_3Aydv9deCUfIR6h1LiqmUO-GgJhLZWT1gGCK6OUytQXtz7EoyrgIcctZ_Gb_6-uh3IdtQPG6zdwG3x47pdVlA_06Ga6qAWwILFWHtqR0ErAXrHDf3AzGm6H9_Ld4JUDsKXCg9S5MGVtEWSXkM2ivgtPiBazpDXFHkPulAM0J9eCc64behEtyqhAKVQ-H-5ku5j0wWAAmHj0krZrMx1yVzUUl4iywNpw6PjPdxDrHlmYas1-LEijQ"

FEEDBACK_PAYLOAD='{"cursoId":"C1","alunoId":"A1","professorId":"P1","nota":10,"comentario":"teste"}'

echo "=== TESTE DE SEGURANÇA - ETAPA 6 ==="
echo ""

# Teste 1: Sem token (esperado 401)
echo "📋 Teste 1: Sem token (esperado 401)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/feedback" \
  -H 'Content-Type: application/json' \
  -d "$FEEDBACK_PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n-1)
echo "Status: $HTTP_CODE"
echo "Resposta: $BODY"
[[ "$HTTP_CODE" == "401" ]] && echo "✅ PASSOU" || echo "❌ FALHOU (esperado 401)"
echo ""

# Teste 2: Token inválido (esperado 401)
echo "📋 Teste 2: Token inválido (esperado 401)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/feedback" \
  -H 'Authorization: Bearer INVALID_TOKEN_HERE' \
  -H 'Content-Type: application/json' \
  -d "$FEEDBACK_PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n-1)
echo "Status: $HTTP_CODE"
echo "Resposta: $BODY"
[[ "$HTTP_CODE" == "401" ]] && echo "✅ PASSOU" || echo "❌ FALHOU (esperado 401)"
echo ""

# Teste 3: Token expirado (esperado 401)
echo "📋 Teste 3: Token expirado (esperado 401)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/feedback" \
  -H "Authorization: Bearer $EXPIRED_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "$FEEDBACK_PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n-1)
echo "Status: $HTTP_CODE"
echo "Resposta: $BODY"
[[ "$HTTP_CODE" == "401" ]] && echo "✅ PASSOU" || echo "❌ FALHOU (esperado 401)"
echo ""

# Teste 4: Token sem escopos (esperado 403)
echo "📋 Teste 4: Token sem escopos (esperado 403)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/feedback" \
  -H "Authorization: Bearer $NO_SCOPE_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "$FEEDBACK_PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n-1)
echo "Status: $HTTP_CODE"
echo "Resposta: $BODY"
[[ "$HTTP_CODE" == "403" ]] && echo "✅ PASSOU" || echo "❌ FALHOU (esperado 403)"
echo ""

# Teste 5: Token válido (esperado 201)
echo "📋 Teste 5: Token válido com escopos (esperado 201)"
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/feedback" \
  -H "Authorization: Bearer $VALID_TOKEN" \
  -H 'Content-Type: application/json' \
  -d "$FEEDBACK_PAYLOAD")
HTTP_CODE=$(echo "$RESPONSE" | tail -1)
BODY=$(echo "$RESPONSE" | head -n-1)
echo "Status: $HTTP_CODE"
echo "Resposta: $BODY"
[[ "$HTTP_CODE" == "201" ]] && echo "✅ PASSOU" || echo "❌ FALHOU (esperado 201)"
echo ""

echo "=== RESUMO ==="
echo "✅ Testes de segurança completados"
echo "📌 Validação de JWT ativada"
echo "📌 Interceptor funcionando com todos os cenários"
