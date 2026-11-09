# Mazebound Portal

A static front-end foundation for the Mazebound player portal.

## Preview locally

Open `index.html` in a browser, or serve this folder with any static web server.

## Current state

- Responsive portal layout and navigation
- Overview dashboard
- Personal exploration page placeholder
- Leaderboards and profile placeholders
- No fabricated player statistics or maze geometry

## Planned integration

The Paper plugin already tracks discovered maze cell IDs per Minecraft UUID and stores them in player YAML files. The next integration stage should:

1. Add an authenticated backend API.
2. Publish the generated maze cell layout from the plugin or a shared source.
3. Send a player's newly discovered cell IDs to the backend.
4. Link website accounts to Minecraft UUIDs with a secure verification flow.
5. Return only the authenticated player's discovered cells to the website.

Do not expose Minecraft server credentials or accept arbitrary UUIDs from unauthenticated browser requests.

## Deployment

This folder can be deployed as a static site. Configure the hosting provider's publish/root directory as `Website`.
