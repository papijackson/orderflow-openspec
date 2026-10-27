# Spec Delta

## Purpose

Keeps the initial application structure understandable and prevents dependencies from crossing the documented web, application, persistence, and entity responsibilities.

## ADDED Requirements

### Requirement: Application responsibilities have explicit package boundaries
When an application responsibility exists, the application SHALL place it beneath the agreed base package in a package that reflects its layer, and SHALL create packages only for responsibilities that exist.

#### Scenario: An application component is added
- **WHEN** a controller, service, repository, entity, exception handler, or configuration component is introduced
- **THEN** it is placed in the corresponding package beneath the agreed application base package
- **AND** no empty package or layer is required solely to match a template

### Requirement: Layer dependencies respect application responsibilities
The application SHALL keep controllers, services, repositories, and entities within the documented dependency boundaries: controllers do not access repositories; services do not depend on web components; repositories do not depend on services or controllers; and entities do not depend on application or infrastructure components.

#### Scenario: Architecture checks find a forbidden dependency
- **WHEN** a compiled application component introduces a forbidden dependency between layers
- **THEN** the architecture test suite fails and identifies the violating dependency

#### Scenario: Architecture checks find no forbidden dependency
- **WHEN** the compiled application components follow the documented dependency boundaries
- **THEN** the architecture test suite passes
