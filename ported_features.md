# Ported features

- Generic math, file, web, input, enum, cycle, event, threading, player/world, NBT, OS, terminal, user, option, property, FFmpeg, reload, lifecycle, and window utilities from `fancymenu/util`.
- Full Markdown parser/renderer, including tables, links, images, formatting, scrolling, preprocessing, and renderer hooks.
- Full generic placeholder framework: namespace-aware registry/parser, 160 reusable built-ins, provider hooks, legacy `_fm` aliases, lifecycle handling, and 10 complete locales.
- Full reusable resource system: resource indexing/reload/preload, file types, text/audio/image/GIF/APNG/video handlers, FMA/AFMA decoding, AFMA creator tools/UI, ownership cleanup, and configurable safety limits.
- Generic packet/networking framework for Fabric and NeoForge: codecs, registry, handshakes/capability negotiation, chunked bridge transport, lifecycle isolation, limits, and explicit send results.
- Full reusable Rinku/browser bridge and superclass video player stack, with trusted-origin JavaScript bridge, placeholder adapter, input/audio/load lifecycle, configurable dispatch, and optional gated Mixins.
- Full reusable Watermedia bridge and superclass media backend stack, including deferred release, reflection-safe availability, OpenGL/Vulkan interop, and optional gated Mixins.
- Optional reflection-backed Fancy Entity Renderer player-widget bridge with no unpublished runtime/build dependency.
- Reusable rendering/UI utilities, shaders, fonts, icons, cursors, widgets, dialogs, context menus, smooth text, blur, and vanilla-widget customization hooks.
- Platform/service extensions for Fabric and NeoForge, including immutable `UniversalModContainer` metadata, registry/resource enumeration, branding, and loader compatibility defaults.
- Related accessors, behavior Mixins, access widening/transformers, loader entrypoints, reload/tick/shutdown integration, optional-mod gates, assets, translations, and third-party notices.

# Skipped features

- `util/auth/ModValidator` and its tests: FancyMenu genuine-build/JAR validation.
- `util/resource/preload/ManageResourcePreLoadWindowBody`: FancyMenu-specific PiP/configuration editor; the generic preloader engine is ported.
- FancyMenu's concrete application packets; the reusable networking framework and remote-placeholder provider boundary are ported.
- Product-only placeholders for FancyMenu variables, layout elements, text fields/buttons, audio/video elements, and video backgrounds.
- Product-only customization, editor, screen/background/layout, music-controller, action, and listener portions of otherwise shared Mixins.
- Dead or product-only upstream assets and hooks, including Buddy/editor/music/cape/logo/panorama resources and the unused CreateWorld `UniqueLabeledSwitchCycleButton` chain.

# Impossible features

- None.
