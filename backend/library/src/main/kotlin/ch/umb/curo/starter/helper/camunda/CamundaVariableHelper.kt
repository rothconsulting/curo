package ch.umb.curo.starter.helper.camunda

import ch.umb.curo.starter.exception.CamundaVariableException
import ch.umb.curo.starter.helper.camunda.annotation.InitWithEmpty
import ch.umb.curo.starter.helper.camunda.annotation.InitWithNull
import com.fasterxml.jackson.databind.ObjectMapper
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.variable.impl.value.ObjectValueImpl
import org.camunda.spin.impl.json.jackson.JacksonJsonNode
import org.camunda.spin.plugin.variable.value.impl.JsonValueImpl
import java.lang.reflect.Modifier
import java.util.*
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

open class CamundaVariableHelper(private val delegateExecution: DelegateExecution) {

    /**
     * @param variableDefinition CamundaVariableDefinition<T> of variable to read
     * @param defaultValue provide value here if variable is null / not defined
     * @return result of Camunda variable matching the given definition (name/type)
     * @throws CamundaVariableException if variable not found and defaultValue is null / not provided
     * @throws CamundaVariableException if variable can not be casted in defined type
     */
    @JvmOverloads
    @Throws(CamundaVariableException::class)
    operator fun <T : Any> get(variableDefinition: CamundaVariableDefinition<T>, defaultValue: T? = null): T {
        return getOrNull(variableDefinition) ?: defaultValue ?: throw CamundaVariableException("Camunda variable '" + variableDefinition.value + "' is not defined")
    }

    /**
     * @param variableListDefinition CamundaVariableListDefinition<T> of variable list to be read
     * @param defaultValue provide value here if variable is null / not defined
     * @return result of Camunda variable matching the given definition (name/type)
     * @throws CamundaVariableException if variable not found and defaultValue is null / not provided
     * @throws CamundaVariableException if variable can not be casted in defined type
     */

    @JvmOverloads
    @Throws(CamundaVariableException::class)
    operator fun <T : Any> get(variableListDefinition: CamundaVariableListDefinition<T>, defaultValue: List<T>? = null): List<T?> {
        return getOrNull(variableListDefinition) ?: defaultValue ?: throw CamundaVariableException("Camunda variable '" + variableListDefinition.value + "' is not defined")
    }

    /**
     * @param variableDefinition CamundaVariableDefinition<T> of variable to read
     * @return result of Camunda variable matching the given definition (name/type) or null if variable is null / not defined
     * @throws CamundaVariableException if variable can not be casted in defined type
     */
    @Throws(CamundaVariableException::class)
    fun <T : Any> getOrNull(variableDefinition: CamundaVariableDefinition<T>): T? {
        return try {
            val content: Any? = delegateExecution.getVariable(variableDefinition.value)
            try {
                variableDefinition.type.cast(content)
            } catch (e: ClassCastException) {
                ObjectMapper().readValue((content as JacksonJsonNode).toString(), variableDefinition.type)
            }
        } catch (e: Exception) {
            throw CamundaVariableException("Could not cast variable '" + variableDefinition.value + "' to type " + variableDefinition.type.toString(), e)
        }
    }

    /**
     * @param variableDefinition CamundaVariableDefinition<T> of variable to read
     * @param exception exception thrown if variable is not present or null
     * @return result of Camunda variable matching the given definition (name/type) or null if variable is null / not defined
     * @throws CamundaVariableException if variable can not be casted in defined type
     */
    @Throws(CamundaVariableException::class)
    fun <T : Any> getOrThrow(variableDefinition: CamundaVariableDefinition<T>, exception: Throwable): T? {
        return try {
            val content: Any = delegateExecution.getVariable(variableDefinition.value) ?: throw exception
            try {
                variableDefinition.type.cast(content)
            } catch (e: ClassCastException) {
                ObjectMapper().readValue((content as JacksonJsonNode).toString(), variableDefinition.type)
            }
        } catch (e: Exception) {
            throw if (e.cause == exception) {
                e
            } else {
                CamundaVariableException("Could not cast variable '" + variableDefinition.value + "' to type " + variableDefinition.type.toString(), e)
            }
        }
    }

    /**
     * @param variableListDefinition name of variable to read
     * @return result of Camunda variable matching the given definition (name/type) or null if variable is null / not defined
     * @throws CamundaVariableException if variable can not be casted in defined type
     */
    @Throws(CamundaVariableException::class)
    fun <T : Any> getOrNull(variableListDefinition: CamundaVariableListDefinition<T>): List<T?>? {
        try {
            val content: Any? = delegateExecution.getVariable(variableListDefinition.value)
            if (content != null) {
                val list: List<*> = content as List<*>

                list.filterNotNull().forEach {
                    try {
                        variableListDefinition.type.cast(it)
                    } catch (e: ClassCastException) {
                        ObjectMapper().readValue((it as JacksonJsonNode).toString(), variableListDefinition.type)
                    }
                }

                @Suppress("UNCHECKED_CAST")
                return list as List<T?>
            } else {
                return null
            }
        } catch (e: Exception) {
            throw CamundaVariableException("Could not cast variable '" + variableListDefinition.value + "' to type " + variableListDefinition.type.toString(), e)
        }
    }


