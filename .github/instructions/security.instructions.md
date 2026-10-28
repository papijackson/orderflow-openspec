---
applyTo: "**/src/main/java/**/*.java,**/src/test/java/**/*.java,**/src/main/resources/**/*.yml,**/src/main/resources/**/*.yaml,**/src/main/resources/**/*.properties,**/*.xml"
---

# Security Instructions

## Principles
Secure by default:
- least privilege;
- validate untrusted input;
- authenticate and authorize;
- fail securely;
- protect secrets;
- patch dependencies;
- make security controls testable.

## Input Validation
Use Jakarta Bean Validation at external boundaries.

Validate:
- type;
- length;
- range;
- format;
- allowed values;
- collection size;
- payload size.

Business invariants belong in service/entity logic.

## Authentication vs Authorization
Authentication establishes identity. Authorization determines allowed actions.

Never trust client-provided roles, user IDs, or permissions. Use the trusted security context.

Test both unauthorized and forbidden behavior.

## REST
Review:
- authentication;
- authorization;
- validation;
- payload limits;
- error disclosure;
- rate limiting where relevant;
- CORS/CSRF based on application model;
- secure headers;
- content type.

## Errors
Centralized error responses must not disclose:
- stack traces;
- SQL;
- database details;
- hostnames;
- credentials;
- internal class names;
- sensitive implementation details.

Log useful diagnostics securely on the server side.

## Secrets
Never store passwords, API keys, AWS credentials, tokens, private keys, or certificates in:
- source code;
- Git;
- logs;
- metrics;
- tests committed to source;
- screenshots/images.

Use approved secret/configuration mechanisms.

## AWS IAM
Apply least privilege. Distinguish:
- ECS task execution role;
- application task role;
- deployment permissions.

Avoid wildcard permissions unless explicitly justified.

## Database
Use parameterized JPA/SQL queries. Never concatenate untrusted values into SQL.

Dynamic sort/filter fields must be allowlisted.

Use least-privilege DB accounts.

## JPA / Deserialization
Review:
- JPQL/native SQL injection;
- entity binding;
- mass assignment;
- unsafe polymorphic deserialization;
- overly permissive type handling.

Do not enable unsafe deserialization shortcuts.

## Messaging
Validate message payloads. Design for:
- replay;
- duplicates;
- malicious payloads;
- poison messages;
- malformed contracts;
- sensitive data minimization.

## Dependencies
Scan and patch vulnerable dependencies. Do not blindly suppress vulnerability findings.

## Cryptography
Use established platform/library primitives. Never implement custom cryptography.

Do not store passwords in plaintext. Use secure password hashing when passwords are actually managed by the application.

Avoid obsolete algorithms and predictable randomness.

## Logging and Metrics
Never put secrets or sensitive identifiers into logs or metric tags.

## Files and Resources
Validate:
- file names;
- path boundaries;
- content types;
- size;
- storage destinations.

Prevent path traversal.

## SSRF
For untrusted URLs:
- allowlist protocols/hosts where possible;
- prevent access to internal infrastructure;
- validate redirects;
- use explicit timeouts.

## Security Testing
Test:
- unauthorized;
- forbidden;
- malformed input;
- injection;
- secret leakage;
- path traversal;
- SSRF where relevant;
- sensitive error disclosure.

## AI Review
Ask:
1. What inputs are untrusted?
2. Is authentication required?
3. Is authorization enforced server-side?
4. Can a user access another user's resource?
5. Are SQL/JPA queries safe?
6. Are secrets exposed?
7. Are logs/metrics safe?
8. Are AWS permissions minimal?
9. Are dependencies vulnerable?
10. Are failure responses safe?

## Validation
No security-sensitive change is complete without appropriate tests and review.
## Authorization Object Ownership
For resource APIs, verify that the authenticated principal is authorized for the specific resource. Do not rely solely on endpoint-level roles when ownership is also required.

## Mass Assignment
Do not bind untrusted request fields directly onto entities when clients could alter fields they should not control, such as status, owner, audit fields, or internal identifiers.

