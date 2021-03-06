package ch.umb.curo.starter.interceptor

import ch.umb.curo.starter.auth.CamundaAuthUtil
import ch.umb.curo.starter.exception.ApiException
import ch.umb.curo.starter.property.CuroProperties
import ch.umb.curo.starter.util.JWTUtil
import com.auth0.jwt.interfaces.DecodedJWT
import org.camunda.bpm.engine.IdentityService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import javax.servlet.http.HttpServletRequest

/**
 * Default user federation interceptor which can be used together with the auth type OAuth2.
 * Settings for this implementation can be configured over properties (`curo.auth.oauth2.user-federation`)
 */
@Component
@ConditionalOnProperty(prefix = "curo", value = ["auth.oauth2.user-federation.enabled"], havingValue = "true")
class CamundaUserFederationInterceptor(
    private val properties: CuroProperties,
    private val identityService: IdentityService
) : Oauth2SuccessInterceptor {
    override val name: String = "Camunda:UserFederation"
    override val async: Boolean = false
    override val order: Int = 10

    private val logger = LoggerFactory.getLogger(CamundaUserFederationInterceptor::class.java)

    override fun onIntercept(jwt: DecodedJWT, jwtRaw: String, request: HttpServletRequest): Boolean {
        if (properties.auth.type != "oauth2" || !properties.auth.oauth2.userFederation.enabled) {
            return false
        }

        return CamundaAuthUtil.runWithoutAuthentication({ processUserFederation(jwt) }, identityService)
    }

    private fun processUserFederation(jwt: DecodedJWT): Boolean {
        val userId = jwt.getClaim(properties.auth.oauth2.userIdClaim).asString()
            ?: throw ApiException.invalidArgument400(arrayListOf("The Claim '${properties.auth.oauth2.userIdClaim}' does not exist."))
                .printException(properties.printStacktrace)

        //Check if user exists
        val user = identityService.createUserQuery().userId(userId).singleResult()
        if (!properties.auth.oauth2.userFederation.createNonExistingUsers && user == null) {
            throw ApiException.curoErrorCode(ApiException.CuroErrorCode.USER_NOT_FOUND)
                .printException(properties.printStacktrace)
        }

        if (user == null) {
            //Create user
            val newUser = identityService.newUser(userId)
            newUser.email = jwt.getClaim(properties.auth.oauth2.userFederation.emailClaim).asString()
                ?: throw ApiException.invalidArgument400(arrayListOf("The Claim '${properties.auth.oauth2.userFederation.emailClaim}' does not exist."))
                    .printException(properties.printStacktrace)
            newUser.firstName = jwt.getClaim(properties.auth.oauth2.userFederation.firstNameClaim).asString()
                ?: throw ApiException.invalidArgument400(arrayListOf("The Claim '${properties.auth.oauth2.userFederation.firstNameClaim}' does not exist."))
                    .printException(properties.printStacktrace)
            newUser.lastName = jwt.getClaim(properties.auth.oauth2.userFederation.lastNameClaim).asString()
                ?: throw ApiException.invalidArgument400(arrayListOf("The Claim '${properties.auth.oauth2.userFederation.lastNameClaim}' does not exist."))
                    .printException(properties.printStacktrace)
            identityService.saveUser(newUser)
            logger.debug("CURO: User '$userId' got created")
        }

        //Try to unlock user
        identityService.unlockUser(userId)

        //Update groups
        val jwtGroups = JWTUtil.getRoles(jwt, properties)

        val camundaGroups = identityService.createGroupQuery().list().map { it.id }
        val userGroups = identityService.createGroupQuery().groupMember(userId).list().map { it.id }

        var nonExistingGroups = jwtGroups.filterNot { it in camundaGroups }
        if (properties.auth.oauth2.userFederation.createNonExistingGroups && nonExistingGroups.isNotEmpty()) {
            nonExistingGroups.forEach {
                val newGroup = identityService.newGroup(it)
                identityService.saveGroup(newGroup)
                logger.debug("CURO: Create group '$it' because it does not exist")
            }
            nonExistingGroups = arrayListOf() // reset non existing groups
        } else if (properties.auth.oauth2.userFederation.printNonExistingGroups && nonExistingGroups.isNotEmpty()) {
            logger.info("CURO: Following jwt groups do not exist on camunda:\n${nonExistingGroups.joinToString("\n") { "- $it" }}")
        }

        val groupsToAdd = jwtGroups.filterNot { it in nonExistingGroups }.filterNot { it in userGroups }.distinct()
        var groupsToRemove = userGroups.filterNot { it in jwtGroups }.distinct()

        if (groupsToRemove.contains("camunda-admin") && !properties.auth.oauth2.userFederation.revokeCamundaAdminGroup) {
            groupsToRemove = groupsToRemove.filter { it != "camunda-admin" }
        }

        groupsToRemove.forEach {
            identityService.deleteMembership(userId, it)
            logger.debug("CURO: -> User '$userId' got removed from '$it'")
        }

        groupsToAdd.forEach {
            identityService.createMembership(userId, it)
            logger.debug("CURO: -> User '$userId' got added to '$it'")
        }

        return true
    }
}
