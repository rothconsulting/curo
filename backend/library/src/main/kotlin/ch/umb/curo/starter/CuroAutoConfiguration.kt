package ch.umb.curo.starter

import ch.umb.curo.starter.auth.CuroBasicAuthAuthentication
import ch.umb.curo.starter.auth.CuroOAuth2Authentication
import ch.umb.curo.starter.property.CuroProperties
import org.camunda.bpm.engine.ManagementService
import org.camunda.bpm.engine.rest.security.auth.AuthenticationProvider
import org.camunda.bpm.engine.rest.util.EngineUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.event.ApplicationStartedEvent
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.event.EventListener

@EnableConfigurationProperties(CuroProperties::class)
@Configuration
@ComponentScan(basePackages = ["ch.umb.curo.starter.*"])
open class CuroAutoConfiguration {

    @Autowired
    lateinit var properties: CuroProperties

    @Autowired
    lateinit var context: ConfigurableApplicationContext

    private val logger = LoggerFactory.getLogger("ch.umb.curo.starter.CuroAutoConfiguration")

    @Bean
    @ConditionalOnMissingBean
    open fun defaultAuthenticationProvider(): AuthenticationProvider {
        return when (properties.auth.type) {
            "basic" -> {
                CuroBasicAuthAuthentication()
            }
            "oauth2" -> {
                CuroOAuth2Authentication()
            }
            else -> CuroBasicAuthAuthentication()
        }
    }

    @EventListener(ApplicationStartedEvent::class)
    fun setTelemetry() {
        if (properties.camundaTelemetry != null) {
            logger.info("CURO: Set camunda telemetry to: ${properties.camundaTelemetry}")
            val engine = EngineUtil.lookupProcessEngine(null)
            val managementService: ManagementService = engine.managementService
            managementService.toggleTelemetry(properties.camundaTelemetry!!)
        }
    }

    @EventListener(ApplicationStartedEvent::class)
    fun setCamundaUserIdPattern() {
        val engine = EngineUtil.lookupProcessEngine(null)
        if (properties.camundaUserIdPattern != null) {
            logger.info("CURO: Set userResourceWhitelistPattern to: ${properties.camundaUserIdPattern}")
            engine.processEngineConfiguration.userResourceWhitelistPattern = properties.camundaUserIdPattern
        }

        //show warning if userIdClaim is email or mail and userResourceWhitelistPattern is default
        val isDefaultPattern = engine.processEngineConfiguration.userResourceWhitelistPattern == null ||
                engine.processEngineConfiguration.userResourceWhitelistPattern == "[a-zA-Z0-9]+|camunda-admin"
        if (properties.auth.type == "oauth2" && properties.auth.oauth2.userIdClaim in arrayListOf("mail", "email", "preferred_username") && isDefaultPattern) {
            logger.warn("CURO: email seems to be used as userIdClaim but camundaUserIdPattern is no set. This may result in Curo not be able to authenticate users as the camunda default pattern does not allow email addresses.")
        }
    }

    @EventListener(ApplicationStartedEvent::class)
    fun setCamundaGroupIdPattern() {
        val engine = EngineUtil.lookupProcessEngine(null)
        if (properties.camundaGroupIdPattern != null) {
            logger.info("CURO: Set groupResourceWhitelistPattern to: ${properties.camundaGroupIdPattern}")
            engine.processEngineConfiguration.groupResourceWhitelistPattern = properties.camundaGroupIdPattern
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun setStringContext() {
        SpringContext.applicationContext = context
    }

    @EventListener(ApplicationStartedEvent::class)
    fun createInitialGroups() {
        if (properties.initialGroups != null && properties.initialGroups!!.isNotEmpty()) {
            logger.info("CURO: Create initial groups: " + properties.initialGroups!!.joinToString(", ") { it })
            val engine = EngineUtil.lookupProcessEngine(null)
            properties.initialGroups!!.forEach {
                if (engine.identityService.createGroupQuery().groupId(it).count() == 0L) {
                    val group = engine.identityService.newGroup(it)
                    group.name = it
                    engine.identityService.saveGroup(group)
                }
            }
        }
    }

    @EventListener(ApplicationReadyEvent::class)
    fun createInitialUsers() {
        if (properties.initialUsers != null && properties.initialUsers!!.isNotEmpty()) {
            logger.info("CURO: Create initial users: " + properties.initialUsers!!.joinToString(", ") { it.id })
            val engine = EngineUtil.lookupProcessEngine(null)
            properties.initialUsers!!.forEach { userProperty ->
                if (engine.identityService.createUserQuery().userId(userProperty.id).count() == 0L) {
                    val user = engine.identityService.newUser(userProperty.id)
                    user.email = userProperty.email
                    user.firstName = userProperty.firstname
                    user.lastName = userProperty.lastname
                    if (userProperty.password != null) {
                        user.password = userProperty.password
                    }
                    engine.identityService.saveUser(user)
                }

                if (userProperty.groups != null && userProperty.groups!!.isNotEmpty()) {
                    userProperty.groups!!.forEach {
                        if(engine.identityService.createGroupQuery().groupId(it).count() != 0L){
                            engine.identityService.createMembership(userProperty.id, it)
                        }else{
                            logger.warn("CURO: Group '$it' does not exist and can therefore not be assigned to the user '${userProperty.id}'")
                        }
                    }
                }

            }
        }
    }
}
