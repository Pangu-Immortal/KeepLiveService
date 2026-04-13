# Security Policy

## About This Project

KeepLiveService (Fw) is an **open-source security research and educational project** that documents and implements Android process keep-alive techniques. The project is intended for:

- Security researchers studying Android process lifecycle management
- Developers who need to understand how background services survive on various Android versions and OEM ROMs
- Educational purposes demonstrating the interaction between applications and the Android framework

**This project does not encourage malicious use.** All strategies are implemented transparently and require explicit opt-in configuration.

## Supported Versions

| Version | Supported          |
|---------|--------------------|
| 2.0.x   | :white_check_mark: |
| 1.x.x   | :x:                |

Only the latest major version receives security updates.

## Reporting a Vulnerability

If you discover a security vulnerability in the KeepLiveService framework, please follow the responsible disclosure process below. **Do not open a public GitHub issue for security vulnerabilities.**

### Responsible Disclosure Process

1. **Email**: Send a detailed report to the project maintainer via the [Telegram group](https://t.me/+V7HSo1YNzkFkY2M1) as a private message to the group admin.
2. **Include**:
   - A clear description of the vulnerability
   - Steps to reproduce the issue
   - The potential impact or severity
   - Any suggested fixes (if available)
3. **Response time**: We aim to acknowledge reports within **72 hours** and provide a resolution timeline within **7 days**.
4. **Disclosure**: We will coordinate with you on a public disclosure timeline after a fix is released. We typically target a **90-day** disclosure window.

### Scope

The following are considered in scope for security reports:

- Vulnerabilities in the framework library (`io.github.pangu-immortal:keeplive-framework`) that could be exploited by a third party
- Privilege escalation beyond what the framework is designed to provide
- Data exfiltration or unintended network communication
- Vulnerabilities in the native C++ layer (daemon process, Binder interactions)

The following are **out of scope**:

- The keep-alive strategies themselves (these are the documented purpose of the project)
- Issues requiring physical access to an unlocked device
- Issues in third-party dependencies (please report those to the respective projects)
- Social engineering attacks

## Security Best Practices for Users

When integrating KeepLiveService into your application:

1. **Only enable strategies you need** -- disable aggressive strategies (e.g., `enableForceStopResistance`, `enableVpnService`) unless required.
2. **Respect user consent** -- always inform users about background services and obtain appropriate permissions.
3. **Follow Google Play policies** -- some strategies may not comply with Google Play's background execution policies. Review the [Google Play policy center](https://support.google.com/googleplay/android-developer/answer/11044org) before publishing.
4. **Keep the framework updated** -- always use the latest version for security patches and compatibility fixes.
5. **Review permissions** -- audit the permissions declared by the framework and remove any that are unnecessary for your use case.

## Acknowledgments

We appreciate the security research community's efforts in making this project safer. Reporters who follow responsible disclosure will be credited in the project's release notes (unless they prefer to remain anonymous).
