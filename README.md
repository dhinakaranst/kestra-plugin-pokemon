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


## License
Apache 2.0 © [Kestra Technologies](https://kestra.io)


## Stay up to date

We release new versions every month. Give the [main repository](https://github.com/kestra-io/kestra) a star to stay up to date with the latest releases and get notified about future updates.

![Star the repo](https://kestra.io/star.gif)

# Sifflet Plugin for Kestra

This plugin provides integration with Sifflet's data quality platform, allowing you to run data quality rules from your Kestra workflows.

## Installation

Add the plugin to your Kestra installation by adding the following to your `kestra.yml`:

```yaml
plugins:
  - io.kestra.plugin:sifflet:0.1.0
```

## Tasks

### RunRule

The `RunRule` task allows you to execute a Sifflet rule and wait for its completion.

#### Example

```yaml
id: run-sifflet-rule
type: io.kestra.plugin.sifflet.tasks.RunRule
url: https://api.siffletdata.com
apiKey: "{{ secret('SIFFLET_API_KEY') }}"
ruleId: "rule-123"
```

#### Properties

| Property        | Type    | Description                                    | Required | Default |
|-----------------|---------|------------------------------------------------|----------|---------|
| url             | string  | The base URL for Sifflet API                   | Yes      | -       |
| apiKey          | string  | Your Sifflet API key                           | Yes      | -       |
| ruleId          | string  | The ID of the rule to run                      | Yes      | -       |
| pollingInterval | integer | Seconds between status checks                  | No       | 5       |
| timeout         | integer | Maximum seconds to wait for completion         | No       | 3600    |

#### Outputs

| Property     | Type   | Description                    |
|--------------|--------|--------------------------------|
| executionId  | string | The ID of the rule execution   |
| status       | string | The final status of execution  |

### ListRules

The `ListRules` task allows you to retrieve a list of rules from Sifflet.

#### Example

```yaml
id: list-sifflet-rules
type: io.kestra.plugin.sifflet.tasks.ListRules
url: https://api.siffletdata.com
apiKey: "{{ secret('SIFFLET_API_KEY') }}"
pageSize: 100
pageNumber: 1
```

#### Properties

| Property   | Type    | Description                                    | Required | Default |
|------------|---------|------------------------------------------------|----------|---------|
| url        | string  | The base URL for Sifflet API                   | Yes      | -       |
| apiKey     | string  | Your Sifflet API key                           | Yes      | -       |
| pageSize   | integer | Number of rules to return per page             | No       | 100     |
| pageNumber | integer | Page number to retrieve                        | No       | 1       |

#### Outputs

| Property    | Type                | Description                    |
|-------------|---------------------|--------------------------------|
| rules       | List<Rule>          | List of rules                  |
| totalCount  | integer             | Total number of rules          |
| pageSize    | integer             | Number of rules per page       |
| pageNumber  | integer             | Current page number            |

#### Rule Object

| Property    | Type   | Description                    |
|-------------|--------|--------------------------------|
| id          | string | Rule ID                        |
| name        | string | Rule name                      |
| description | string | Rule description               |
| status      | string | Rule status                    |
| createdAt   | string | Creation timestamp             |
| updatedAt   | string | Last update timestamp          |

## Error Handling

The tasks handle various error scenarios:

1. **API Authentication Errors**
   - HTTP 401 responses
   - Invalid API keys
   - Solution: Verify your API key is correct and has proper permissions

2. **Rule Execution Errors**
   - Invalid rule IDs
   - Rule execution failures
   - Solution: Check the rule ID and ensure the rule is properly configured in Sifflet

3. **Timeout Errors**
   - Rule execution takes longer than the specified timeout
   - Solution: Increase the timeout value or investigate why the rule is taking longer than expected

4. **JSON Parsing Errors**
   - Invalid response format from Sifflet API
   - Solution: Contact Sifflet support if this occurs

## Best Practices

1. **API Key Security**
   - Always use Kestra secrets to store your Sifflet API key
   - Never hardcode API keys in your workflows
   - Rotate API keys regularly

2. **Timeout Configuration**
   - Set appropriate timeout values based on your rule complexity
   - Consider using longer timeouts for complex rules
   - Monitor execution times to optimize timeout settings

3. **Error Handling**
   - Implement proper error handling in your workflows
   - Use Kestra's error handling features to manage failures
   - Monitor failed executions and investigate root causes

## Development

To build the plugin:

```bash
./gradlew build
```

## License

Apache 2.0
