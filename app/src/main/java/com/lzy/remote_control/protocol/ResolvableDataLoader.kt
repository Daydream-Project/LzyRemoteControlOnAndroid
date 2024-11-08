package com.lzy.remote_control.protocol

import kotlin.RuntimeException
import kotlin.reflect.KClass
import org.reflections.Reflections

class ResolvableDataLoader {

    companion object {
        private fun getClassesInPackage(packageName: String): List<KClass<*>> {
            val reflections = Reflections()
            reflections
        }
    }

    var resolvableDataTypes : HashMap<Int, KClass<*>> = HashMap()

    init {
        val packageInfo = ResolvableDataLoader::class.java.`package`
            ?: throw RuntimeException("Can not get current package.")

        val packageClasses = getClassesInPackage(packageInfo.name)

        for (classInfo in packageClasses)
        {
            if (classInfo.supertypes.find { typeInfo -> typeInfo == ResolvableData::class } != null) {
                val constructorInfo = classInfo.java.constructors.find { c -> c.typeParameters.isEmpty() }
                if (constructorInfo != null)
                {
                    val temp = constructorInfo.newInstance() as ResolvableData

                    if (resolvableDataTypes.containsKey(temp.getDataType()))
                        throw RuntimeException("multi ResolvableData type has same type id")

                    resolvableDataTypes.put(temp.getDataType(), classInfo)
                }
            }
            else {
                throw RuntimeException("ResolvableLoader can not load ResolvableData type without constructor width on parameter.")
            }
        }

    }

}