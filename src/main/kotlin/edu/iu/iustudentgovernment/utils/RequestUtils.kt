package edu.iu.iustudentgovernment.utils

import spark.Request

fun Request.setLastUrl() = session().attribute("lastUrl", url() + "?" + (queryString() ?: "didyoufindthis=easteregg"))
