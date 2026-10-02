# High-Level Design

The complete HLD is split into focused documents:

- [Architecture](architecture.md): actual components, responsibilities, request flows, caching, click persistence, expiry, failure handling, and security.
- [Capacity Estimation](capacity-estimation.md): assignment traffic assumptions, QPS arithmetic, storage estimate, and the seven-character namespace.
- [Architecture Diagrams](architecture-diagram.md): editable Mermaid component, redirect, and scheduled-job diagrams.

All documents describe the current single Spring Boot implementation. They do not add a load balancer, queue, microservice, authentication system, or rate limiter.
