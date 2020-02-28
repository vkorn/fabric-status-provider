# Status Provider

Fabric mod for servers that provides player information through a REST api. Requires Fabric API.

## Configuration

Example configuration properties file to place at `config/fabric_status_provider.properties` in the server base directory.

```
# HTTP port to run on
port=8080
# Secret to specify in x-fabric-status-provider header. Leave blank to disable.
secret=
```