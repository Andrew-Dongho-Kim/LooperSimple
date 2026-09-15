allprojects {
    afterEvaluate { project ->
        if (project.name == 'app') {
            android.defaultConfig.applicationId = 'com.pnd.android.loop.notificationpreview'
            android.defaultConfig.testInstrumentationRunner = 'androidx.test.runner.AndroidJUnitRunner'
            android.buildTypes.debug.minifyEnabled = false
        }
    }
}