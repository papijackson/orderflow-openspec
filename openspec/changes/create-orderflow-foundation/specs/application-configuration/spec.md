# Spec Delta

## Purpose

Allows OrderFlow to run with environment-specific settings supplied outside the application artifact, while rejecting invalid runtime configuration safely.

## ADDED Requirements

### Requirement: Environment-specific settings are externalized
The application SHALL allow deployment-specific non-secret settings, including persistence connection settings, to be supplied externally, so an environment can be configured without changing or rebuilding application source.

#### Scenario: An environment supplies an override
- **WHEN** the application starts with a valid externally supplied setting that overrides its default
- **THEN** the application uses the supplied value for that runtime
- **AND** the application artifact remains unchanged

#### Scenario: Persistence endpoint differs by environment
- **WHEN** the application starts in an environment with an externally supplied database endpoint
- **THEN** the application uses that endpoint for its persistence connection
- **AND** the endpoint is not embedded in application source

### Requirement: Invalid required configuration prevents startup
The application SHALL fail startup when a required setting is missing or invalid, and its diagnostic SHALL identify the setting without disclosing secret values.

#### Scenario: A required setting is missing
- **WHEN** the application starts without a required setting
- **THEN** startup fails with a diagnostic identifying the missing setting
- **AND** the diagnostic does not contain secret values

#### Scenario: A setting has an invalid value
- **WHEN** the application starts with a value that violates the setting's declared constraints
- **THEN** startup fails with a diagnostic identifying the invalid setting

### Requirement: Secrets are not embedded in application configuration
The application SHALL obtain credentials and other secrets through external runtime configuration rather than committing their values in source-controlled application configuration.

#### Scenario: Runtime credentials are supplied externally
- **WHEN** a runtime needs a credential
- **THEN** the credential is obtained from the approved external configuration mechanism
- **AND** no credential value is required in a tracked configuration file
