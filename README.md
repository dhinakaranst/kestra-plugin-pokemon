<p align="center">
  <a href="https://www.kestra.io">
    <img src="https://kestra.io/banner.png"  alt="Kestra workflow orchestrator" />
  </a>
</p>

<h1 align="center" style="border-bottom: none">
    Event-Driven Declarative Orchestrator
</h1>

<div align="center">
 <a href="https://github.com/kestra-io/kestra/releases"><img src="https://img.shields.io/github/tag-pre/kestra-io/kestra.svg?color=blueviolet" alt="Last Version" /></a>
  <a href="https://github.com/kestra-io/kestra/blob/develop/LICENSE"><img src="https://img.shields.io/github/license/kestra-io/kestra?color=blueviolet" alt="License" /></a>
  <a href="https://github.com/kestra-io/kestra/stargazers"><img src="https://img.shields.io/github/stars/kestra-io/kestra?color=blueviolet&logo=github" alt="Github star" /></a> <br>
<a href="https://kestra.io"><img src="https://img.shields.io/badge/Website-kestra.io-192A4E?color=blueviolet" alt="Kestra infinitely scalable orchestration and scheduling platform"></a>
<a href="https://kestra.io/slack"><img src="https://img.shields.io/badge/Slack-Join%20Community-blueviolet?logo=slack" alt="Slack"></a>
</div>

<br />

<p align="center">
    <a href="https://twitter.com/kestra_io"><img height="25" src="https://kestra.io/twitter.svg" alt="twitter" /></a> &nbsp;
    <a href="https://www.linkedin.com/company/kestra/"><img height="25" src="https://kestra.io/linkedin.svg" alt="linkedin" /></a> &nbsp;
<a href="https://www.youtube.com/@kestra-io"><img height="25" src="https://kestra.io/youtube.svg" alt="youtube" /></a> &nbsp;
</p>

<br />
<p align="center">
    <a href="https://go.kestra.io/video/product-overview" target="_blank">
        <img src="https://kestra.io/startvideo.png" alt="Get started in 4 minutes with Kestra" width="640px" />
    </a>
</p>
<p align="center" style="color:grey;"><i>Get started with Kestra in 4 minutes.</i></p>


# Kestra Sifflet Plugin

This plugin provides integration with Sifflet's data quality platform, allowing you to execute Sifflet rules from your Kestra workflows.

## Features

- Execute Sifflet rules and monitor their results
- Seamless integration with Kestra's workflow engine
- Secure API key management using Kestra secrets

## Installation

Add the plugin to your Kestra instance by adding the following dependency to your `plugins.yml`:

```yaml
plugins:
  - name: kestra-plugin-sifflet
    group: io.kestra.plugin
    version: 1.0.0
```

## Tasks

### RunRule

The `RunRule` task allows you to execute a Sifflet rule and get its execution status.

#### Properties

| Property | Type | Required | Description |
|----------|------|----------|-------------|
| `apiKey` | String | Yes | Sifflet API key. Should be stored as a Kestra secret. |
| `ruleId` | String | Yes | The ID of the Sifflet rule to execute. |

#### Example

```yaml
id: run-sifflet-rule
type: io.kestra.plugin.templates.RunRule
apiKey: "{{ secret('SIFFLET_API_KEY') }}"
ruleId: "data-quality-rule-123"
```

#### Output

The task returns an output with the following properties:

| Property | Type | Description |
|----------|------|-------------|
| `status` | String | The execution status ("success" or "error") |
| `message` | String | A descriptive message about the execution result |

## Usage Examples

### Basic Rule Execution

```yaml
id: run-sifflet-rule
type: io.kestra.plugin.templates.RunRule
apiKey: "{{ secret('SIFFLET_API_KEY') }}"
ruleId: "data-quality-rule-123"
```

### Error Handling

The task will throw an `IllegalVariableEvaluationException` if:
- The API key is invalid
- The rule ID doesn't exist
- The API request fails
- The rule execution fails

You can handle these errors in your flow:

```yaml
id: run-sifflet-rule
type: io.kestra.plugin.templates.RunRule
apiKey: "{{ secret('SIFFLET_API_KEY') }}"
ruleId: "data-quality-rule-123"
error:
  - type: io.kestra.core.exceptions.IllegalVariableEvaluationException
    then:
      - type: io.kestra.core.tasks.log.Log
        message: "Failed to execute Sifflet rule: {{ error.message }}"
```

## Development

### Building

To build the plugin, run:

```bash
./gradlew shadowJar
```

### Testing

To run the tests:

```bash
./gradlew test
```

## License

This plugin is licensed under the Apache License 2.0.

# Kestra Plugin Template

> A template for creating Kestra plugins

This repository serves as a general template for creating a new [Kestra](https://github.com/kestra-io/kestra) plugin. It should take only a few minutes! Use this repository as a scaffold to ensure that you've set up the plugin correctly, including unit tests and CI/CD workflows.

![Kestra orchestrator](https://kestra.io/video.gif)

## Running the project in local
### Prerequisites
- Java 21
- Docker

### Running tests
```
./gradlew check --parallel
```

### Launching the whole app
```
./gradlew shadowJar && docker build -t kestra-custom . && docker run --rm -p 8080:8080 kestra-custom server local
```
> [!NOTE]
> You need to relaunch this whole command everytime you make a change to your plugin

go to http://localhost:8080, your plugin will be available to use

## Documentation
* Full documentation can be found under: [kestra.io/docs](https://kestra.io/docs)
* Documentation for developing a plugin is included in the [Plugin Developer Guide](https://kestra.io/docs/plugin-developer-guide/)


## Stay up to date

We release new versions every month. Give the [main repository](https://github.com/kestra-io/kestra) a star to stay up to date with the latest releases and get notified about future updates.

![Star the repo](https://kestra.io/star.gif)
