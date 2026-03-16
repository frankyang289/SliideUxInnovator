package com.sliide.usermanagement

import platform.Foundation.NSBundle

actual fun goRestToken(): String {
    return NSBundle.mainBundle
        .objectForInfoDictionaryKey("GOREST_TOKEN") as? String ?: ""
}