Feature: The driver receives "publisher" and "subscriber" connections. When one of the publishers sends a message
  then all the subscribers get the message in JSON format. When a client connects it can indicate to the driver
  whether it is a publisher or a subscriber by sending an initial "PUBLISHER" or "SUBSCRIBER" message

  Background:
    Given the driver has been started

  Scenario: The publisher sends a message to the subscriber and the subscriber gets it
    When the publisher connects
    And the publisher sends "PUBLISHER"
    And the publisher receives "Hello PUBLISHER"
    And the subscriber connects
    And the subscriber sends "SUBSCRIBER"
    And the subscriber receives "Hello SUBSCRIBER"
    And the publisher sends "Good evening"
    Then the subscriber receives "Good evening" in JSON format
    And end