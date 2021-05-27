package com.mmunoz.core.di.holder

/**
 * Utility class that holds the instance of application wide components
 * so it can be easily accessible a cross the different features
 */
object ComponentHolder {

    /**
     * Set that holds the components that want be shareable a cross the application,
     * the main case would be the ApplicationComponent
     */
    val components = mutableSetOf<Any>()

    /**
     * Method that provides the instance of a component filtering by the requested class
     * it should be noted that the application component must implement the interface for the requested component
     * in order to work
     */
    inline fun <reified T> component(): T = components
        .filterIsInstance<T>()
        .single()
}