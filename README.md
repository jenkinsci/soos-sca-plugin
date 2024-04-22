# SOOS SCA Plugin

<img src="assets/SOOS_logo.png" style="margin-bottom: 10px;" width="350" alt="SOOS Icon">

SOOS is the affordable, easy-to-integrate Software Composition Analysis solution for your whole team.

Use SOOS to scan your software for [vulnerabilities](https://app.soos.io/research/vulnerabilities) and [open source license](https://app.soos.io/research/licenses) issues with [SOOS Core SCA](https://soos.io/sca-product). [Generate SBOMs](https://kb.soos.io/help/soos-reports-for-export). Govern your open source dependencies. Run the [SOOS DAST vulnerability scanner](https://soos.io/dast-product) against your web apps or APIs.

[Demo SOOS](https://app.soos.io/demo) or [Register for a Free Trial](https://app.soos.io/register).

If you maintain an Open Source project, sign up for the Free as in Beer [SOOS Community Edition](https://soos.io/products/community-edition).

## How to Use

The **SOOS SCA Plugin** will locate and analyze any supported manifest files under the specified directory.

To use SOOS SCA Plugin you need to:

1. [Install the SOOS SCA Plugin](#install-the-soos-sca-plugin)
2. [Configure authorization](#configure-authorization)
3. [Configure other plugin parameters](#configure-other-plugin-parameters)

## Supported Languages and Package Managers

* C++ - Conan
* Cargo - Rust
* Composer - PHP
* Dart PM (Pub Package Manager) - Dart
* Go Modules - GoLang
* Gradle - Java & Kotlin
* Homebrew - (various languages)
* Maven - Java
* Mix - Elixir
* NuGet - .NET
* NPM (Node Package Manager) - Node 
* PyPI - Python 
* Rebar3 - Erlang
* Ruby Gems - Ruby

Our full list of supported manifest formats can be found [here](https://kb.soos.io/help/soos-languages-supported).

## Need an Account?
**Visit [soos.io](https://app.soos.io/register) to create your trial account.**

## Setup

### Install the SOOS SCA Plugin

Install or upgrade the SOOS SCA Plugin from Jenkins Plugin Manager with these steps. Once complete, you’re all set to add a SOOS SCA step to your projects.

Log in to your Jenkins instance to install the SOOS SCA Plugin. Navigate to **Manage Jenkins > Manage Plugins** and select **Available** tab. Search for **SOOS SCA** and from list, check the radio button to install the plugin for your Jenkins installation. After that just need to click the install button.

<blockquote style="margin-bottom: 10px;">
<details>
<summary> Show example </summary>

<img src="assets/prompt-image-to-show.png" style="margin-top: 10px; margin-bottom: 10px;" alt="Prompt-image-to-show">

</details>
</blockquote>

To manually install the plugin you have two options:

1.  Download and copy the plugin .hpi file into <jenkins-home>/plugins/, and restart the server.
2.  Download the .hpi file, log in to your Jenkins instance, go to **Manage Jenkins > Manage Plugins** and select **Advanced** tab. Just choose the **soos-sca.hpi** file, and click the Upload button to install it.

<blockquote style="margin-bottom: 10px;">
<details>
<summary> Show example </summary>

<img src="assets/upload-plugin-hpi-example.png" style="margin-top: 10px; margin-bottom: 10px;" alt="Upload Plugin Example">

</details>
</blockquote>

### Configure authorization

**SOOS SCA** needs environment variables which are passed as parameters. These environment variables are stored by checking "Environment variables" on **Manage Jenkins > Configure System > Global Properties**, and they are required for the plugin to operate.

| Property | Description |
| --- | --- |
| SOOS_CLIENT_ID | Provided to you when subscribing to SOOS services. |
| SOOS_API_KEY | Provided to you when subscribing to SOOS services. |

These values can be found in the SOOS App under Integrate.

### Configure other plugin parameters

<blockquote style="margin-bottom: 10px;">
<details>
<summary> Show parameters </summary>

| Select/Inputs                    | Default | Description                                                                                                                                                                                                                                   |
|----------------------------------| --- |-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Project Name                     | ""  | REQUIRED. A custom project name that will present itself as a collection of test results within your soos.io dashboard.                                                                                                                       |
| Directories To Exclude           | ""  | List (comma separated) of directories (relative to ./) to exclude from the search for manifest files. Example - Correct: bin/start/ ... Example - Incorrect: ./bin/start/ ... Example - Incorrect: /bin/start/'                               |
| Files To Exclude                 | ""  | List (comma separated) of files (relative to ./) to exclude from the search for manifest files. Example - Correct: bin/start/manifest.txt ... Example - Incorrect: ./bin/start/manifest.txt ... Example - Incorrect: /bin/start/manifest.txt' |
| On Failure                       | "Fail the build"  | Stop the building in case of failure, alternative: "Continue on failure"                                                                                                                                                                      |
| Analysis Result Max Wait         | 300  | Maximum seconds to wait for Analysis Result before exiting with error.                                                                                                                                                                        |
| Analysis Result Polling Interval | 10  | Polling interval (in seconds) for analysis result completion (success/failure.). Min 10.                                                                                                                                                      |
| API Base URL                     | "https://api.soos.io/api/"  | The API BASE URI provided to you when subscribing to SOOS services.                                                                                                                                                                           |
| Package Manager                  | ""  | Comma separated list of names of Package Managers to filter manifest search.                                                                                                                                                                  |

</details>
</blockquote>

## Feedback and Support
### Knowledge Base
[Go To Knowledge Base](https://kb.soos.io/help)

