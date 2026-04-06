package com.google.ai.edge.gallery.customtasks.textplay

import com.google.ai.edge.gallery.customtasks.common.CustomTask
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/**
 * Hilt DI module that registers TextPlay as a custom task in the Gallery app.
 *
 * When integrated into the AI Edge Gallery project, add this module
 * to enable TextPlay alongside TinyGarden and MobileActions.
 */
@Module
@InstallIn(SingletonComponent::class)
object TextPlayTaskModule {

    @Provides
    @IntoSet
    fun provideTextPlayTask(): CustomTask = TextPlayTask
}
