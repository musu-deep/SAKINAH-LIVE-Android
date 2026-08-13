# Live Broadcast Studio

## Product Positioning
A reusable, white-label mobile live-broadcast and guided-content studio for education, faith-based content, events, training, media and community programs.

## Product Family
Part of the **Enterprise Intelligence Suite** as the live/mobile experience layer.

## Architecture Principle
Separate the product into four layers:
1. Core Engine — playback/broadcast logic, sessions, scheduling, media controls, notifications, analytics and integrations.
2. Modules — live sessions, scheduled programs, playlists, reading/presentation mode, chat/engagement, moderation and media library.
3. Tenant Configuration — organization settings, content types, moderation rules, schedules, locale and integrations.
4. Brand Theme — product name, logo, colors, typography, icons, splash assets and store metadata.

## Current Implementation
SAKINAH LIVE should be retained as one themed/reference deployment instead of defining the reusable mobile product.

## Target Product Name
**Live Broadcast Studio**

## Target Repository Name
`live-broadcast-studio-android`

## Short Description
A modular Android live-broadcast and guided-content studio for organizations, educators and media initiatives.

## Migration Rule
Preserve the current SAKINAH experience while moving brand- and content-specific assumptions into configuration and theme layers.