package ch.umb.curo.starter.interceptor

import com.auth0.jwt.interfaces.DecodedJWT
import javax.servlet.http.HttpServletRequest

/**
 * AuthSuccessInterceptors are called on the /curo-api/auth/success endpoint.
 */
interface Oauth2SuccessInterceptor {

    val name: String

    /**
     * Defines if this intercept is called asynchronous
     */
    val async: Boolean

    /**
     * Execution is ordered from lowest to highest
     */
    val order: Int

    /**
     * On intercept method
     *
     * @param jwt Decoded JWT
     * @param jwtRaw Raw JWT extracted from the request
     * @param request Request which got intercepted
     */
    fun onIntercept(jwt: DecodedJWT, jwtRaw: String, request: HttpServletRequest): Boolean
}
