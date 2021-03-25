# Kong Dev Portal User Provisioner Action
[![Quality](https://img.shields.io/badge/quality-experiment-red)](https://curity.io/resources/code-examples/status/)
[![Availability](https://img.shields.io/badge/availability-source-blue)](https://curity.io/resources/code-examples/status/)

A custom authentication action plugin for the Curity Identity Server that automates the provisioning of a user to the Kong Dev Portal.

## System Requirements

The plugin is only tested with Curity Identity Server 6.0.0 but should work with older version also. More details in the [Systems Requirement](https://developer.curity.io/docs/latest/system-admin-guide/system-requirements.html) section of the product documentation.

## Building the Plugin

You can build the plugin by issuing the command ``mvn package``. This will produce JAR file(s) in the ``target`` directory,
which can be installed.

## Installing the Plugin

To install the plugin, copy the compiled JAR (and all of its dependencies) into the :file:`${IDSVR_HOME}/usr/share/plugins/${pluginGroup}` on each node, including the admin node. For more information about installing plugins, refer to the [Product Documentation](https://support.curity.io/docs/latest/developer-guide/plugins/index.html#plugin-installation).

## Configuration

There are only two configuration parameters:

1. An Http Client is needed. This will be used as the client to communicate with the Kong Dev Portal API. By default the HTTP Authentication of the Http Client can be left as-is. The scheme (http/https) needs to match the Kong dev Portal URL (Se next config parameter).
2. The Kong Dev Portal URL needs to be set. This is the host that the Kong Dev Portal is running on and by default uses the `/register` endpoint.

![Configure Action](etc/kong-dev-portal-action.png?raw=true "Configure Action")

## How does it work?
The plugin needs to pass two attributes in order to successfully register a user in the Kong Dev Portal, the users full name and the email address. The payload that the plugin sends to register the user looks like this:

```json
{
    "email": "bob@example.com", 
    "meta": "{\"full_name\": \"Bob Builder\"}"
}
```

The plugin is currently operating with the Curity Identity Server default account table schema in order to resolve the information needed. The email is readily available in a column. The full name of the user is not, however. The default schema of the column containing first and last name looks is the attributes column and contains a JSON blog.

```json
{"emails":[{"value":"alice@example.com","primary":true}],"phoneNumbers":[{"value":"555-123-1234","primary":true}],"name":{"givenName":"alice","familyName":"anderson"},"agreeToTerms":"on","urn:se:curity:scim:2.0:Devices":[]}
```

The plugin will parse this structure in order to send the full name in the provisioning call to the Kong Dev Portal API.


## More Information

Please visit [curity.io](https://curity.io/) for more information about the Curity Identity Server.
