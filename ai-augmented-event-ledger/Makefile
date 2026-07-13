.PHONY: demo test quality contract coverage compose

demo:
	./scripts/demo-flow.sh

test:
	mvn clean verify

quality:
	./scripts/run-quality-gates.sh

contract:
	./scripts/validate-api-contract.sh

coverage:
	./scripts/generate-coverage-summary.sh

compose:
	docker compose up --build
