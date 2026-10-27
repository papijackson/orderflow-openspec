# Spec Delta

## Purpose

Gives REST clients a predictable error representation while keeping internal implementation and sensitive data out of public responses.

## ADDED Requirements

### Requirement: Handled REST errors use a consistent problem representation
The application SHALL represent handled REST errors using RFC 9457 Problem Details with the `application/problem+json` media type, and the response status SHALL match the problem status. This requirement assumes Spring `ProblemDetail` as the implementation; confirm the public format during review.

#### Scenario: A handled REST error is returned
- **WHEN** a REST request fails with an error handled by the application
- **THEN** the response uses the `application/problem+json` media type
- **AND** its problem status matches the HTTP response status

### Requirement: Invalid REST input returns a safe client error
The application SHALL return HTTP 400 for request-body validation failures and SHALL provide useful validation information without echoing sensitive input values.

#### Scenario: A request body fails validation
- **WHEN** a client submits a request body that violates a declared input constraint
- **THEN** the application returns HTTP 400 using the consistent problem representation
- **AND** the response identifies the validation failure without exposing sensitive submitted values

### Requirement: Unexpected REST failures do not expose internals
The application SHALL return a generic HTTP 500 problem for an unexpected server failure and SHALL NOT expose stack traces, SQL, credentials, or internal infrastructure details in the response.

#### Scenario: An unexpected server failure occurs
- **WHEN** a REST request causes an unhandled technical failure
- **THEN** the client receives HTTP 500 using the consistent problem representation
- **AND** the response contains no stack trace, SQL, credential, or infrastructure detail
