# Tripmate - Frequently Used Commands

## Prerequisites

- Docker / Docker Compose
- Java 21 (optional if you only run via Docker)

## Build and Run

- Build jar locally:
  - `./mvnw clean package -DskipTests`
- Start services (planner + db + mcp):
  - `docker-compose up -d --build`
- Start only backend services:
  - `docker-compose up -d --build db mcp-airbnb planner`
- Stop services:
  - `docker-compose down`
- Stop services and remove volumes:
  - `docker-compose down -v`

## Health and API Checks

- Planner health:
  - `curl -sS "http://localhost:8080/planner/actuator/health"`
- Chat endpoint sample:
  - `curl -sS -X POST "http://localhost:8080/planner/api/ai/chat" -H "Content-Type: application/json" -d '{"message":"Find me properties near zip 15213 within 10 miles with coffee maker and summarize options.","userSuppliedTopK":8,"modelName":"claude-haiku-4-5","temperature":"0.3"}'`

## Logs

- Planner logs:
  - `docker logs -f tripmate-planner`
- MCP logs:
  - `docker logs -f tripmate-mcp-airbnb`
- DB logs:
  - `docker logs -f tripmate-db`

## PGVector / Database Checks

- Open psql shell:
  - `docker exec -it tripmate-db psql -U user -d postgres`
- Count embeddings:
  - `docker exec tripmate-db psql -U user -d postgres -c "SELECT COUNT(*) AS total_rows FROM vector_store;"`
- Preview embeddings:
  - `docker exec tripmate-db psql -U user -d postgres -c "SELECT id, LEFT(content, 90) AS content_preview FROM vector_store ORDER BY id LIMIT 20;"`
- Delete all embeddings:
  - `docker exec tripmate-db psql -U user -d postgres -c "TRUNCATE TABLE vector_store;"`

