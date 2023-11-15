Feature: Broadcasting messages

  Background:
    Given the driver has been started

  Scenario: The publisher sends messages to the subscribers and the subscriber gets them
    Then the publisher connects
    Then the publisher sends "PUBLISHER"
    Then the publisher receives "Hello PUBLISHER"
    Then the subscriber connects
    Then the subscriber sends "SUBSCRIBER"
    Then the subscriber receives "Hello SUBSCRIBER"
    Then the publisher sends "Good evening"
    Then the subscriber receives "Good evening" in JSON format
    Then end