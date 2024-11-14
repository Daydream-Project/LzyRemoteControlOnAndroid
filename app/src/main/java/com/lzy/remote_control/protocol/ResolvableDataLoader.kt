package com.lzy.remote_control.protocol

import kotlin.RuntimeException
import kotlin.reflect.KClass

class ResolvableDataLoader {
    var resolvableDataTypeMap : HashMap<Int, KClass<*>> = HashMap()

    init {

        val resolvableDataSubTypes: MutableList<KClass<*>> = mutableListOf()

        resolvableDataSubTypes.add(NetworkPackage::class)
        resolvableDataSubTypes.add(GetCurrentActivityRequest::class)
        resolvableDataSubTypes.add(BroadcastRemoteControlServer::class)

        //Foreach subtype of ResolvableData, using data type id for key and KClass for value store in resolvableDataTypeMap
        for (classInfo in resolvableDataSubTypes) {
            val dataTypeInfo = getDataTypeInfo(classInfo) ?: throw RuntimeException("Can not get data type info for subtype of ResolvableData")

            if (resolvableDataTypeMap.containsKey(dataTypeInfo.id))
                throw RuntimeException("Multi subtype of ResolvableData using same data type id")

            resolvableDataTypeMap[dataTypeInfo.id] = classInfo
        }
    }
}