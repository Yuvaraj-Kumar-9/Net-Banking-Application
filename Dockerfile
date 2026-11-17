# syntax=docker/dockerfile:1

# ===========================================================================
# HFDC Bank - production image
# One container serves the React build and the Spring Boot API on the same
# origin, so the HttpOnly JWT cookie stays first-party (no CORS, no token in
# localStorage).
# ===========================================================================

# ---- Stage 1: build the React frontend ------------------------------------
FROM node:20-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
# Relative /api works because the backend serves the UI on the same origin.
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL
RUN npm run build

# ---- Stage 2: build the Spring Boot jar -----------------------------------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline
COPY src ./src
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -B -DskipTests package

# ---- Stage 3: minimal runtime ---------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S hfdc && adduser -S hfdc -G hfdc
COPY --from=backend /app/target/hfdc-bank.jar ./app.jar
USER hfdc
EXPOSE 8080
ENV PORT=8080 \
    DEV_MODE=false \
    COOKIE_SECURE=true \
    COOKIE_SAME_SITE=lax
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- "http://127.0.0.1:${PORT}/api/auth/health" || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
