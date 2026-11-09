.PHONY: db-up db-down db-reset db-logs backend frontend test test-backend test-frontend build verify

db-up:
	docker compose up -d --wait db

db-down:
	docker compose down

db-reset:
	docker compose down -v
	docker compose up -d --wait db

db-logs:
	docker compose logs -f db

backend:
	cd backend && ./mvnw spring-boot:run

frontend:
	cd frontend && npm run dev

test: test-backend test-frontend

test-backend:
	cd backend && ./mvnw test

test-frontend:
	cd frontend && npm run lint && npm run build

build:
	cd backend && ./mvnw clean package
	cd frontend && npm run build

verify: test build
