Feature: Broadcasting messages

  Background:
    Given the driver has been started

  Scenario: The publisher sends messages to the subscribers and the subscriber gets them
    When the publisher connects and gets "Hello publisher"
#    Then the subscriber connects and gets "Hello subscriber"
#    Then the publisher sends "Good evening"