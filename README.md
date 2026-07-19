# Voice-First Driving Assistant

A voice-powered Android driving assistant that converts natural-language requests into concise, reviewable actions.

## MVP

The first vertical slice supports typed input, GPT-5.6 structured planning, explicit confirmation, and local note or reminder execution. Realtime voice follows after the typed workflow is stable.

## Repository structure

```text
android/   Android Kotlin/Compose application
backend/   TypeScript/Fastify API
docs/      Product and architecture documentation
```

## Backend setup

```bash
cd backend
cp .env.example .env.local
npm install
npm run dev
```

Set `OPENAI_API_KEY` only in `.env.local` or in your deployment secret manager. Never commit it.

## Safety principles

- No continuous listening.
- No vehicle-control functions.
- State-changing actions require confirmation.
- Raw audio is not stored.
- Permanent API credentials remain on the backend.
