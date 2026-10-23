<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Goctl-plugin Changelog

## [Unreleased]

### Fixed
- Resolve shared types in split App/Web API files through ancestor entry points that import them, including declaration navigation and unresolved-type highlighting.
- Generate lexer and PSI sources during clean builds and use Java 21 for the existing pattern-switch syntax.

### Added
- Regression tests for explicit, transitive, cyclic and entry-point imports, missing types, unrelated API scopes and editing entry points.
- Optional `-PlocalPlatformPath=/path/to/GoLand.app` for building against an installed IDE.
