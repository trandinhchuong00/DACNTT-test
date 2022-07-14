@logout @regression
Feature: Login


  Scenario Outline: Logout of account
    Given the user is on the login page
    When the user logins with account
      | username    | password   |
      | <username>  | <password> |
    And the user logs out
    Then the user should see the "Login" page
    Examples:
      | username | password |
      | admin_1  | 1234567  |
      | chuongt  | 1234567  |