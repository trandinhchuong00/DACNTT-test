@login @regression
  Feature: Login


   Scenario Outline: Login with valid admin account
      Given the user is on the login page
      When the user logins with account
          | username    | password   |
          | <username>  | <password> |
      Then the user should see the "Home" page
     Examples:
     | username | password |
     | admin_1  | 1234567  |

    Scenario Outline: Login with valid user account
      Given the user is on the login page
      When the user logins with account
        | username    | password   |
        | <username>  | <password> |
      Then the user should see the "Home" page
      Examples:
        | username | password |
        | chuongt  | 1234567  |