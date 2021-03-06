package ch.umb.curo.starter.controller

import ch.umb.curo.starter.models.response.CuroUser
import ch.umb.curo.starter.models.response.CuroUserResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "user", description = "Curo User API")
@RequestMapping("/curo-api/users")
interface UserController {

    @Operation(
        summary = "Load list of users",
        operationId = "getUsers",
        description = "",
        security = [SecurityRequirement(name = "CuroBasic")]
    )
    @GetMapping("", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getUsers(
        @Parameter(
            description = "Only select users where the email matches the given parameter. The syntax is that of SQL, eg. %test%.",
            required = false
        )
        @RequestParam("emailLike", required = false, defaultValue = "")
        emailLike: String = "%",

        @Parameter(
            description = "Only select users where the last name matches the given parameter. The syntax is that of SQL, eg. %test%.",
            required = false
        )
        @RequestParam("lastnameLike", required = false, defaultValue = "")
        lastnameLike: String = "%",

        @Parameter(
            description = "Only select users where the first name matches the given parameter. The syntax is that of SQL, eg. %test%.",
            required = false
        )
        @RequestParam("firstnameLike", required = false, defaultValue = "")
        firstnameLike: String = "%",

        @Parameter(
            description = "Only select users that belong to one of the given groups.",
            required = false
        )
        @RequestParam("memberOfGroup", required = false, defaultValue = "")
        memberOfGroup: ArrayList<String> = arrayListOf(),

        @Parameter(
            description = "Define which fields should be returned. If not present, all fields are returned",
            required = false
        )
        @RequestParam("attributes", required = false, defaultValue = "")
        attributes: ArrayList<String> = arrayListOf()
    ): CuroUserResponse

    @Operation(
        summary = "Load user by id",
        operationId = "getUser",
        description = "",
        security = [SecurityRequirement(name = "CuroBasic")]
    )
    @GetMapping("/{id}", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getUser(
        @Parameter(description = "ID of a user", required = true)
        @PathVariable("id", required = true)
        id: String
    ): CuroUser

    @Operation(
        summary = "Load current session user",
        operationId = "getCurrentUsers",
        description = "",
        security = [SecurityRequirement(name = "CuroBasic")]
    )
    @GetMapping("/me", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getCurrentUser(): CuroUser
}
