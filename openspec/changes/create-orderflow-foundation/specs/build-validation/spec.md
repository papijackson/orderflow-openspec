# Spec Delta

## Purpose

Provides repeatable build and test feedback for changes to OrderFlow, including automated enforcement of the declared application architecture.

## ADDED Requirements

### Requirement: Pull requests and pushes receive build and test validation
The GitHub Actions validation workflow SHALL run the Maven build and automated test suite for pull requests and pushes, and SHALL report failure when any required build or test check fails.

#### Scenario: A pull request passes required checks
- **WHEN** a pull request triggers the validation workflow and all required build and test checks pass
- **THEN** the workflow reports success for the validation checks

#### Scenario: A required test fails
- **WHEN** a pull request or push triggers the validation workflow and a required automated test fails
- **THEN** the workflow reports failure
- **AND** the failure prevents the validation check from being reported as successful

### Requirement: Architecture rules are part of automated validation
The automated validation suite SHALL execute architecture checks that verify the declared application dependency boundaries.

#### Scenario: An architecture rule is violated
- **WHEN** a compiled application component violates a declared architecture boundary
- **THEN** the validation suite fails and identifies the failing architecture check

### Requirement: Build validation does not deploy the application
The build validation workflow SHALL validate source changes without deploying the application or invoking production deployment steps.

#### Scenario: A change passes build validation
- **WHEN** the build and test checks complete successfully
- **THEN** the workflow reports validation results only
- **AND** no production deployment is performed
