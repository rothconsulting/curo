package ch.umb.curo.starter.property

import org.springframework.boot.context.properties.NestedConfigurationProperty

class CuroAuthProperties {

    /**
     * Type of authentication
     * supported by Curo: basic, oauth2
     */
    var type: String = "basic"

    /**
     * OAuth2 configuration
     */
    @NestedConfigurationProperty
    var oauth2: CuroOAuth2Properties = CuroOAuth2Properties()


    /**
     * BasicAuth configuration
     */
    @NestedConfigurationProperty
    var basic: CuroBasicAuthProperties = CuroBasicAuthProperties()

    /**
     * Custom routes which should be filtered by Curo.
     * Curo will inject the default engine for these endpoints.
     */
    var customRoutes: List<String> = arrayListOf()
}