## Dependency Security
When adding or upgrading a dependency:
- review vulnerability status;
- prefer maintained versions;
- run dependency scanning;
- do not suppress findings without documented justification.

## Error Handling
Public errors should expose stable categories/codes where useful, not raw exception messages.

## Security in Messages
Assume message payloads can be malformed or replayed. Validate schema and semantic authorization assumptions at the consumer boundary.

## Secrets in Tests
Test credentials must be synthetic and non-sensitive. Never copy production credentials into fixtures.

## Compléments détaillés v1.0.2
## 14. Detailed security review rules

### 14.1 Authentication, authorization, and ownership
- Authentication establishes identity; authorization decides whether that identity may perform the requested action.
- Enforce authorization server-side on every protected use case, not only in the UI or controller routing.
- Check object-level ownership/tenant boundaries when accessing records by identifier; knowing an ID must not grant access.
- Use least privilege for application roles, database users, AWS task roles, and deployment roles.
- Deny by default when authorization context is missing or cannot be verified.

### 14.2 Validation and mass assignment
- Treat all HTTP, queue, batch-file, database-import, and external-service payloads as untrusted at their boundaries.
- Use allowlists for writable fields and dynamic sort/filter properties.
- Do not bind external request objects directly to JPA entities where it could let callers alter IDs, ownership, status, privilege, or audit fields.
- Enforce business invariants in the service/entity and database constraints where concurrent writes could violate them.
- Bound payload size, collection size, string length, nesting, and parsing cost.

### 14.3 Error handling and data exposure
- Return stable, minimal error responses; never return stack traces, SQL, database details, internal hostnames, credentials, or implementation internals.
- Keep detailed diagnostics in protected logs with sensitive values redacted.
- Avoid revealing whether a protected resource exists when the security model requires a uniform response.
- Do not swallow exceptions and continue as if a security-sensitive operation succeeded.

### 14.4 Secrets and dependency supply chain
- Never commit credentials, tokens, private keys, certificates with secrets, or production data.
- Use the approved secret manager/configuration mechanism and rotate compromised credentials.
- Keep dependencies on supported versions; use repository-approved dependency/security scanning.
- Review transitive vulnerabilities and license constraints where applicable.
- Do not suppress a vulnerability without a documented risk assessment, mitigation, owner, and review date.

### 14.5 AWS, database, and messaging security
- Separate ECS task execution permissions from application runtime permissions and deployment permissions.
- Scope IAM actions/resources to the minimum required; wildcard permissions require explicit justification.
- Use parameterized JPA/SQL queries; never concatenate untrusted input into SQL.
- Validate message schema and producer trust; assume messages can be duplicated, delayed, malformed, replayed, or malicious.
- Avoid placing secrets or unnecessary personal data in events. Encrypt transport/storage according to the platform standard.
- Protect DLQs and replay procedures because they may contain sensitive or poisoned payloads.

### 14.6 SSRF, files, and serialization
- If users or messages can supply URLs, use protocol/host allowlists, block internal metadata/private network targets as appropriate, constrain redirects, and set timeouts.
- If files are accepted, validate type/size/content and prevent path traversal; never trust filenames as paths.
- Avoid unsafe polymorphic deserialization or permissive type metadata.
- Validate redirect destinations and external callback URLs.

### 14.7 Security test scenarios
Where relevant, test unauthenticated access, forbidden roles, cross-tenant/object ownership, mass assignment, injection, invalid signatures/claims, malicious message payloads, replay/duplicate behavior, SSRF attempts, oversized input, and secret leakage in errors/logs/metrics.

### 14.8 Security Definition of Done
- [ ] Authentication and authorization are explicit at each protected boundary.
- [ ] Object ownership and tenant isolation are tested.
- [ ] External input is validated and bounded.
- [ ] Secrets and sensitive data are not exposed in code, logs, metrics, or responses.
- [ ] IAM and database permissions follow least privilege.
- [ ] Dependency findings and exceptions are documented.
- [ ] Negative security tests pass.