    /**
     * @param variableListDefinition name of variable to read
     * @return result of Camunda variable matching the given definition (name/type) or null if variable is null / not defined
     * @throws CamundaVariableException if variable can not be casted in defined type
     */
    @Throws(CamundaVariableException::class)
    fun <T : Any> getOrThrow(variableListDefinition: CamundaVariableListDefinition<T>, exception: Throwable): List<T?>? {
        try {
            val content: Any = delegateExecution.getVariable(variableListDefinition.value) ?: throw exception
            val list: List<*> = content as List<*>

            list.filterNotNull().forEach {
                try {
                    variableListDefinition.type.cast(it)
                } catch (e: ClassCastException) {
                    ObjectMapper().readValue((it as JacksonJsonNode).toString(), variableListDefinition.type)
                }
            }

            @Suppress("UNCHECKED_CAST")
            return list as List<T?>
        } catch (e: Exception) {
            throw if (e.cause == exception) {
                e
            } else {
                CamundaVariableException("Could not cast variable '" + variableListDefinition.value + "' to type " + variableListDefinition.type.toString(), e)
            }
        }
    }

    /**
     * @param variableListDefinition name of variable to read
     * @return result of Camunda variable matching the given definition (name/type) or an Empty List<T> if variable is null / not defined
     * @throws CamundaVariableException if variable can not be casted in defined type
     */
    @Throws(CamundaVariableException::class)
    fun <T : Any> getOrEmpty(variableListDefinition: CamundaVariableListDefinition<T>): List<T?> {
        return getOrNull(variableListDefinition) ?: ArrayList<T>()
    }

    fun getAllVariables(camundaVariables: Class<out Any>): List<CamundaVariableDefinitionConvention<out Any>> {
        return camundaVariables.declaredFields
            .filter { Modifier.isStatic(it.modifiers) }
            .map { it.isAccessible = true; it.get(null) }
            .filterIsInstance<CamundaVariableDefinitionConvention<Any>>()
    }

    fun getAllMap(camundaVariables: Class<out Any>): Map<String, Any> {
        return getAllMapWithNullables(camundaVariables).filterValues { it != null }.mapValues { it.value as Any }
    }

    fun getAllMapWithNullables(camundaVariables: Class<out Any>): Map<String, Any?> {
        return getAllVariables(camundaVariables).associate {
            Pair(
                it.value,
                when (it) {
                    is CamundaVariable -> getOrNull(it)
                    is CamundaVariableList -> getOrNull(it)
                    else -> null
                })
        }
    }

    operator fun <T : Any> set(variableName: CamundaVariableDefinition<T>, value: T?) {
        delegateExecution.setVariable(variableName.value, value)
    }

    operator fun <T : Any> set(variableName: CamundaVariableListDefinition<T>, value: List<T?>) {
        delegateExecution.setVariable(variableName.value, value)
    }

    fun initVariables(variableClass: Any) {
        val doAllEmpty = variableClass.javaClass.isAnnotationPresent(InitWithEmpty::class.java)
        val doAllNull = variableClass.javaClass.isAnnotationPresent(InitWithNull::class.java)
        variableClass.javaClass.declaredFields
            .filter { it.isAnnotationPresent(InitWithNull::class.java) || it.isAnnotationPresent(InitWithEmpty::class.java) || doAllEmpty || doAllNull }
            .filter { it.name != "Companion" }
            .forEach {
                it.isAccessible = true
                val definition = it.get(variableClass) as CamundaVariableDefinition<*>
                val emptyInit = if (it.isAnnotationPresent(InitWithEmpty::class.java)) {
                    true
                } else {
                    doAllEmpty && !it.isAnnotationPresent(InitWithNull::class.java)
                }

                when {
                    (!it.type.isAssignableFrom(JvmType.Object::class.java)) && emptyInit -> {
                        val value = if (it.type.isAssignableFrom(Boolean::class.java)) { //Set Boolean to false
                            false
                        } else {
                            definition.type.getConstructor().newInstance()
                        }
                        delegateExecution.setVariable(definition.value, value)
                    }
                    (!it.type.isAssignableFrom(JvmType.Object::class.java)) && !emptyInit -> delegateExecution.setVariable(definition.value, null)
                    (it.type == JacksonJsonNode::class.java) && emptyInit -> delegateExecution.setVariable(definition.value,
                                                                                                           JsonValueImpl(ObjectMapper().writeValueAsString(definition.type.getConstructor()
                                                                                                                                                               .newInstance()),
                                                                                                                         "application/json"))
                    (it.type == JacksonJsonNode::class.java) && !emptyInit -> delegateExecution.setVariable(definition.value, JsonValueImpl("", "application/json"))
                    else -> {
                        if (emptyInit) {
                            delegateExecution.setVariable(definition.value,
                                                          ObjectValueImpl(definition.type.getConstructor().newInstance(),
                                                                          ObjectMapper().writeValueAsString(definition.type.getConstructor().newInstance()),
                                                                          "application/json",
                                                                          definition.type.canonicalName,
                                                                          true))
                        } else {
                            delegateExecution.setVariable(definition.value, ObjectValueImpl(null, "", "application/json", definition.type.canonicalName, true))
                        }
                    }
                }
            }
    }

    companion object {
        fun initVariables(variableClass: Any, delegateExecution: DelegateExecution) {
            CamundaVariableHelper(delegateExecution).initVariables(variableClass)
        }
    }

}

fun DelegateExecution.variableHelper(): CamundaVariableHelper {
    return CamundaVariableHelper(this)
}