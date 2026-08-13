# Live Broadcast Studio

**A reusable Android broadcasting studio for mobile-first live content, education, events, communities, and specialized live experiences.**

Part of the **Enterprise Intelligence Suite** — modular products that can be adapted to different brands, audiences, and operating environments.

## Overview

Live Broadcast Studio provides a mobile-native foundation for producing and publishing live content directly from Android. The reusable core is designed to support multiple themed experiences instead of being tied to a single content vertical or brand.

## Core capabilities

- Portrait-first live studio for mobile platforms
- RTMP / RTMPS publishing
- Screen-capture broadcasting through Android MediaProjection
- Local audio, microphone, or mixed-audio workflows
- Themeable live scenes and visual overlays
- Broadcast presets and configurable experiences
- Mobile-native control surface
- Extensible post-live outcomes and engagement flows

## Product architecture

```text
Broadcast Core
├── Capture
├── Audio
├── Streaming
├── Scene Rendering
└── Session Control
        │
        ▼
Experience Modules
├── Education
├── Events
├── Community
├── Faith / Reflection
└── Specialized Verticals
        │
        ▼
Brand + Tenant Layer
├── Name
├── Theme
├── Content Packs
└── Distribution Configuration
```

## Technology

- Kotlin
- Native Android
- MediaProjection
- RTMP / RTMPS workflows
- GitHub Actions for APK builds

## Build

The project includes an automated Android build workflow. For local development, use a compatible Android Studio / Gradle environment and keep signing credentials outside the repository.

**Never commit signing keys, keystore passwords, live-stream keys, platform access tokens, private endpoints, user data, or production credentials.**

## White-label model

The reusable broadcast engine should remain brand-neutral. Visual themes, content packs, stream destinations, titles, logos, and vertical-specific experiences should be supplied as configuration or modules.

## Enterprise Intelligence Suite

| Product | Focus |
|---|---|
| [**Growth & Opportunity OS**](https://github.com/musu-deep/araak-marketing) | Growth, marketing, opportunities and tenders |
| [**Executive Office OS**](https://github.com/musu-deep/araak-ceo) | Executive office, decisions and follow-up |
| [**Strategy Execution OS**](https://github.com/musu-deep/araak-development-command-center) | Strategy, initiatives, KPIs and execution |
| [**Logistics Business Platform**](https://github.com/musu-deep/araak-logistics-website) | Logistics services and digital customer journeys |
| [**Learning & Academy OS**](https://github.com/musu-deep/Araak-university) | Learning, academies and capability development |
| **Live Broadcast Studio** | Mobile live broadcasting and interactive content |

## Product status

**Generalization in progress.** Existing themed experiences are being treated as reference implementations layered on top of the reusable broadcast core.

See [`PRODUCT_STRATEGY.md`](./PRODUCT_STRATEGY.md) for the product direction.

## Security

Review [`SECURITY.md`](./SECURITY.md) before distributing builds or configuring production stream destinations.
