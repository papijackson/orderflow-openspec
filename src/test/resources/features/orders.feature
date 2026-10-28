Feature: Manage orders
  As an API client
  I want to create and retrieve orders
  So that accepted orders retain their submitted references and items

  Scenario: Create and retrieve an order with opaque references
    Given the customer and product references are not registered
    When the customer places an order for 2 units
    Then the order is created with the submitted references
    When the customer retrieves the order
    Then the order can be retrieved with its item

  Scenario: Reject an order without items
    Given the order contains no items
    When the customer submits the order
    Then the request is rejected with a validation problem

  Scenario: Report an order that does not exist
    Given an order identifier that does not exist
    When the customer requests that missing order
    Then the order is reported as missing

  Scenario: Return the original order for an identical retry
    Given a customer has a new idempotency key
    When the customer places an order for 2 units with that key
    And the customer retries the same order with that key
    Then the original order is returned

  Scenario: Reject changed order data for an existing key
    Given a customer has a new idempotency key
    When the customer places an order for 2 units with that key
    And the customer reuses that key for 3 units
    Then the key reuse is rejected with a conflict problem
