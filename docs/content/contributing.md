---
title: Contributing
description: How to contribute to the WildFly AI Feature Pack project.
layout: page
---

# Contributing

Thank you for your interest in contributing to the WildFly AI Feature Pack! This guide will help you get started.

## Prerequisites

- JDK 11+
- Git
- Maven 3.3.9+
- An IDE of your choice

## Setup

1. Fork the [wildfly-ai-feature-pack](https://github.com/wildfly/wildfly-ai-feature-pack) repository on GitHub.

2. Clone your fork:

```bash
git clone https://github.com/YOUR_USERNAME/wildfly-ai-feature-pack.git
cd wildfly-ai-feature-pack
```

3. Add the upstream remote:

```bash
git remote add upstream https://github.com/wildfly/wildfly-ai-feature-pack
```

4. Build the project:

```bash
mvn clean install
```

To skip tests:

```bash
mvn clean install -DskipTests=true
```

## Guidelines

1. **Squash commits** -- In general, squash your commits into a single commit per PR. For larger changes, multiple meaningful commits are acceptable.

2. **Reference issues** -- Include the GitHub issue number in both your PR title and commit message.

3. **Link issues** -- Include a link to the GitHub issue in the PR description.

## Resources

- [Git Setup](https://github.com/wildfly/wildfly/blob/main/docs/src/main/asciidoc/_hacking/github_setup.adoc)
- [Contributing Guide](https://github.com/wildfly/wildfly/blob/main/docs/src/main/asciidoc/_hacking/contributing.adoc)
- [Pull Request Standards](https://github.com/wildfly/wildfly/blob/main/docs/src/main/asciidoc/_hacking/pullrequest_standards.adoc)

## Issues

All issues are tracked on [GitHub Issues](https://github.com/wildfly/wildfly-ai-feature-pack/issues).

## Community

- [Twitter / X](https://twitter.com/WildFlyAS)
- [Mastodon](https://fosstodon.org/@wildflyas)
- [YouTube](https://www.youtube.com/@WildFlyAS)
